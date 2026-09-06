package com.kismetcalc.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;


public final class CombinationTable {

    private static final String RESOURCE = "/assets/kismetcalc/kismet/m7_combinations.csv.gz";

    private static volatile CombinationTable instance;
    private static volatile String failure;

    private final long[] keys;
    private final int[] qualities;
    private final double[] probabilities;

    private CombinationTable(long[] keys, int[] qualities, double[] probabilities) {
        this.keys = keys;
        this.qualities = qualities;
        this.probabilities = probabilities;
    }

    public static CombinationTable get() {
        return instance;
    }

    public static String failure() {
        return failure;
    }

    public static boolean loaded() {
        return instance != null;
    }

    // Blocking. The caller picks the thread.
    public static synchronized void load() {
        if (instance != null || failure != null) return;
        try (InputStream raw = CombinationTable.class.getResourceAsStream(RESOURCE)) {
            if (raw == null) {
                failure = "combination table missing from the jar";
                return;
            }
            instance = parse(raw);
        } catch (IOException | RuntimeException e) {
            failure = String.valueOf(e.getMessage());
        }
    }

    private static CombinationTable parse(InputStream raw) throws IOException {
        int capacity = 11_000;
        long[] keys = new long[capacity];
        int[] qualities = new int[capacity];
        double[] probabilities = new double[capacity];
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                int first = line.indexOf(',');
                int second = line.indexOf(',', first + 1);
                if (first < 0 || second < 0) continue;

                if (count == keys.length) {
                    int grown = count * 2;
                    keys = java.util.Arrays.copyOf(keys, grown);
                    qualities = java.util.Arrays.copyOf(qualities, grown);
                    probabilities = java.util.Arrays.copyOf(probabilities, grown);
                }
                keys[count] = Long.parseLong(line, 0, first, 10);
                qualities[count] = Integer.parseInt(line, first + 1, second, 10);
                probabilities[count] = Double.parseDouble(line.substring(second + 1));
                count++;
            }
        }
        if (count == 0) throw new IOException("combination table was empty");

        return new CombinationTable(
            java.util.Arrays.copyOf(keys, count),
            java.util.Arrays.copyOf(qualities, count),
            java.util.Arrays.copyOf(probabilities, count));
    }

    public int size() {
        return keys.length;
    }

    public long key(int row) {
        return keys[row];
    }

    public int quality(int row) {
        return qualities[row];
    }

    public double probability(int row) {
        return probabilities[row];
    }
}
