package ui.Components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class SettingsView extends VBox {

    public SettingsView() {
        setStyle("-fx-background-color: #0f1117;");
        setPadding(new Insets(16));
        setSpacing(12);
        VBox.setVgrow(this, Priority.ALWAYS);

        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label sub = new Label("Configuración general de la aplicación");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label wip = new Label(
                "⚙  Configuración en desarrollo. Aquí irán las preferencias de SharePoint, rutas y notificaciones.");
        wip.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #8b92a5;" +
                        "-fx-background-color: #13161f; -fx-background-radius: 10;" +
                        "-fx-border-color: #2a2d3a; -fx-border-radius: 10;" +
                        "-fx-padding: 14 16 14 16;");
        wip.setWrapText(true);

        getChildren().addAll(new VBox(3, title, sub), wip);
    }
}