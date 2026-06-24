package core;

import java.io.InputStream;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

/**
 * Builds the map using the Mapbox and Mapbox Directions API.
 * @author Nina Mason
 * @version 12/11/2025
 */

public class MapboxService {
    private static final String BASE_URL = "https://api.mapbox.com/directions/v5/mapbox/driving/";
    private static final String STYLE_URL = "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/";
    private final String token;

    public MapboxService() {
         this.token = loadToken();
    }

    /**
     * Gets the encoded polyline for use in the URL.
     * @param lonA, longitude from coordinate 1
     * @param latA, latitude from coordinate 1
     * @param lonB, longitude from coordinate 2
     * @param latB, latitude from coordinate 2
     * @return String, encoded polyline as a String
     */
    public String getEncodedPolyline(double lonA, double latA, double lonB, double latB) throws Exception {
        String urlStr = String.format(
            BASE_URL + "%f,%f;%f,%f?geometries=polyline&overview=simplified&access_token=%s",
            lonA, latA, lonB, latB, token
        );

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            JSONObject json = new JSONObject(response.toString());
            JSONObject route = json.getJSONArray("routes").getJSONObject(0);

            String polyline = route.getString("geometry");

            return URLEncoder.encode(polyline, StandardCharsets.UTF_8.toString());
        }
    }

    /**
     * Builds the map URL to load the map.
     * @param encodedPolyline, the encoded polyline String
     * @param lonA, longitude of coordinate 1
     * @param latA, latitude of coordinate 1
     * @param lonB, longitude of coordinate 2
     * @param latB, latitude of coordinate 2
     * @param centerLon, center longitude
     * @param centerLat, center latitude
     * @param zoom, amount of zoom the map should create around the points
     * @return String, the URL that will be used to render the map
     */
    public String buildStaticMapUrl(String encodedPolyline,
                                    double lonA, double latA,
                                    double lonB, double latB,
                                    double centerLon, double centerLat,
                                    int zoom) {
        return String.format(
            "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/"
        + "path-5+ff0000-0.8(%s),"
        + "pin-s+ff0000(%f,%f),"
        + "pin-s+0000ff(%f,%f)/%f,%f,%d/600x400?access_token=%s",
            encodedPolyline,
            lonA, latA,
            lonB, latB, 
            centerLon, centerLat,
            zoom,
            token 
        );
    }

    /**
     * Returns a human-readable place name for the given coordinates using the
     * Mapbox Geocoding API. Returns null if the API returns no results.
     * @param lon, longitude of the coordinate
     * @param lat, latitude of the coordinate
     * @return String, the place name, or null if none found
     */
    public String reverseGeocode(double lon, double lat) throws Exception {
        String urlStr = String.format(
            "https://api.mapbox.com/geocoding/v5/mapbox.places/%f,%f.json?access_token=%s",
            lon, lat, token
        );

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            JSONArray features = new JSONObject(response.toString()).getJSONArray("features");
            if (features.isEmpty()) return null;
            return features.getJSONObject(0).getString("place_name");
        }
    }

    /**
     * Returns the latitude and longitude for the given address or place name
     * using the Mapbox Geocoding API. Returns null if the API returns no results.
     * @param query, an address or place name to search for
     * @return double[] { lat, lon }, or null if no result found
     */
    public double[] forwardGeocode(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String urlStr = String.format(
            "https://api.mapbox.com/geocoding/v5/mapbox.places/%s.json?access_token=%s",
            encoded, token
        );

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            JSONArray features = new JSONObject(response.toString()).getJSONArray("features");
            if (features.isEmpty()) return null;
            JSONArray coords = features.getJSONObject(0)
                                       .getJSONObject("geometry")
                                       .getJSONArray("coordinates");
            return new double[]{ coords.getDouble(1), coords.getDouble(0) };
        }
    }

    /**
     * This function loads the API token for MapboxService.
     * @return String, the token as a String.
    */
    public static String loadToken() {
        try (InputStream input = MapboxService.class.getResourceAsStream("/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("mapbox.token");
        } catch (Exception e) {
            throw new RuntimeException("Missing or unreadable config.properties file!");
        }
    }
}