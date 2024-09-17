package com.github.johantiden.osmheat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static com.github.johantiden.osmheat.Tile.IMAGE_SIZE;

@RestController
public class RenderingController {
    private static final Logger logger = LoggerFactory.getLogger(RenderingController.class);
    private final Tiles tiles;

    // https://api.openstreetmap.org/api/0.6/trackpoints?bbox=0,51.5,0.25,51.75&page=0
    // https://tile.openstreetmap.org/17/72167/38557.png

    // https://tile.openstreetmap.org/17/72173/38554.png
    // https://tile.openstreetmap.org/16/36079/19280.png
    // https://tile.openstreetmap.org/14/9026/4818.png
    // http://localhost:8080/14/9026/4818.png


    public RenderingController(JsonMapper jsonMapper, CloseableHttpClient closeableHttpClient) {
        this.tiles = new Tiles(jsonMapper, closeableHttpClient);
    }

    @GetMapping(value = "/{z}/{x}/{y}.png", produces = "image/png")
    public byte[] tile(
        @PathVariable int z,
        @PathVariable int x,
        @PathVariable int y
    ) throws Exception {
        logger.info("Rendering tile {}/{}/{}", z, x, y);
        if (z < 14) {
            logger.info("z < 14 skipping...");
            return new Image(1,1).toPng();
        }

        TileCoordinate tileCoordinate = new TileCoordinate(z, x, y);

        return tiles.renderTile(tileCoordinate);
    }

    public record TileCoordinate(int z, int x, int y) {
        int getKernelSize() {
            return 16;
        }

        // Inspired by https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
        public LonLatCoordinate getTopLeftPaddedWithKernelSize() {
            double n = Math.pow(2, z);
            double kernelPaddingInTileSpace = getKernelSize() / (double) IMAGE_SIZE;
            double longitude = (x - kernelPaddingInTileSpace*2) / n * 360 - 180;
            double latitudeRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (y- kernelPaddingInTileSpace*2) / n)));
            double latitude = latitudeRad * 180.0 / Math.PI;
            return new LonLatCoordinate(longitude, latitude);
        }

        public LonLatCoordinate getBottomRightPaddedWithKernelSize() {
            return new TileCoordinate(z, x+1, y+1)
                    .getTopLeftPaddedWithKernelSize();
        }
    }
}
