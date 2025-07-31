package core;

import gui.GpsAppGui;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        // Launch JavaFX platform to show the dialog first
        Platform.startup(() -> {
            boolean useGui = askUserViaGui();
            if (useGui) {
                Application.launch(GpsAppGui.class);
            } else {
                runCli();
                Platform.exit();  // exit JavaFX thread after CLI runs
            }
        });
    }

    private static boolean askUserViaGui() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Choose Mode");
        alert.setHeaderText("Run GPS App in GUI or CLI mode?");
        ButtonType guiBtn = new ButtonType("GUI");
        ButtonType cliBtn = new ButtonType("CLI");

        alert.getButtonTypes().setAll(guiBtn, cliBtn);

        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(guiBtn) == guiBtn;
    }

    private static void runCli() {
        core.MainCLI.main(new String[]{}); // Assuming you move CLI logic to MainCLI.java
    }
}