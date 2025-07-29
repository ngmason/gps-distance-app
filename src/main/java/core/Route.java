package core;

/**
 * Route class to hold information about a gps route.
 * @author Nina Mason
 * @version 7/25/2025
 */

public class Route {

    private Location start;
    private Location end;
    private double distanceKm;
    private double distanceMiles;
    private double timeHrs;
    private String name;

    final static double R = 6371.0;

    public Route(Location start, Location end, double mph, String name) {
        this.start = start;
        this.end = end;
        double[] distances = haversine(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
        this.distanceKm = distances[0];
        this.distanceMiles = distances[1];
        this.timeHrs = calculateTime(distanceMiles, mph);
        this.name = name;
    }

    public Route(Location start, Location end, double distanceKm, double distanceMiles, double timeHrs, String name) {
        this.start = start;
        this.end = end;
        this.distanceKm = distanceKm;
        this.distanceMiles = distanceMiles;
        this.timeHrs = timeHrs;
        this.name = name;
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getDistanceMiles() {
        return distanceMiles;
    }

    public double getTimeHrs() {
        return timeHrs;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        int hours = (int) timeHrs;
        int minutes = (int) ((timeHrs - hours) * 60);
        String output = String.format(name + 
                                    "| Distance: %.2f km (%.2f miles)     Estimated travel time: %d hr %d min (%.2f hours)%n", 
                                    distanceKm, distanceMiles, hours, minutes, timeHrs);
        return output;
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

    /**
     * Calculates estimated travel time in hours.
     * @param miles, distance in miles
     * @param mph, average speed in miles per hour
     * @return double, the time a route takes in hours
     */
    public static double calculateTime (double miles, double mph) {
        if (mph != 0) {
            return (miles/mph);
        } else {
            return 0;
        }
    }

}