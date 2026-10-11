package com.skyblockutils.features.voice;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

final class VoiceNetwork {
    static final byte REGISTER = 0x01;
    static final byte REGISTER_ACK = 0x02;
    static final byte PING = 0x10;
    static final byte PONG = 0x11;
    static final byte AUDIO = 0x12;
    static final byte BYE = 0x13;
    static final byte STATE = 0x14;

    private static final int HEADER = 13;
    private static final int TAG_BYTES = 16;
    private static final byte[] EMPTY = new byte[0];

    private final String host;
    private final Cipher encrypt;
    private final Cipher decrypt;

    private DatagramSocket socket;
    private volatile InetSocketAddress server;
    private volatile byte[] token;
    private volatile byte[] lanCandidate;
    volatile boolean registered = false;

    VoiceNetwork(String host) {
        this.host = host;
        try {
            this.encrypt = Cipher.getInstance("AES/GCM/NoPadding");
            this.decrypt = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    boolean isOpen() {
        return socket != null && !socket.isClosed();
    }

    void start() {
        if (isOpen()) return;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            socket = null;
            return;
        }
        DatagramSocket s = socket;
        Thread thread = new Thread(() -> receiveLoop(s), "SSU Voice Network");
        thread.setDaemon(true);
        thread.start();
    }

    void stop() {
        clearServer();
        if (socket != null) socket.close();
        socket = null;
    }

    void setServer(byte[] token, int port) {
        this.token = token;
        this.registered = false;
        this.server = null;
        this.lanCandidate = null;
        CompletableFuture.runAsync(() -> {
            InetSocketAddress resolved = new InetSocketAddress(host, port);
            if (this.token != token || resolved.isUnresolved()) return;
            lanCandidate = findLanCandidate(resolved);
            server = resolved;
        });
    }

    void clearServer() {
        token = null;
        server = null;
        registered = false;
    }

    boolean hasServer() {
        return server != null && token != null;
    }

    void sendRegister() {
        InetSocketAddress target = server;
        byte[] t = token;
        DatagramSocket s = socket;
        if (target == null || t == null || s == null) return;

        byte[] lan = lanCandidate;
        byte[] out = new byte[1 + t.length + (lan != null ? lan.length : 0)];
        out[0] = REGISTER;
        System.arraycopy(t, 0, out, 1, t.length);
        if (lan != null) System.arraycopy(lan, 0, out, 1 + t.length, lan.length);
        try {
            s.send(new DatagramPacket(out, out.length, target));
        } catch (IOException ignored) {
        }
    }

    void send(VoicePeer peer, byte type) {
        send(peer, type, EMPTY, 0);
    }

    void send(VoicePeer peer, byte type, byte[] payload, int length) {
        send(peer, peer.address, type, payload, length);
    }

    void sendTo(VoicePeer peer, InetSocketAddress target, byte type) {
        send(peer, target, type, EMPTY, 0);
    }

    private synchronized void send(VoicePeer peer, InetSocketAddress target, byte type, byte[] payload, int length) {
        DatagramSocket s = socket;
        if (target == null || s == null) return;

        long counter = peer.sendCounter.getAndIncrement();
        byte[] out = new byte[HEADER + length + TAG_BYTES];
        out[0] = type;
        putInt(out, 1, peer.pairId);
        putLong(out, 5, counter);

        try {
            encrypt.init(Cipher.ENCRYPT_MODE, peer.secret, new GCMParameterSpec(TAG_BYTES * 8, nonce(peer.role, counter)));
            encrypt.updateAAD(out, 0, HEADER);
            encrypt.doFinal(payload, 0, length, out, HEADER);
            s.send(new DatagramPacket(out, out.length, target));
            peer.lastSent = System.currentTimeMillis();
        } catch (Exception ignored) {
        }
    }

    private void receiveLoop(DatagramSocket s) {
        byte[] buf = new byte[1500];
        while (!s.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                s.receive(packet);
            } catch (IOException e) {
                if (s.isClosed()) return;
                continue;
            }

            int length = packet.getLength();
            if (length == 1 && buf[0] == REGISTER_ACK) {
                if (token != null && !registered) {
                    registered = true;
                    VoiceChat.LOGGER.info("Registered with voice server, ack from {}", packet.getSocketAddress());
                }
                continue;
            }
            if (length < HEADER + TAG_BYTES) continue;

            VoicePeer peer = VoiceChat.peerById(getInt(buf, 1));
            if (peer == null) continue;

            long counter = getLong(buf, 5);
            if (counter < 0 || counter <= peer.maxCounter - 512) continue;

            byte[] plain;
            try {
                decrypt.init(Cipher.DECRYPT_MODE, peer.secret, new GCMParameterSpec(TAG_BYTES * 8, nonce(1 - peer.role, counter)));
                decrypt.updateAAD(buf, 0, HEADER);
                plain = decrypt.doFinal(buf, HEADER, length - HEADER);
            } catch (Exception e) {
                continue;
            }

            if (counter > peer.maxCounter) peer.maxCounter = counter;
            peer.address = (InetSocketAddress) packet.getSocketAddress();
            peer.lastRecv = System.currentTimeMillis();
            VoiceChat.onPeerPacket(peer, buf[0], plain);
        }
    }

    private byte[] findLanCandidate(InetSocketAddress target) {
        DatagramSocket s = socket;
        if (s == null) return null;
        try (DatagramSocket probe = new DatagramSocket()) {
            probe.connect(target);
            InetAddress local = probe.getLocalAddress();
            if (!(local instanceof Inet4Address) || !local.isSiteLocalAddress()) return null;
            byte[] out = new byte[6];
            System.arraycopy(local.getAddress(), 0, out, 0, 4);
            int port = s.getLocalPort();
            out[4] = (byte) (port >>> 8);
            out[5] = (byte) port;
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] nonce(int senderRole, long counter) {
        byte[] nonce = new byte[12];
        nonce[0] = (byte) senderRole;
        putLong(nonce, 4, counter);
        return nonce;
    }

    static int getInt(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off + 1] & 0xFF) << 16) | ((b[off + 2] & 0xFF) << 8) | (b[off + 3] & 0xFF);
    }

    static void putInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    private static long getLong(byte[] b, int off) {
        return ((long) getInt(b, off) << 32) | (getInt(b, off + 4) & 0xFFFFFFFFL);
    }

    private static void putLong(byte[] b, int off, long v) {
        putInt(b, off, (int) (v >>> 32));
        putInt(b, off + 4, (int) v);
    }

    static byte[] slice(byte[] b, int from) {
        return Arrays.copyOfRange(b, from, b.length);
    }
}
