package com.gqo6.tiertag.data;

import java.util.Optional;
import java.util.regex.Pattern;

public enum Tier {
    HT1(400.0, 0xFFDA55),
    MT1(350.0, 0xE8BE3C),
    LT1(300.0, 0xC4A34B),
    HT2(250.0, 0x9EB3D1),
    MT2(200.0, 0x91A0B7),
    LT2(150.0, 0x858D9C),
    HT3(125.0, 0xEB9138),
    MT3(100.0, 0xD47D32),
    LT3(75.0, 0xBD6B2B),
    HT4(50.0, 0x9FD27F),
    MT4(30.0, 0x89BD68),
    LT4(15.0, 0x739F57),
    HT5(5.0, 0x4974B5),
    MT5(1.0, 0x385C94),
    LT5(0.75, 0x284775),
    HT6(0.5, 0x4C3B30),
    MT6(0.25, 0x45352B),
    LT6(0.125, 0x36291F),
    ST6(0.0, 0x383838);

    public static final Pattern CODE = Pattern.compile("^(?:[HML]T[1-6]|ST6)$");

    private final double minPoints;
    private final int rgb;

    Tier(double minPoints, int rgb) {
        this.minPoints = minPoints;
        this.rgb = rgb;
    }

    public double minPoints() {
        return minPoints;
    }

    public int rgb() {
        return rgb;
    }

    public boolean isBetterThan(Tier other) {
        return other == null || ordinal() < other.ordinal();
    }

    public static Optional<Tier> parse(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String trimmed = code.trim().toUpperCase();
        if (!CODE.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Tier.valueOf(trimmed));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
