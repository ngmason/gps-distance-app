import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main program to run gps distance app.
 * @author Nina Mason
 * @version 7/15/2025
 */

public class Main {

    final static double AVERAGE_SPEED = 30.0;
    static final String FILE_PATH = "routes.json";
    static ArrayList<Route> savedRoutes = new ArrayList<>();

    public static void main(String args[]) {

        loadRoutes();
        Location coordinate1 = new Location("", 0, 0);
        Location coordinate2 = new Location("", 0, 0);

        int count = savedRoutes.size();
        String name1 = "";
        String name2 = "";
        String input = "";
        String continueAns = "";
        Scanner in = new Scanner(System.in);

        System.out.println("\n\n---------Welcome to the GPS Distance Calculator---------");
        do {
            System.out.println("\nPlease enter the name of the location 1: ");
            name1 = in.nextLine();

            do {
                System.out.println("\nPlease enter coordinate 1 as (lat, long): ");
                input = in.nextLine();
                coordinate1 = parseCoordinate(input, name1);
            } while (coordinate1.getLatitude() == 0 && coordinate1.getLongitude() == 0);

            System.out.println("\nPlease enter the name of the location 2: ");
            name2 = in.nextLine();

            do {
                System.out.println("\nPlease enter coordinate 2 as (lat, long): ");
                input = in.nextLine();
                coordinate2 = parseCoordinate(input, name2);
            } while (coordinate2.getLatitude() == 0 &&  coordinate2.getLongitude() == 0);


            savedRoutes.add(new Route(coordinate1, coordinate2, AVERAGE_SPEED, ("Route " + ++count)));

            System.out.println("\nListing saved routes:\n");
            for(Route currentRoute : savedRoutes) {
                System.out.println(currentRoute.toString());

            }
            System.out.println("\nWould you like to calculate another distance? (y/n)");
            continueAns = in.nextLine();
            
        } while (continueAns.equalsIgnoreCase("y"));

        saveRoutes();
        System.out.println("\n\nThanks for using the GPS distance calculator! Goodbye!\n\n");
        in.close();
    }

    /**
     * Parses coordinates in the form (lat, long) into a Location object.
     * @param coordinate, The coordinate value in the form (lat, long)
     * @param name, The name of the coordinate location
     * @return Location, A new location object with a coordinate and a name
     */
    static Location parseCoordinate (String coordinate, String name) {
        double [] latAndLong = {0,0};
        if (coordinate.contains(",") && coordinate.contains("(") && coordinate.contains(")")) {
            try {
                coordinate = coordinate.replace("(", "").replace(")", "").replace(" ", "");
                String[] pair = coordinate.split(",");
                latAndLong[0] = Double.parseDouble(pair[0]);
                latAndLong[1] = Double.parseDouble(pair[1]);
                return new Location(name, latAndLong[0], latAndLong[1]);
                
            } catch (NumberFormatException e) {
                System.err.println("Error: Number formatting exception was caught! Please check coordinate input and make sure it is in the form: (lat, long)");
                return new Location("", 0, 0);
            }
            
        } else {
            System.out.println("Oops! Invalid input. Make sure to input the coordinates as (lat, long).");
            return new Location("", 0, 0);
        }
        

    }

    /**
     * Saves the current list of routes to a JSON file.
     * This ensures that all routes persist across software restarts.
     * Each route is stored with name, distance in kilometers, distance in miles, estimated travel time and path.
     */
    public static void saveRoutes() {
        try {
            JSONArray jsonRoutes = new JSONArray();
            for (Route route : savedRoutes) {
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
            File file = new File(FILE_PATH);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    System.out.println("Error: Failed to create directories for " 
                        + FILE_PATH);
                    return;
                }
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(jsonRoutes.toString(4));
            }

            System.out.println("routes.json saved successfully!");

        } catch (IOException e) {
            System.out.println("Error saving routes.json: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Loads routes from the "routes.json" file into memory when the program starts.
     * If the file does not exist, it starts with an empty list of routes.
     * The JSON data is read and converted back into Route objects, 
     * restoring previously stored routes.
     */
    public static void loadRoutes() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            System.out.println("No previous routes found. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            new FileInputStream(FILE_PATH), StandardCharsets.UTF_8))) {
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }

            JSONArray jsonRoutes = new JSONArray(jsonText.toString());
            for (int i = 0; i < jsonRoutes.length(); i++) {
                JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
                String name = jsonRoute.getString("name");
                double distanceKm = jsonRoute.getDouble("distanceKm");
                double distanceMiles = jsonRoute.getDouble("distanceMiles");
                double timeHrs = jsonRoute.getDouble("time");

                JSONArray pathArray = jsonRoute.getJSONArray("path");
                JSONObject startObject = pathArray.getJSONObject(0);
                Location start = new Location (startObject.getString("name"), startObject.getDouble("latitude"), startObject.getDouble("longitude"));
                JSONObject endObject = pathArray.getJSONObject(1);
                Location end = new Location (endObject.getString("name"), endObject.getDouble("latitude"), endObject.getDouble("longitude"));

                Route loadedRoute = new Route(start, end, distanceKm, distanceMiles, timeHrs, name);
                savedRoutes.add(loadedRoute);
            }
        } catch (IOException e) {
            System.out.println("I/O error loading routes: " + e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println("JSON error parsing routes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}