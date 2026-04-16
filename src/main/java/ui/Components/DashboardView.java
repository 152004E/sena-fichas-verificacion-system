package ui.Components;

import database.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DashboardView extends VBox {

    private static final String DARK_BG = "-fx-background-color: #0f1117;";
    private Label totalLbl;
    private Label completasLbl;
    private Label alertasLbl;
    private Label erroresLbl;

    public DashboardView() {
        setStyle(DARK_BG);
        setPadding(new Insets(16));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);
        build();
    }

private void build() {

    Label title = new Label("Dashboard");
    title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

    Label sub = new Label("Resumen general del sistema de fichas SENA");
    sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

    VBox header = new VBox(3, title, sub);

    // 🔥 inicializar labels
    totalLbl = new Label("0");
    completasLbl = new Label("0");
    alertasLbl = new Label("0");
    erroresLbl = new Label("0");

    HBox cards = new HBox(12);
    cards.setAlignment(Pos.CENTER_LEFT);
    cards.setFillHeight(true);
    cards.setPrefWidth(Double.MAX_VALUE);

    VBox card1 = buildCard("Total Fichas", totalLbl, "#378add");
    VBox card2 = buildCard("Completas", completasLbl, "#39A900");
    VBox card3 = buildCard("Alertas", alertasLbl, "#d97706");
    VBox card4 = buildCard("Con Errores", erroresLbl, "#e24b4a");

    HBox.setHgrow(card1, Priority.ALWAYS);
    HBox.setHgrow(card2, Priority.ALWAYS);
    HBox.setHgrow(card3, Priority.ALWAYS);
    HBox.setHgrow(card4, Priority.ALWAYS);

    cards.getChildren().addAll(card1, card2, card3, card4);

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

    // 🔥 cargar datos iniciales
    refresh();
}
private VBox buildCard(String label, Label valueLabel, String accent) {

    valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");

    Label nameLbl = new Label(label);
    nameLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

    VBox card = new VBox(4, nameLbl, valueLabel);
    card.setPadding(new Insets(14, 16, 14, 16));
    card.setMinWidth(0);
    card.setPrefWidth(0);
    card.setMaxWidth(Double.MAX_VALUE);
    card.setStyle(
            "-fx-background-color: #13161f; -fx-background-radius: 10;" +
                    "-fx-border-color: " + accent + "; -fx-border-radius: 10;" +
                    "-fx-border-width: 0 0 0 3;"
    );

    return card;
}

public void refresh() {
    try (DatabaseManager db = new DatabaseManager()) {
        db.conectar();

        int total = db.contarFichas();
        int completas = db.contarFichasPorEstado("Finalizado");
        int alertas = db.contarFichasPorEstado("En ejecución");
        int errores = db.contarFichasPorEstado("Desconocido");

        totalLbl.setText(String.valueOf(total));
        completasLbl.setText(String.valueOf(completas));
        alertasLbl.setText(String.valueOf(alertas));
        erroresLbl.setText(String.valueOf(errores));

    } catch (Exception e) {
        e.printStackTrace();
    }
}

}