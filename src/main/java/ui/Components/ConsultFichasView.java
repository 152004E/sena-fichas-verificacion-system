package ui.Components;

import Model.Ficha;
import database.DatabaseManager;
import excel.LocalExcelSource;
import service.SyncService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.Normalizer;
import java.util.List;

public class ConsultFichasView extends VBox {

    private final ObservableList<Ficha> fichas = FXCollections.observableArrayList();
    private DashboardView dashboard;
    private FilteredList<Ficha> filtro;
    private Label estadoLabel;

    public void setDashboard(DashboardView dashboard) {
        this.dashboard = dashboard;
    }

    private static final String DARK_BG = "-fx-background-color: #0f1117;";
    private static final String CARD_BG = "-fx-background-color: #13161f; -fx-background-radius: 10;";
    private static final String BORDER_SEP = "-fx-border-color: #2a2d3a; -fx-border-width: 0 0 1 0;";

    public ConsultFichasView() {
        setStyle(DARK_BG);
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addAll(buildHeroBanner(), buildTableSection());
    }

    // ── Hero banner con número de ficha ─────────────────────────
    private HBox buildHeroBanner() {
        Label fichaNum = new Label("Ficha: —");
        fichaNum.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label desc = new Label("Verificación detallada del progreso académico y la asignación de instructores.");
        desc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: rgba(255,255,255,0.65); -fx-wrap-text: true;");
        desc.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(desc, Priority.ALWAYS);

        ToggleButton toggleEjecucion = new ToggleButton("En ejecución");
        ToggleButton toggleFinalizado = new ToggleButton("Finalizado");

        ToggleButton toggleTecnologo = new ToggleButton("Tecnólogo");
        ToggleButton toggleTecnico = new ToggleButton("Técnico");
        TextField qsField = new TextField();
        // estilos iniciales
        toggleEjecucion.setStyle(getToggleStyle(false));
        toggleFinalizado.setStyle(getToggleStyle(false));
        toggleTecnologo.setStyle(getToggleStyle(false));
        toggleTecnico.setStyle(getToggleStyle(false));

        toggleEjecucion.selectedProperty().addListener((obs, o, n) -> {
            toggleEjecucion.setStyle(getToggleStyle(n));
            aplicarFiltros(qsField.getText(), toggleEjecucion, toggleFinalizado, toggleTecnologo, toggleTecnico);
        });

        toggleFinalizado.selectedProperty().addListener((obs, o, n) -> {
            toggleFinalizado.setStyle(getToggleStyle(n));
            aplicarFiltros(qsField.getText(), toggleEjecucion, toggleFinalizado, toggleTecnologo, toggleTecnico);
        });

        toggleTecnologo.selectedProperty().addListener((obs, o, n) -> {
            toggleTecnologo.setStyle(getToggleStyle(n));
            aplicarFiltros(qsField.getText(), toggleEjecucion, toggleFinalizado, toggleTecnologo, toggleTecnico);
        });

        toggleTecnico.selectedProperty().addListener((obs, o, n) -> {
            toggleTecnico.setStyle(getToggleStyle(n));
            aplicarFiltros(qsField.getText(), toggleEjecucion, toggleFinalizado, toggleTecnologo, toggleTecnico);
        });
        HBox tags = new HBox(8, toggleEjecucion, toggleFinalizado, toggleTecnologo, toggleTecnico);
        VBox left = new VBox(6, fichaNum, desc, tags);
        left.setAlignment(Pos.CENTER_LEFT);

        // 🔍 Buscador
        Label qsLabel = new Label("BÚSQUEDA RÁPIDA");
        qsLabel.setStyle("-fx-font-size: 8.5px; -fx-text-fill: rgba(255,255,255,0.5); -fx-letter-spacing: 0.08em;");

        qsField.setPromptText("Número de ficha...");
        qsField.setPrefWidth(260); // 👈 ancho real

        qsField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.3); -fx-background-radius: 6;" +
                        "-fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6;" +
                        "-fx-font-size: 12px; -fx-padding: 5 8 5 8;" // 👈 padding normal
        );
        qsField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                qsField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        qsField.textProperty().addListener((obs, o, n) -> {
            aplicarFiltros(n, toggleEjecucion, toggleFinalizado, toggleTecnologo, toggleTecnico);
        });

        qsField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.trim().isEmpty()) {
                fichaNum.setText("Ficha: " + n.trim());
            } else {
                fichaNum.setText("Ficha: —");
            }
        });

        VBox qsBox = new VBox(6, qsLabel, qsField);
        qsBox.setStyle(
                "-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 10;" +
                        "-fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10; -fx-padding: 10 12 10 12;");
        qsBox.setMinWidth(140);

        HBox hero = new HBox(16, left, qsBox);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(16, 18, 16, 18));
        hero.setStyle(
                "-fx-background-color: linear-gradient(to right, #1c3a12, #254d18, #39A900);" +
                        "-fx-background-radius: 12;");

        HBox wrapper = new HBox(hero);
        wrapper.setPadding(new Insets(14, 16, 10, 16));
        HBox.setHgrow(hero, Priority.ALWAYS);

        return wrapper;
    }

    // ── Sección de tabla ─────────────────────────────────────────
    private VBox buildTableSection() {
        // Header row: estado + botón cargar
        estadoLabel = new Label("Selecciona un archivo Excel para iniciar");
        estadoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b92a5;");

        Button btnCargar = new Button("📂  Cargar Excel");
        btnCargar.setStyle(
                "-fx-background-color: #39A900; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14 7 14;");
        btnCargar.setOnMouseEntered(e -> btnCargar.setStyle(
                "-fx-background-color: #2d8a00; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14 7 14;"));
        btnCargar.setOnMouseExited(e -> btnCargar.setStyle(
                "-fx-background-color: #39A900; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 7 14 7 14;"));
        btnCargar.setOnAction(e -> cargarArchivo(btnCargar));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerRow = new HBox(12, estadoLabel, spacer, btnCargar);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 16, 10, 16));

        // Tabla
        TableView<Ficha> table = buildTable();
        filtro = new FilteredList<>(fichas, p -> true);
        table.setItems(filtro);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox section = new VBox(10, headerRow, table);
        section.setPadding(new Insets(10, 16, 16, 16));
        VBox.setVgrow(section, Priority.ALWAYS);
        section.setStyle(DARK_BG);
        return section;
    }

    private void cargarArchivo(Button btn) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar archivo Excel");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        Stage stage = (Stage) btn.getScene().getWindow();
        File archivo = chooser.showOpenDialog(stage);
        if (archivo != null) {
            try {
                DatabaseManager db = new DatabaseManager();
                SyncService sync = new SyncService(db);
                List<Ficha> lista = sync.ejecutarSincronizacion(new LocalExcelSource(archivo.toPath()));
                fichas.setAll(lista);
                dashboard.refresh();
                estadoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5ed01a;");
                estadoLabel.setText("✓  " + archivo.getName() + "  —  " + lista.size() + " fichas cargadas");
            } catch (Exception ex) {
                estadoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e24b4a;");
                estadoLabel.setText("✗  Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private TableView<Ficha> buildTable() {
        TableView<Ficha> table = new TableView<>();
        table.setStyle(
                "-fx-background-color: #13161f; -fx-table-cell-border-color: #2a2d3a;" +
                        "-fx-control-inner-background: #13161f; -fx-background-radius: 10;" +
                        "-fx-border-color: #2a2d3a; -fx-border-radius: 10; -fx-border-width: 0.5;");
        table.setFixedCellSize(38);
        table.setPlaceholder(buildPlaceholder());

        // ── Columnas ─────────────────────────────────────────────
        TableColumn<Ficha, Integer> colNum = col("Ficha", 90);
        colNum.setCellValueFactory(new PropertyValueFactory<>("numero"));

        TableColumn<Ficha, String> colProg = col("Programa", 220);
        colProg.setCellValueFactory(new PropertyValueFactory<>("programa"));

        TableColumn<Ficha, String> colNivel = col("Nivel", 120);
        colNivel.setCellValueFactory(new PropertyValueFactory<>("nivel"));

        TableColumn<Ficha, Integer> colApr = col("Aprendices", 100);
        colApr.setCellValueFactory(new PropertyValueFactory<>("aprendices"));

        TableColumn<Ficha, String> colInicio = col("Inicio", 110);
        colInicio.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));

        TableColumn<Ficha, String> colFinLec = col("Fin Lectiva", 110);
        colFinLec.setCellValueFactory(new PropertyValueFactory<>("fechaFinLec"));

        TableColumn<Ficha, String> colFin = col("Fin", 110);
        colFin.setCellValueFactory(new PropertyValueFactory<>("fechaFin"));

        // Estado con color
        TableColumn<Ficha, String> colEstado = col("Estado", 130);
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado().getLabel()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                String style;
                if (item.toLowerCase().contains("completa") || item.toLowerCase().contains("ok")) {
                    style = "-fx-background-color: #1c3a12; -fx-text-fill: #5ed01a; -fx-background-radius: 20; -fx-padding: 2 8 2 8; -fx-font-size: 10.5px;";
                } else if (item.toLowerCase().contains("error") || item.toLowerCase().contains("crítico")) {
                    style = "-fx-background-color: #2d1515; -fx-text-fill: #e24b4a; -fx-background-radius: 20; -fx-padding: 2 8 2 8; -fx-font-size: 10.5px;";
                } else {
                    style = "-fx-background-color: #2a2010; -fx-text-fill: #d97706; -fx-background-radius: 20; -fx-padding: 2 8 2 8; -fx-font-size: 10.5px;";
                }
                badge.setStyle(style);
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: center-left; -fx-background-color: transparent;");
            }
        });

        TableColumn<Ficha, String> colI25 = col("Instructor 2025", 160);
        colI25.setCellValueFactory(new PropertyValueFactory<>("instructorTecnico2025"));

        TableColumn<Ficha, String> colBil = col("Bilingüismo", 160);
        colBil.setCellValueFactory(new PropertyValueFactory<>("instructorBilinguismo"));

        TableColumn<Ficha, String> colI26 = col("Instructor 2026", 160);
        colI26.setCellValueFactory(new PropertyValueFactory<>("instructorTecnico2026"));

        TableColumn<Ficha, String> colTrans = col("Transversales faltantes", 200);
        colTrans.setCellValueFactory(new PropertyValueFactory<>("transversalesFaltantes"));

        table.getColumns().addAll(
                colNum, colProg, colNivel, colApr,
                colInicio, colFinLec, colFin,
                colEstado, colI25, colBil, colI26, colTrans);

        // Estilo de filas
        table.setRowFactory(tv -> {
            TableRow<Ficha> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            row.hoverProperty().addListener((obs, wasHover, isNowHover) -> {
                if (!row.isSelected()) {
                    row.setStyle(isNowHover
                            ? "-fx-background-color: #1e2230;"
                            : "-fx-background-color: transparent;");
                }
            });
            return row;
        });

        return table;
    }

    private <T> TableColumn<Ficha, T> col(String title, double width) {
        TableColumn<Ficha, T> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b92a5;");
        return c;
    }

    private Label buildPlaceholder() {
        Label lbl = new Label("Sin datos. Carga un archivo Excel para comenzar.");
        lbl.setStyle("-fx-text-fill: #4b5263; -fx-font-size: 13px;");
        return lbl;
    }

    public void applySearch(String term) {
        if (filtro == null)
            return;
        String t = term == null ? "" : term.trim().toLowerCase();
        filtro.setPredicate(f -> {
            if (t.isEmpty())
                return true;
            return String.valueOf(f.getNumero()).contains(t)
                    || safe(f.getPrograma()).contains(t)
                    || safe(f.getNivel()).contains(t)
                    || safe(f.getEstado().getLabel()).contains(t);
        });
    }

    private void aplicarFiltros(
            String term,
            ToggleButton ejecucion,
            ToggleButton finalizado,
            ToggleButton tecnologo,
            ToggleButton tecnico) {
        if (filtro == null)
            return;

        String t = term == null ? "" : term.toLowerCase();

        filtro.setPredicate(f -> {

            String tNorm = normalize(t);

            boolean busqueda = tNorm.isEmpty()
                    || String.valueOf(f.getNumero()).contains(tNorm)
                    || normalize(f.getPrograma()).contains(tNorm)
                    || normalize(f.getNivel()).contains(tNorm)
                    || normalize(f.getEstado().getLabel()).contains(tNorm);

            boolean estadoOK = (!ejecucion.isSelected() && !finalizado.isSelected())
                    || (ejecucion.isSelected() && normalize(f.getEstado().getLabel()).contains("ejecucion"))
                    || (finalizado.isSelected() && normalize(f.getEstado().getLabel()).contains("finalizado"));

            boolean nivelOK = (!tecnologo.isSelected() && !tecnico.isSelected())
                    || (tecnologo.isSelected() && normalize(f.getNivel()).contains("tecnologo"))
                    || (tecnico.isSelected() && normalize(f.getNivel()).contains("tecnico"));

            return busqueda && estadoOK && nivelOK;
        });
    }

    private String safe(String v) {
        return v == null ? "" : v.toLowerCase();
    }

    private String getToggleStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #39A900; -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-padding: 5 12 5 12;";
        } else {
            return "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-padding: 5 12 5 12; " +
                    "-fx-border-color: rgba(255,255,255,0.2); -fx-border-radius: 20;";
        }
    }

    private String normalize(String text) {
        if (text == null)
            return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }

}