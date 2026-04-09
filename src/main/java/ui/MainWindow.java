package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindow {

    public void show(Stage stage) {
        // ── Título ──────────────────────────────────────────────
        Label titulo = new Label("🚀 SENA Fichas Manager");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #39A900;");

        Label subtitulo = new Label("Sistema de Validación y Gestión de Fichas");
        subtitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        Label estado = new Label("✅ Proyecto corriendo correctamente");
        estado.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");

        // ── Botón de prueba ──────────────────────────────────────
        Button btnCargar = new Button("📂 Cargar Excel");
        btnCargar.setStyle(
            "-fx-background-color: #39A900; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-cursor: hand;"
        );
        btnCargar.setOnAction(e -> {
            estado.setText("🔄 Próximamente: carga del Excel...");
        });

        // ── Layout ───────────────────────────────────────────────
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #f5f5f5;");
        root.getChildren().addAll(titulo, subtitulo, estado, btnCargar);

        // ── Escena ───────────────────────────────────────────────
        Scene scene = new Scene(root, 800, 500);
        stage.setTitle("SENA Fichas Manager");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.show();
    }
}