package com.skyblockutils.features.voice;

import io.github.jaredmdobson.concentus.OpusDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

final class VoicePeer {
    enum State { IDLE, PUNCHING, CONNECTED, FAILED }

    final String name;
    final String key;
    final int pairId;
    final int role;
    final SecretKeySpec secret;
    final JitterBuffer jitter = new JitterBuffer();
    final AtomicLong sendCounter = new AtomicLong();

    volatile InetSocketAddress address;
    final InetSocketAddress lanAddress;
    volatile State state = State.IDLE;
    volatile boolean active = false;
    volatile long lastRecv = 0;
    volatile long lastSent = 0;
    volatile long lastAudio = 0;
    volatile long byeAt = 0;
    volatile boolean away = false;
    volatile boolean remoteMuted = false;
    volatile boolean remoteDeafened = false;

    long stateSince = 0;
    long lastPing = 0;
    boolean fresh = true;
    boolean announced = false;

    long maxCounter = -1;
    OpusDecoder decoder;

    VoicePeer(String name, int pairId, int role, byte[] key, InetSocketAddress address, InetSocketAddress lanAddress) {
        this.name = name;
        this.key = name.toLowerCase(java.util.Locale.ROOT);
        this.pairId = pairId;
        this.role = role;
        this.secret = new SecretKeySpec(key, "AES");
        this.address = address;
        this.lanAddress = lanAddress;
    }

    void setState(State next, long now) {
        state = next;
        stateSince = now;
        lastPing = 0;
    }

    boolean isLive() {
        return active && state == State.CONNECTED;
    }

    boolean isSpeaking(long now) {
        return isLive() && now - lastAudio < VoiceChat.SPEAKING_MS;
    }
}
