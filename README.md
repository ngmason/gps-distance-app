# GPS Distance Calculator (Java)

A simple yet powerful Java application with both GUI and CLI modes that calculates distance between coordinates, estimates travel time, and manages saved routes with a user-friendly interface.

---

## 🌟 Features

- Calculates great-circle distance using the Haversine formula
- Enter custom coordinates, names, and travel speeds
- Automatically saves all routes to `routes.json`
- View and select previous routes from a live-updating dropdown
- Dual support: GUI (JavaFX) and CLI (terminal)
- Displays distances in both kilometers and miles
- Modular codebase using custom `Location`, `Route`, and `RouteLoader` classes
- JSON-powered route persistence

---

## 📦 Dependencies

- Java 21+
- JavaFX 21+
- [`org.json`](https://github.com/stleary/JSON-java) library (included in `lib/`)
- Gradle 8.14.3

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

1. Run from main source folder to compile:
- Mac OS:
   ```bash
   javac -cp ".:lib/json-20231013.jar" src/main/java/core/*.java
- Windows OS:
   ```bash
   javac -cp ".;lib/json-20231013.jar" src/main/java/core/*.java

2. Next to run the command line program:

- Mac OS:
   ```bash
   java -cp ".:lib/json-20231013.jar:src/main/java" core.Main

- Windows OS:
   ```bash
   java -cp ".;lib/json-20231013.jar;src/main/java" core.Main