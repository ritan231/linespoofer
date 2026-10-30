package my.shanunid.linespoofer.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocationCoordinatesParser {

    private static final Pattern COORD_PATTERN = Pattern.compile(
        "([-+]?\\d{1,2}(?:\\.\\d+)?)[,\\s]+([-+]?\\d{1,3}(?:\\.\\d+)?)"
    );

    public static class Coordinates {
        public final double latitude;
        public final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String toString() {
            return latitude + ", " + longitude;
        }
    }

    public static Coordinates parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = COORD_PATTERN.matcher(input);
        while (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                if (lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0) {
                    return new Coordinates(lat, lng);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
