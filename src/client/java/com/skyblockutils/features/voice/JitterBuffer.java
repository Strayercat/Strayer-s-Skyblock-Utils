package com.skyblockutils.features.voice;

import java.util.TreeMap;

final class JitterBuffer {
    static final byte[] LOST = new byte[0];

    private static final int START_FRAMES = 3;
    private static final int MAX_FRAMES = 25;
    private static final int MAX_CONCEALED = 3;
    private static final int RESET_DISTANCE = 100;

    private final TreeMap<Integer, byte[]> frames = new TreeMap<>();
    private boolean playing = false;
    private int next = 0;
    private int concealed = 0;
    private Integer endSeq = null;
    private boolean resetDecoder = false;

    synchronized void put(int seq, byte[] data) {
        if (playing && seq < next) {
            if (next - seq <= RESET_DISTANCE) return;
            reset();
        }

        frames.put(seq, data);
        if (frames.size() <= MAX_FRAMES) return;

        while (frames.size() > START_FRAMES) frames.pollFirstEntry();
        if (playing) next = frames.firstKey();
    }

    synchronized void end(int seq) {
        if (playing && seq < next) return;
        endSeq = seq;
    }

    synchronized boolean takeReset() {
        boolean reset = resetDecoder;
        resetDecoder = false;
        return reset;
    }

    synchronized byte[] poll() {
        if (!playing) {
            if (endSeq != null && !frames.isEmpty() && frames.firstKey() >= endSeq) endSeq = null;
            boolean ending = endSeq != null && !frames.isEmpty();
            if (frames.size() < START_FRAMES && !ending) return null;
            playing = true;
            next = frames.firstKey();
            concealed = 0;
        }

        if (endSeq != null && next >= endSeq) {
            frames.headMap(endSeq, true).clear();
            endSeq = null;
            finish();
            return null;
        }

        byte[] data = frames.remove(next);
        if (data != null) {
            next++;
            concealed = 0;
            return data;
        }

        if (++concealed <= MAX_CONCEALED) {
            next++;
            return LOST;
        }

        if (frames.isEmpty()) {
            finish();
            return null;
        }

        next = frames.firstKey();
        concealed = 0;
        data = frames.remove(next);
        next++;
        return data;
    }

    synchronized void clear() {
        reset();
    }

    private void finish() {
        playing = false;
        concealed = 0;
        resetDecoder = true;
    }

    private void reset() {
        frames.clear();
        playing = false;
        concealed = 0;
        endSeq = null;
        resetDecoder = true;
    }
}
