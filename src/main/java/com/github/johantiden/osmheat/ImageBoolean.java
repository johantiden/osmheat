package com.github.johantiden.osmheat;

public class ImageBoolean {
    private final boolean[] pixels;
    private final int width;
    private final int height;

    protected ImageBoolean(boolean[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public ImageBoolean(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new boolean[width * height];
    }

    public boolean[] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPixel(final int x, final int y, final boolean pixel) {
        pixels[getIndex(x, y)] = pixel;
    }

    private int getIndex(int x, int y) {
        return width * y + x;
    }

    public boolean getPixel(int x, int y) {
        return pixels[getIndex(x, y)];
    }

}
