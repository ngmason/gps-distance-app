import java.util.Scanner;

public class Main {
    public static void main(String args[]) {
        double [] coordinate1 = {0,0};
        double [] coordinate2 = {0,0};

        String input = "";
        Scanner in = new Scanner(System.in);

        System.out.println("\n\n---------Welcome to the GPS Distance Calculator---------");
        System.out.println("\nPlease enter coordinate 1 as (lat, long): ");
        input = in.nextLine();
        coordinate1 = parseCoordinate(input);
        System.out.println("\nLatitude is " + coordinate1[0] + " and longitude is " + coordinate1[1]);
        System.out.println("Is that correct? Please type y or n...");
        String ans = in.nextLine();
        
        if (ans.equalsIgnoreCase("y")) {
            System.out.println("\nYay! Coordinate parser is working!");
        } else if (ans.equalsIgnoreCase("n")) {
            System.out.println("\nOh no! Coordinate parser might need some work...");
        } else {
            System.out.println("\nError: Invalid answer! Next time pleaser type y or n!");
        }

        System.out.println("\n\nThanks for using the GPS distance calculator! Goodbye!\n\n");
    }

    static double[] parseCoordinate (String coordinate) {
        double [] latAndLong = {0,0};
        if (coordinate.contains(",") && coordinate.contains("(") && coordinate.contains(")")) {
            try {
                coordinate = coordinate.replace("(", "").replace(")", "").replace(" ", "");
                String[] pair = coordinate.split(",");
                latAndLong[0] = Integer.parseInt(pair[0]);
                System.out.println("Lat=" + pair[0]);
                System.out.println("Long=" + pair[1]);
                latAndLong[1] = Integer.parseInt(pair[1]);
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
}