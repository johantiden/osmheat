package com.github.johantiden.osmheat;

public class ImageGrayscaleDouble {
    private final double[] pixels;
    private final int width;
    private final int height;

    protected ImageGrayscaleDouble(double[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public ImageGrayscaleDouble(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new double[width * height];
    }

    public double[] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPixel(final int x, final int y, final double pixel) {
        pixels[getIndex(x, y)] = pixel;
    }

    private int getIndex(int x, int y) {
        return width * y + x;
    }

    public double getPixel(int x, int y) {
        return pixels[getIndex(x, y)];
    }

}
