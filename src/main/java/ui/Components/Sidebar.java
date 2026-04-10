package ui.Components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Sidebar {

    private final Consumer<String> onNavigate;
    private final Map<String, HBox> navItems = new LinkedHashMap<>();
    private String activeKey = "consult";

    // ── Estilos ──────────────────────────────────────────────────
    private static final String SIDEBAR_BG   = "-fx-background-color: #13161f;";
    private static final String ITEM_DEFAULT =
        "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String ITEM_ACTIVE  =
        "-fx-background-color: #1c3a12; -fx-background-radius: 8; -fx-cursor: hand;";
    private static final String ITEM_HOVER   =
        "-fx-background-color: #1e2230; -fx-background-radius: 8; -fx-cursor: hand;";

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    public VBox build() {
        // ── Marca ────────────────────────────────────────────────
        VBox brandBox = buildBrand();

        // ── Items de navegación ──────────────────────────────────
        VBox nav = new VBox(2);
        nav.setPadding(new Insets(10, 8, 10, 8));
        VBox.setVgrow(nav, Priority.ALWAYS);

        nav.getChildren().addAll(
            buildNavItem("dashboard", "Panel",                "M1 1h6v6H1zM9 1h6v6H9zM1 9h6v6H1zM9 9h6v6H9z"),
            buildNavItem("consult",   "Consultar Fichas",     "M6.5 11a4.5 4.5 0 100-9 4.5 4.5 0 000 9zm3.5-1l3.5 3.5"),
            buildNavItem("alarms",    "Alertas de Validación","M8 1a5 5 0 015 5v3l1.5 2.5h-13L3 9V6a5 5 0 015-5zm-1 12h2"),
            buildNavItem("reports",   "Reportes",             "M2 3h12v10H2zM5 7h6M5 9.5h4"),
            buildNavItem("settings",  "Configuración",        "M8 10a2 2 0 100-4 2 2 0 000 4zm5.3-1.7l1.2-.7-1-1.7-1.2.7a5 5 0 00-1-.6L11 5H9l-.3 1a5 5 0 00-1 .6L6.5 6l-1 1.7 1.2.7a5 5 0 000 1.2L5.5 10.4l1 1.7 1.2-.7a5 5 0 001 .6L9 13h2l.3-1a5 5 0 001-.6l1.2.7 1-1.7-1.2-.7a5 5 0 000-1.2z")
        );

        // Espaciador
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Botón sincronizar ────────────────────────────────────
        Button btnSync = new Button("⟳  Sincronizar Registros");
        btnSync.setStyle(
            "-fx-background-color: #39A900; -fx-text-fill: white;" +
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-padding: 9 14 9 14;"
        );
        btnSync.setMaxWidth(Double.MAX_VALUE);
        btnSync.setOnMouseEntered(e -> btnSync.setStyle(
            "-fx-background-color: #2d8a00; -fx-text-fill: white;" +
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-padding: 9 14 9 14;"
        ));
        btnSync.setOnMouseExited(e -> btnSync.setStyle(
            "-fx-background-color: #39A900; -fx-text-fill: white;" +
            "-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;" +
            "-fx-padding: 9 14 9 14;"
        ));

        VBox footer = new VBox(btnSync);
        footer.setPadding(new Insets(8, 8, 12, 8));
        footer.setStyle("-fx-border-color: #2a2d3a; -fx-border-width: 1 0 0 0;");

        VBox sidebar = new VBox(brandBox, nav, spacer, footer);
        sidebar.setStyle(SIDEBAR_BG + " -fx-border-color: #2a2d3a; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(200);

        return sidebar;
    }

    private VBox buildBrand() {
        Label icon = new Label("◈");
        icon.setStyle(
            "-fx-font-size: 16px; -fx-text-fill: white;" +
            "-fx-background-color: #39A900; -fx-background-radius: 6;" +
            "-fx-padding: 4 6 4 6;"
        );

        Label name = new Label("Editorial\nEnterprise");
        name.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0; -fx-line-spacing: 1;");

        HBox logoRow = new HBox(8, icon, name);
        logoRow.setAlignment(Pos.CENTER_LEFT);

        Label sub = new Label("SISTEMA DE VERIFICACIÓN");
        sub.setStyle("-fx-font-size: 8.5px; -fx-text-fill: #4b5263; -fx-letter-spacing: 0.08em;");

        VBox brand = new VBox(4, logoRow, sub);
        brand.setPadding(new Insets(14, 14, 12, 14));
        brand.setStyle("-fx-border-color: #2a2d3a; -fx-border-width: 0 0 1 0;");
        return brand;
    }

    private HBox buildNavItem(String key, String label, String svgData) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgData);
        icon.setStyle("-fx-fill: transparent; -fx-stroke: currentColor; -fx-stroke-width: 1.4;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12.5px;");

        HBox item = new HBox(9, icon, lbl);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 10, 8, 10));
        item.setMaxWidth(Double.MAX_VALUE);

        applyItemStyle(item, lbl, icon, key.equals(activeKey));

        item.setOnMouseEntered(e -> {
            if (!key.equals(activeKey)) {
                item.setStyle(ITEM_HOVER);
                lbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #c8ccd8;");
                icon.setStyle("-fx-fill: transparent; -fx-stroke: #c8ccd8; -fx-stroke-width: 1.4;");
            }
        });
        item.setOnMouseExited(e -> {
            applyItemStyle(item, lbl, icon, key.equals(activeKey));
        });
        item.setOnMouseClicked(e -> {
            activeKey = key;
            navItems.forEach((k, v) -> {
                Label l = (Label) v.getChildren().get(1);
                SVGPath ic = (SVGPath) v.getChildren().get(0);
                applyItemStyle(v, l, ic, k.equals(activeKey));
            });
            onNavigate.accept(key);
        });

        navItems.put(key, item);
        return item;
    }

    private void applyItemStyle(HBox item, Label lbl, SVGPath icon, boolean active) {
        if (active) {
            item.setStyle(ITEM_ACTIVE);
            lbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #5ed01a;");
            icon.setStyle("-fx-fill: transparent; -fx-stroke: #5ed01a; -fx-stroke-width: 1.4;");
        } else {
            item.setStyle(ITEM_DEFAULT);
            lbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #8b92a5;");
            icon.setStyle("-fx-fill: transparent; -fx-stroke: #8b92a5; -fx-stroke-width: 1.4;");
        }
    }

    public void setActiveItem(String key) {
        activeKey = key;
        navItems.forEach((k, item) -> {
            Label lbl = (Label) item.getChildren().get(1);
            SVGPath icon = (SVGPath) item.getChildren().get(0);
            applyItemStyle(item, lbl, icon, k.equals(activeKey));
        });
    }
}