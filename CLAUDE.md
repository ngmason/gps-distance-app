# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java 21 / JavaFX 21 app that calculates distances between GPS coordinates and renders driving routes on interactive Mapbox maps. Has both a GUI (two-tab JavaFX interface) and a CLI mode.

## Build & Run Commands

The Gradle Wrapper is the preferred build interface: `gradlew.bat`, pinned to Gradle 8.10.2 (bin distribution, SHA-256-verified in `gradle/wrapper/gradle-wrapper.properties`). It removes the dependency on a system-wide Gradle install and guarantees every contributor and CI run uses the same Gradle version. A system `gradle` install still works if present, but `gradlew.bat` is the source of truth going forward.

```powershell
gradlew.bat build                   # compile and assemble
gradlew.bat run                     # launch the JavaFX GUI (default main: gui.GpsAppGui)
gradlew.bat runCli --console=plain  # launch the text CLI (core.MainCLI)
gradlew.bat clean                   # wipe build artifacts (including build/jpackage/, see Packaging below)
gradlew.bat test                    # run the automated test suite
gradlew.bat test jacocoTestReport   # run tests and generate HTML + XML coverage report
```

Run `gradlew.bat test` before every commit. `gradlew.bat build` compiles and assembles but does not run the test suite.

## Required Setup

The app will not start without a Mapbox API token. Create `src/main/resources/config.properties` (gitignored) using the provided template:

```properties
mapbox.token=pk.eyJ1Ijoiyour_token_here
```

A template is at `src/main/resources/config.properties.example`.

## Architecture

### Core layer (`src/main/java/core/`)

| Class | Responsibility |
|---|---|
| `Location` | Data-only: name + lat/long |
| `Route` | Stores `List<Location> locations`; `getWaypoints()` returns unmodifiable list; `getStart()`/`getEnd()` delegate to `locations.get(0)` / `locations.get(size-1)`; Haversine summed over consecutive pairs for N-leg total distance, then `× 0.621371` for miles; `time = distance_miles / speed_mph`; legacy 2-arg constructors delegate to N-location primary constructors |
| `MapboxService` | HTTP calls to Mapbox Directions API (encoded polyline), Static Maps API (map image URL), and Geocoding API; `getEncodedPolyline(List<Location>)` builds semicolon-separated coordinate string for N waypoints; `buildStaticMapUrl(String, List<Location>, ...)` renders red start pin, orange middle pins, blue end pin; old 4-coordinate overloads delegate to the list-based versions; `reverseGeocode(lon, lat)` → place name string (`null` if no result); `forwardGeocode(String query)` → nullable `GeoResult` (`lat`, `lon`, `placeName`), adds `proximity=ip`; nested `record GeoResult(double lat, double lon, String placeName)`; reads token via `loadToken()` from `config.properties` |
| `AppPaths` | Resolves the SQLite database path: `%LOCALAPPDATA%\GpsApp\routes.db` on Windows, falling back to `user.home\AppData\Local` if `LOCALAPPDATA` is unset; returns a `Path` only — directory creation is the caller's responsibility; isolates the path decision so packaging changes require editing only this class |
| `SQLiteRouteRepository` | SQLite persistence via JDBC (`AutoCloseable`); `loadRoutes()` — single JOIN query returns all routes with waypoints ordered by `route_id ASC, seq ASC`; `saveRoute(Route)` — inserts route row + waypoints batch in one transaction; `replaceRoute(String, Route)` — `DELETE … COLLATE NOCASE` + insert in one transaction, throws if zero rows deleted; `deleteRoute(String)` — `DELETE … COLLATE NOCASE`, returns `boolean`; constructor applies `PRAGMA foreign_keys = ON` and `PRAGMA journal_mode = WAL` then creates tables idempotently; schema: `routes` (`name TEXT UNIQUE`, `speed_mph`) + `waypoints` (`route_id FK ON DELETE CASCADE`, `seq`, `UNIQUE(route_id,seq)`) |
| `MainCLI` | Interactive terminal loop; parses `(lat, lon)` input strings |

### GUI layer (`src/main/java/gui/GpsAppGui.java`)

Single JavaFX class with two tabs:
- **"Enter new route"** — card-based layout; location cards live in a `VBox waypointsContainer` backed by `List<LocationCard> locationCards` (initially Location 1 + Location 2); each `LocationCard` (private static inner class) holds address field, Search button, resolved label, lat/lon fields, and a styled `VBox card`; search handlers are wired via `wireSearchHandler(LocationCard, MapboxService)` (one shared method); **"+ Add Stop"** button appends a new `LocationCard("Location N", ...)` with a wired search handler to the list and container; **Route Options** card (name field, speed dropdown, full-width Calculate button); **Map Preview** card (map image + Export as PNG); output card (**Route Summary** + **Resolved Places**); on Calculate, all `locationCards` are iterated to collect coordinates, a `Task<RouteCalcResult>` runs N `reverseGeocode` calls and `getEncodedPolyline(List<Location>)` in the background, `RouteCalcResult(List<Location> locations, String polyline)` is the typed return record; `setOnSucceeded` builds `Route(List<Location>, speed, name)`, calls `buildStaticMapUrl(polyline, List<Location>, ...)`, rebuilds **Resolved Places** grid with N rows; zoom computed via `calculateZoomLevel(List<Location>)` (bounding-box diagonal)
- **"Select previous route"** — card-based layout with three cards: **Pick a Route** (dropdown populated via `SQLiteRouteRepository.loadRoutes()` + **Delete Route** button in the same row, disabled until a route is selected; clicking shows a confirmation dialog naming the selected route, then calls `repo.deleteRoute()`; if it returns `true`, refreshes the dropdown, clears the selection, summary grid, waypoints grid, and map image; if it returns `false`, shows a warning alert and leaves all UI state unchanged; `SQLException` routes to `showDbError()`); **Map Preview** (map image + Export as PNG); **Route Summary** + **Waypoints** output card (distance, travel time, then a grid of all waypoint names populated when a route is selected); selecting a route calls `selected.getWaypoints()`, `getEncodedPolyline(List<Location>)`, `buildStaticMapUrl(polyline, List<Location>, ...)`, and `calculateZoomLevel(List<Location>)` on the FX thread

Both tabs have an **Export as PNG** button (`exportMapAsPng(Image, Window)` in `GpsAppGui`): opens a `FileChooser`, appends `.png` if the user omits it, shows a success dialog with the saved path, or an error dialog on failure.

Auto-zoom: `calculateZoomLevel(List<Location>)` computes the bounding box of all waypoints and delegates to `calculateZoomLevel(minLat, minLon, maxLat, maxLon)` (shorter span → higher zoom). Duplicate detection compares all waypoints in order and prompts the user to rename or overwrite.

### Data flow

```
User input → Route (Haversine calc) → MapboxService (API calls) → static map URL → JavaFX ImageView
                                    ↘ SQLiteRouteRepository ↙
                            %LOCALAPPDATA%\GpsApp\routes.db
```

### Dependencies

- `org.json` (json-20231013.jar, bundled in `lib/`) — JSON parsing for Mapbox API responses in `MapboxService`
- `org.xerial:sqlite-jdbc:3.47.1.0` (resolved via Maven Central) — SQLite JDBC driver; bundles a native binary for Windows x64, no separate DLL required
- JavaFX 21 via `org.openjfx.javafxplugin` (modules: controls, fxml, web, swing) — `swing` required for `SwingFXUtils` used in PNG export
- `org.junit.jupiter:junit-jupiter:5.10.2` (test scope) — JUnit 5 test engine; `useJUnitPlatform()` configured in `test` task
- JaCoCo 0.8.12 via Gradle `jacoco` plugin — `jacocoTestReport` task produces HTML at `build/reports/jacoco/test/html/index.html` and XML alongside it

### Testing

#### Test classes (`src/test/java/core/`)

| Class | What it covers |
|---|---|
| `RouteTest` | `haversine()` accuracy and symmetry, `calculateTime()` zero-speed guard, N-waypoint distance summing, `getWaypoints()` unmodifiability, `getStart()`/`getEnd()` delegation |
| `SQLiteRouteRepositoryTest` | `loadRoutes()` on empty DB, full field round-trip for 2- and 3-waypoint routes, insertion-order loading, duplicate-name constraint, `deleteRoute()` return value and case-insensitivity, `replaceRoute()` case-insensitivity, unknown-name throw, transactional rollback (DELETE succeeds then INSERT fails — verifies both original routes survive) |
| `AppPathsTest` | Nonblank `LOCALAPPDATA` used as base, `null` fallback to `userHome\AppData\Local`, blank fallback, correct `GpsApp` directory and `routes.db` filename in both branches |
| `MapboxServiceTest` | Mapbox static-map endpoint, access token, polyline path overlay, `600x400` dimensions, center/zoom, red/blue pins for 2-waypoint route, red/orange/blue pins for 3-waypoint route, empty polyline embedded gracefully |

#### Coverage (as of last run)

| Class | Line coverage |
|---|---|
| `SQLiteRouteRepository` | 95.9% |
| `Location` | 88.9% |
| `AppPaths` | 80.0% |
| `Route` | 79.6% |
| `MapboxService` | 15.9% (only `buildStaticMapUrl` is testable without HTTP) |
| `GpsAppGui`, `MainCLI` | 0% — intentionally excluded (see below) |

Overall testable-core coverage: ~71% lines. Run `gradlew.bat test jacocoTestReport` to regenerate.

#### Intentionally untested

- **`GpsAppGui`** — JavaFX; requires TestFX or a display; out of scope. GUI behavior (including the Delete Route flow: button enable/disable, confirmation dialog, state clearing) is verified through manual smoke testing. Repository behavior underlying all GUI operations is covered by `SQLiteRouteRepositoryTest`.
- **`MainCLI`** — interactive stdin loop; out of scope
- **`MapboxService` HTTP methods** (`getEncodedPolyline`, `reverseGeocode`, `forwardGeocode`) — make live API calls; excluded to keep the suite fast and offline-capable

#### Developer guidance

- New features in `core/` must ship with appropriate automated tests.
- GUI-only changes are verified through manual smoke testing rather than automated UI tests.
- Run `gradlew.bat test` before every commit; a failing test suite blocks merges.
- Perform a quick manual smoke test for any GUI changes before merging.
- Run `gradlew.bat test jacocoTestReport` before releases to verify coverage has not regressed.
- Update README.md and CLAUDE.md whenever new user-facing features are added.
- Do not write tests solely to raise coverage percentages — only add tests that assert correct behavior or protect against real regression risk.
- `MapboxService(String token)` and `AppPaths.resolvePath(String, String)` are package-private entry points for tests; do not make them public.

### Persistent data files

- `%LOCALAPPDATA%\GpsApp\routes.db` (outside the project, never committed) — SQLite database; created automatically on first run by `SQLiteRouteRepository`; shared by both the GUI and the CLI

### Packaging (jpackage staging)

Long-term goal: package the app as a self-contained Windows desktop application via `jpackage`. Work completed so far covers only reproducible jar staging — **no app-image or installer yet**.

#### Task dependency flow

```
jpackageInput (Sync)                                    → build/jpackage/input/
  └─ depends on jpackageJar (Jar)                        → build/jpackage/libs/gps-distance-app.jar
        ├─ depends on generatePackagingConfig (WriteProperties) → build/jpackage/config/config.properties
        │     └─ fails fast (GradleException) if GPS_APP_MAPBOX_DEPLOY_TOKEN is unset/blank
        │     └─ outputs.upToDateWhen { false } — always reruns, so a changed token can never be
        │        skipped because Gradle thinks the previously generated file is up to date
        ├─ from(sourceSets.main.output) { exclude 'config.properties' }  — dev token never enters this jar
        └─ from(generatePackagingConfig.destinationFile)                 — deployment token only
```

`jpackageInput` also copies every file in `configurations.runtimeClasspath` flat into `build/jpackage/input/` alongside the jar — JavaFX 21's controls/fxml/media/swing/web/graphics/base modules (both the classifier-less and `-win` native jars), plus `sqlite-jdbc-3.47.1.0.jar` and `json-20231013.jar`. This is exactly the same resolved set `gradlew.bat run` already uses, just staged as flat files for a future `jpackage --input` invocation.

#### Two separate jar artifacts — do not conflate

| | Task | Output path | Contains |
|---|---|---|---|
| Development | `jar` (standard) | `build/libs/gps-distance-app.jar` | local dev `src/main/resources/config.properties`, if present |
| Packaging | `jpackageJar` | `build/jpackage/libs/gps-distance-app.jar` | generated deployment `config.properties` only |

Both produce a file named `gps-distance-app.jar`, but in different directories — `jpackageJar` sets an explicit `destinationDirectory` for exactly this reason. Earlier in development, `jpackageJar` defaulted to Gradle's standard `build/libs/` output directory, and running `jpackageInput` silently overwrote the development jar with the deployment-token jar. **Do not remove that `destinationDirectory` override** or the collision returns.

#### Credential handling rules

- Development: `MapboxService.loadToken()` (unmodified) reads `/config.properties` off the classpath at runtime. Locally that's the gitignored `src/main/resources/config.properties` with your personal token, used by `gradlew.bat run`/`test`/`build`.
- Packaging: `generatePackagingConfig` reads a **dedicated public Mapbox deployment token** from the `GPS_APP_MAPBOX_DEPLOY_TOKEN` environment variable and writes it to `build/jpackage/config/config.properties`. This file exists only under `build/` and is deleted by `gradlew.bat clean` (no special-casing needed — it's covered by the default `clean` task deleting the whole build directory).
- The deployment token must never be committed to Git, and should be a distinct token from any local dev token (keep them separately revocable on the Mapbox account).
- `jpackageJar` explicitly excludes `config.properties` from `sourceSets.main.output` before layering in the generated packaging config, so a local dev token can never leak into a packaged jar even if `src/main/resources/config.properties` exists at packaging time.
- Packaging fails immediately (`GradleException`, non-zero exit) if `GPS_APP_MAPBOX_DEPLOY_TOKEN` is missing or blank. That check runs inside `generatePackagingConfig`'s `doFirst` — at execution time, not configuration time — so `run`/`test`/`build` never evaluate it and never require the variable to be set.

#### Commands

```powershell
gradlew.bat clean                                        # also wipes build/jpackage/ entirely
gradlew.bat test                                          # unaffected by packaging infra; no deploy token needed
gradlew.bat build                                         # unaffected by packaging infra; no deploy token needed
set GPS_APP_MAPBOX_DEPLOY_TOKEN=pk.your_deploy_token_here
gradlew.bat jpackageInput                                 # stages build/jpackage/input/; fails clearly if the token is unset/blank
```

#### Not yet implemented

- `jpackage --type app-image` invocation (turning `build/jpackage/input/` into a runnable app image)
- Windows application icon
- WiX/installer generation (`.msi` / `.exe`)
- Smoke testing of the packaged app

#### Guidance for future contributors

- Do not point `jpackageJar`'s `destinationDirectory` back at `build/libs/` — see the collision note above.
- Do not remove `outputs.upToDateWhen { false }` from `generatePackagingConfig` — without it, Gradle's up-to-date check snapshots task inputs before `doFirst` runs, so a changed `GPS_APP_MAPBOX_DEPLOY_TOKEN` could be silently ignored and a stale token reused.
- When adding the `jpackage --type app-image` task, wire it to depend on `jpackageInput` rather than duplicating its file-staging logic.
