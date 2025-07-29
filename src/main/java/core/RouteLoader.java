package core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.json.*;

public class RouteLoader {
    private static final String DEFAULT_SAVE_FILE = "saved_routes.json";

    /**
     * Loads routes from a file into memory when called.
     * If the file does not exist, it loads the default saved_routes.json.
     * The JSON data is read and converted back into Route objects saved to an ArrayList, 
     * restoring previously stored routes.
     * @param filePath the String of the file path name
     * @return ArrayList<Route> an ArrayList of all the routes loaded in
     */
    public static ArrayList<Route> loadRoutes(String filePath) {
        ArrayList<Route> routes = new ArrayList<>();
        InputStream input = null;
        File saveFile = new File(filePath);

        try {
            if (saveFile.exists()) {
                input = new FileInputStream(saveFile);
            } else {
                input = RouteLoader.class.getClassLoader().getResourceAsStream("routes.json");
            }

            if (input == null) return routes;

            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonText.append(line);

            JSONArray jsonRoutes = new JSONArray(jsonText.toString());
            for (int i = 0; i < jsonRoutes.length(); i++) {
                JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
                String name = jsonRoute.getString("name");
                double distanceKm = jsonRoute.getDouble("distanceKm");
                double distanceMiles = jsonRoute.getDouble("distanceMiles");
                double timeHrs = jsonRoute.getDouble("time");

                JSONArray path = jsonRoute.getJSONArray("path");
                JSONObject start = path.getJSONObject(0);
                JSONObject end = path.getJSONObject(1);

                Location startLoc = new Location(start.getString("name"), start.getDouble("latitude"), start.getDouble("longitude"));
                Location endLoc = new Location(end.getString("name"), end.getDouble("latitude"), end.getDouble("longitude"));

                routes.add(new Route(startLoc, endLoc, distanceKm, distanceMiles, timeHrs, name));
            }

        } catch (Exception e) {
            System.out.println("Error loading routes: " + e.getMessage());
        }

        return routes;
    }

    public static void saveRoutes(ArrayList<Route> routes) {
        saveRoutes(routes, DEFAULT_SAVE_FILE);
    }

    /**
     * Saves the current list of routes to a JSON file.
     * This ensures that all routes persist across software restarts.
     * Each route is stored with name, distance in kilometers, distance in miles, estimated travel time and path.
     * @param routes the ArrayList of routes
     * @param filePath the String of the file path name
     */
    public static void saveRoutes(ArrayList<Route> routes, String filePath) {
        try {
            JSONArray jsonRoutes = new JSONArray();
            for (Route route : routes) {
                JSONObject jsonRoute = new JSONObject();
                jsonRoute.put("name", route.getName());
                jsonRoute.put("distanceKm", route.getDistanceKm());
                jsonRoute.put("distanceMiles", route.getDistanceMiles());
                jsonRoute.put("time", route.getTimeHrs());

                JSONArray pathArray = new JSONArray();

                JSONObject startObject = new JSONObject();
                startObject.put("name", route.getStart().getName());
                startObject.put("latitude", route.getStart().getLatitude());
                startObject.put("longitude", route.getStart().getLongitude());
                pathArray.put(startObject);

                JSONObject endObject = new JSONObject();
                endObject.put("name", route.getEnd().getName());
                endObject.put("latitude", route.getEnd().getLatitude());
                endObject.put("longitude", route.getEnd().getLongitude());
                pathArray.put(endObject);

                jsonRoute.put("path", pathArray);
                jsonRoutes.put(jsonRoute);
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
                writer.write(jsonRoutes.toString(4));
                System.out.println("Routes saved to " + filePath);
            }

        } catch (IOException e) {
            System.out.println("Error saving routes: " + e.getMessage());
        }
    }
}