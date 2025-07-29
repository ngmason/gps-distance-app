package gui;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import core.Location;
import core.Route;
import core.RouteLoader;
import java.util.ArrayList;

public class GpsAppGui extends Application {

    private static final String SAVE_FILE_PATH = "saved_routes.json";

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
        VBox newRouteLayout = new VBox(15);
        newRouteLayout.setPadding(new Insets(20));
        newRouteLayout.setStyle("-fx-background-color: #D3D3D3; -fx-border-radius: 20; -fx-background-radius: 20;");

        // Coordinate inputs
        GridPane coordinatesGrid = new GridPane();
        coordinatesGrid.setHgap(5);
        coordinatesGrid.setVgap(5);

        Label coord1Label = new Label("Coordinate 1");
        TextField lat1Field = new TextField();
        TextField long1Field = new TextField();

        Label coord2Label = new Label("Coordinate 2");
        TextField lat2Field = new TextField();
        TextField long2Field = new TextField();

        coordinatesGrid.add(coord1Label, 0, 0);
        coordinatesGrid.add(new Label("Lat:"), 0, 1);
        coordinatesGrid.add(lat1Field, 1, 1);
        coordinatesGrid.add(new Label("Long:"), 2, 1);
        coordinatesGrid.add(long1Field, 3, 1);

        coordinatesGrid.add(coord2Label, 0, 2);
        coordinatesGrid.add(new Label("Lat:"), 0, 3);
        coordinatesGrid.add(lat2Field, 1, 3);
        coordinatesGrid.add(new Label("Long:"), 2, 3);
        coordinatesGrid.add(long2Field, 3, 3);

        // Route name input
        Label nameLabel = new Label("Route name:");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Route 4, NYC to LA");

        // Speed input
        Label speedLabel = new Label("Speed (mph):");
        ComboBox<Double> speedDropdown = new ComboBox<>();
        speedDropdown.getItems().addAll(30.0, 50.0, 65.0, 75.0);
        speedDropdown.setValue(30.0); // default speed

        // Button
        Button calculateBtn = new Button("Calculate distance");
        calculateBtn.setStyle("-fx-background-color: #00BFFF; -fx-text-fill: black; -fx-font-weight: bold;");
        Label resultLabel = new Label();

        calculateBtn.setOnAction(e -> {
            try {
                double lat1 = Double.parseDouble(lat1Field.getText());
                double lon1 = Double.parseDouble(long1Field.getText());
                double lat2 = Double.parseDouble(lat2Field.getText());
                double lon2 = Double.parseDouble(long2Field.getText());
                double speed = speedDropdown.getValue();
                String routeName = nameField.getText().isEmpty() ? "Untitled Route" : nameField.getText();

                Location loc1 = new Location("Start", lat1, lon1);
                Location loc2 = new Location("End", lat2, lon2);
                Route newRoute = new Route(loc1, loc2, speed, routeName);

                // Save to file
                ArrayList<Route> currentRoutes = RouteLoader.loadRoutes(SAVE_FILE_PATH);
                currentRoutes.add(newRoute);
                RouteLoader.saveRoutes(currentRoutes, SAVE_FILE_PATH);

                resultLabel.setText(newRoute.toString());

                refreshRouteDropdown(routeComboBox);
            } catch (NumberFormatException ex) {
                resultLabel.setText("⚠️ Please enter valid numbers for all coordinates!");
            }
        });

        // Placeholder map
        ImageView mapImageView = new ImageView(new Image("file:///Users/ninamason/Desktop/Screenshot%202025-07-22%20at%208.40.02%E2%80%AFPM.png"));
        mapImageView.setFitWidth(300);
        mapImageView.setPreserveRatio(true);

        newRouteLayout.getChildren().addAll(coordinatesGrid, nameLabel,  nameField, speedLabel, speedDropdown, calculateBtn, mapImageView, resultLabel);
        newRouteTab.setContent(newRouteLayout);

        // ----- PREVIOUS ROUTE TAB -----
        VBox previousRouteLayout = new VBox(10);
        previousRouteLayout.setPadding(new Insets(20));
        previousRouteLayout.setStyle("-fx-background-color: #9ec7eaff; -fx-border-radius: 20; -fx-background-radius: 20;");

        Label pickLabel = new Label("Pick a Route:");
        
        routeComboBox.setPromptText("Select saved route...");

        Label routeDetails = new Label();
        routeDetails.setWrapText(true);
        routeDetails.setStyle("-fx-font-family: 'monospace'; -fx-padding: 10;");

        // Load saved routes
        refreshRouteDropdown(routeComboBox);
        //ArrayList<Route> loadedRoutes = RouteLoader.loadRoutes(SAVE_FILE_PATH);
        //routeComboBox.getItems().addAll(loadedRoutes);

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
            if (selected != null) {
                routeDetails.setText(selected.toString());
            }
        });

        previousRouteLayout.getChildren().addAll(pickLabel, routeComboBox, routeDetails);
        previousRouteTab.setContent(previousRouteLayout);

        // Scene
        VBox root = new VBox(15);
        Label title = new Label("🧭 GPS Distance Calculator");
        title.setStyle("-fx-font-size: 22px; -fx-text-fill: #4B0082; -fx-font-weight: bold;");
        title.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, tabPane);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void refreshRouteDropdown(ComboBox<Route> routeComboBox) {
        routeComboBox.getItems().clear();
        ArrayList<Route> updatedRoutes = RouteLoader.loadRoutes(SAVE_FILE_PATH);
        routeComboBox.getItems().addAll(updatedRoutes);
    }

    /**private String calculateRouteSummary(String name1, double lat1, double lon1,
                                        String name2, double lat2, double lon2) {
        Location loc1 = new Location(name1, lat1, lon1);
        Location loc2 = new Location(name2, lat2, lon2);
        Route route = new Route(loc1, loc2, 30.0, "Route from " + name1 + " to " + name2);
        return route.toString(); 
    }*/
}