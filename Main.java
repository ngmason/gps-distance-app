import java.util.Scanner;

/**
 * Main program to run gps distance app.
 * @author Nina Mason
 * @version 7/12/2025
 */

public class Main {

    final static double R = 6371.0;

    public static void main(String args[]) {
        Location coordinate1 = new Location("", 0, 0);
        Location coordinate2 = new Location("", 0, 0);
        double distMiles = 0.0;
        double distKm = 0.0;
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
            
            double[] distances = haversine(coordinate1.getLatitude(), 
                                            coordinate1.getLongitude(), 
                                            coordinate2.getLatitude(), 
                                            coordinate2.getLongitude());
            distKm = distances[0];
            distMiles = distances[1];

            System.out.println("\nLocation " + coordinate1.getName() + " has latitude " + coordinate1.getLatitude() + " and longitude " + coordinate1.getLongitude());
            System.out.println("\nLocation " + coordinate2.getName() + " has latitude " + coordinate2.getLatitude() + " and longitude " + coordinate2.getLongitude());
            System.out.printf("\n\nDistance in miles is %.2f and distance in kilometers is %.2f.\n", distMiles, distKm);
            System.out.println("\nWould you like to calculate another distance? (y/n)");
            continueAns = in.nextLine();
            
        } while (continueAns.equalsIgnoreCase("y"));

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
     * Calculates the haversine distance between two coordinates.
     * @param lat1, latitude of coordinate 1
     * @param lon1, longitude of coordinate 1
     * @param lat2, latitude of coordinate 2
     * @param lon2, longitude of coordinate 2
     * @return double[], the distance in kilometers and miles
     */
    public static double[] haversine(double lat1, double lon1, double lat2, double lon2) {

        double[] results = new double[2];

        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Differences
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        results[0] = R * c;
        results[1] = R * c * 0.621371; // Convert km to miles
        return results;
    }
}