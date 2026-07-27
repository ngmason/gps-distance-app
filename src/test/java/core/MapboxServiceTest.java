package core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapboxServiceTest {

    private static final String TEST_TOKEN = "pk.test_token";

    // Package-private constructor bypasses loadToken() — no config.properties needed.
    private static final MapboxService SERVICE = new MapboxService(TEST_TOKEN);

    private static final Location PARIS    = new Location("Paris",    48.8566,  2.3522);
    private static final Location LONDON   = new Location("London",   51.5074, -0.1278);
    private static final Location BRUSSELS = new Location("Brussels", 50.8503,  4.3517);

    private static final String POLY = "encodedPolyABC";

    // Expected static-map base URL — changing this is a breaking contract change.
    private static final String EXPECTED_ENDPOINT =
            "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/";

    // -------------------------------------------------------------------------
    // Two-waypoint route — covers all structural contract obligations
    // -------------------------------------------------------------------------

    @Test
    void buildStaticMapUrl_twoWaypoints_containsAllStructuralComponents() {
        double centerLon = 1.1127, centerLat = 50.1820;
        int zoom = 6;
        String url = SERVICE.buildStaticMapUrl(POLY, List.of(PARIS, LONDON),
                centerLon, centerLat, zoom);

        // Mapbox static-map endpoint
        assertTrue(url.startsWith(EXPECTED_ENDPOINT),
                "URL must start with the Mapbox static-map endpoint");

        // Access token
        assertTrue(url.contains("access_token=" + TEST_TOKEN),
                "URL must contain the injected access token");

        // Encoded polyline embedded in the path overlay
        assertTrue(url.contains("path-5+ff0000-0.8(" + POLY + ")"),
                "URL must embed the encoded polyline in the path overlay");

        // Image dimensions
        assertTrue(url.contains("600x400"),
                "URL must specify 600x400 image dimensions");

        // Center coordinates and zoom — %f,%f,%d matches production String.format call
        assertTrue(url.contains(String.format("%f,%f,%d", centerLon, centerLat, zoom)),
                "URL must contain centerLon,centerLat,zoom");
    }

    // -------------------------------------------------------------------------
    // Two-waypoint pin styling — start red, end blue
    // -------------------------------------------------------------------------

    @Test
    void buildStaticMapUrl_twoWaypoints_startPinIsRedEndPinIsBlue() {
        String url = SERVICE.buildStaticMapUrl(POLY, List.of(PARIS, LONDON), 1.11, 50.18, 6);

        // Start pin: red (ff0000), longitude first then latitude — matches %f,%f in production
        assertTrue(url.contains(String.format("pin-s+ff0000(%f,%f)",
                        PARIS.getLongitude(), PARIS.getLatitude())),
                "first waypoint must have a red (ff0000) pin at its coordinates");

        // End pin: blue (0000ff)
        assertTrue(url.contains(String.format("pin-s+0000ff(%f,%f)",
                        LONDON.getLongitude(), LONDON.getLatitude())),
                "last waypoint must have a blue (0000ff) pin at its coordinates");
    }

    // -------------------------------------------------------------------------
    // Three-waypoint route — middle pin is orange; start and end unchanged
    // -------------------------------------------------------------------------

    @Test
    void buildStaticMapUrl_threeWaypoints_pinsAreRedOrangeBlue() {
        String url = SERVICE.buildStaticMapUrl(POLY, List.of(PARIS, BRUSSELS, LONDON),
                3.0, 50.5, 6);

        // Start pin: red
        assertTrue(url.contains(String.format("pin-s+ff0000(%f,%f)",
                        PARIS.getLongitude(), PARIS.getLatitude())),
                "first waypoint must have a red (ff0000) pin");

        // Middle pin: orange (ff8c00)
        assertTrue(url.contains(String.format("pin-s+ff8c00(%f,%f)",
                        BRUSSELS.getLongitude(), BRUSSELS.getLatitude())),
                "middle waypoint must have an orange (ff8c00) pin");

        // End pin: blue
        assertTrue(url.contains(String.format("pin-s+0000ff(%f,%f)",
                        LONDON.getLongitude(), LONDON.getLatitude())),
                "last waypoint must have a blue (0000ff) pin");
    }

    // -------------------------------------------------------------------------
    // Empty polyline — method must not throw; overlay is embedded as-is
    // -------------------------------------------------------------------------

    @Test
    void buildStaticMapUrl_emptyPolyline_embeddedWithoutError() {
        String url = SERVICE.buildStaticMapUrl("", List.of(PARIS, LONDON), 0.0, 0.0, 1);
        assertNotNull(url, "URL must not be null for an empty polyline");
        assertTrue(url.contains("path-5+ff0000-0.8()"),
                "empty polyline must produce a path overlay with empty parentheses");
    }
}
