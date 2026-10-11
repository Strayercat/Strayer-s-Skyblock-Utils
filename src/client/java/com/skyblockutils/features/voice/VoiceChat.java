package com.skyblockutils.features.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skyblockutils.ModFunctions;
import com.skyblockutils.ModKeyBindings;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.coop.CoopListParser;
import com.skyblockutils.features.party.PartyInfo;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceChat {
    public static final Logger LOGGER = LoggerFactory.getLogger("SSU Voice");
    public static final String DEFAULT_DEVICE = VoiceAudio.DEFAULT_DEVICE;
    static final long SPEAKING_MS = 250;

    private static final String HOST = "cawcah.duckdns.org";
    private static final long OUT_OF_WORLD_GRACE_MS = 5_000;
    private static final long REGISTER_INTERVAL_MS = 20_000;
    private static final long REGISTER_RETRY_MS = 1_000;
    private static final int REGISTER_WARN_ATTEMPTS = 8;
    private static final long PUNCH_PING_MS = 250;
    private static final long PUNCH_TIMEOUT_MS = 10_000;
    private static final long KEEPALIVE_MS = 5_000;
    private static final long PEER_TIMEOUT_MS = 15_000;
    private static final long FAILED_RETRY_MS = 60_000;
    private static final int MAX_WANTS = 20;

    private static final Map<String, VoicePeer> peers = new ConcurrentHashMap<>();
    private static final Map<Integer, VoicePeer> peersById = new ConcurrentHashMap<>();
    private static final Map<String, String> active = new LinkedHashMap<>();
    private static final Set<String> sentWants = new HashSet<>();

    private static final VoiceNetwork network = new VoiceNetwork(HOST);
    private static final VoiceAudio audio = new VoiceAudio();

    public static volatile boolean muted = false;
    public static volatile boolean deafened = false;
    static volatile boolean pttDown = false;

    private static boolean running = false;
    private static boolean helloSent = false;
    private static boolean wantsDirty = true;
    private static boolean udpWarned = false;
    private static String audioDevices = "";
    private static long lastRegister = 0;
    private static long outOfWorldSince = -1;
    private static int unackedRegisters = 0;

    public static void tick(Minecraft client) {
        long now = System.currentTimeMillis();

        if (!ModConfig.INSTANCE.voiceChatEnabled) {
            if (running) shutdown(true);
            return;
        }

        if (client.player == null) {
            if (outOfWorldSince < 0) outOfWorldSince = now;
            if (running && now - outOfWorldSince > OUT_OF_WORLD_GRACE_MS) shutdown(false);
            return;
        }
        outOfWorldSince = -1;

        if (!running) startup();
        if (!network.isOpen()) return;

        handleKeys();
        handleRegistration(now);
        updateActive(client);
        sendWants();
        managePeers(now);
        updateAudio();
    }

    public static void handleServerMessage(String type, JsonObject msg) {
        if (!running) return;
        try {
            switch (type) {
                case "voice_token" -> {
                    network.setServer(Base64.getDecoder().decode(msg.get("token").getAsString()), msg.get("port").getAsInt());
                    lastRegister = 0;
                    unackedRegisters = 0;
                    sentWants.clear();
                    wantsDirty = true;
                    LOGGER.info("Received voice token, registering over UDP port {}", msg.get("port").getAsInt());
                }
                case "voice_peer" -> addPeer(msg);
                default -> {
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Bad voice message {}", msg, e);
        }
    }

    public static void onSocketClosed() {
        helloSent = false;
        sentWants.clear();
        wantsDirty = true;
        network.clearServer();
    }

    public static String[] inputDevices() {
        return VoiceAudio.inputDevices();
    }

    public static String[] outputDevices() {
        return VoiceAudio.outputDevices();
    }

    public static String deviceDisplay(String configured, String[] available) {
        if (configured == null || configured.isBlank()) return DEFAULT_DEVICE;
        for (String device : available) if (device.equals(configured)) return device;
        return DEFAULT_DEVICE;
    }

    public static String deviceValue(String selected) {
        return selected == null || selected.equals(DEFAULT_DEVICE) ? "" : selected;
    }

    static VoicePeer peerById(int id) {
        return peersById.get(id);
    }

    static Collection<VoicePeer> peers() {
        return peers.values();
    }

    static boolean isRunning() {
        return running;
    }

    static boolean isTransmitting() {
        return audio.transmitting;
    }

    static List<VoicePeer> hudPeers() {
        List<VoicePeer> list = new ArrayList<>();
        for (VoicePeer peer : peers.values()) if (peer.active && !peer.away) list.add(peer);
        list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return list;
    }

    static void broadcastAudio(byte[] payload, int length) {
        for (VoicePeer peer : peers.values()) {
            if (peer.isLive()) network.send(peer, VoiceNetwork.AUDIO, payload, length);
        }
    }

    static void onPeerPacket(VoicePeer peer, byte type, byte[] payload) {
        switch (type) {
            case VoiceNetwork.PING -> {
                readState(peer, payload);
                if (peer.active) sendWithState(peer, VoiceNetwork.PONG);
            }
            case VoiceNetwork.PONG, VoiceNetwork.STATE -> readState(peer, payload);
            case VoiceNetwork.AUDIO -> {
                if (!peer.isLive() || payload.length < 4) return;
                int seq = VoiceNetwork.getInt(payload, 0);
                if (payload.length == 4) {
                    peer.jitter.end(seq);
                    return;
                }
                peer.jitter.put(seq, VoiceNetwork.slice(payload, 4));
                peer.lastAudio = System.currentTimeMillis();
            }
            case VoiceNetwork.BYE -> peer.byeAt = System.currentTimeMillis();
            default -> {
            }
        }
    }

    private static void readState(VoicePeer peer, byte[] payload) {
        if (payload.length < 1) return;
        peer.remoteMuted = (payload[0] & 0x01) != 0;
        peer.remoteDeafened = (payload[0] & 0x02) != 0;
    }

    private static void sendWithState(VoicePeer peer, byte type) {
        byte flags = (byte) ((muted ? 0x01 : 0) | (deafened ? 0x02 : 0));
        network.send(peer, type, new byte[]{flags}, 1);
    }

    private static void broadcastState() {
        for (VoicePeer peer : peers.values()) {
            if (peer.isLive()) sendWithState(peer, VoiceNetwork.STATE);
        }
    }

    private static void startup() {
        muted = ModConfig.INSTANCE.voiceMuted;
        deafened = ModConfig.INSTANCE.voiceDeafened;
        network.start();
        SSUIndicator.connectNow();
        running = true;
        udpWarned = false;
        wantsDirty = true;
        LOGGER.info("Voice chat started");
    }

    private static void shutdown(boolean disabled) {
        for (VoicePeer peer : peers.values()) {
            if (peer.state == VoicePeer.State.CONNECTED) network.send(peer, VoiceNetwork.BYE);
            peer.jitter.clear();
        }
        if (disabled && helloSent) sendJson("voice_bye", null);

        peers.clear();
        peersById.clear();
        active.clear();
        sentWants.clear();
        audio.stop();
        network.stop();

        running = false;
        helloSent = false;
        wantsDirty = true;
        lastRegister = 0;
        unackedRegisters = 0;
        pttDown = false;
        LOGGER.info("Voice chat stopped");
    }

    private static void handleKeys() {
        pttDown = ModKeyBindings.VOICE_PTT_KEY.isDown();

        while (ModKeyBindings.VOICE_MUTE_KEY.consumeClick()) {
            ModConfig.INSTANCE.voiceMuted = !ModConfig.INSTANCE.voiceMuted;
            muted = ModConfig.INSTANCE.voiceMuted;
            ModConfig.save();
            broadcastState();
            ModFunctions.sendSystemMessage(muted ? "Voice chat microphone muted" : "Voice chat microphone unmuted", false);
        }

        while (ModKeyBindings.VOICE_DEAFEN_KEY.consumeClick()) {
            ModConfig.INSTANCE.voiceDeafened = !ModConfig.INSTANCE.voiceDeafened;
            deafened = ModConfig.INSTANCE.voiceDeafened;
            ModConfig.save();
            broadcastState();
            ModFunctions.sendSystemMessage(deafened ? "Voice chat deafened" : "Voice chat undeafened", false);
        }
    }

    private static void handleRegistration(long now) {
        if (!SSUIndicator.isReady()) return;

        if (!helloSent) {
            helloSent = sendJson("voice_hello", null);
            return;
        }

        if (!network.hasServer()) return;
        if (network.registered) {
            unackedRegisters = 0;
            udpWarned = false;
        }

        long interval = network.registered ? REGISTER_INTERVAL_MS : REGISTER_RETRY_MS;
        if (now - lastRegister < interval) return;
        lastRegister = now;

        if (!network.registered && ++unackedRegisters == REGISTER_WARN_ATTEMPTS && !udpWarned) {
            udpWarned = true;
            LOGGER.warn("No UDP ack from voice server after {} attempts", REGISTER_WARN_ATTEMPTS);
            ModFunctions.sendSystemMessage("Voice chat can't reach the SSU server over UDP, still retrying...", false);
        }
        network.sendRegister();
    }

    private static void updateActive(Minecraft client) {
        Set<String> previous = new HashSet<>(active.keySet());
        active.clear();
        String self = client.getUser().getName().toLowerCase(Locale.ROOT);

        if (ModConfig.INSTANCE.voiceInParty && PartyInfo.isInParty) {
            addActive(PartyInfo.leader, self);
            for (String member : PartyInfo.members) addActive(member, self);
        }

        boolean partyLike = PartyInfo.isInParty || Boolean.TRUE.equals(ModFunctions.isInDungeons(client));
        if (ModConfig.INSTANCE.voiceInCoop && !(ModConfig.INSTANCE.voiceNoCoopInParty && partyLike)) {
            for (String member : CoopListParser.members) addActive(member, self);
        }

        if (!previous.equals(active.keySet())) wantsDirty = true;
        for (VoicePeer peer : peers.values()) peer.active = active.containsKey(peer.key);
    }

    private static void addActive(String name, String self) {
        if (name == null || name.isBlank()) return;
        String key = name.toLowerCase(Locale.ROOT);
        if (!key.equals(self) && active.size() < MAX_WANTS) active.putIfAbsent(key, name);
    }

    private static void sendWants() {
        if (!wantsDirty || !SSUIndicator.isReady() || !network.registered) return;
        if (sentWants.equals(active.keySet())) {
            wantsDirty = false;
            return;
        }

        JsonArray names = new JsonArray();
        for (String key : active.keySet()) names.add(key);
        if (!sendJson("voice_want", names)) return;

        sentWants.clear();
        sentWants.addAll(active.keySet());
        wantsDirty = false;
        LOGGER.info("Sent voice wants {}", sentWants);
    }

    private static void managePeers(long now) {
        for (VoicePeer peer : peers.values()) {
            if (!peer.active) {
                if (peer.state != VoicePeer.State.IDLE) {
                    if (peer.state == VoicePeer.State.CONNECTED) network.send(peer, VoiceNetwork.BYE);
                    peer.setState(VoicePeer.State.IDLE, now);
                    peer.announced = false;
                    peer.away = false;
                    peer.jitter.clear();
                }
                continue;
            }

            if (peer.byeAt > peer.stateSince && peer.state != VoicePeer.State.IDLE) {
                peer.setState(VoicePeer.State.FAILED, now);
                peer.away = true;
                peer.jitter.clear();
                LOGGER.info("{} left voice", peer.name);
                continue;
            }

            if (peer.state != VoicePeer.State.CONNECTED && peer.state != VoicePeer.State.IDLE && peer.lastRecv > peer.stateSince) {
                peer.setState(VoicePeer.State.CONNECTED, now);
                peer.fresh = false;
                peer.away = false;
                LOGGER.info("Voice connected with {} via {}", peer.name, peer.address);
                if (!peer.announced) {
                    peer.announced = true;
                    ModFunctions.sendSystemMessage("Voice chat connected with " + peer.name, false);
                }
                continue;
            }

            switch (peer.state) {
                case IDLE -> peer.setState(VoicePeer.State.PUNCHING, now);
                case PUNCHING -> {
                    if (now - peer.stateSince > PUNCH_TIMEOUT_MS) {
                        LOGGER.info("Punch to {} timed out (public {}, lan {}, fresh {})", peer.name, peer.address, peer.lanAddress, peer.fresh);
                        if (peer.fresh) {
                            peer.setState(VoicePeer.State.FAILED, now);
                            ModFunctions.sendSystemMessage("Couldn't open a direct voice connection with " + peer.name + ", retrying in a minute", false);
                        } else {
                            retry(peer);
                        }
                    } else if (now - peer.lastPing >= PUNCH_PING_MS) {
                        peer.lastPing = now;
                        sendWithState(peer, VoiceNetwork.PING);
                        if (peer.lanAddress != null) network.sendTo(peer, peer.lanAddress, VoiceNetwork.PING);
                    }
                }
                case CONNECTED -> {
                    if (now - peer.lastRecv > PEER_TIMEOUT_MS) {
                        LOGGER.info("Voice with {} timed out, re-punching", peer.name);
                        peer.setState(VoicePeer.State.PUNCHING, now);
                        peer.jitter.clear();
                    } else if (now - peer.lastSent >= KEEPALIVE_MS) {
                        sendWithState(peer, VoiceNetwork.PING);
                    }
                }
                case FAILED -> {
                    if (!peer.away && now - peer.stateSince > FAILED_RETRY_MS) retry(peer);
                }
            }
        }
    }

    private static void updateAudio() {
        boolean needed = ModConfig.INSTANCE.voiceMicTest;
        for (VoicePeer peer : peers.values()) {
            if (peer.isLive()) {
                needed = true;
                break;
            }
        }

        String devices = ModConfig.INSTANCE.voiceInputDevice + "\n" + ModConfig.INSTANCE.voiceOutputDevice;
        if (needed && (!audio.isRunning() || !devices.equals(audioDevices))) {
            audioDevices = devices;
            audio.start(ModConfig.INSTANCE.voiceInputDevice, ModConfig.INSTANCE.voiceOutputDevice);
        } else if (!needed && audio.isRunning()) {
            audio.stop();
        }
    }

    private static void addPeer(JsonObject msg) {
        String name = msg.get("name").getAsString();
        String key = name.toLowerCase(Locale.ROOT);

        VoicePeer old = peers.remove(key);
        if (old != null) peersById.remove(old.pairId);

        InetSocketAddress lan = msg.has("lanIp") && msg.has("lanPort")
                ? new InetSocketAddress(msg.get("lanIp").getAsString(), msg.get("lanPort").getAsInt())
                : null;

        VoicePeer peer = new VoicePeer(
                name,
                msg.get("id").getAsInt(),
                msg.get("role").getAsInt(),
                Base64.getDecoder().decode(msg.get("key").getAsString()),
                new InetSocketAddress(msg.get("ip").getAsString(), msg.get("port").getAsInt()),
                lan
        );
        peer.active = active.containsKey(key);
        if (old != null) peer.announced = old.announced;
        peersById.put(peer.pairId, peer);
        peers.put(key, peer);
        LOGGER.info("Paired with {} (public {}, lan {})", name, peer.address, lan);
    }

    private static void retry(VoicePeer peer) {
        peers.remove(peer.key, peer);
        peersById.remove(peer.pairId, peer);

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "voice_retry");
        msg.addProperty("name", peer.key);
        SSUIndicator.sendText(msg.toString());
        LOGGER.info("Requested fresh pairing with {}", peer.name);
    }

    private static boolean sendJson(String type, JsonArray names) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", type);
        if (names != null) msg.add("names", names);
        return SSUIndicator.sendText(msg.toString());
    }
}
