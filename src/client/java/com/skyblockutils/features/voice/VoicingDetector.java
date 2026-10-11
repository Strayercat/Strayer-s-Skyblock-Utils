package com.skyblockutils.features.voice;

final class VoicingDetector {
    private static final int DECIMATION = 4;
    private static final int RATE = 48_000 / DECIMATION;
    private static final int MIN_LAG = RATE / 400;
    private static final int MAX_LAG = RATE / 75;
    private static final int WINDOW = 240;
    private static final double LOWPASS_HZ = 900;
    private static final int BLOCKS = 4;
    private static final double MAX_ENERGY_SPREAD = 2.2;

    private final double[] history;
    private final double alpha;
    private double lp1, lp2;
    private int filled = 0;

    VoicingDetector(int frameSize) {
        this.history = new double[WINDOW + MAX_LAG + frameSize / DECIMATION];
        double rc = 1.0 / (2 * Math.PI * LOWPASS_HZ);
        double dt = 1.0 / 48_000;
        this.alpha = dt / (rc + dt);
    }

    double process(short[] pcm) {
        double periodicity = periodicity(pcm);
        return periodicity > 0 && isSteady(pcm) ? periodicity : 0;
    }

    private static boolean isSteady(short[] pcm) {
        int size = pcm.length / BLOCKS;
        double total = 0;
        double max = 0;
        for (int b = 0; b < BLOCKS; b++) {
            double energy = 0;
            for (int i = b * size; i < (b + 1) * size; i++) energy += (double) pcm[i] * pcm[i];
            total += energy;
            max = Math.max(max, energy);
        }
        if (total <= 0) return false;
        return max / (total / BLOCKS) <= MAX_ENERGY_SPREAD;
    }

    private double periodicity(short[] pcm) {
        int count = pcm.length / DECIMATION;
        System.arraycopy(history, count, history, 0, history.length - count);
        int base = history.length - count;

        for (int i = 0; i < count; i++) {
            double sum = 0;
            for (int j = 0; j < DECIMATION; j++) {
                lp1 += alpha * (pcm[i * DECIMATION + j] - lp1);
                lp2 += alpha * (lp1 - lp2);
                sum += lp2;
            }
            history[base + i] = sum / DECIMATION;
        }
        filled = Math.min(history.length, filled + count);
        if (filled < history.length) return 0;

        int start = history.length - WINDOW;
        double energy = 0;
        double mean = 0;
        for (int n = start; n < history.length; n++) mean += history[n];
        mean /= WINDOW;
        for (int n = start; n < history.length; n++) energy += (history[n] - mean) * (history[n] - mean);
        if (energy <= 1e-6) return 0;

        double best = 0;
        for (int lag = MIN_LAG; lag <= MAX_LAG; lag++) {
            double cross = 0;
            double lagged = 0;
            for (int n = start; n < history.length; n++) {
                double a = history[n] - mean;
                double b = history[n - lag] - mean;
                cross += a * b;
                lagged += b * b;
            }
            if (lagged <= 1e-6) continue;
            double r = cross / Math.sqrt(energy * lagged);
            if (r > best) best = r;
        }
        return best;
    }
}
