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
import java.io.InputStream;

import core.Location;
import core.Route;
import core.RouteLoader;
import java.util.ArrayList;

/**
 * GUI for the gps distance app.
 * @author Nina Mason
 * @version 7/31/2025
 */

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
        VBox newRouteLayout = new VBox(16);
        newRouteLayout.setPadding(new Insets(20));
        newRouteLayout.setStyle("-fx-background-color: #D3D3D3; -fx-border-radius: 20; -fx-background-radius: 20;");

        // Coordinate inputs
        // Section: Coordinates
        Label coordsHdr = new Label("Coordinates");
        coordsHdr.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");

        GridPane coordinatesGrid = new GridPane();
        coordinatesGrid.setHgap(10);
        coordinatesGrid.setVgap(8);

        // Narrow label columns, wide text field columns
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(50); // fixed-ish width for labels
        labelCol.setHgrow(Priority.NEVER);

        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS); // fields take remaining space

        coordinatesGrid.getColumnConstraints().setAll(
            labelCol, fieldCol, labelCol, fieldCol
        );

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

        //Long and Lat hints
        Label latHint = new Label("Latitude must be −90 to 90");
        Label lonHint = new Label("Longitude must be −180 to 180");
        latHint.setStyle("-fx-font-size:11px; -fx-text-fill:#444;");
        lonHint.setStyle("-fx-font-size:11px; -fx-text-fill:#444;");

        coordinatesGrid.add(latHint, 1, 4);     // spans columns 1..3 under row 1
        coordinatesGrid.add(lonHint, 3, 4);     // spans columns 1..3 under row 3

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

                summaryGrid.getChildren().clear();
                summaryGrid.add(new Label("Distance (km):"), 0, 0);
                summaryGrid.add(new Label(String.format("%.2f", newRoute.getDistanceKm())), 1, 0);
                summaryGrid.add(new Label("Distance (miles):"), 0, 1);
                summaryGrid.add(new Label(String.format("%.2f", newRoute.getDistanceMiles())), 1, 1);
                summaryGrid.add(new Label("Travel Time (hrs):"), 0, 2);
                summaryGrid.add(new Label(String.format("%.2f", newRoute.getTimeHrs())), 1, 2);

                refreshRouteDropdown(routeComboBox);
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText("Invalid Input");
                alert.setContentText("Please enter valid numbers for all coordinates!");

                alert.showAndWait();
            }
        });

        // Placeholder map
        ImageView mapImageView = new ImageView();
        mapImageView.setFitWidth(300);
        mapImageView.setPreserveRatio(true);
        
        // load from classpath (src/main/resources)
        InputStream mapStream = getClass().getResourceAsStream("/map-preview.png");
        if (mapStream != null) {
            mapImageView.setImage(new Image(mapStream));
        } else {
            System.err.println("map-preview.png not found on classpath");
        }

        newRouteLayout.getChildren().addAll(
            coordsHdr,                // section header for the coordinate grid
            coordinatesGrid, 
            nameLabel, nameField, 
            speedLabel, speedDropdown, 
            calculateBtn, 
            new Label("Map preview (static)"),  // caption above the map
            mapImageView, 
            summaryTitleBox, summaryGrid
        );
        ScrollPane scrollPane = new ScrollPane(newRouteLayout);
        scrollPane.setFitToWidth(true);
        newRouteTab.setContent(scrollPane);

        // ----- PREVIOUS ROUTE TAB -----
        VBox previousRouteLayout = new VBox(10);
        previousRouteLayout.setPadding(new Insets(20));
        previousRouteLayout.setStyle("-fx-background-color: #9ec7eaff; -fx-border-radius: 20; -fx-background-radius: 20;");

        Label pickLabel = new Label("Pick a Route:");
        pickLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");
        
        routeComboBox.setPromptText("Select saved route...");

        Label routeDetails = new Label();
        routeDetails.setWrapText(true);
        routeDetails.setStyle("-fx-font-family: 'monospace'; -fx-padding: 10;");

        // Load saved routes
        refreshRouteDropdown(routeComboBox);

        // Placeholder map again
        ImageView mapImageView2 = new ImageView();
        mapImageView2.setFitWidth(300);
        mapImageView2.setPreserveRatio(true);
        
        // load from classpath (src/main/resources)
        InputStream mapStream2 = getClass().getResourceAsStream("/map-preview.png");
        if (mapStream2 != null) {
            mapImageView2.setImage(new Image(mapStream2));
        } else {
            System.err.println("map-preview.png not found on classpath");
        }

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
                //routeDetails.setText(selected.toString());
                summaryGrid2.getChildren().clear();
                summaryGrid2.add(new Label("Distance (km):"), 0, 0);
                summaryGrid2.add(new Label(String.format("%.2f", selected.getDistanceKm())), 1, 0);
                summaryGrid2.add(new Label("Distance (miles):"), 0, 1);
                summaryGrid2.add(new Label(String.format("%.2f", selected.getDistanceMiles())), 1, 1);
                summaryGrid2.add(new Label("Travel Time (hrs):"), 0, 2);
                summaryGrid2.add(new Label(String.format("%.2f", selected.getTimeHrs())), 1, 2);
            }
        });

        previousRouteLayout.getChildren().addAll(pickLabel, routeComboBox, new Label("Map preview (static)"),
            mapImageView2, summaryTitleBox2, summaryGrid2);
        previousRouteTab.setContent(previousRouteLayout);

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

    private void refreshRouteDropdown(ComboBox<Route> routeComboBox) {
        routeComboBox.getItems().clear();
        ArrayList<Route> updatedRoutes = RouteLoader.loadRoutes(SAVE_FILE_PATH);
        routeComboBox.getItems().addAll(updatedRoutes);
    }

}