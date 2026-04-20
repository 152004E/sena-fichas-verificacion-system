package ui;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import ui.Components.ConsultFichasView;
import ui.Components.DashboardView;
import ui.Components.DescargaMasivaView;
import ui.Components.ReportsView;
import ui.Components.SettingsView;
import ui.Components.Sidebar;
import ui.Components.ValidationAlarmsView;

public class MainWindow {

    private Sidebar sidebar;
    private BorderPane root;

    // Vistas lazy
    private ConsultFichasView consultFichasView;
    private DashboardView dashboardView;
    private ValidationAlarmsView alarmsView;
    private ReportsView reportsView;
    private SettingsView settingsView;
    private DescargaMasivaView descargaMasivaView;

public void show(Stage stage) {
    sidebar = new Sidebar(this::navigateTo);

    root = new BorderPane();  // ← asigna al campo de instancia
    root.setLeft(sidebar.build());
    root.setStyle("-fx-background-color: #0f1117;");

    StackPane stackRoot = new StackPane(root);
    stackRoot.setStyle("-fx-background-color: #0f1117;");

    navigateTo("consult");

    Scene scene = new Scene(stackRoot, 1200, 720);
    stage.setTitle("SENA Fichas Manager");
    stage.setScene(scene);
    stage.setMinWidth(1000);
    stage.setMinHeight(650);
    stage.setMaximized(true);
    stage.show();
}

    private void navigateTo(String view) {
        sidebar.setActiveItem(view);
        switch (view) {
            case "dashboard" -> root.setCenter(getDashboard());
            case "consult" -> root.setCenter(getConsult());
            case "alarms" -> root.setCenter(getAlarms());
            case "reports" -> root.setCenter(getReports());
            case "settings" -> root.setCenter(getSettings());
            case "descarga" -> root.setCenter(getDescarga());
        }
    }

    // onSearch method removed (unused locally)

    // ── Lazy getters ────────────────────────────────────────────
    private DashboardView getDashboard() {
        if (dashboardView == null)
            dashboardView = new DashboardView();
        return dashboardView;
    }

    private ConsultFichasView getConsult() {
        if (consultFichasView == null)
            consultFichasView = new ConsultFichasView();
        consultFichasView.setDashboard(getDashboard());
        return consultFichasView;
    }

    private ValidationAlarmsView getAlarms() {
        if (alarmsView == null)
            alarmsView = new ValidationAlarmsView();
        return alarmsView;
    }

    private ReportsView getReports() {
        if (reportsView == null)
            reportsView = new ReportsView();
        return reportsView;
    }

    private SettingsView getSettings() {
        if (settingsView == null)
            settingsView = new SettingsView();
        return settingsView;
    }

    private DescargaMasivaView getDescarga() {
        if (descargaMasivaView == null)
            descargaMasivaView = new DescargaMasivaView();
        return descargaMasivaView;
    }
}