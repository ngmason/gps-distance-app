package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }
}
