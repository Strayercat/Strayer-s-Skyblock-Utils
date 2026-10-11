package com.skyblockutils.features.voice;

import de.maxhenkel.rnnoise4j.Denoiser;

final class VoiceDenoiser implements AutoCloseable {
    private static boolean unavailable = false;

    private final Denoiser rnnoise;
    private final short[] chunk;

    VoiceDenoiser() {
        this.rnnoise = create();
        this.chunk = new short[rnnoise != null ? rnnoise.getFrameSize() : 0];
    }

    float process(short[] pcm, boolean suppress) {
        if (rnnoise == null || chunk.length == 0 || pcm.length % chunk.length != 0) return -1;
        try {
            float total = 0;
            int chunks = pcm.length / chunk.length;
            for (int c = 0; c < chunks; c++) {
                System.arraycopy(pcm, c * chunk.length, chunk, 0, chunk.length);
                total += suppress ? rnnoise.denoiseInPlace(chunk) : rnnoise.getSpeechProbability(chunk);
                if (suppress) System.arraycopy(chunk, 0, pcm, c * chunk.length, chunk.length);
            }
            return Math.clamp(total / chunks, 0f, 1f);
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    public void close() {
        if (rnnoise == null) return;
        try {
            rnnoise.close();
        } catch (Throwable ignored) {
        }
    }

    private static Denoiser create() {
        if (unavailable) return null;
        try {
            Denoiser denoiser = new Denoiser();
            VoiceChat.LOGGER.info("Using RNNoise for noise suppression");
            return denoiser;
        } catch (Throwable t) {
            unavailable = true;
            VoiceChat.LOGGER.warn("RNNoise unavailable, noise suppression disabled", t);
            return null;
        }
    }
}
