import java.util.Scanner;

public class Main {

    final static double R = 6371.0;
    static double distMiles = 0.0;
    static double distKm = 0.0;

    public static void main(String args[]) {
        double [] coordinate1 = {0,0};
        double [] coordinate2 = {0,0};
        

        String input = "";
        Scanner in = new Scanner(System.in);

        System.out.println("\n\n---------Welcome to the GPS Distance Calculator---------");
        System.out.println("\nPlease enter coordinate 1 as (lat, long): ");
        input = in.nextLine();
        coordinate1 = parseCoordinate(input);

        System.out.println("\nPlease enter coordinate 2 as (lat, long): ");
        input = in.nextLine();
        coordinate2 = parseCoordinate(input);
        
        haversine(coordinate1[0], coordinate1[1], coordinate2[0], coordinate2[1]);

        System.out.println("\nLatitude 1 is " + coordinate1[0] + " and longitude 1 is " + coordinate1[1]);
        System.out.println("\nLatitude 2 is " + coordinate2[0] + " and longitude 2 is " + coordinate2[1]);
        System.out.printf("\n\nDistance in miles is %.2f and distance in kilometers is %.2f.\n", distMiles, distKm);
        System.out.println("\nIs that correct? Please type y or n...");
        String ans = in.nextLine();
        
        if (ans.equalsIgnoreCase("y")) {
            System.out.println("\nYay! Haversine formula is working!");
        } else if (ans.equalsIgnoreCase("n")) {
            System.out.println("\nOh no! Haversine formula might need some work...");
        } else {
            System.out.println("\nError: Invalid answer! Next time pleaser type y or n!");
        }

        System.out.println("\n\nThanks for using the GPS distance calculator! Goodbye!\n\n");
    }

    /**
     * Parses coordinates in the form (lat, long) into an array of doubles.
     * @param coordinate, The coordinate value in the form (lat, long)
     * @return double[], An array of double, the first slot containing latitude and the second slot containing longitude
     */
    static double[] parseCoordinate (String coordinate) {
        double [] latAndLong = {0,0};
        if (coordinate.contains(",") && coordinate.contains("(") && coordinate.contains(")")) {
            try {
                coordinate = coordinate.replace("(", "").replace(")", "").replace(" ", "");
                String[] pair = coordinate.split(",");
                latAndLong[0] = Double.parseDouble(pair[0]);
                latAndLong[1] = Double.parseDouble(pair[1]);
                return latAndLong;
                
            } catch (NumberFormatException e) {
                System.err.println("Error: Number formatting exception was caught! Please check coordinate input and make sure it is in the form: (lat, long)");
                return latAndLong;
            }
            
        } else {
            System.out.println("Opps! Invalid input. Make sure to input the coordinates as (lat, long).");
            return latAndLong;
        }
        

    }

    /**
     * Calculates the haversine distance between two coordinates.
     * @param lat1, latitude of coordinate 1
     * @param lon1, longitude of coordinate 1
     * @param lat2, latitude of coordinate 2
     * @param lon2, longitude of coordinate 2
     * @return double, the distance in kilometers
     */
    public static void haversine(double lat1, double lon1, double lat2, double lon2) {

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
        distKm = R * c;
        distMiles = distKm * 0.621371; // Convert km to miles
    }
}