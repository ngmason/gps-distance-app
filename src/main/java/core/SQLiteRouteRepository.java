package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SQLiteRouteRepository implements AutoCloseable {

    private final Connection conn;

    public SQLiteRouteRepository(Path dbPath) throws SQLException {
        Path parent = dbPath.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new SQLException("Cannot create database directory: " + parent, e);
            }
        }
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
        applyPragmas();
        initSchema();
    }

    private void applyPragmas() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS routes (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        name         TEXT    NOT NULL UNIQUE,
                        distance_km  REAL    NOT NULL,
                        distance_mi  REAL    NOT NULL,
                        time_hrs     REAL    NOT NULL,
                        speed_mph    REAL    NOT NULL
                    )""");
            st.execute("""
                    CREATE TABLE IF NOT EXISTS waypoints (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        route_id  INTEGER NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
                        seq       INTEGER NOT NULL,
                        name      TEXT    NOT NULL,
                        latitude  REAL    NOT NULL,
                        longitude REAL    NOT NULL,
                        UNIQUE(route_id, seq)
                    )""");
        }
    }

    public List<Route> loadRoutes() throws SQLException {
        String sql = """
                SELECT r.id, r.name, r.distance_km, r.distance_mi, r.time_hrs,
                       w.seq, w.name AS wp_name, w.latitude, w.longitude
                FROM routes r
                JOIN waypoints w ON w.route_id = r.id
                ORDER BY r.id ASC, w.seq ASC
                """;
        List<Route> routes = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            long currentId = -1;
            String currentName = null;
            double currentDistKm = 0, currentDistMi = 0, currentTimeHrs = 0;
            List<Location> currentWaypoints = new ArrayList<>();

            while (rs.next()) {
                long id = rs.getLong("id");
                if (id != currentId) {
                    if (currentId != -1) {
                        routes.add(new Route(currentWaypoints, currentDistKm, currentDistMi, currentTimeHrs, currentName));
                    }
                    currentId = id;
                    currentName = rs.getString("name");
                    currentDistKm = rs.getDouble("distance_km");
                    currentDistMi = rs.getDouble("distance_mi");
                    currentTimeHrs = rs.getDouble("time_hrs");
                    currentWaypoints = new ArrayList<>();
                }
                currentWaypoints.add(new Location(
                        rs.getString("wp_name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")));
            }
            if (currentId != -1) {
                routes.add(new Route(currentWaypoints, currentDistKm, currentDistMi, currentTimeHrs, currentName));
            }
        }
        return routes;
    }

    public void saveRoute(Route route) throws SQLException {
        List<Location> waypoints = route.getWaypoints();
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Route must have at least 2 waypoints");
        }
        double speedMph = route.getTimeHrs() > 0
                ? route.getDistanceMiles() / route.getTimeHrs()
                : 0.0;

        conn.setAutoCommit(false);
        try (PreparedStatement insRoute = conn.prepareStatement(
                     "INSERT INTO routes (name, distance_km, distance_mi, time_hrs, speed_mph) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insWp = conn.prepareStatement(
                     "INSERT INTO waypoints (route_id, seq, name, latitude, longitude) VALUES (?, ?, ?, ?, ?)")) {

            insRoute.setString(1, route.getName());
            insRoute.setDouble(2, route.getDistanceKm());
            insRoute.setDouble(3, route.getDistanceMiles());
            insRoute.setDouble(4, route.getTimeHrs());
            insRoute.setDouble(5, speedMph);
            insRoute.executeUpdate();

            long routeId;
            try (ResultSet keys = insRoute.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to retrieve generated route ID");
                }
                routeId = keys.getLong(1);
            }

            for (int i = 0; i < waypoints.size(); i++) {
                Location loc = waypoints.get(i);
                insWp.setLong(1, routeId);
                insWp.setInt(2, i);
                insWp.setString(3, loc.getName());
                insWp.setDouble(4, loc.getLatitude());
                insWp.setDouble(5, loc.getLongitude());
                insWp.addBatch();
            }
            insWp.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void replaceRoute(String existingName, Route replacement) throws SQLException {
        List<Location> waypoints = replacement.getWaypoints();
        if (waypoints.size() < 2) {
            throw new IllegalArgumentException("Route must have at least 2 waypoints");
        }
        double speedMph = replacement.getTimeHrs() > 0
                ? replacement.getDistanceMiles() / replacement.getTimeHrs()
                : 0.0;

        conn.setAutoCommit(false);
        try (PreparedStatement del = conn.prepareStatement(
                     "DELETE FROM routes WHERE name = ? COLLATE NOCASE");
             PreparedStatement insRoute = conn.prepareStatement(
                     "INSERT INTO routes (name, distance_km, distance_mi, time_hrs, speed_mph) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insWp = conn.prepareStatement(
                     "INSERT INTO waypoints (route_id, seq, name, latitude, longitude) VALUES (?, ?, ?, ?, ?)")) {

            del.setString(1, existingName);
            int deleted = del.executeUpdate();
            if (deleted == 0) {
                throw new SQLException("Route not found for overwrite: " + existingName);
            }

            insRoute.setString(1, replacement.getName());
            insRoute.setDouble(2, replacement.getDistanceKm());
            insRoute.setDouble(3, replacement.getDistanceMiles());
            insRoute.setDouble(4, replacement.getTimeHrs());
            insRoute.setDouble(5, speedMph);
            insRoute.executeUpdate();

            long routeId;
            try (ResultSet keys = insRoute.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Failed to retrieve generated route ID");
                routeId = keys.getLong(1);
            }

            for (int i = 0; i < waypoints.size(); i++) {
                Location loc = waypoints.get(i);
                insWp.setLong(1, routeId);
                insWp.setInt(2, i);
                insWp.setString(3, loc.getName());
                insWp.setDouble(4, loc.getLatitude());
                insWp.setDouble(5, loc.getLongitude());
                insWp.addBatch();
            }
            insWp.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public boolean deleteRoute(String routeName) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement("DELETE FROM routes WHERE name = ? COLLATE NOCASE")) {
            st.setString(1, routeName);
            return st.executeUpdate() > 0;
        }
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
