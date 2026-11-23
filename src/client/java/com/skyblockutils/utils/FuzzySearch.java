package com.skyblockutils.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class FuzzySearch {
    private FuzzySearch() {
    }

    public static List<String> search(String query, Collection<String> items) {
        return searchAny(query, items, List::of);
    }

    public static <T> List<T> searchAny(String query, Collection<T> items, Function<T, Collection<String>> keys) {
        if (query.isEmpty()) return new ArrayList<>(items);

        record Scored<T>(T item, int score) {
        }

        List<Scored<T>> scored = new ArrayList<>();
        for (T item : items) {
            int best = -1;
            for (String key : keys.apply(item)) best = Math.max(best, score(query, key));
            if (best >= 0) scored.add(new Scored<>(item, best));
        }

        scored.sort(Comparator.comparingInt((Scored<T> s) -> s.score).reversed());
        return scored.stream().map(Scored::item).toList();
    }

    public static int score(String query, String target) {
        if (query.isEmpty()) return 0;
        String q = query.toLowerCase(Locale.ROOT);
        String t = target.toLowerCase(Locale.ROOT);

        if (t.equals(q)) return 10_000;
        if (t.startsWith(q)) return 8_000 - Math.min(t.length() - q.length(), 999);

        int index = t.indexOf(q);
        if (index >= 0) return 6_000 + (isWordStart(t, index) ? 1_000 : 0) - Math.min(index * 10 + t.length(), 999);

        return Math.max(subsequenceScore(q, t), typoScore(q, t));
    }

    private static int subsequenceScore(String q, String t) {
        int score = 0;
        int from = 0;
        int previous = -2;
        int streak = 0;

        for (int i = 0; i < q.length(); i++) {
            int found = t.indexOf(q.charAt(i), from);
            if (found < 0) return -1;

            if (found == previous + 1) {
                streak++;
                score += 15 * streak;
            } else {
                streak = 0;
                if (i > 0) score -= Math.min(found - from, 10) * 3;
            }
            if (isWordStart(t, found)) score += 30;
            if (i == 0) score -= Math.min(found, 20) * 2;

            score += 10;
            previous = found;
            from = found + 1;
        }

        score -= Math.min(t.length() - q.length(), 50);
        return Math.clamp(score + 1_000, 600, 5_000);
    }

    private static int typoScore(String q, String t) {
        if (q.length() < 3) return -1;
        int limit = q.length() >= 5 ? 2 : 1;
        int best = Integer.MAX_VALUE;

        for (int start = 0; start < t.length(); start++) {
            if (!isWordStart(t, start)) continue;
            String segment = t.substring(start, Math.min(t.length(), start + q.length()));
            best = Math.min(best, editDistance(q, segment));
            if (best == 0) break;
        }

        if (best > limit) return -1;
        return Math.clamp(3_000 - best * 1_200L - Math.min(Math.abs(t.length() - q.length()), 99) * 5, 1, 4_000);
    }

    private static int editDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    private static boolean isWordStart(String text, int index) {
        return index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
    }
}
