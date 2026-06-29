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
| `Route` | Distance & time calculations; uses Haversine formula for great-circle km, then `× 0.621371` for miles; `time = distance_miles / speed_mph` |
| `MapboxService` | HTTP calls to Mapbox Directions API (encoded polyline), Static Maps API (map image URL), and Geocoding API; `reverseGeocode(lon, lat)` → place name string (`null` if no result); `forwardGeocode(String query)` → nullable `GeoResult` (`lat`, `lon`, `placeName`), adds `proximity=ip` to bias results toward the user's location; nested `record GeoResult(double lat, double lon, String placeName)`; reads token via `loadToken()` from `config.properties` |
| `RouteLoader` | JSON persistence: reads `saved_routes.json` (falls back to `src/main/resources/routes.json`); writes back with `org.json` |
| `MainCLI` | Interactive terminal loop; parses `(lat, lon)` input strings |

### GUI layer (`src/main/java/gui/GpsAppGui.java`)

Single JavaFX class with two tabs:
- **"Enter new route"** — card-based layout with five cards: **Location 1** and **Location 2** (each has an address search field + Search button firing `Task<MapboxService.GeoResult>` calling `forwardGeocode`; on success populates lat/lon fields and sets a `"Search result: <placeName>"` label beneath the field; `"Search result: Not found"` on no result or failure) and directly-editable lat/lon fields (**source of truth for Calculate**); **Route Options** (name field, speed dropdown, full-width Calculate button); **Map Preview** (map image + Export as PNG); output card (**Route Summary** + **Resolved Places**); on Calculate, a separate `Task` runs two `reverseGeocode` calls and `getEncodedPolyline` in the background; reverse-geocoded names populate **Resolved Places** and are used as `Location` names (fallback: `"Start"` / `"End"`); map image and save all happen in `setOnSucceeded` on the FX thread
- **"Select previous route"** — card-based layout with three cards: **Pick a Route** (dropdown of routes from `saved_routes.json`); **Map Preview** (map image + Export as PNG); **Route Summary** (distance, travel time); selecting a route fires `getEncodedPolyline` and updates the map and summary grid on the FX thread

Both tabs have an **Export as PNG** button (`exportMapAsPng(Image, Window)` in `GpsAppGui`): opens a `FileChooser`, appends `.png` if the user omits it, shows a success dialog with the saved path, or an error dialog on failure.

Auto-zoom: zoom level is derived from Haversine distance (shorter distance → higher zoom). Duplicate detection prompts the user to rename or overwrite.

### Data flow

```
User input → Route (Haversine calc) → MapboxService (API calls) → static map URL → JavaFX ImageView
                                    ↘ RouteLoader (JSON) ↙
                               saved_routes.json
```

### Dependencies

- `org.json` (json-20231013.jar, bundled in `lib/`) — JSON parse/write
- JavaFX 21 via `org.openjfx.javafxplugin` (modules: controls, fxml, web, swing) — `swing` required for `SwingFXUtils` used in PNG export

### Persistent data files

- `saved_routes.json` (project root, gitignored) — user-saved routes; auto-created on first save
- `src/main/resources/routes.json` — bundled example routes; used as fallback when no saved file exists
