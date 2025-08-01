package core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import core.RouteLoader;
/**
 * Text console for the gps distance app.
 * @author Nina Mason
 * @version 7/31/2025
 */

public class MainCLI {

    final static double AVERAGE_SPEED = 30.0;
    static final String FILE_PATH = "routes.json";
    static final String SAVED_FILE_PATH = "saved_routes.json";
    static ArrayList<Route> savedRoutes = new ArrayList<>();

    public static void main(String[] args) {
        gpsAppTextConsole();
    }

    public static void gpsAppTextConsole() {

        savedRoutes = RouteLoader.loadRoutes(SAVED_FILE_PATH);
        Location coordinate1 = new Location("", 0, 0);
        Location coordinate2 = new Location("", 0, 0);

        int count = savedRoutes.size();
        String name1 = "";
        String name2 = "";
        String input = "";
        String continueAns = "";
        Scanner in = new Scanner(System.in);

        System.out.println("""
            \n\n
            =========================================
             Welcome to the GPS Distance Calculator
            =========================================
            """);
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

        RouteLoader.saveRoutes(savedRoutes, SAVED_FILE_PATH);
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

}