package core;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppPathsTest {

    @Test
    void resolvePath_nonblankLocalAppData_usesItAsBase() {
        Path result = AppPaths.resolvePath("C:\\TestAppData", "C:\\Users\\Ignored");
        assertEquals(Path.of("C:\\TestAppData"), result.getParent().getParent(),
                "nonblank LOCALAPPDATA must be the base directory");
    }

    @Test
    void resolvePath_nullLocalAppData_fallsBackToUserHomeAppDataLocal() {
        Path result = AppPaths.resolvePath(null, "C:\\Users\\TestUser");
        assertEquals(Path.of("C:\\Users\\TestUser", "AppData", "Local"),
                result.getParent().getParent(),
                "null LOCALAPPDATA must fall back to userHome\\AppData\\Local");
    }

    @Test
    void resolvePath_blankLocalAppData_fallsBackToUserHomeAppDataLocal() {
        Path result = AppPaths.resolvePath("   ", "C:\\Users\\TestUser");
        assertEquals(Path.of("C:\\Users\\TestUser", "AppData", "Local"),
                result.getParent().getParent(),
                "blank LOCALAPPDATA must fall back to userHome\\AppData\\Local");
    }

    @Test
    void resolvePath_alwaysEndsWithGpsAppDirAndRoutesDbFilename() {
        Path withEnv      = AppPaths.resolvePath("C:\\TestAppData", "ignored");
        Path withNullFb   = AppPaths.resolvePath(null,  "C:\\Users\\TestUser");
        Path withBlankFb  = AppPaths.resolvePath("   ", "C:\\Users\\TestUser");

        for (Path result : new Path[]{withEnv, withNullFb, withBlankFb}) {
            assertEquals("routes.db", result.getFileName().toString(),
                    "database filename must be routes.db");
            assertEquals("GpsApp", result.getParent().getFileName().toString(),
                    "application directory must be GpsApp");
        }
    }
}
