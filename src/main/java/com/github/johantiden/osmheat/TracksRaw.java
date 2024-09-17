package com.github.johantiden.osmheat;

import com.github.johantiden.osmheat.cache.FileCache;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static com.github.johantiden.osmheat.Tiles.asPath;

public class TracksRaw {
    private final CloseableHttpClient closeableHttpClient;
    private final FileCache<byte[]> tracksCache;

    public TracksRaw(CloseableHttpClient closeableHttpClient) {
        this.closeableHttpClient = closeableHttpClient;
        tracksCache = new FileCache<>(
                Path.of("/tmp/osmheat/tracks_raw"),
                b -> b,
                b -> b, ".gpx"
        );
    }

    @Nonnull
    private static String createGpxUrl(double left, double bottom, double right, double top, int page) {
        return "https://api.openstreetmap.org/api/0.6/trackpoints?bbox=%.8f,%.8f,%.8f,%.8f&page=%s".formatted(
                left, bottom, right, top, page
        );
    }

    public Document getGpx(RenderingController.TileCoordinate tileCoordinate, int page) throws Exception {
        String path = asPath(tileCoordinate, page);

        tracksCache.get(path, () -> {
            LonLatCoordinate topLeft = tileCoordinate.getTopLeftPaddedWithKernelSize();
            LonLatCoordinate bottomRight = tileCoordinate.getBottomRightPaddedWithKernelSize();
            String gpxUrl = createGpxUrl(
                    topLeft.longitude(),
                    bottomRight.latitude(),
                    bottomRight.longitude(),
                    topLeft.latitude(),
                    page);
            return closeableHttpClient.execute(new HttpGet(gpxUrl), new AbstractHttpClientResponseHandler<byte[]>() {
                @Override
                public byte[] handleEntity(HttpEntity entity) throws IOException {
                    return entity.getContent().readAllBytes();
                }
            });
        });

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = tracksCache.getFullPath(path).toFile();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();

        System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

        return doc;
    }
}
