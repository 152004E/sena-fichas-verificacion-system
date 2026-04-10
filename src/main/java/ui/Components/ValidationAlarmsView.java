package ui.Components;



import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class ValidationAlarmsView extends VBox {

    public ValidationAlarmsView() {
        setStyle("-fx-background-color: #0f1117;");
        setPadding(new Insets(16));
        setSpacing(12);
        VBox.setVgrow(this, Priority.ALWAYS);

        Label title = new Label("Validation Alarms");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label sub = new Label("Fichas con inconsistencias o datos incompletos");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label empty = new Label("⚠  Sin alertas activas. Carga un Excel para analizar las fichas.");
        empty.setStyle(
            "-fx-font-size: 13px; -fx-text-fill: #d97706;" +
            "-fx-background-color: #2a2010; -fx-background-radius: 10;" +
            "-fx-border-color: rgba(217,119,6,0.3); -fx-border-radius: 10;" +
            "-fx-padding: 14 16 14 16;"
        );
        empty.setWrapText(true);

        getChildren().addAll(new VBox(3, title, sub), empty);
    }
}