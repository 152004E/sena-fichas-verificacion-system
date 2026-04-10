package ui.Components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TopBar {

    private final Stage stage;
    private final Consumer<String> onSearch;
    private final Map<String, Label> tabs = new LinkedHashMap<>();

    private static final String TAB_DEFAULT =
        "-fx-font-size: 12px; -fx-text-fill: #6b7280; -fx-cursor: hand;" +
        "-fx-padding: 0 12 0 12; -fx-border-width: 0 0 2 0; -fx-border-color: transparent;";
    private static final String TAB_ACTIVE =
        "-fx-font-size: 12px; -fx-text-fill: #39A900; -fx-cursor: hand;" +
        "-fx-padding: 0 12 0 12; -fx-border-width: 0 0 2 0; -fx-border-color: #39A900;";

    public TopBar(Stage stage, Consumer<String> onSearch) {
        this.stage    = stage;
        this.onSearch = onSearch;
    }

    public HBox build() {
        // ── Tabs ─────────────────────────────────────────────────
        Label tabFichas    = buildTab("", "consult");
        Label tabDashboard = buildTab("Panel",       "dashboard");
        Label tabSync      = buildTab("Estado", "alarms");

        tabs.put("consult",   tabFichas);
        tabs.put("dashboard", tabDashboard);
        tabs.put("alarms",    tabSync);

        HBox tabsBox = new HBox(0, tabFichas, tabDashboard, tabSync);
        tabsBox.setAlignment(Pos.CENTER_LEFT);
        tabsBox.setMinHeight(46);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Buscador ─────────────────────────────────────────────
        TextField search = new TextField();
        search.setPromptText("Buscar número de ficha...");
        search.setPrefWidth(170);
        search.setStyle(
            "-fx-background-color: #1e2230; -fx-text-fill: #c8ccd8;" +
            "-fx-prompt-text-fill: #4b5263; -fx-background-radius: 8;" +
            "-fx-border-color: #2a2d3a; -fx-border-radius: 8; -fx-font-size: 12px;" +
            "-fx-padding: 5 10 5 10;"
        );
        search.textProperty().addListener((obs, o, n) -> onSearch.accept(n == null ? "" : n));

        // ── Avatar ───────────────────────────────────────────────
        Label avatar = new Label("JS");
        avatar.setStyle(
            "-fx-background-color: #39A900; -fx-text-fill: white;" +
            "-fx-font-size: 11px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-padding: 6 8 6 8;"
        );

        // ── Icono sincronizar ────────────────────────────────────
        Label syncIcon = new Label("↻");
        syncIcon.setStyle(
            "-fx-background-color: #1e2230; -fx-text-fill: #6b7280;" +
            "-fx-font-size: 14px; -fx-background-radius: 8;" +
            "-fx-border-color: #2a2d3a; -fx-border-radius: 8;" +
            "-fx-padding: 4 8 4 8; -fx-cursor: hand;"
        );

        HBox bar = new HBox(0, tabsBox, spacer, search, syncIcon, avatar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 14, 0, 0));
        bar.setMinHeight(46);
        bar.setMaxHeight(46);
        bar.setSpacing(8);
        bar.setStyle(
            "-fx-background-color: #0f1117;" +
            "-fx-border-color: #2a2d3a; -fx-border-width: 0 0 1 0;"
        );

        return bar;
    }

    public void setActiveTab(String key) {
        tabs.forEach((k, tab) ->
            tab.setStyle(k.equals(key) ? TAB_ACTIVE : TAB_DEFAULT)
        );
    }

    private Label buildTab(String text, String key) {
        Label tab = new Label(text);
        tab.setStyle(TAB_DEFAULT);
        tab.setMinHeight(46);
        tab.setAlignment(Pos.CENTER);
        return tab;
    }
}