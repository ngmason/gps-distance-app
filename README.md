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
   - Calculate uses all location cards in order; total distance is the sum across all legs
   - Live-updating map preview
   - Saved routes dropdown regenerates the map dynamically; waypoint names are listed in the output card
   - Route summary includes:
      - Distance (km & miles)
      - Travel time
      - Selected speed
   - Export the current map preview as a PNG from either tab (FileChooser dialog; `.png` extension appended automatically if omitted)
   - After Calculate, reverse geocoding identifies canonical place names for all waypoints; results appear in a "Resolved Places" section

- Route Persistence
   - Saved to a SQLite database at `%LOCALAPPDATA%\GpsApp\routes.db` (created automatically on first run)
   - GUI and CLI share the same database
   - All waypoints (not just start and end) stored with reverse-geocoded place names
   - Overwrite/rename/duplicate detection (compares all waypoints)
   - Loads instantly into the GUI on startup; multi-waypoint routes display correctly in the Previous Route tab

- CLI Mode
   - Fast terminal-based route calculation
   - Same distance + travel time logic as GUI

- SQLite Route Persistence
   - Routes saved to `%LOCALAPPDATA%\GpsApp\routes.db` (created automatically on first run)
   - GUI and CLI share the same database file
   - Full CRUD with case-insensitive duplicate detection and transactional overwrite

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
- Gradle 8.14.3
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

1. Make sure Java and Gradle are installed:
   ```bash
   java -version
   gradle -version
2. Next run gradle build:
   ```bash
   gradle build
3. Then run:
   ```bash
   gradle run
---

## 🔧 How to Run CLI

1. Make sure Java and Gradle are installed:
   ```bash
   java -version
   gradle -version
2. Next run gradle build:
   ```bash
   gradle build
3. Then run:
   ```bash
   gradle runCli --console=plain

---

## 🧪 Testing

The project uses **JUnit 5** for automated tests and **JaCoCo** for code coverage reporting. No Mapbox token is required to run the test suite.

### Run the test suite

```bash
./gradlew clean test
```

### Generate a coverage report

```bash
./gradlew test jacocoTestReport
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

- **JavaFX GUI** — requires a display and a framework such as TestFX; out of scope for this suite
- **Interactive CLI** — requires stdin simulation; out of scope
- **Live Mapbox HTTP calls** — `getEncodedPolyline`, `reverseGeocode`, and `forwardGeocode` make real API calls that depend on a live token and network; excluded to keep the suite fast and offline-capable

---

## 🧩 Coming Soon (Future Enhancements)
- User-clickable map for coordinate selection
- Dark-mode map styles
