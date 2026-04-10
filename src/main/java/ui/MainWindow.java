package ui;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ui.Components.ConsultFichasView;
import ui.Components.DashboardView;
import ui.Components.ReportsView;
import ui.Components.SettingsView;
import ui.Components.Sidebar;
import ui.Components.TopBar;
import ui.Components.ValidationAlarmsView;

public class MainWindow {

    private Sidebar sidebar;
    private TopBar topBar;
    private BorderPane root;

    // Vistas lazy
    private ConsultFichasView consultFichasView;
    private DashboardView dashboardView;
    private ValidationAlarmsView alarmsView;
    private ReportsView reportsView;
    private SettingsView settingsView;

    public void show(Stage stage) {
        sidebar = new Sidebar(this::navigateTo);
        topBar  = new TopBar(stage, this::onSearch);

        root = new BorderPane();
        root.setLeft(sidebar.build());
        root.setTop(topBar.build());
        root.setStyle("-fx-background-color: #0f1117;");

        navigateTo("consult");

        Scene scene = new Scene(root, 1200, 720);
        stage.setTitle("SENA Fichas Manager");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        stage.show();
    }

    private void navigateTo(String view) {
        topBar.setActiveTab(view);
        sidebar.setActiveItem(view);
        switch (view) {
            case "dashboard"  -> root.setCenter(getDashboard());
            case "consult"    -> root.setCenter(getConsult());
            case "alarms"     -> root.setCenter(getAlarms());
            case "reports"    -> root.setCenter(getReports());
            case "settings"   -> root.setCenter(getSettings());
        }
    }

    private void onSearch(String term) {
        if (consultFichasView != null) {
            navigateTo("consult");
            consultFichasView.applySearch(term);
        }
    }

    // ── Lazy getters ────────────────────────────────────────────
    private DashboardView getDashboard() {
        if (dashboardView == null) dashboardView = new DashboardView();
        return dashboardView;
    }

    private ConsultFichasView getConsult() {
        if (consultFichasView == null) consultFichasView = new ConsultFichasView();
        consultFichasView.setDashboard(getDashboard());
        return consultFichasView;
    }

    private ValidationAlarmsView getAlarms() {
        if (alarmsView == null) alarmsView = new ValidationAlarmsView();
        return alarmsView;
    }

    private ReportsView getReports() {
        if (reportsView == null) reportsView = new ReportsView();
        return reportsView;
    }

    private SettingsView getSettings() {
        if (settingsView == null) settingsView = new SettingsView();
        return settingsView;
    }
}