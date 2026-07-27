package core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RouteTest {

    // Representative coordinates used across multiple tests
    private static final Location PARIS  = new Location("Paris",  48.8566,  2.3522);
    private static final Location LONDON = new Location("London", 51.5074, -0.1278);
    // Brussels is off the Paris–London straight line, so A→B→C > A→C by haversine
    private static final Location BRUSSELS = new Location("Brussels", 50.8503, 4.3517);

    // -------------------------------------------------------------------------
    // haversine()
    // -------------------------------------------------------------------------

    @Test
    void haversine_samePoint_returnsZero() {
        double[] result = Route.haversine(48.8566, 2.3522, 48.8566, 2.3522);
        assertEquals(0.0, result[0], 1e-10, "km should be 0 for identical points");
        assertEquals(0.0, result[1], 1e-10, "miles should be 0 for identical points");
    }

    @Test
    void haversine_parisToLondon_returnsExpectedKm() {
        double[] result = Route.haversine(
                PARIS.getLatitude(),  PARIS.getLongitude(),
                LONDON.getLatitude(), LONDON.getLongitude());
        assertEquals(343.0, result[0], 1.0,
                "Paris–London straight-line distance should be ~343 km");
    }

    @Test
    void haversine_milesIsKmTimesConversionFactor() {
        double[] result = Route.haversine(
                PARIS.getLatitude(),  PARIS.getLongitude(),
                LONDON.getLatitude(), LONDON.getLongitude());
        assertEquals(result[0] * 0.621371, result[1], 1e-9,
                "result[1] must equal result[0] * 0.621371");
    }

    @Test
    void haversine_isSymmetric() {
        double[] ab = Route.haversine(
                PARIS.getLatitude(),  PARIS.getLongitude(),
                LONDON.getLatitude(), LONDON.getLongitude());
        double[] ba = Route.haversine(
                LONDON.getLatitude(), LONDON.getLongitude(),
                PARIS.getLatitude(),  PARIS.getLongitude());
        assertEquals(ab[0], ba[0], 1e-10, "haversine(A,B) must equal haversine(B,A)");
    }

    // -------------------------------------------------------------------------
    // calculateTime()
    // -------------------------------------------------------------------------

    @Test
    void calculateTime_normal_returnsDistanceDividedBySpeed() {
        assertEquals(1.0, Route.calculateTime(60.0, 60.0), 1e-10,
                "60 miles at 60 mph should take exactly 1 hour");
    }

    @Test
    void calculateTime_zeroSpeed_returnsZero() {
        assertEquals(0.0, Route.calculateTime(100.0, 0.0), 0.0,
                "zero-speed guard should return 0 without throwing");
    }

    // -------------------------------------------------------------------------
    // Route constructor math
    // -------------------------------------------------------------------------

    @Test
    void twoWaypointRoute_distanceMatchesHaversine() {
        Route route = new Route(List.of(PARIS, LONDON), 60.0, "Paris–London");
        double expected = Route.haversine(
                PARIS.getLatitude(),  PARIS.getLongitude(),
                LONDON.getLatitude(), LONDON.getLongitude())[0];
        assertEquals(expected, route.getDistanceKm(), 1e-9,
                "two-waypoint Route distance must equal direct haversine result");
    }

    @Test
    void threeWaypointRoute_distanceIsSumOfLegs() {
        // Paris → Brussels → London is longer than Paris → London directly
        Route route = new Route(List.of(PARIS, BRUSSELS, LONDON), 60.0, "three-stop");
        double leg1 = Route.haversine(
                PARIS.getLatitude(),    PARIS.getLongitude(),
                BRUSSELS.getLatitude(), BRUSSELS.getLongitude())[0];
        double leg2 = Route.haversine(
                BRUSSELS.getLatitude(), BRUSSELS.getLongitude(),
                LONDON.getLatitude(),   LONDON.getLongitude())[0];
        assertEquals(leg1 + leg2, route.getDistanceKm(), 1e-9,
                "three-waypoint distance must be the sum of the two legs, not haversine(Paris,London)");
        assertTrue(route.getDistanceKm() > Route.haversine(
                PARIS.getLatitude(), PARIS.getLongitude(),
                LONDON.getLatitude(), LONDON.getLongitude())[0],
                "Paris→Brussels→London must exceed the direct Paris–London distance");
    }

    // -------------------------------------------------------------------------
    // Waypoint access
    // -------------------------------------------------------------------------

    @Test
    void getWaypoints_returnsUnmodifiableList() {
        Route route = new Route(List.of(PARIS, LONDON), 60.0, "test");
        assertThrows(UnsupportedOperationException.class,
                () -> route.getWaypoints().add(BRUSSELS),
                "getWaypoints() must return an unmodifiable list");
    }

    @Test
    void getStartAndEnd_delegateToFirstAndLast() {
        Route route = new Route(List.of(PARIS, BRUSSELS, LONDON), 60.0, "test");
        assertSame(route.getWaypoints().get(0), route.getStart(),
                "getStart() must return the first waypoint");
        assertSame(route.getWaypoints().get(2), route.getEnd(),
                "getEnd() must return the last waypoint");
    }
}
