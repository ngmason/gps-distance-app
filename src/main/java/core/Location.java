package core; 

/**
 * Location class to hold latitude, longitude coordinates along with a name of location.
 * @author Nina Mason
 * @version 7/25/2025
 */

public class Location {

    private String name;
    private double latitude;
    private double longitude;

    public Location(String name, double lat, double lon) {
        this.name = name;
        this.latitude = lat;
        this.longitude = lon;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String toString() {
        return name + " (" + latitude + ", " + longitude + ")";
    }

}