package core;

import java.nio.file.Path;

public final class AppPaths {

    private static final String APP_DIR = "GpsApp";
    private static final String DB_FILE = "routes.db";

    private AppPaths() {}

    public static Path resolveDbPath() {
        return resolvePath(System.getenv("LOCALAPPDATA"), System.getProperty("user.home"));
    }

    static Path resolvePath(String localAppData, String userHome) {
        Path base = (localAppData != null && !localAppData.isBlank())
                ? Path.of(localAppData)
                : Path.of(userHome, "AppData", "Local");
        return base.resolve(APP_DIR).resolve(DB_FILE);
    }
}
