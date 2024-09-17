package com.github.johantiden.osmheat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.johantiden.osmheat.cache.FileCache;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.nio.file.Path;
import java.util.List;

public class Tiles {
    private final FileCache<byte[]> tileCache;
    private final Tracks tracks;

    public Tiles(JsonMapper jsonMapper, CloseableHttpClient closeableHttpClient) {
        tracks = new Tracks(jsonMapper, closeableHttpClient);
        tileCache = new FileCache<>(
                Path.of("/tmp/osmheat/tiles"),
                b -> b,
                b -> b,
                ".png"
        );
    }

    @Nonnull
    public byte[] renderTile(RenderingController.TileCoordinate tileCoordinate) throws Exception {
        return tileCache.get(asPath(tileCoordinate), () -> {
            List<Track> gpxs = tracks.getPublicTracks(tileCoordinate);
            System.out.println(gpxs.size());

            Tile tile = new Tile(tileCoordinate);
            for (Track track : gpxs) {
                for (Track.Segment segment : track.segments()) {
                    for (Track.Segment.Point point : segment.points()) {
                        tile.add(point);
                    }
//
//                    for (int i = 0; i < segment.points().size() - 1; i++) {
//                        Track.Segment.Point a = segment.points().get(i);
//                        Track.Segment.Point b = segment.points().get(i + 1);
//                        tile.drawLine(a, b);
//                    }
                }
            }
            return tile.render().toPng();
        });
    }

    @Nonnull
    public static String asPath(RenderingController.TileCoordinate tileCoordinate) {
        return "%s/%s/%s".formatted(tileCoordinate.z(), tileCoordinate.x(), tileCoordinate.y());
    }
    @Nonnull
    public static String asPath(RenderingController.TileCoordinate tileCoordinate, int page) {
        return "%s/%s/%s/%s".formatted(tileCoordinate.z(), tileCoordinate.x(), tileCoordinate.y(), page);
    }
}
