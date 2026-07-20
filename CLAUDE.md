# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java 21 / JavaFX 21 app that calculates distances between GPS coordinates and renders driving routes on interactive Mapbox maps. Has both a GUI (two-tab JavaFX interface) and a CLI mode.

## Build & Run Commands

```powershell
gradle build           # compile and assemble
gradle run             # launch the JavaFX GUI (default main: gui.GpsAppGui)
gradle runCli --console=plain   # launch the text CLI (core.MainCLI)
gradle clean           # wipe build artifacts
```

There are no tests in this project. `gradle build` is the only build verification step.

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
- **"Select previous route"** — card-based layout with three cards: **Pick a Route** (dropdown populated via `SQLiteRouteRepository.loadRoutes()`); **Map Preview** (map image + Export as PNG); **Route Summary** + **Waypoints** output card (distance, travel time, then a grid of all waypoint names populated when a route is selected); selecting a route calls `selected.getWaypoints()`, `getEncodedPolyline(List<Location>)`, `buildStaticMapUrl(polyline, List<Location>, ...)`, and `calculateZoomLevel(List<Location>)` on the FX thread

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

### Persistent data files

- `%LOCALAPPDATA%\GpsApp\routes.db` (outside the project, never committed) — SQLite database; created automatically on first run by `SQLiteRouteRepository`; shared by both the GUI and the CLI
