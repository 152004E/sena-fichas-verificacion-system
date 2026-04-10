package ui.Components;

import database.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DashboardView extends VBox {

    private static final String DARK_BG = "-fx-background-color: #0f1117;";

    public DashboardView() {
        setStyle(DARK_BG);
        setPadding(new Insets(16));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);
        build();
    }

    private void build() {
        // ── Header ───────────────────────────────────────────────
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        Label sub = new Label("Resumen general del sistema de fichas SENA");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        VBox header = new VBox(3, title, sub);

        // ── KPI Cards ────────────────────────────────────────────
        int total      = 0;
        int completas  = 0;
        int alertas    = 0;
        int errores    = 0;
        try {
            DatabaseManager db = new DatabaseManager();
            total     = db.contarFichas();
            completas = db.contarFichasPorEstado("COMPLETA");
            alertas   = db.contarFichasPorEstado("INCOMPLETA");
            errores   = db.contarFichasPorEstado("CON_ERRORES");
        } catch (Exception ignored) {}

        HBox cards = new HBox(12,
            buildCard("Total Fichas",    String.valueOf(total),     "#378add", "#0c447c"),
            buildCard("Completas",       String.valueOf(completas), "#39A900", "#1c3a12"),
            buildCard("Alertas",         String.valueOf(alertas),   "#d97706", "#2a2010"),
            buildCard("Con Errores",     String.valueOf(errores),   "#e24b4a", "#2d1515")
        );
        cards.setAlignment(Pos.CENTER_LEFT);

        // ── Info banner ──────────────────────────────────────────
        Label info = new Label(
            "Carga un archivo Excel desde la vista 'Consult Fichas' para comenzar a ver datos en tiempo real."
        );
        info.setWrapText(true);
        info.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #8b92a5;");

        VBox infoBanner = new VBox(info);
        infoBanner.setPadding(new Insets(14));
        infoBanner.setStyle(
            "-fx-background-color: #13161f; -fx-background-radius: 10;" +
            "-fx-border-color: #2a2d3a; -fx-border-radius: 10; -fx-border-width: 0.5;"
        );

        getChildren().addAll(header, cards, infoBanner);
    }

    private VBox buildCard(String label, String value, String accent, String bg) {
        Label valLbl = new Label(value.isEmpty() ? "—" : value);
        valLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");

        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        VBox card = new VBox(4, nameLbl, valLbl);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle(
            "-fx-background-color: #13161f; -fx-background-radius: 10;" +
            "-fx-border-color: " + accent + "; -fx-border-radius: 10;" +
            "-fx-border-width: 0 0 0 3;"
        );
        card.setPrefWidth(160);
        card.setMinWidth(140);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }
}