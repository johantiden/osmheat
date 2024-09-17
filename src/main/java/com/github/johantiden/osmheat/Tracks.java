package com.github.johantiden.osmheat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.johantiden.osmheat.cache.FileCache;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Tracks {
    private final FileCache<List<Track>> tracksCache;
    private final TracksRaw tracksRaw;

    public Tracks(JsonMapper jsonMapper, CloseableHttpClient closeableHttpClient) {
        this.tracksRaw = new TracksRaw(closeableHttpClient);
        this.tracksCache = new FileCache<>(
                Path.of("/tmp/osmheat/tracks"),
                jsonMapper::writeValueAsBytes,
                bytes -> jsonMapper.readValue(bytes, new TypeReference<List<Track>>() {
                }), ".json"
        );
    }

    @Nonnull
    public List<Track> getPublicTracks(RenderingController.TileCoordinate tileCoordinate) throws Exception {
        return tracksCache.get(Tiles.asPath(tileCoordinate), () -> {
            List<Track> gpxs = new ArrayList<>();
            Collection<Track> tracks = new ArrayList<>();
            int page = 0;
            do {
                Document gpx = tracksRaw.getGpx(tileCoordinate, page);
                tracks = Track.from(gpx);
                gpxs.addAll(tracks);
                page++;
            }
            while (!tracks.isEmpty());

            return gpxs.stream()
                    .filter(track -> !track.isEmpty())
                    .toList();
        });
    }
}
