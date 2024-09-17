package com.github.johantiden.osmheat;

public enum Channel {
    ALPHA(24, 0x00FFFFFF),
    RED(16, 0xFF00FFFF),
    GREEN(8, 0xFFFF00FF),
    BLUE(0, 0xFFFFFF00),
    ;

    private final int bit;
    private final int invertedMask;

    Channel(int bit, int invertedMask) {
        this.bit = bit;
        this.invertedMask = invertedMask;
    }

    public int getBit() {
        return bit;
    }

    int getInvertedMask() {
        return invertedMask;
    }

    public int getValue(final int pixel) {
        return (pixel >> bit) & 0xFF;
    }

    public float getValueAsPercent(final int pixel) {
        return getValue(pixel) / 255f;
    }

    public int changeValue(final int pixel, final int value) {
        return pixel & invertedMask | value << bit;
    }
}
