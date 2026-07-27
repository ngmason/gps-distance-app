package core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteRouteRepositoryTest {

    // JUnit 5 injects a fresh directory per test when @TempDir is on an instance field.
    @TempDir
    Path tempDir;

    private static final Location PARIS    = new Location("Paris",    48.8566,  2.3522);
    private static final Location LONDON   = new Location("London",   51.5074, -0.1278);
    private static final Location BRUSSELS = new Location("Brussels", 50.8503,  4.3517);

    private static Route twoStop(String name) {
        return new Route(List.of(PARIS, LONDON), 60.0, name);
    }

    private static Route threeStop(String name) {
        return new Route(List.of(PARIS, BRUSSELS, LONDON), 60.0, name);
    }

    private Path dbPath() {
        return tempDir.resolve("routes.db");
    }

    // -------------------------------------------------------------------------
    // Load tests
    // -------------------------------------------------------------------------

    @Test
    void loadRoutes_emptyDatabase_returnsEmptyList() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            List<Route> routes = repo.loadRoutes();
            assertNotNull(routes);
            assertTrue(routes.isEmpty());
        }
    }

    @Test
    void saveAndLoad_multipleRoutes_loadsInInsertionOrder() throws SQLException {
        // loadRoutes() uses ORDER BY r.id ASC; AUTOINCREMENT IDs are monotonically
        // increasing, so insertion order == id order.
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("A"));
            repo.saveRoute(twoStop("B"));
            repo.saveRoute(twoStop("C"));

            List<Route> routes = repo.loadRoutes();
            assertEquals(3, routes.size());
            assertEquals("A", routes.get(0).getName());
            assertEquals("B", routes.get(1).getName());
            assertEquals("C", routes.get(2).getName());
        }
    }

    // -------------------------------------------------------------------------
    // Round-trip tests
    // -------------------------------------------------------------------------

    @Test
    void saveAndLoad_twoWaypointRoute_roundTripsAllFields() throws SQLException {
        Route original = twoStop("Paris–London");
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(original);
            List<Route> loaded = repo.loadRoutes();

            assertEquals(1, loaded.size());
            Route r = loaded.get(0);
            assertEquals("Paris–London", r.getName());
            assertEquals(original.getDistanceKm(),    r.getDistanceKm(),    1e-9);
            assertEquals(original.getDistanceMiles(), r.getDistanceMiles(), 1e-9);
            assertEquals(original.getTimeHrs(),       r.getTimeHrs(),       1e-9);

            List<Location> wps = r.getWaypoints();
            assertEquals(2, wps.size());
            assertWaypoint(wps.get(0), "Paris",   48.8566,  2.3522);
            assertWaypoint(wps.get(1), "London",  51.5074, -0.1278);
        }
    }

    @Test
    void saveAndLoad_threeWaypointRoute_preservesAllWaypointsInOrder() throws SQLException {
        Route original = threeStop("three-city");
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(original);
            List<Route> loaded = repo.loadRoutes();

            assertEquals(1, loaded.size());
            List<Location> wps = loaded.get(0).getWaypoints();
            assertEquals(3, wps.size());
            assertWaypoint(wps.get(0), "Paris",    48.8566,  2.3522);
            assertWaypoint(wps.get(1), "Brussels", 50.8503,  4.3517);
            assertWaypoint(wps.get(2), "London",   51.5074, -0.1278);
        }
    }

    // -------------------------------------------------------------------------
    // saveRoute constraint test
    // -------------------------------------------------------------------------

    @Test
    void saveRoute_duplicateName_throwsSQLException() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("Route 1"));
            assertThrows(SQLException.class, () -> repo.saveRoute(twoStop("Route 1")));

            // The first route must still be intact — the failed save must not corrupt it.
            List<Route> routes = repo.loadRoutes();
            assertEquals(1, routes.size());
            assertEquals("Route 1", routes.get(0).getName());
            List<Location> wps = routes.get(0).getWaypoints();
            assertEquals(2, wps.size());
            assertWaypoint(wps.get(0), "Paris",  48.8566,  2.3522);
            assertWaypoint(wps.get(1), "London", 51.5074, -0.1278);
        }
    }

    // -------------------------------------------------------------------------
    // deleteRoute tests
    // -------------------------------------------------------------------------

    @Test
    void deleteRoute_existingName_returnsTrueAndRouteIsGone() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("Route 1"));
            assertTrue(repo.deleteRoute("Route 1"));
            assertTrue(repo.loadRoutes().isEmpty());
        }
    }

    @Test
    void deleteRoute_unknownName_returnsFalse() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            assertFalse(repo.deleteRoute("Nonexistent"));
        }
    }

    @Test
    void deleteRoute_isCaseInsensitive() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("Route One"));
            assertTrue(repo.deleteRoute("route one"));
            assertTrue(repo.loadRoutes().isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // replaceRoute tests
    // -------------------------------------------------------------------------

    @Test
    void replaceRoute_updatesNameAndWaypoints() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("Route 1"));
            repo.replaceRoute("Route 1", threeStop("Route 1 Updated"));

            List<Route> routes = repo.loadRoutes();
            assertEquals(1, routes.size());
            assertEquals("Route 1 Updated", routes.get(0).getName());
            List<Location> wps = routes.get(0).getWaypoints();
            assertEquals(3, wps.size());
            assertWaypoint(wps.get(0), "Paris",    48.8566,  2.3522);
            assertWaypoint(wps.get(1), "Brussels", 50.8503,  4.3517);
            assertWaypoint(wps.get(2), "London",   51.5074, -0.1278);
        }
    }

    @Test
    void replaceRoute_isCaseInsensitive() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("Route One"));
            repo.replaceRoute("route one", threeStop("Route One"));

            List<Route> routes = repo.loadRoutes();
            assertEquals(1, routes.size());
            List<Location> wps = routes.get(0).getWaypoints();
            assertEquals(3, wps.size());
            assertWaypoint(wps.get(0), "Paris",    48.8566,  2.3522);
            assertWaypoint(wps.get(1), "Brussels", 50.8503,  4.3517);
            assertWaypoint(wps.get(2), "London",   51.5074, -0.1278);
        }
    }

    @Test
    void replaceRoute_unknownName_throwsSQLException() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            assertThrows(SQLException.class,
                    () -> repo.replaceRoute("Nonexistent", twoStop("x")));
        }
    }

    @Test
    void replaceRoute_rollbackOnFailure_preservesOriginalData() throws SQLException {
        try (SQLiteRouteRepository repo = new SQLiteRouteRepository(dbPath())) {
            repo.saveRoute(twoStop("Route A"));
            repo.saveRoute(twoStop("Route B"));

            // Inside replaceRoute("Route A", ...named "Route B"):
            //   1. DELETE "Route A" — succeeds
            //   2. INSERT "Route B" — fails (UNIQUE constraint; "Route B" already exists)
            //   3. Transaction rolls back, restoring "Route A"
            assertThrows(SQLException.class,
                    () -> repo.replaceRoute("Route A", twoStop("Route B")));

            List<Route> routes = repo.loadRoutes();
            assertEquals(2, routes.size(),
                    "both routes must exist after rollback");

            Route routeA = routes.stream()
                    .filter(r -> r.getName().equals("Route A"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Route A is missing after rollback"));
            assertEquals(2, routeA.getWaypoints().size());
            assertWaypoint(routeA.getWaypoints().get(0), "Paris",  48.8566,  2.3522);
            assertWaypoint(routeA.getWaypoints().get(1), "London", 51.5074, -0.1278);

            Route routeB = routes.stream()
                    .filter(r -> r.getName().equals("Route B"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Route B is missing after rollback"));
            assertEquals(2, routeB.getWaypoints().size());
            assertWaypoint(routeB.getWaypoints().get(0), "Paris",  48.8566,  2.3522);
            assertWaypoint(routeB.getWaypoints().get(1), "London", 51.5074, -0.1278);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static void assertWaypoint(Location loc, String name, double lat, double lon) {
        assertEquals(name, loc.getName());
        assertEquals(lat,  loc.getLatitude(),  1e-9);
        assertEquals(lon,  loc.getLongitude(), 1e-9);
    }
}
