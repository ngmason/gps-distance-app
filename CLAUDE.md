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
| `MapboxService` | HTTP calls to Mapbox Directions API (encoded polyline) and Static Maps API (map image URL); reads token via `loadToken()` from `config.properties` |
| `RouteLoader` | JSON persistence: reads `saved_routes.json` (falls back to `src/main/resources/routes.json`); writes back with `org.json` |
| `MainCLI` | Interactive terminal loop; parses `(lat, lon)` input strings |

### GUI layer (`src/main/java/gui/GpsAppGui.java`)

Single JavaFX class with two tabs:
- **"Enter new route"** — coordinate inputs + speed dropdown → calls `MapboxService` for polyline → renders static map image, saves to `saved_routes.json`
- **"Select previous route"** — dropdown of routes from `saved_routes.json` → shows stored data + map

Auto-zoom: zoom level is derived from Haversine distance (shorter distance → higher zoom). Duplicate detection prompts the user to rename or overwrite.

### Data flow

```
User input → Route (Haversine calc) → MapboxService (API calls) → static map URL → JavaFX ImageView
                                    ↘ RouteLoader (JSON) ↙
                               saved_routes.json
```

### Dependencies

- `org.json` (json-20231013.jar, bundled in `lib/`) — JSON parse/write
- JavaFX 21 via `org.openjfx.javafxplugin` (modules: controls, fxml, web)

### Persistent data files

- `saved_routes.json` (project root, gitignored) — user-saved routes; auto-created on first save
- `src/main/resources/routes.json` — bundled example routes; used as fallback when no saved file exists
