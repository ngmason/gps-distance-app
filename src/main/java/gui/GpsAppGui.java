package gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import java.util.Optional;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import core.Location;
import core.Route;
import core.RouteLoader;
import core.MapboxService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 * GUI for the gps distance app.
 * @author Nina Mason
 * @version 12/09/2025
 */

public class GpsAppGui extends Application {

    private static final String SAVE_FILE_PATH = "saved_routes.json";
    private static final double EPS = 1e-6;
    // -- Map state (must be fields for lambda updates) --
    private double lonA;
    private double latA;
    private double lonB;
    private double latB;
    private double centerLon;
    private double centerLat;
    private int zoom;
    private ImageView mapPreview;
    private ImageView mapPreview2; 
    private double prevLonA, prevLatA, prevLonB, prevLatB;
    private double prevCenterLon, prevCenterLat;
    private int prevZoom;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GPS App");

        // TabPane
        TabPane tabPane = new TabPane();
        Tab newRouteTab = new Tab("Enter new route");
        Tab previousRouteTab = new Tab("Select previous route");
        tabPane.getTabs().addAll(newRouteTab, previousRouteTab);
        ComboBox<Route> routeComboBox = new ComboBox<>();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);


        // ----- NEW ROUTE TAB -----
        VBox newRouteLayout = new VBox(16);
        newRouteLayout.setPadding(new Insets(20));
        newRouteLayout.setStyle("-fx-background-color: #D3D3D3; -fx-border-radius: 20; -fx-background-radius: 20;");

        LocationCard card1 = new LocationCard("Location 1", "e.g., Times Square, New York");
        LocationCard card2 = new LocationCard("Location 2", "e.g., Eiffel Tower, Paris");
        List<LocationCard> locationCards = new ArrayList<>(List.of(card1, card2));
        VBox waypointsContainer = new VBox(16, card1.card, card2.card);

        // Route name input
        Label nameLabel = new Label("Route name:");
        nameLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Route 4, NYC to LA");

        // Speed input
        Label speedLabel = new Label("Speed (mph):");
        speedLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        ComboBox<Double> speedDropdown = new ComboBox<>();
        speedDropdown.getItems().addAll(30.0, 50.0, 65.0, 75.0);
        speedDropdown.setValue(30.0); // default speed

        // Button
        Button calculateBtn = new Button("Calculate distance");
        calculateBtn.setStyle("-fx-background-color: #00BFFF; -fx-text-fill: black; -fx-font-weight: bold;");
        calculateBtn.setMaxWidth(Double.MAX_VALUE);

        Label routeOptionsHdr = new Label("Route Options");
        routeOptionsHdr.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox routeOptionsCard = new VBox(8,
            routeOptionsHdr, nameLabel, nameField, speedLabel, speedDropdown, calculateBtn
        );
        routeOptionsCard.setPadding(new Insets(12));
        routeOptionsCard.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");

        // Summary grid
        Image mapImg = new Image(getClass().getResourceAsStream("/map_icon.png"));
        ImageView mapIcon = new ImageView(mapImg);
        mapIcon.setFitHeight(20);
        mapIcon.setFitWidth(20);
        mapIcon.setPreserveRatio(true);
        Label summaryHeader = new Label("Route Summary:");
        summaryHeader.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        HBox summaryTitleBox = new HBox(10, mapIcon, summaryHeader);
        summaryTitleBox.setAlignment(Pos.CENTER_LEFT);

        GridPane summaryGrid = new GridPane();
        summaryGrid.setVgap(10);
        summaryGrid.setHgap(15);
        summaryGrid.setPadding(new Insets(10));

        lonA = -118.2437;
        latA = 34.0522;
        lonB = -74.0060;
        latB = 40.7128;
        centerLon = -96.0;
        centerLat = 39.0;
        zoom = calculateZoomLevel(latA, lonA, latB, lonB);
        String token = MapboxService.loadToken();
        MapboxService mapbox = new MapboxService();

        wireSearchHandler(card1, mapbox);
        wireSearchHandler(card2, mapbox);

        Button addStopBtn = new Button("+ Add Stop");
        addStopBtn.setOnAction(e -> {
            int locationNumber = locationCards.size() + 1;
            LocationCard newCard = new LocationCard("Location " + locationNumber, "e.g., Denver, CO");
            wireSearchHandler(newCard, mapbox);
            locationCards.add(newCard);
            waypointsContainer.getChildren().add(newCard.card);
        });

        try {
            String polyline = mapbox.getEncodedPolyline(lonA, latA, lonB, latB);
            String mapUrl = mapbox.buildStaticMapUrl(polyline, lonA, latA, lonB, latB, centerLon, centerLat, zoom);
            Image mapImage = new Image(mapUrl, 600, 400, false, false);
            mapPreview = new ImageView(mapImage);
        } catch (Exception ex) {
            ex.printStackTrace();
            // fallback: just show pins if MapboxService fails
            String fallbackUrl = String.format(
                "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/"
            + "pin-s+ff0000(%f,%f),pin-s+0000ff(%f,%f)/auto/600x400?access_token=%s",
                lonA, latA, lonB, latB, token
            );
            Image fallbackImage = new Image(fallbackUrl, 600, 400, false, false);
            mapPreview = new ImageView(fallbackImage);
        }
        mapPreview.setFitWidth(600);
        mapPreview.setPreserveRatio(true);


        Label detectedHdr = new Label("Resolved Places:");
        detectedHdr.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");

        GridPane detectedGrid = new GridPane();
        detectedGrid.setHgap(15);
        detectedGrid.setVgap(10);
        detectedGrid.setPadding(new Insets(10));

        calculateBtn.setOnAction(e -> {
            List<double[]> coords = new ArrayList<>();
            for (LocationCard lc : locationCards) {
                try {
                    double lat = Double.parseDouble(lc.latField.getText());
                    double lon = Double.parseDouble(lc.lonField.getText());
                    coords.add(new double[]{lat, lon});
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Input Error");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText("Please enter valid numbers for all coordinates!");
                    alert.showAndWait();
                    return;
                }
            }

            double speed = speedDropdown.getValue();
            String routeName = nameField.getText().trim().isEmpty()
                    ? "Untitled Route"
                    : nameField.getText().trim();

            detectedGrid.getChildren().clear();
            detectedGrid.add(new Label("Detecting..."), 0, 0);
            calculateBtn.setDisable(true);

            Task<RouteCalcResult> task = new Task<>() {
                @Override
                protected RouteCalcResult call() throws Exception {
                    List<Location> locations = new ArrayList<>();
                    for (int i = 0; i < coords.size(); i++) {
                        double[] pair = coords.get(i);
                        String name = mapbox.reverseGeocode(pair[1], pair[0]);
                        String fallback = (i == 0) ? "Start" : (i == coords.size() - 1) ? "End" : "Stop " + i;
                        locations.add(new Location(name != null ? name : fallback, pair[0], pair[1]));
                    }
                    String polyline = mapbox.getEncodedPolyline(locations);
                    return new RouteCalcResult(locations, polyline);
                }
            };

            task.setOnSucceeded(ev -> {
                List<Location> locations = task.getValue().locations();
                String polyline = task.getValue().polyline();

                detectedGrid.getChildren().clear();
                for (int i = 0; i < locations.size(); i++) {
                    detectedGrid.add(new Label("Location " + (i + 1) + ":"), 0, i);
                    Label placeLabel = new Label(locations.get(i).getName());
                    placeLabel.setWrapText(true);
                    detectedGrid.add(placeLabel, 1, i);
                }

                Route newRoute = new Route(locations, speed, routeName);

                boolean saved = addRouteWithDuplicateChecks(newRoute, calculateBtn.getScene().getWindow());
                if (saved) {
                    summaryGrid.getChildren().clear();
                    summaryGrid.add(new Label("Distance (km):"),   0, 0);
                    summaryGrid.add(new Label(String.format("%.2f", newRoute.getDistanceKm())), 1, 0);
                    summaryGrid.add(new Label("Distance (miles):"),0, 1);
                    summaryGrid.add(new Label(String.format("%.2f", newRoute.getDistanceMiles())), 1, 1);
                    summaryGrid.add(new Label("Travel Time (hrs):"),0, 2);
                    summaryGrid.add(new Label(String.format("%.2f", newRoute.getTimeHrs())), 1, 2);

                    refreshRouteDropdown(routeComboBox);
                }

                lonA = locations.get(0).getLongitude();
                latA = locations.get(0).getLatitude();
                lonB = locations.get(locations.size() - 1).getLongitude();
                latB = locations.get(locations.size() - 1).getLatitude();

                double sumLat = 0, sumLon = 0;
                for (Location loc : locations) { sumLat += loc.getLatitude(); sumLon += loc.getLongitude(); }
                centerLon = sumLon / locations.size();
                centerLat = sumLat / locations.size();
                zoom = calculateZoomLevel(locations);

                String mapUrl = mapbox.buildStaticMapUrl(polyline, locations, centerLon, centerLat, zoom);
                mapPreview.setImage(new Image(mapUrl, 600, 400, false, false));

                calculateBtn.setDisable(false);
            });

            task.setOnFailed(ev -> {
                detectedGrid.getChildren().clear();
                for (int i = 0; i < locationCards.size(); i++) {
                    detectedGrid.add(new Label("Location " + (i + 1) + ":"), 0, i);
                    detectedGrid.add(new Label("Not detected yet"), 1, i);
                }
                calculateBtn.setDisable(false);
                Throwable ex = task.getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Could not calculate route");
                alert.setContentText(ex != null ? ex.getMessage() : "An unexpected error occurred.");
                alert.showAndWait();
            });

            new Thread(task).start();
        });

        Button exportBtn1 = new Button("Export as PNG");
        exportBtn1.setOnAction(e ->
            exportMapAsPng(mapPreview.getImage(), exportBtn1.getScene().getWindow())
        );

        Label mapPreviewHdr = new Label("Map preview (dynamic)");
        mapPreviewHdr.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox mapCard = new VBox(8, mapPreviewHdr, mapPreview, exportBtn1);
        mapCard.setPadding(new Insets(12));
        mapCard.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");

        VBox outputCard = new VBox(12, summaryTitleBox, summaryGrid, detectedHdr, detectedGrid);
        outputCard.setPadding(new Insets(12));
        outputCard.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");

        newRouteLayout.getChildren().addAll(
            waypointsContainer,
            addStopBtn,
            routeOptionsCard,
            mapCard,
            outputCard
        );
        ScrollPane scrollPane = new ScrollPane(newRouteLayout);
        scrollPane.setFitToWidth(true);
        newRouteTab.setContent(scrollPane);

        // ----- PREVIOUS ROUTE TAB -----
        VBox previousRouteLayout = new VBox(10);
        previousRouteLayout.setPadding(new Insets(20));
        previousRouteLayout.setStyle("-fx-background-color: #9ec7eaff; -fx-border-radius: 20; -fx-background-radius: 20;");

        //Label pickLabel = new Label("Pick a Route:");
        //pickLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        
        routeComboBox.setPromptText("Select saved route...");

        Label routeDetails = new Label();
        routeDetails.setWrapText(true);
        routeDetails.setStyle("-fx-font-family: 'monospace'; -fx-padding: 10;");

        // Load saved routes
        refreshRouteDropdown(routeComboBox);
        
        // Repeat of Summary grid
        Image mapImg2 = new Image(getClass().getResourceAsStream("/map_icon.png"));
        ImageView mapIcon2 = new ImageView(mapImg2);
        mapIcon2.setFitHeight(20);
        mapIcon2.setFitWidth(20);
        mapIcon2.setPreserveRatio(true);
        Label summaryHeader2 = new Label("Route Summary:");
        summaryHeader2.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        HBox summaryTitleBox2 = new HBox(10, mapIcon2, summaryHeader2);
        summaryTitleBox2.setAlignment(Pos.CENTER_LEFT);

        GridPane summaryGrid2 = new GridPane();
        summaryGrid2.setVgap(10);
        summaryGrid2.setHgap(15);
        summaryGrid2.setPadding(new Insets(10));

        GridPane waypointsGrid2 = new GridPane();
        waypointsGrid2.setHgap(15);
        waypointsGrid2.setVgap(6);

        try {
            String polyline2 = mapbox.getEncodedPolyline(lonA, latA, lonB, latB);
            String mapUrl2 = mapbox.buildStaticMapUrl(polyline2, lonA, latA, lonB, latB, centerLon, centerLat, zoom);
            Image mapImage2 = new Image(mapUrl2, 600, 400, false, false);
            mapPreview2 = new ImageView(mapImage2);
        } catch (Exception ex) {
            ex.printStackTrace();
            // fallback: just show pins if MapboxService fails
            String fallbackUrl2 = String.format(
                "https://api.mapbox.com/styles/v1/mapbox/streets-v11/static/"
            + "pin-s+ff0000(%f,%f),pin-s+0000ff(%f,%f)/auto/600x400?access_token=%s",
                lonA, latA, lonB, latB, token
            );
            Image fallbackImage2 = new Image(fallbackUrl2, 600, 400, false, false);
            mapPreview2 = new ImageView(fallbackImage2);
        }
        mapPreview2.setFitWidth(600);
        mapPreview2.setPreserveRatio(true);

        // Only show route name in dropdown & selected item
        routeComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Route route, boolean empty) {
                super.updateItem(route, empty);
                setText(empty || route == null ? null : route.getName());
            }
        });

        // Also show route name when one is selected
        routeComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Route route, boolean empty) {
                super.updateItem(route, empty);
                setText(empty || route == null ? null : route.getName());
            }
        });

        routeComboBox.setOnAction(e -> {
            Route selected = routeComboBox.getValue();
            if (selected == null) return;

            try {
                List<Location> waypoints = selected.getWaypoints();

                String polyline = mapbox.getEncodedPolyline(waypoints);

                double sumLat = 0, sumLon = 0;
                for (Location loc : waypoints) { sumLat += loc.getLatitude(); sumLon += loc.getLongitude(); }
                prevCenterLon = sumLon / waypoints.size();
                prevCenterLat = sumLat / waypoints.size();
                prevZoom = calculateZoomLevel(waypoints);

                String mapUrl = mapbox.buildStaticMapUrl(polyline, waypoints, prevCenterLon, prevCenterLat, prevZoom);
                mapPreview2.setImage(new Image(mapUrl, 600, 400, false, false));

                summaryGrid2.getChildren().clear();
                summaryGrid2.add(new Label("Distance (km):"), 0, 0);
                summaryGrid2.add(new Label(String.format("%.2f", selected.getDistanceKm())), 1, 0);
                summaryGrid2.add(new Label("Distance (miles):"), 0, 1);
                summaryGrid2.add(new Label(String.format("%.2f", selected.getDistanceMiles())), 1, 1);
                summaryGrid2.add(new Label("Travel Time (hrs):"), 0, 2);
                summaryGrid2.add(new Label(String.format("%.2f", selected.getTimeHrs())), 1, 2);

                waypointsGrid2.getChildren().clear();
                for (int i = 0; i < waypoints.size(); i++) {
                    waypointsGrid2.add(new Label("Location " + (i + 1) + ":"), 0, i);
                    Label wLabel = new Label(waypoints.get(i).getName());
                    wLabel.setWrapText(true);
                    waypointsGrid2.add(wLabel, 1, i);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        Button exportBtn2 = new Button("Export as PNG");
        exportBtn2.setOnAction(e ->
            exportMapAsPng(mapPreview2.getImage(), exportBtn2.getScene().getWindow())
        );

        Label pickRouteHdr = new Label("Pick a Route");
        pickRouteHdr.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox pickCard = new VBox(8, pickRouteHdr, routeComboBox);
        pickCard.setPadding(new Insets(12));
        pickCard.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");

        Label mapPreviewHdr2 = new Label("Map preview (static)");
        mapPreviewHdr2.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox mapAndExport2 = new VBox(4, mapPreview2, exportBtn2);
        VBox mapCard2 = new VBox(8, mapPreviewHdr2, mapAndExport2);
        mapCard2.setPadding(new Insets(12));
        mapCard2.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");

        Label waypointsHdr2 = new Label("Waypoints:");
        waypointsHdr2.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        VBox outputCard2 = new VBox(12, summaryTitleBox2, summaryGrid2, waypointsHdr2, waypointsGrid2);
        outputCard2.setPadding(new Insets(12));
        outputCard2.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");

        previousRouteLayout.getChildren().addAll(pickCard, mapCard2, outputCard2);
        ScrollPane prevScrollPane = new ScrollPane(previousRouteLayout);
        prevScrollPane.setFitToWidth(true);
        prevScrollPane.setStyle("-fx-background-color: #9ec7eaff;");
        previousRouteTab.setContent(prevScrollPane);


        // Scene
        VBox root = new VBox(15);
        Image compassImage = new Image(getClass().getResourceAsStream("/compass_icon.png"));
        ImageView iconView = new ImageView(compassImage);
        iconView.setFitWidth(32);
        iconView.setFitHeight(32);
        iconView.setPreserveRatio(true);

        Label title = new Label("GPS Distance Calculator");
        title.setStyle("-fx-font-size: 22px; -fx-text-fill: #4B0082; -fx-font-weight: bold;");

        HBox titleBox = new HBox(10, iconView, title);
        titleBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleBox, tabPane);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 720, 680);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // ----- HELPER METHODS -----
    private void exportMapAsPng(Image image, Window owner) {
        if (image == null || image.isError()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle("Export failed");
            alert.setContentText("No map image is available to export.");
            alert.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Map Image");
        fileChooser.setInitialFileName("route_map.png");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );

        File file = fileChooser.showSaveDialog(owner);
        if (file == null) return;

        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getParentFile(), file.getName() + ".png");
        }

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        if (bufferedImage == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle("Export failed");
            alert.setContentText("The map is still loading. Please wait and try again.");
            alert.showAndWait();
            return;
        }

        try {
            ImageIO.write(bufferedImage, "png", file);
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.initOwner(owner);
            success.setTitle("Export successful");
            success.setHeaderText(null);
            success.setContentText("Map saved to: " + file.getAbsolutePath());
            success.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle("Export failed");
            alert.setContentText("Could not write file: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void refreshRouteDropdown(ComboBox<Route> routeComboBox) {
        routeComboBox.getItems().clear();
        ArrayList<Route> updatedRoutes = RouteLoader.loadRoutes(SAVE_FILE_PATH);
        routeComboBox.getItems().addAll(updatedRoutes);
    }

    private boolean nameTaken(String candidate, java.util.List<Route> routes) {
        String c = candidate.trim();
        if (c.isEmpty()) return true;
        return routes.stream().anyMatch(r -> r.getName().equalsIgnoreCase(c));
    }

    private String suggestUnique(String base, java.util.List<Route> routes) {
        if (!nameTaken(base, routes)) return base;
        int i = 2;
        while (nameTaken(base + " (" + i + ")", routes)) i++;
        return base + " (" + i + ")";
    }

    private boolean coordsEqual(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    private boolean coordinatesMatch(Route r, List<Location> coords) {
        List<Location> waypoints = r.getWaypoints();
        if (waypoints.size() != coords.size()) return false;
        for (int i = 0; i < coords.size(); i++) {
            if (!coordsEqual(waypoints.get(i).getLatitude(),  coords.get(i).getLatitude()) ||
                !coordsEqual(waypoints.get(i).getLongitude(), coords.get(i).getLongitude())) return false;
        }
        return true;
    }

    /**
     * Adds a route with duplicate checks. Returns true if something was saved.
     * Name duplicates -> Overwrite/Rename/Cancel dialog.
     * Coordinate duplicates -> blocks save (informational alert).
     * @param newRoute, the new route information
     * @return boolean, whether save was blocked or not
     */
    private boolean addRouteWithDuplicateChecks(Route newRoute, Window owner) {
        ArrayList<Route> currentRoutes = RouteLoader.loadRoutes(SAVE_FILE_PATH);

        String routeName = newRoute.getName();
        List<Location> newWaypoints = newRoute.getWaypoints();

        boolean coordsExist = currentRoutes.stream()
            .anyMatch(r -> coordinatesMatch(r, newWaypoints));
        if (coordsExist) {
            Alert warn = new Alert(Alert.AlertType.INFORMATION);
            warn.initOwner(owner);
            warn.setTitle("Duplicate route");
            warn.setHeaderText("These coordinates are already saved.");
            warn.setContentText("This route appears to be a duplicate. It won't be added again.");
            warn.showAndWait();
            return false;
        }

        boolean nameExists = currentRoutes.stream()
            .anyMatch(r -> r.getName().equalsIgnoreCase(routeName));
        if (nameExists) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(owner);
            confirm.setTitle("Duplicate name");
            confirm.setHeaderText("A route named \"" + routeName + "\" already exists.");
            confirm.setContentText("Do you want to overwrite it?");
            ButtonType overwrite = new ButtonType("Overwrite");
            ButtonType rename    = new ButtonType("Rename");
            ButtonType cancel    = ButtonType.CANCEL;
            confirm.getButtonTypes().setAll(overwrite, rename, cancel);

            ButtonType result = confirm.showAndWait().orElse(cancel);
            if (result == overwrite) {
                currentRoutes.removeIf(r -> r.getName().equalsIgnoreCase(routeName));
                currentRoutes.add(newRoute);
                RouteLoader.saveRoutes(currentRoutes, SAVE_FILE_PATH);
                return true;
            } else if (result == rename) {
                String newName = promptForNewRouteName(owner, routeName, currentRoutes);
                if (newName != null) {
                    // If Route is immutable, construct a new one here
                    newRoute.setName(newName);
                    currentRoutes.add(newRoute);
                    RouteLoader.saveRoutes(currentRoutes, SAVE_FILE_PATH);
                    return true;
                }
                return false;
            }
            return false;
        }

        // No conflicts
        currentRoutes.add(newRoute);
        RouteLoader.saveRoutes(currentRoutes, SAVE_FILE_PATH);
        return true;
    }

    /** 
     * A separate pop-up window if user decides to rename a duplicate route.
     * This window takes the new name and renames the route, or just cancels.
     * @param owner, the pop-up window
     * @param currentName, the current duplicate routes name
     * @param allRoutes, the list of current routes
    */
    private String promptForNewRouteName(Window owner, String currentName, java.util.List<Route> allRoutes) {
        String initial = suggestUnique(currentName, allRoutes);

        TextInputDialog dlg = new TextInputDialog(initial);
        dlg.setTitle("Rename route");
        dlg.setHeaderText("Pick a new name");
        dlg.setContentText("Route name:");
        if (owner != null) dlg.initOwner(owner);

        // Disable OK when invalid (blank or duplicate)
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(nameTaken(initial, allRoutes)); 

        dlg.getEditor().textProperty().addListener((obs, oldV, newV) -> {
            okBtn.setDisable(nameTaken(newV, allRoutes));
        });

        Optional<String> result = dlg.showAndWait();
        return result.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
    }

    /**
     * This function uses the Haversine formula to calculate distance and
     * the amount of zoom for the map based on the distance.
     * @param latA, the latitude of coordinate A
     * @param lonA, the longitude of coordinate A
     * @param latB, the latitude of coordinate B
     * @param lonB, the longitude of coordinate B
     * @return int, the level of zoom the map needs based on the Haversine distance
    */
    private int calculateZoomLevel(List<Location> locations) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (Location loc : locations) {
            if (loc.getLatitude()  < minLat) minLat = loc.getLatitude();
            if (loc.getLatitude()  > maxLat) maxLat = loc.getLatitude();
            if (loc.getLongitude() < minLon) minLon = loc.getLongitude();
            if (loc.getLongitude() > maxLon) maxLon = loc.getLongitude();
        }
        return calculateZoomLevel(minLat, minLon, maxLat, maxLon);
    }

    private int calculateZoomLevel(double latA, double lonA, double latB, double lonB) {
        double distanceMiles = Route.haversine(latA, lonA, latB, lonB)[1];

        if (distanceMiles < 5) return 10;
        if (distanceMiles < 20) return 8;
        if (distanceMiles < 50) return 6;
        if (distanceMiles < 1000) return 4;
        return 3;
    }

    private record RouteCalcResult(List<Location> locations, String polyline) {}

    private static class LocationCard {
        final Label headerLabel;
        final TextField addrField;
        final Button searchBtn;
        final Label resolvedLabel;
        final TextField latField;
        final TextField lonField;
        final VBox card;

        LocationCard(String header, String promptText) {
            headerLabel = new Label(header);
            headerLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
            addrField = new TextField();
            addrField.setPromptText(promptText);
            searchBtn = new Button("Search");
            searchBtn.setDisable(true);
            addrField.textProperty().addListener((obs, oldV, newV) ->
                searchBtn.setDisable(newV.trim().isEmpty()));
            resolvedLabel = new Label("");
            resolvedLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#444;");
            latField = new TextField();
            lonField = new TextField();

            HBox addrRow = new HBox(8, new Label("Address or place:"), addrField, searchBtn);
            HBox.setHgrow(addrField, Priority.ALWAYS);
            addrRow.setAlignment(Pos.CENTER_LEFT);
            HBox coordRow = new HBox(8, new Label("Lat:"), latField, new Label("Long:"), lonField);
            HBox.setHgrow(latField, Priority.ALWAYS);
            HBox.setHgrow(lonField, Priority.ALWAYS);
            coordRow.setAlignment(Pos.CENTER_LEFT);
            Label latHint = new Label("Latitude must be -90 to 90");
            Label lonHint = new Label("Longitude must be -180 to 180");
            latHint.setStyle("-fx-font-size:11px; -fx-text-fill:#444;");
            lonHint.setStyle("-fx-font-size:11px; -fx-text-fill:#444;");
            HBox hints = new HBox(16, latHint, lonHint);
            card = new VBox(8, headerLabel, addrRow, resolvedLabel, coordRow, hints);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: #EFEFEF; -fx-background-radius: 8; -fx-border-color: #BBBBBB; -fx-border-radius: 8;");
        }
    }

    private void wireSearchHandler(LocationCard card, MapboxService mapbox) {
        card.searchBtn.setOnAction(e -> {
            String query = card.addrField.getText().trim();
            card.searchBtn.setDisable(true);
            Task<MapboxService.GeoResult> searchTask = new Task<>() {
                @Override
                protected MapboxService.GeoResult call() throws Exception {
                    return mapbox.forwardGeocode(query);
                }
            };
            searchTask.setOnSucceeded(ev -> {
                MapboxService.GeoResult result = searchTask.getValue();
                if (result == null) {
                    card.resolvedLabel.setText("Search result: Not found");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("No results");
                    alert.setHeaderText(null);
                    alert.setContentText("No location found for \"" + query + "\".");
                    alert.showAndWait();
                } else {
                    card.latField.setText(String.valueOf(result.lat()));
                    card.lonField.setText(String.valueOf(result.lon()));
                    card.resolvedLabel.setText("Search result: " + result.placeName());
                }
                card.searchBtn.setDisable(card.addrField.getText().trim().isEmpty());
            });
            searchTask.setOnFailed(ev -> {
                card.resolvedLabel.setText("Search result: Not found");
                card.searchBtn.setDisable(card.addrField.getText().trim().isEmpty());
                Throwable ex = searchTask.getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Search failed");
                alert.setHeaderText("Could not search for location");
                alert.setContentText(ex != null ? ex.getMessage() : "An unexpected error occurred.");
                alert.showAndWait();
            });
            new Thread(searchTask).start();
        });
    }

}