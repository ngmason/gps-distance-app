# GPS Distance Calculator (Java)

A Java/JavaFX application that calculates distances along multi-waypoint routes, estimates travel time, and renders real driving routes dynamically using the Mapbox Directions + Static Maps API. Supports routes with 2 or more stops. Includes persistent route storage, a clean UI, and full CLI support.

---

## 🌟 Features

- Dynamic Route Mapping
   - Draws real driving routes through 2 or more coordinates using Mapbox's Directions API
   - Visualizes the route using Mapbox Static Maps with color-coded pins (red start, orange stops, blue end)
   - Automatically encodes & draws polylines
   - Auto-adjusting zoom level based on the bounding box of all waypoints

- Interactive JavaFX GUI
   - Enter coordinates directly or search by address or place name; address search uses the Mapbox Geocoding API and populates the latitude and longitude fields automatically
   - Resolved place name shown below each search field immediately after Search (e.g., `"Search result: Red Rocks Amphitheatre, Morrison, Colorado"`); latitude and longitude fields remain directly editable and are always the source of truth for Calculate
   - Card-based layout groups related controls into distinct visual sections: location inputs, route options, map preview, and route output
   - Click "+ Add Stop" to add additional waypoint cards (Location 3, Location 4, …); each card has its own address search and lat/lon fields
   - Optional stops (Location 3 and beyond) can be removed individually via a "Remove Stop" button; remaining stops renumber automatically to stay sequential. Location 1 and Location 2 are always present and cannot be removed.
   - Calculate uses all location cards in order; total distance is the sum across all legs
   - Live-updating map preview
   - Saved routes dropdown regenerates the map dynamically; waypoint names are listed in the output card
   - Delete a saved route directly from the Previous Route tab; a confirmation dialog names the route before deletion
   - Route summary includes:
      - Distance (km & miles)
      - Travel time
      - Selected speed
   - Export the current map preview as a PNG from either tab (FileChooser dialog; `.png` extension appended automatically if omitted)
   - After Calculate, reverse geocoding identifies canonical place names for all waypoints; results appear in a "Resolved Places" section

- Route Persistence
   - Saved to a local SQLite database at `%LOCALAPPDATA%\GpsApp\routes.db` (created automatically on first run)
   - GUI and CLI share the same database file
   - All waypoints (not just start and end) stored with reverse-geocoded place names
   - Full CRUD: save, load, transactional overwrite, and delete
   - Case-insensitive duplicate detection; prompts to overwrite or rename on name conflict
   - Routes load instantly into the Previous Route tab on startup

- CLI Mode
   - Fast terminal-based route calculation
   - Same distance + travel time logic as GUI

- Automated Testing
   - 30 unit and integration tests across four test classes (JUnit 5)
   - Covers route math, SQLite persistence, path resolution, and Mapbox URL generation
   - JaCoCo code coverage reporting; 96% line coverage on SQLiteRouteRepository

- Clean OOP Architecture
   - Location
   - Route
   - AppPaths (resolves the SQLite database path)
   - SQLiteRouteRepository (SQLite persistence: save, load, replace, delete)
   - MapboxService (handles Directions, Static Maps, and Geocoding API calls)

---

## 📸 Screenshots

![Enter New Route Tab](screenshots/GPS_Calculator_UI_1.png)

![Enter New Route Tab Map](screenshots/GPS_Calculator_UI_2.png)

![Enter New Route Tab w/ Data](screenshots/GPS_Calculator_UI_3.png)

![Select Previous Route Tab Map w/ Data](screenshots/GPS_Calculator_UI_4.png)

---

## 📦 Dependencies

- Java 21+
- JavaFX 21+ (modules: `controls`, `fxml`, `web`, `swing`)
- [`org.json`](https://github.com/stleary/JSON-java) library (included in `lib/`)
- `org.xerial:sqlite-jdbc:3.47.1.0` (SQLite JDBC driver, resolved via Maven Central)
- Gradle 8.10.2 (via the included Gradle Wrapper — no separate Gradle install required)
- Mapbox API Token (required)
- JUnit Jupiter 5.10.2 (test scope)
- JaCoCo 0.8.12 (coverage reporting)

---

## 🔑 Environment Setup (Mapbox)

This app requires a Mapbox Directions API token: 
1. Create a free Mapbox account
2. Generate a public access token
3. Create a file at:
```bash
src/main/resources/config.properties
```
4. Do not commit this file. A template config.properties.example is provided.


---

## 🔧 How to Run GUI

The included Gradle Wrapper (`gradlew.bat`, pinned to Gradle 8.10.2) is the preferred way to build and run — no separate Gradle install needed.

1. Make sure Java 21+ is installed:
   ```bash
   java -version
2. Build:
   ```bash
   gradlew.bat build
3. Run:
   ```bash
   gradlew.bat run
---

## 🔧 How to Run CLI

1. Make sure Java 21+ is installed:
   ```bash
   java -version
2. Build:
   ```bash
   gradlew.bat build
3. Run:
   ```bash
   gradlew.bat runCli --console=plain

---

## 🧪 Testing

The project uses **JUnit 5** for automated tests and **JaCoCo** for code coverage reporting. No Mapbox token is required to run the test suite.

### Run the test suite

```bash
gradlew.bat clean test
```

### Generate a coverage report

```bash
gradlew.bat test jacocoTestReport
```

The HTML report is written to:

```
build/reports/jacoco/test/html/index.html
```

### What is tested

| Test class | What it covers |
|---|---|
| `RouteTest` | Haversine formula accuracy and symmetry, travel-time calculation, N-waypoint distance summing, unmodifiable waypoint list |
| `SQLiteRouteRepositoryTest` | Full CRUD round-trips, insertion-order loading, case-insensitive delete/replace, transactional rollback when a replace fails mid-operation |
| `AppPathsTest` | `LOCALAPPDATA` branch, null and blank fallback to `userHome\AppData\Local`, correct application directory and database filename |
| `MapboxServiceTest` | Static map URL endpoint, access token, polyline overlay, pin color and coordinate placement for 2- and 3-waypoint routes, empty polyline handling |

### What is intentionally not covered

- **JavaFX GUI** — requires a display and a framework such as TestFX; out of scope for this suite. GUI-only changes (such as the Delete Route button) are verified through manual smoke testing. Repository behavior underlying all GUI operations is covered by `SQLiteRouteRepositoryTest`.
- **Interactive CLI** — requires stdin simulation; out of scope
- **Live Mapbox HTTP calls** — `getEncodedPolyline`, `reverseGeocode`, and `forwardGeocode` make real API calls that depend on a live token and network; excluded to keep the suite fast and offline-capable

---

## 🖥️ Packaging (Windows Desktop App)

The app is packaged as a self-contained Windows desktop application using `jpackage` — end users need no separate Java install. The full pipeline (staging → app-image → installer) has been built and verified end-to-end, including installing and running the packaged app.

### Prerequisites

- The included Gradle Wrapper (`gradlew.bat`, Gradle 8.10.2) — no local Gradle install needed.
- [WiX Toolset](https://wixtoolset.org) v3.x (`candle.exe`/`light.exe` on `PATH`) — required only for `jpackageInstaller`. Verified working with WiX 3.14.1, whose default install location is `C:\Program Files (x86)\WiX Toolset v3.14\bin` — that folder must be added to `PATH`.
- A dedicated **public Mapbox deployment token**, set via the `GPS_APP_MAPBOX_DEPLOY_TOKEN` environment variable — never your local dev `config.properties`, and never committed to Git. The build fails immediately with a clear error if it's missing or blank.

### Release commands

Git Bash:

```bash
export GPS_APP_MAPBOX_DEPLOY_TOKEN='pk.your_deploy_token_here'
gradlew.bat jpackageInput        # stages jar + runtime deps -> build/jpackage/input/
gradlew.bat jpackageAppImage     # builds self-contained app -> build/jpackage/app-image/GPS Distance Calculator/
gradlew.bat jpackageInstaller    # builds the installer -> build/jpackage/installer/GPS Distance Calculator-1.0.0.exe
unset GPS_APP_MAPBOX_DEPLOY_TOKEN
```

Command Prompt equivalent: `set GPS_APP_MAPBOX_DEPLOY_TOKEN=pk.your_deploy_token_here` beforehand, and `set GPS_APP_MAPBOX_DEPLOY_TOKEN=` afterward to clear it.

### Manual smoke test (perform after installing)

Install and launch the built `.exe`, then confirm: app launch, application icon, address/geocoding search, route calculation and map rendering, Add/Remove Stop, route save/load/delete, PNG export, app restart, and SQLite persistence. All of the above have passed manual verification on this pipeline.

**Not yet automated:** there is no CI pipeline producing these artifacts — release commands are run manually on a machine with WiX installed. Only the EXE installer type has been built; MSI was intentionally not pursued.

See CLAUDE.md for the full task dependency flow, the JavaFX packaging gotcha it uncovers, and credential-handling rules.

---

## 🧩 Coming Soon (Future Enhancements)
- User-clickable map for coordinate selection
- Dark-mode map styles
