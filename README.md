# GPS Distance Calculator (Java)

A simple command-line Java application that calculates the distance between two geographical coordinates using the Haversine formula.

---

## 🌟 Features

- Accepts user input for two sets of latitude and longitude
- Calculates the great-circle distance between them (Haversine formula)
- Displays results in both kilometers and miles
- Calculates estimated travel time based on user-defined average speed
- Stores routes with names and outputs a saved list after each calculation
- Clean, modular design using custom `Location` and `Route` classes
- Supports multiple calculations per session (loop)
- Saves and loads routes using a `routes.json` file

---

## 📦 Dependencies

- Java 8+
- [`org.json`](https://github.com/stleary/JSON-java) library (included in `lib/`)

---

## 🔧 How to Run

1. Make sure Java is installed:
   ```bash
   java -version
2. Next run from the src folder:
   ```bash
   javac -cp ".:lib/json-20231013.jar" *.java
3. Then run:
   ```bash
   java -cp ".:lib/json-20231013.jar" Main