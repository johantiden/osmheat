package com.github.johantiden.osmheat;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Tile {

    private static final Logger logger = LoggerFactory.getLogger(Tile.class);
    public static final int IMAGE_SIZE = 256;
    private final ImageBoolean touchMap;
    private final RenderingController.TileCoordinate tileCoordinate;

    public Tile(RenderingController.TileCoordinate tileCoordinate) {
        this.tileCoordinate = tileCoordinate;
        touchMap = new ImageBoolean(IMAGE_SIZE, IMAGE_SIZE);
    }

    public RenderingController.TileCoordinate getTileCoordinate() {
        return tileCoordinate;
    }

    public Image render() {
        ImageGrayscaleDouble heatMap = new ImageGrayscaleDouble(IMAGE_SIZE, IMAGE_SIZE);
        Image image = new Image(IMAGE_SIZE, IMAGE_SIZE);
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                boolean value = touchMap.getPixel(x, y);
                if (value) {
                    double radius = tileCoordinate.getKernelSize();
                    drawKernel(heatMap, x, y, radius);
                }
            }
        }

        double max = max(heatMap.getPixels());
//        double sqrtMax = Math.sqrt(max);
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                double pixel = heatMap.getPixel(x, y)/max*255;
                if (pixel > 0) {
                    int heat = (int) pixel;
                    image.setPixel(x, y, getRgb(heat, 128 + heat / 2, 255, 128 - heat / 2));
                }
            }
        }

        return image;
    }

    private void drawKernel(ImageGrayscaleDouble heatMap, int centerX, int centerY, double radius) {
        int minY = (int)Math.max(centerY - radius, 0);
        int maxY = (int)Math.min(centerY + radius, IMAGE_SIZE);
        int minX = (int)Math.max(centerX - radius, 0);
        int maxX = (int)Math.min(centerX + radius, IMAGE_SIZE);


        double radiusSquare = radius * radius;

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                int distanceSquared = (y - centerY) * (y - centerY) + (x - centerX) * (x - centerX);
                double strength = radiusSquare - distanceSquared;
                if (strength > 0) {
                    double pixel = heatMap.getPixel(x, y);
                    heatMap.setPixel(x, y, pixel + strength);
                }
            }
        }
    }

    private static double max(double[] gammaHeatMap) {
        double max = 0;
        for (double v : gammaHeatMap) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    static void fastBoxBlur(ImageGrayscaleDouble pixels, int kernelSize) {
        final int WIDTH = kernelSize;
        final int HEIGHT = kernelSize;
        final int BLUR_BOX_SIZE = WIDTH * HEIGHT;

        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                double sum = 0;

                for (int dy = -HEIGHT/2; dy < HEIGHT/2; dy++) {
                    for (int dx = -WIDTH/2; dx < WIDTH/2; dx++) {
                        int y1 = y + dy;
                        if (y1 < 0) {
                            y1 = 0;
                        }
                        if (y1 >= IMAGE_SIZE) {
                            y1 = IMAGE_SIZE - 1;
                        }
                        int x1 = x + dx;
                        if (x1 < 0) {
                            x1 = 0;
                        }
                        if (x1 >= IMAGE_SIZE) {
                            x1 = IMAGE_SIZE - 1;
                        }
                        sum += pixels.getPixel(x1, y1);
                    }
                }

                double avg = sum / BLUR_BOX_SIZE;
                pixels.setPixel(x, y, avg);
            }
        }
    }

    public void add(Track.Segment.Point point) {
        add(getPointInImageSpace(point));
    }

    public void add(PointInImageSpace point) {
        int x = (int) (point.x);
        int y = (int) (point.y);

        touchMap.setPixel(x, y, true);
        if (x > 0) {
            touchMap.setPixel(x - 1, y, true);
        }
        if (y > 0) {
            touchMap.setPixel(x, y - 1, true);
        }
        if (x < IMAGE_SIZE - 1) {
            touchMap.setPixel(x + 1, y, true);
        }
        if (y < IMAGE_SIZE - 1) {
            touchMap.setPixel(x, y + 1, true);
        }
    }

//    public void drawPixel(PointInImageSpace point, int color) {
//        int x = (int) (point.x);
//        int y = (int) (point.y);
//        int oldPixel = gammaHeatMap.getPixel(x, y);
//        int newPixel = compositeArgb(oldPixel, color);
//        gammaHeatMap.setPixel(
//                x,
//                y,
//                newPixel
//        );
//    }

    public static int compositeArgb(int colorUnder, int colorOver) {
        final int overAlpha = Channel.ALPHA.getValue(colorOver);
        final float underAlpha = Channel.ALPHA.getValueAsPercent(colorUnder);

        if (overAlpha == 255 || underAlpha == 0) {
            return colorOver;
        } else if (overAlpha > 0) {
            final int overRed = Channel.RED.getValue(colorOver);
            final int underRed = Channel.RED.getValue(colorUnder);
            final int overGreen = Channel.GREEN.getValue(colorOver);
            final int underGreen = Channel.GREEN.getValue(colorUnder);
            final int overBlue = Channel.BLUE.getValue(colorOver);
            final int underBlue = Channel.BLUE.getValue(colorUnder);

            float overAlphaFloat = Channel.ALPHA.getValueAsPercent(colorOver);
            float overAlphaInvertedFloat = 1 - overAlphaFloat;
            final float newAlphaFloat = overAlphaFloat + (underAlpha * overAlphaInvertedFloat);
            final int newAlpha = Math.round(newAlphaFloat * 255);
            final float newAlphaFloatComp = Math.max(1f, 1f / newAlphaFloat);
            final int newRed = calcNewColor(overRed, overAlphaFloat, overAlphaInvertedFloat, underRed, underAlpha, newAlphaFloatComp);
            final int newGreen = calcNewColor(overGreen, overAlphaFloat, overAlphaInvertedFloat, underGreen, underAlpha, newAlphaFloatComp);
            final int newBlue = calcNewColor(overBlue, overAlphaFloat, overAlphaInvertedFloat, underBlue, underAlpha, newAlphaFloatComp);

            return getRgb(newAlpha, newRed, newGreen, newBlue);
        } else {
            return colorUnder;
        }
    }

    private static final int BITS_8 = 0xFF;
    public static int getRgb(int alpha, int red, int green, int blue) {
        return (alpha & BITS_8) << Channel.ALPHA.getBit() | (red & BITS_8) << Channel.RED.getBit()
                | (green & BITS_8) << Channel.GREEN.getBit() | blue & BITS_8;
    }

    private static int calcNewColor(int overColor, float overAlpha, float overAlphaInverted, int underColor, float underAlpha, float newAlphaFloatComp) {
        return Math.round(((overColor * overAlpha) + (underColor * underAlpha * overAlphaInverted)) *
                newAlphaFloatComp);
    }

    @Nonnull
    private PointInImageSpace getPointInImageSpace(Track.Segment.Point point) {
        int zoom = tileCoordinate.z();
        double lon = point.lonLatCoordinate().longitude();
        double lat = point.lonLatCoordinate().latitude();

        double xtile = ( (lon + 180) / 360 * (1<<zoom) ) ;
        double yTile = ( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;

        xtile = (xtile % 1) * IMAGE_SIZE;
        yTile = (yTile % 1) * IMAGE_SIZE;

        return new PointInImageSpace(xtile, yTile);
    }
//
//    public boolean isInside(Track.Segment.Point point) {
//        int zoom = tileCoordinate.z();
//        double lon = point.lonLatCoordinate().longitude();
//        double lat = point.lonLatCoordinate().latitude();
//
//        double xtile = ( (lon + 180) / 360 * (1<<zoom) ) ;
//        double yTile = ( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
//
//        int xTileCoord = (int) Math.floor(xtile);
//        int yTileCoord = (int) Math.floor(yTile);
//        return tileCoordinate.x() == xTileCoord && tileCoordinate.y() == yTileCoord;
//    }

    public record PointInImageSpace(double x, double y) {}

//    public record LineInImageSpace(PointInImageSpace start, PointInImageSpace end) {}

//    public void drawLine(Track.Segment.Point a, Track.Segment.Point b) {
//        LineInImageSpace line = new LineInImageSpace(
//                getPointInImageSpace(a),
//                getPointInImageSpace(b)
//        );
//        double dx = line.end.x - line.start.x;
//        double dy = line.end.y - line.start.y;
//        double slope = dy / dx;
//
//        if (Double.isInfinite(slope)) {
//            int yMin = (int) Math.ceil(Math.min(line.start.y, line.end.y));
//            int yMax = (int) Math.ceil(Math.max(line.start.y, line.end.y));
//            double x = line.end.x;
//            for (int y = yMin; y <= yMax; y++) {
//                //drawPixelIfInside(x, y, 0x44FFFF00);
//            }
//        } else {
//            int xMin = (int) Math.floor(Math.min(line.start.x, line.end.x));
//            int xMax = (int) Math.ceil(Math.max(line.start.x, line.end.x));
//
//            for (double p = 0; p < 1; p+=0.01) {
//                double x = line.start.x + dx * p;
//                double y = slope * (x-line.start.x) + line.start.y;
//                //drawPixelIfInside(x, y, 0x4400FFFF);
//            }
//        }
//
//        add(line.start, 1);
//    }
//
//    private void drawPixelIfInside(double x, double y, int color) {
//        PointInImageSpace point = new PointInImageSpace(x, y);
//        if (point.x >= 0 && point.x < IMAGE_SIZE && point.y >= 0 && point.y < IMAGE_SIZE) {
//            drawPixel(point, color);
//        }
//    }
}
