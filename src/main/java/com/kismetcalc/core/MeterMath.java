package com.kismetcalc.core;

// h_m from the Desmos graph (desmos.com/calculator/owjzg4mluz) (splooder <3)
public final class MeterMath {

    private MeterMath() {
    }

    public static double meterWeight(double baseWeight, double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return baseWeight * (1.0 + 2.0 * clamped);
    }

    // A chest rolls until its quality budget is spent, so a light item gets several chances.
    public static double hitChance(double weight) {
        final double a = weight + 13875.0;
        final double b = weight + 13870.0;
        final double c = weight + 13865.0;

        double total = (1.0 / a) * (15.0 / a) * (10.0 / b) * (weight / c)
            + (15.0 / a) * (1.0 / b) * (10.0 / b) * (weight / c)
            + (15.0 / a) * (10.0 / b) * (1.0 / c) * (weight / c);

        for (int n = 0; n <= 13; n++) {
            total += weight / Math.pow(a, n + 1);
        }

        for (int n = 0; n <= 7; n++) {
            double inner = (1000.0 / a) * survives(weight, 1 - n, weight + 12875.0)
                + (400.0 / a) * survives(weight, 1 - n, weight + 13475.0)
                + (2000.0 / a) * survives(weight, 3 - n, weight + 12875.0)
                + (800.0 / a) * survives(weight, 3 - n, weight + 13075.0)
                + (600.0 / a) * survives(weight, 3 - n, weight + 13275.0)
                + (1000.0 / a) * survives(weight, 3 - n, weight + 13375.0)
                + (1000.0 / a) * survives(weight, 5 - n, weight + 12875.0)
                + (320.0 / a) * survives(weight, 5 - n, weight + 13555.0)
                + (15.0 / a) * survives(weight, 7 - n, weight + 13870.0);
            total += inner / Math.pow(a, n);
        }
        return total;
    }

    // sum(n=0..limit) w / den^(n+1). A negative limit is an empty sum.
    private static double survives(double weight, int limit, double denominator) {
        double sum = 0.0;
        for (int n = 0; n <= limit; n++) {
            sum += weight / Math.pow(denominator, n + 1);
        }
        return sum;
    }

    public static double handleChance(double progress) {
        return hitChance(meterWeight(M7LootTable.HANDLE.weight(), progress));
    }
}
