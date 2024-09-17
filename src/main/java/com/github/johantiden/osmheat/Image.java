package com.github.johantiden.osmheat;

import com.pngencoder.PngEncoder;
import jakarta.annotation.Nonnull;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class Image {
    private final int[] pixels;
    private final int width;
    private final int height;

    protected Image(int[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    public Image(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new int[width * height];
    }

    public int[] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setPixel(final int x, final int y, final int pixel) {
        pixels[getIndex(x, y)] = pixel;
    }

    private int getIndex(int x, int y) {
        return width * y + x;
    }

    public byte[] toPng() {
        BufferedImage bufferedImage = ToPng.createFromIntArgb(pixels, width, height);
        return new PngEncoder()
                .withBufferedImage(bufferedImage)
                .toBytes();
    }

    public int getPixel(int x, int y) {
        return pixels[getIndex(x, y)];
    }

    private class ToPng {
        private static final int[] BAND_MASKS_INT_ARGB = {
                0x00ff0000,
                0x0000ff00,
                0x000000ff,
                0xff000000};

        private static final ColorModel COLOR_MODEL_INT_ARGB = ColorModel.getRGBdefault();
        private static BufferedImage createFromIntArgb(@Nonnull int[] data, int width, int height) {
            DataBuffer dataBuffer = new DataBufferInt(data, data.length);
            WritableRaster raster = Raster.createPackedRaster(dataBuffer, width, height, width, BAND_MASKS_INT_ARGB, null);
            return new BufferedImage(COLOR_MODEL_INT_ARGB, raster, false, null);
        }
    }
}
