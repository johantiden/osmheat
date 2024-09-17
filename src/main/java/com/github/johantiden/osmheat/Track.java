package com.github.johantiden.osmheat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record Track(List<Segment> segments) {

    public static Collection<Track> from(Document gpxFile) {
        List<Track> tracks = new ArrayList<>();
        Element gpx = gpxFile.getDocumentElement();
        NodeList trks = gpx.getElementsByTagName("trk");
        for (int i = 0; i < trks.getLength(); i++) {
            Node trk = trks.item(i);
            if (trk instanceof Element trkElement) {
                Track track = Track.from(trkElement);
                tracks.add(track);
            }
        }
        return tracks;
    }

    private static Track from(Element trkElement) {
        NodeList trksegs = trkElement.getElementsByTagName("trkseg");
        List<Segment> segments = new ArrayList<>();

        for (int j = 0; j < trksegs.getLength(); j++) {
            Node seg = trksegs.item(j);
            if (seg instanceof Element segElement) {
                Segment segment = Segment.from(segElement);
                segments.add(segment);
                break;
            }
        }
        return new Track(segments);
    }

    boolean isEmpty() {
        return segments.isEmpty();
    }

    public record Segment(List<Point> points) {
        public static Segment from(Element segElement) {
            List<Point> points = new ArrayList<>();
            NodeList trkpts = segElement.getElementsByTagName("trkpt");

            for (int i = 0; i < trkpts.getLength(); i++) {
                Node trkpt = trkpts.item(i);
                if (trkpt instanceof Element trkptElement) {
                    points.add(Point.from(trkptElement));
                }
            }
            return new Segment(points);
        }

        public record Point(LonLatCoordinate lonLatCoordinate) {
            public static Point from(Element trkptElement) {
                String lat = trkptElement.getAttribute("lat");
                String lon = trkptElement.getAttribute("lon");
                return new Point(
                        new LonLatCoordinate(
                                Double.parseDouble(lon),
                                Double.parseDouble(lat))
                );
            }
        }
    }
}
