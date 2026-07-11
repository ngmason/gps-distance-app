package core;

import java.nio.file.Path;

public final class AppPaths {

    private static final String APP_DIR = "GpsApp";
    private static final String DB_FILE = "routes.db";

    private AppPaths() {}

    public static Path resolveDbPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = (localAppData != null && !localAppData.isBlank())
                ? Path.of(localAppData)
                : Path.of(System.getProperty("user.home"), "AppData", "Local");
        return base.resolve(APP_DIR).resolve(DB_FILE);
    }
}
