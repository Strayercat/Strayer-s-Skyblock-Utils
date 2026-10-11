package com.skyblockutils.features.voice;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import net.minecraft.client.Minecraft;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusDecoder;
import io.github.jaredmdobson.concentus.OpusEncoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

final class VoiceAudio {
    static final String DEFAULT_DEVICE = "Default";

    private static final int SAMPLE_RATE = 48_000;
    private static final int FRAME = 960;
    private static final int FRAME_BYTES = FRAME * 2;
    private static final int MIC_BUFFER_FRAMES = 10;
    private static final int SPEAKER_BUFFER_FRAMES = 4;
    private static final int BITRATE = 32_000;
    private static final int MAX_OPUS = 1_000;
    private static final int LEVEL_HOLD_FRAMES = 25;
    private static final int LOOKBACK_FRAMES = 3;
    private static final int SMART_CONFIRM_FRAMES = 2;
    private static final int SMART_HOLD_FRAMES = 25;
    private static final double SMART_FLOOR_DB = -60;
    private static final double VOICING_FLOOR_DB = -60;
    private static final double SMART_KEEP_VOICING = 0.45;
    private static final float SMART_MIN_SPEECH = 0.5f;
    private static final float SMART_KEEP_SPEECH = 0.3f;
    private static final float DUCK_SPEECH = 0.3f;
    private static final double DUCK_VOICING = 0.5;
    private static final float DUCK_ONSET_SPEECH = 0.1f;
    private static final int DUCK_HOLD_FRAMES = 4;
    private static final float DUCK_GAIN = 0.05f;
    private static final int LOOPBACK_MAX_FRAMES = 5;
    private static final float BASE_OUTPUT_GAIN = 3.0f;
    private static final float LIMITER_KNEE = 0.8f;
    private static final long LEVEL_LOG_MS = 5_000;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private volatile boolean running = false;
    private TargetDataLine mic;
    private SourceDataLine speaker;
    volatile boolean transmitting = false;
    private final ConcurrentLinkedQueue<short[]> loopback = new ConcurrentLinkedQueue<>();

    boolean isRunning() {
        return running;
    }

    void start(String inputDevice, String outputDevice) {
        stop();
        running = true;

        try {
            mic = openLine(TargetDataLine.class, inputDevice);
            mic.open(FORMAT, FRAME_BYTES * MIC_BUFFER_FRAMES);
            mic.start();
            VoiceChat.LOGGER.info("Opened microphone {}", inputDevice == null || inputDevice.isBlank() ? DEFAULT_DEVICE : inputDevice);
            TargetDataLine line = mic;
            startThread("SSU Voice Capture", () -> captureLoop(line));
        } catch (Exception e) {
            mic = null;
            VoiceChat.LOGGER.warn("Couldn't open microphone", e);
            notify("Couldn't open microphone: " + e.getMessage());
        }

        try {
            speaker = openLine(SourceDataLine.class, outputDevice);
            speaker.open(FORMAT, FRAME_BYTES * SPEAKER_BUFFER_FRAMES);
            speaker.start();
            VoiceChat.LOGGER.info("Opened speaker {}", outputDevice == null || outputDevice.isBlank() ? DEFAULT_DEVICE : outputDevice);
            SourceDataLine line = speaker;
            startThread("SSU Voice Playback", () -> playbackLoop(line));
        } catch (Exception e) {
            speaker = null;
            VoiceChat.LOGGER.warn("Couldn't open speaker", e);
            notify("Couldn't open speaker: " + e.getMessage());
        }
    }

    void stop() {
        running = false;
        transmitting = false;
        loopback.clear();
        if (mic != null) {
            mic.stop();
            mic.close();
            mic = null;
        }
        if (speaker != null) {
            speaker.stop();
            speaker.close();
            speaker = null;
        }
    }

    static String[] inputDevices() {
        return devices(TargetDataLine.class);
    }

    static String[] outputDevices() {
        return devices(SourceDataLine.class);
    }

    private void captureLoop(TargetDataLine line) {
        OpusEncoder encoder = createEncoder();
        if (encoder == null) return;

        VoiceDenoiser denoiser = new VoiceDenoiser();
        VoicingDetector voicing = new VoicingDetector(FRAME);
        byte[] raw = new byte[FRAME_BYTES];
        short[] pcm = new short[FRAME];
        ArrayDeque<short[]> lookback = new ArrayDeque<>();
        byte[] opus = new byte[MAX_OPUS];
        byte[] packet = new byte[4 + MAX_OPUS];
        int seq = 0;
        int levelHold = 0;
        int smartHold = 0;
        int smartStreak = 0;
        boolean smartOpen = false;
        boolean wasOpen = false;
        long lastLevelLog = 0;
        int duckHold = 0;
        float duckGain = 1f;
        short[] out = new short[FRAME];

        while (running && line.isOpen()) {
            int read = line.read(raw, 0, FRAME_BYTES);
            if (read < FRAME_BYTES) {
                if (!line.isOpen()) break;
                continue;
            }

            float gain = ModConfig.INSTANCE.voiceInputVolume / 100f;
            for (int i = 0; i < FRAME; i++) {
                int sample = (short) ((raw[2 * i] & 0xFF) | (raw[2 * i + 1] << 8));
                pcm[i] = (short) clamp(Math.round(sample * gain));
            }

            ModConfig.VoiceActivation mode = ModConfig.INSTANCE.voiceActivation;
            boolean needsSpeech = mode == ModConfig.VoiceActivation.SMART;
            boolean ducking = ModConfig.INSTANCE.voiceKeyboardSuppression;
            float speech = ModConfig.INSTANCE.voiceNoiseSuppression || needsSpeech || ducking
                    ? denoiser.process(pcm, ModConfig.INSTANCE.voiceNoiseSuppression)
                    : -1;
            double peakDb = peakDb(pcm);
            double voiced = voicing.process(pcm);
            if (peakDb < VOICING_FLOOR_DB) voiced = 0;

            boolean levelOpen = false;
            if (peakDb >= ModConfig.INSTANCE.voiceActivationLevel) levelHold = LEVEL_HOLD_FRAMES;
            if (levelHold > 0) {
                levelOpen = true;
                levelHold--;
            }

            if (needsSpeech) {
                double voicedAt = 0.8 - ModConfig.INSTANCE.voiceSmartSensitivity / 100.0 * 0.4;
                boolean audible = peakDb >= SMART_FLOOR_DB;
                if (!smartOpen) {
                    boolean speechFrame = audible && voiced >= voicedAt && (speech < 0 || speech >= SMART_MIN_SPEECH);
                    smartStreak = speechFrame ? smartStreak + 1 : 0;
                    if (smartStreak >= SMART_CONFIRM_FRAMES) {
                        smartOpen = true;
                        smartHold = SMART_HOLD_FRAMES;
                    }
                } else if (audible && (speech < 0 || speech >= SMART_KEEP_SPEECH) && voiced >= Math.max(SMART_KEEP_VOICING, voicedAt - 0.15)) {
                    smartHold = SMART_HOLD_FRAMES;
                } else if (--smartHold <= 0) {
                    smartOpen = false;
                    smartStreak = 0;
                }
            }

            boolean open = switch (mode) {
                case PUSH_TO_TALK -> VoiceChat.pttDown;
                case VOICE_ACTIVATION -> levelOpen;
                case SMART -> smartOpen;
            };
            if (VoiceChat.muted || VoiceChat.deafened) open = false;

            boolean speechy = speech < 0 || speech >= DUCK_SPEECH || (voiced >= DUCK_VOICING && speech >= DUCK_ONSET_SPEECH);
            if (speechy) duckHold = DUCK_HOLD_FRAMES;
            else if (duckHold > 0) duckHold--;
            float duckTarget = !ducking || speech < 0 || duckHold > 0 ? 1f : DUCK_GAIN;
            for (int i = 0; i < FRAME; i++) {
                float g = duckGain + (duckTarget - duckGain) * (i + 1) / FRAME;
                out[i] = (short) Math.round(pcm[i] * g);
            }
            duckGain = duckTarget;

            long now = System.currentTimeMillis();
            if (open && now - lastLevelLog >= LEVEL_LOG_MS) {
                lastLevelLog = now;
                VoiceChat.LOGGER.info("[{}] mic peak {} dBFS, speech {}, voicing {}, ducked {}", mode, String.format("%.1f", peakDb),
                        String.format("%.2f", speech), String.format("%.2f", voiced), duckTarget < 1f);
            }

            if (open && !wasOpen && mode != ModConfig.VoiceActivation.PUSH_TO_TALK) {
                for (short[] frame : lookback) seq = sendFrame(encoder, frame, opus, packet, seq);
            }

            if (open) {
                seq = sendFrame(encoder, out, opus, packet, seq);
            } else if (wasOpen) {
                VoiceNetwork.putInt(packet, 0, seq++);
                VoiceChat.broadcastAudio(packet, 4);
                VoiceChat.broadcastAudio(packet, 4);
                OpusEncoder fresh = createEncoder();
                if (fresh != null) encoder = fresh;
            }

            transmitting = open;
            wasOpen = open;
            if (open) {
                lookback.clear();
            } else {
                lookback.addLast(pcm.clone());
                while (lookback.size() > LOOKBACK_FRAMES) lookback.pollFirst();
            }
        }

        transmitting = false;
        denoiser.close();
    }

    private int sendFrame(OpusEncoder encoder, short[] pcm, byte[] opus, byte[] packet, int seq) {
        if (ModConfig.INSTANCE.voiceMicTest) {
            loopback.add(pcm.clone());
            while (loopback.size() > LOOPBACK_MAX_FRAMES) loopback.poll();
        }

        int length;
        try {
            length = encoder.encode(pcm, 0, FRAME, opus, 0, MAX_OPUS);
        } catch (Exception e) {
            return seq;
        }
        if (length <= 0) return seq;

        VoiceNetwork.putInt(packet, 0, seq);
        System.arraycopy(opus, 0, packet, 4, length);
        VoiceChat.broadcastAudio(packet, 4 + length);
        return seq + 1;
    }

    private OpusEncoder createEncoder() {
        try {
            OpusEncoder encoder = new OpusEncoder(SAMPLE_RATE, 1, OpusApplication.OPUS_APPLICATION_VOIP);
            encoder.setBitrate(BITRATE);
            return encoder;
        } catch (Exception e) {
            VoiceChat.LOGGER.warn("Couldn't create Opus encoder", e);
            notify("Couldn't create Opus encoder: " + e.getMessage());
            return null;
        }
    }

    private static double peakDb(short[] pcm) {
        int peak = 0;
        for (short sample : pcm) peak = Math.max(peak, Math.abs((int) sample));
        return peak == 0 ? -127 : 20 * Math.log10(peak / 32768.0);
    }

    private void playbackLoop(SourceDataLine line) {
        byte[] out = new byte[FRAME_BYTES];
        short[] pcm = new short[FRAME];
        int[] mix = new int[FRAME];

        while (running && line.isOpen()) {
            Arrays.fill(mix, 0);
            boolean deaf = VoiceChat.deafened;

            for (VoicePeer peer : VoiceChat.peers()) {
                if (!peer.isLive()) {
                    peer.jitter.clear();
                    continue;
                }

                if (peer.jitter.takeReset()) peer.decoder = null;
                byte[] frame = peer.jitter.poll();
                if (frame == null) continue;

                int decoded;
                try {
                    if (peer.decoder == null) peer.decoder = new OpusDecoder(SAMPLE_RATE, 1);
                    decoded = frame == JitterBuffer.LOST
                            ? peer.decoder.decode(null, 0, 0, pcm, 0, FRAME, false)
                            : peer.decoder.decode(frame, 0, frame.length, pcm, 0, FRAME, false);
                } catch (Exception e) {
                    continue;
                }

                if (deaf) continue;
                for (int i = 0; i < decoded && i < FRAME; i++) mix[i] += pcm[i];
            }

            short[] self = loopback.poll();
            if (self != null && ModConfig.INSTANCE.voiceMicTest && !deaf) {
                for (int i = 0; i < FRAME; i++) mix[i] += self[i];
            }

            float volume = BASE_OUTPUT_GAIN * ModConfig.INSTANCE.voiceOutputVolume / 100f;
            for (int i = 0; i < FRAME; i++) {
                int sample = limit(mix[i] * volume);
                out[2 * i] = (byte) sample;
                out[2 * i + 1] = (byte) (sample >> 8);
            }
            line.write(out, 0, FRAME_BYTES);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Line> T openLine(Class<T> type, String device) throws Exception {
        DataLine.Info info = new DataLine.Info(type, FORMAT);
        Mixer.Info mixer = findMixer(type, device);
        return (T) (mixer == null ? AudioSystem.getLine(info) : AudioSystem.getMixer(mixer).getLine(info));
    }

    private static Mixer.Info findMixer(Class<?> type, String device) {
        if (device == null || device.isBlank() || device.equals(DEFAULT_DEVICE)) return null;
        DataLine.Info info = new DataLine.Info(type, FORMAT);
        for (Mixer.Info mixer : AudioSystem.getMixerInfo()) {
            if (mixer.getName().equals(device) && AudioSystem.getMixer(mixer).isLineSupported(info)) return mixer;
        }
        return null;
    }

    private static String[] devices(Class<?> type) {
        DataLine.Info info = new DataLine.Info(type, FORMAT);
        List<String> names = new ArrayList<>();
        names.add(DEFAULT_DEVICE);
        for (Mixer.Info mixer : AudioSystem.getMixerInfo()) {
            try {
                if (AudioSystem.getMixer(mixer).isLineSupported(info) && !names.contains(mixer.getName())) names.add(mixer.getName());
            } catch (Exception ignored) {
            }
        }
        return names.toArray(String[]::new);
    }

    private static int limit(float sample) {
        float x = sample / 32768f;
        float abs = Math.abs(x);
        if (abs > LIMITER_KNEE) {
            float range = 1f - LIMITER_KNEE;
            abs = LIMITER_KNEE + range * (float) Math.tanh((abs - LIMITER_KNEE) / range);
            x = Math.copySign(abs, x);
        }
        return clamp(Math.round(x * 32767f));
    }

    private static int clamp(int sample) {
        return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
    }

    private static void startThread(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private static void notify(String message) {
        Minecraft.getInstance().execute(() -> ModFunctions.sendSystemMessage(message, false));
    }
}
