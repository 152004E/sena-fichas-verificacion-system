package ui.Components;

import Model.EstadoFicha;
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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;

public class ConsultFichasView extends VBox {

    private final ObservableList<Ficha> fichas = FXCollections.observableArrayList();
    private DashboardView dashboard;
    private FilteredList<Ficha> filtro;
    private Label estadoLabel;

    // View containers
    private StackPane viewContainer;
    private TableView<Ficha> table;
    private ScrollPane gridScrollPane;
    private FlowPane gridPane;

    // Referencias a los botones para actualizar contadores
    private ToggleButton toggleEjecucion;
    private ToggleButton toggleFinalizado;
    private ToggleButton toggleTecnologo;
    private ToggleButton toggleTecnico;

    public void setDashboard(DashboardView dashboard) {
        this.dashboard = dashboard;
    }

    private static final String DARK_BG = "-fx-background-color: #0f1117;";

    public ConsultFichasView() {
        setStyle(DARK_BG);
        setSpacing(0);
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addAll(buildHeroBanner(), buildTableSection());
        cargarDatosGuardados();
    }

    private void cargarDatosGuardados() {
        try {
            DatabaseManager db = new DatabaseManager();
            db.conectar();
            ResultSet rs = db.obtenerFichas();

            List<Ficha> listaFichas = new ArrayList<>();
            while (rs.next()) {
                Ficha f = new Ficha();
                f.setNumero(rs.getInt("numero"));
                f.setPrograma(rs.getString("programa"));
                f.setNivel(rs.getString("nivel"));
                f.setAprendices(rs.getInt("aprendices"));
                f.setFechaInicio(rs.getString("fecha_inicio"));
                f.setFechaFinLec(rs.getString("fecha_fin_lec"));
                f.setFechaFin(rs.getString("fecha_fin"));
                f.setInstructorTecnico2025(rs.getString("instructor_tecnico_2025"));
                f.setInstructorBilinguismo(rs.getString("instructor_bilinguismo"));
                f.setInstructorTecnico2026(rs.getString("instructor_tecnico_2026"));
                String tfStr = rs.getString("transversales_faltantes");
                if (tfStr != null && !tfStr.trim().isEmpty()) {
                    f.setTransversalesFaltantes(new ArrayList<>(List.of(tfStr.split(";"))));
                } else {
                    f.setTransversalesFaltantes(new ArrayList<>());
                }
                // Cargar transversales vistas desde BD
                String tvStr = rs.getString("transversales_vistas");
                f.setTransversalesVistas(DatabaseManager.deserializarVistas(tvStr));

                f.setTrimestre(rs.getString("trimestre"));
                f.setAcuerdo(rs.getString("acuerdo"));
                f.setEvaluacion(rs.getString("evaluacion"));

                String estadoStr = rs.getString("estado");
                f.setEstado(EstadoFicha.fromString(estadoStr));

                listaFichas.add(f);
            }

            fichas.setAll(listaFichas);
            if (!listaFichas.isEmpty()) {
                estadoLabel.setText("✓ " + listaFichas.size() + " fichas cargadas desde BD");
                actualizarContadores();
            }

            db.desconectar();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        toggleEjecucion = new ToggleButton("En ejecución");
        toggleFinalizado = new ToggleButton("Finalizado");
        toggleTecnologo = new ToggleButton("Tecnólogo");
        toggleTecnico = new ToggleButton("Técnico");
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
        hero.setAlignment(Pos.CENTER);
        HBox.setHgrow(left, Priority.ALWAYS);
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

        FontIcon gridIcon = new FontIcon("fas-th-large");
        gridIcon.setIconSize(14);
        gridIcon.setIconColor(Color.web("#39A900"));
        Button btnGrid = new Button("", gridIcon);
        btnGrid.setStyle(
                "-fx-background-color: #2a2d3a; -fx-background-radius: 6 0 0 6; -fx-padding: 6 12; -fx-cursor: hand;");

        FontIcon listIcon = new FontIcon("fas-list");
        listIcon.setIconSize(14);
        listIcon.setIconColor(Color.web("#8b92a5"));
        Button btnList = new Button("", listIcon);
        btnList.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 0 6 6 0; -fx-padding: 6 12; -fx-cursor: hand;");

        HBox toggleBox = new HBox(btnGrid, btnList);
        toggleBox.setStyle(
                "-fx-background-color: #13161f; -fx-background-radius: 6; -fx-border-color: #2a2d3a; -fx-border-radius: 6;");

        HBox headerRow = new HBox(12, estadoLabel, spacer, toggleBox, btnCargar);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 16, 10, 16));

        viewContainer = new StackPane();
        VBox.setVgrow(viewContainer, Priority.ALWAYS);

        table = buildTable();

        gridPane = new FlowPane(16, 16);
        gridPane.setPadding(new Insets(10));
        gridPane.setAlignment(Pos.TOP_LEFT);

        gridScrollPane = new ScrollPane(gridPane);
        gridScrollPane.setFitToWidth(true);
        gridScrollPane.setStyle("-fx-background: #0f1117; -fx-border-color: transparent;");
        gridScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        filtro = new FilteredList<>(fichas, p -> true);
        table.setItems(filtro);

        filtro.addListener((javafx.collections.ListChangeListener.Change<? extends Ficha> c) -> {
            actualizarGrid();
        });

        viewContainer.getChildren().add(gridScrollPane); // Por defecto mostramos grid
        actualizarGrid();

        btnGrid.setOnAction(e -> {
            btnGrid.setStyle(
                    "-fx-background-color: #2a2d3a; -fx-background-radius: 6 0 0 6; -fx-padding: 6 12; -fx-cursor: hand;");
            gridIcon.setIconColor(Color.web("#39A900"));
            btnList.setStyle(
                    "-fx-background-color: transparent; -fx-background-radius: 0 6 6 0; -fx-padding: 6 12; -fx-cursor: hand;");
            listIcon.setIconColor(Color.web("#8b92a5"));
            viewContainer.getChildren().setAll(gridScrollPane);
        });

        btnList.setOnAction(e -> {
            btnList.setStyle(
                    "-fx-background-color: #2a2d3a; -fx-background-radius: 0 6 6 0; -fx-padding: 6 12; -fx-cursor: hand;");
            listIcon.setIconColor(Color.web("#39A900"));
            btnGrid.setStyle(
                    "-fx-background-color: transparent; -fx-background-radius: 6 0 0 6; -fx-padding: 6 12; -fx-cursor: hand;");
            gridIcon.setIconColor(Color.web("#8b92a5"));
            viewContainer.getChildren().setAll(table);
        });

        VBox section = new VBox(10, headerRow, viewContainer);
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
                actualizarContadores();
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
        TableColumn<Ficha, Integer> colNum = col("Ficha", 80);
        colNum.setCellValueFactory(new PropertyValueFactory<>("numero"));

        TableColumn<Ficha, String> colProg = col("Programa", 220);
        colProg.setCellValueFactory(new PropertyValueFactory<>("programa"));

        TableColumn<Ficha, String> colNivel = col("Nivel", 100);
        colNivel.setCellValueFactory(new PropertyValueFactory<>("nivel"));

        TableColumn<Ficha, String> colInicio = col("Inicio", 110);
        colInicio.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));

        TableColumn<Ficha, String> colFinLec = col("Fin Lectiva", 110);
        colFinLec.setCellValueFactory(new PropertyValueFactory<>("fechaFinLec"));

        TableColumn<Ficha, String> colFin = col("Fin", 110);
        colFin.setCellValueFactory(new PropertyValueFactory<>("fechaFin"));

        // Estado con color
        TableColumn<Ficha, String> colEstado = col("Estado", 100);
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

        TableColumn<Ficha, Void> colDetalle = new TableColumn<>("transversales");
        FontIcon icon = new FontIcon("fas-eye");
        icon.setIconSize(14);
        icon.setIconColor(Color.WHITE);
        colDetalle.setCellFactory(col -> new TableCell<>() {

            private final Button btn;

            {
                FontIcon icon = new FontIcon("fas-eye"); // 👈 CORREGIDO
                icon.setIconSize(14);
                icon.setIconColor(Color.WHITE);

                btn = new Button("Ver Transversales", icon);

                btn.setStyle(
                        "-fx-background-color: #39A900; -fx-text-fill: white;" +
                                "-fx-background-radius: 20; -fx-padding: 4 12; -fx-cursor: hand;");

                btn.setContentDisplay(ContentDisplay.LEFT);
                btn.setGraphicTextGap(5);

                btn.setOnAction(e -> {
                    Ficha ficha = getTableView().getItems().get(getIndex());
                    mostrarModalDetalle(ficha);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        TableColumn<Ficha, String> colTrimestre = col("Trimestre", 120);
        colTrimestre.setCellValueFactory(new PropertyValueFactory<>("trimestre"));

        TableColumn<Ficha, String> colAcuerdo = col("Acuerdo", 100);
        colAcuerdo.setCellValueFactory(new PropertyValueFactory<>("acuerdo"));

        TableColumn<Ficha, String> colEvaluacion = col("Evaluación", 100);
        colEvaluacion.setCellValueFactory(new PropertyValueFactory<>("evaluacion"));

        TableColumn<Ficha, Integer> colApr = col("Aprendices", 100);
        colApr.setCellValueFactory(new PropertyValueFactory<>("aprendices"));

        table.getColumns().addAll(List.of(
                colNum, colProg, colNivel,
                colInicio, colFinLec, colFin,
                colEstado, colTrimestre, colAcuerdo, colEvaluacion,
                colI25, colBil, colI26, colApr,
                colDetalle // 👈 NUEVA COLUMNA
        ));

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

        // ← MENÚ CONTEXTUAL AQUÍ, FUERA DEL setRowFactory
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyCell = new MenuItem("📋 Copiar celda");
        MenuItem copyRow = new MenuItem("📋 Copiar fila");

        copyCell.setOnAction(e -> {
            Ficha ficha = table.getSelectionModel().getSelectedItem();
            if (ficha != null) {
                @SuppressWarnings("rawtypes")
                TablePosition pos = table.getFocusModel().getFocusedCell();
                if (pos != null) {
                    @SuppressWarnings("rawtypes")
                    TableColumn col = pos.getTableColumn();
                    Object cellValue = col.getCellData(ficha);
                    copiarAlPortapapeles(cellValue != null ? cellValue.toString() : "");
                }
            }
        });

        copyRow.setOnAction(e -> {
            Ficha ficha = table.getSelectionModel().getSelectedItem();
            if (ficha != null) {
                StringBuilder sb = new StringBuilder();
                sb.append(ficha.getNumero()).append("\t")
                        .append(ficha.getPrograma()).append("\t")
                        .append(ficha.getNivel()).append("\t")
                        .append(ficha.getAprendices()).append("\t")
                        .append(ficha.getFechaInicio()).append("\t")
                        .append(ficha.getFechaFinLec()).append("\t")
                        .append(ficha.getFechaFin()).append("\t")
                        .append(ficha.getEstado().getLabel()).append("\t")
                        .append(ficha.getTrimestre()).append("\t")
                        .append(ficha.getAcuerdo()).append("\t")
                        .append(ficha.getEvaluacion()).append("\t")
                        .append(ficha.getInstructorTecnico2025()).append("\t")
                        .append(ficha.getInstructorBilinguismo()).append("\t")
                        .append(ficha.getInstructorTecnico2026()).append("\t")
                        .append(ficha.getTransversalesFaltantes() != null
                                ? String.join(" ", ficha.getTransversalesFaltantes())
                                : "");

                copiarAlPortapapeles(sb.toString());
            }
        });

        contextMenu.getItems().addAll(copyCell, copyRow);
        table.setContextMenu(contextMenu);

        return table;
    }

    private <T> TableColumn<Ficha, T> col(String title, double width) {
        TableColumn<Ficha, T> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setStyle("-fx-alignment: CENTER; -fx-font-size: 12px; -fx-text-fill: #8b92a5;");
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

    private void copiarAlPortapapeles(String texto) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(texto);
        clipboard.setContent(content);
    }

    private void mostrarModalDetalle(Ficha ficha) {
        ModalDetalleFicha modal = new ModalDetalleFicha(ficha);
        StackPane stackRoot = (StackPane) this.getScene().getRoot();
        modal.prefWidthProperty().bind(stackRoot.widthProperty());
        modal.prefHeightProperty().bind(stackRoot.heightProperty());
        stackRoot.getChildren().add(modal);
    }

    private void actualizarContadores() {
        if (fichas.isEmpty()) {
            toggleEjecucion.setText("En ejecución");
            toggleFinalizado.setText("Finalizado");
            toggleTecnologo.setText("Tecnólogo");
            toggleTecnico.setText("Técnico");
            return;
        }

        long ejecucion = fichas.stream()
                .filter(f -> normalize(f.getEstado().getLabel()).contains("ejecucion"))
                .count();

        long finalizado = fichas.stream()
                .filter(f -> normalize(f.getEstado().getLabel()).contains("finalizado"))
                .count();

        long tecnologo = fichas.stream()
                .filter(f -> normalize(f.getNivel()).contains("tecnologo"))
                .count();

        long tecnico = fichas.stream()
                .filter(f -> normalize(f.getNivel()).contains("tecnico"))
                .count();

        toggleEjecucion.setText("En ejecución (" + ejecucion + ")");
        toggleFinalizado.setText("Finalizado (" + finalizado + ")");
        toggleTecnologo.setText("Tecnólogo (" + tecnologo + ")");
        toggleTecnico.setText("Técnico (" + tecnico + ")");
    }

    private void actualizarGrid() {
        if (gridPane == null)
            return;
        gridPane.getChildren().clear();
        for (Ficha f : filtro) {
            gridPane.getChildren().add(buildCard(f));
        }
    }

    private VBox buildCard(Ficha ficha) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #13161f; -fx-background-radius: 16; -fx-border-color: #2a2d3a; -fx-border-radius: 16; -fx-border-width: 1;");
        card.setPrefWidth(300);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #1a1e2b; -fx-background-radius: 16; -fx-border-color: #39A900; -fx-border-radius: 16; -fx-border-width: 1; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: #13161f; -fx-background-radius: 16; -fx-border-color: #2a2d3a; -fx-border-radius: 16; -fx-border-width: 1;"));

        card.setOnMouseClicked(e -> mostrarModalDetalle(ficha));

        Label idBadge = new Label(String.valueOf(ficha.getNumero()));
        idBadge.setStyle(
                "-fx-background-color: #1e2230; -fx-text-fill: #e8eaf0; -fx-padding: 4 10; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label statusBadge = new Label(ficha.getEstado().getLabel().toUpperCase());
        String statusStyle = getStatusStyle(ficha.getEstado().getLabel());
        statusBadge.setStyle(statusStyle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(idBadge, spacer, statusBadge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(ficha.getPrograma() != null ? ficha.getPrograma() : "SIN PROGRAMA");
        title.setWrapText(true);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #e8eaf0;");
        title.setMinHeight(40);
        title.setAlignment(Pos.TOP_LEFT);

        Label lblNivel = new Label(ficha.getNivel() != null ? ficha.getNivel().toUpperCase() : "N/A");
        lblNivel.setStyle(
                "-fx-background-color: #1e2230; -fx-text-fill: #8b92a5; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        FontIcon userIcon = new FontIcon("fas-users");
        userIcon.setIconColor(Color.web("#8b92a5"));
        userIcon.setIconSize(10);
        Label lblAprendices = new Label(ficha.getAprendices() + " Aprendices", userIcon);
        lblAprendices.setStyle(
                "-fx-background-color: #1e2230; -fx-text-fill: #8b92a5; -fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;");

        HBox badgesRow = new HBox(8, lblNivel, lblAprendices);

        VBox instBox = new VBox(4);
        instBox.getChildren().add(createInstLabel("Inst. 2025", ficha.getInstructorTecnico2025()));
        instBox.getChildren().add(createInstLabel("Inst. 2026", ficha.getInstructorTecnico2026()));
        instBox.getChildren().add(createInstLabel("Bilingüismo", ficha.getInstructorBilinguismo()));

        HBox datesRow = new HBox(6);
        datesRow.getChildren().addAll(
                createDataBox("INICIO", ficha.getFechaInicio()),
                createDataBox("FIN LEC", ficha.getFechaFinLec()),
                createDataBox("FIN", ficha.getFechaFin()));

        HBox acadRow = new HBox(6);
        acadRow.getChildren().addAll(
                createDataBox("TRIM", ficha.getTrimestre()),
                createDataBox("ACUERDO", ficha.getAcuerdo()),
                createDataBox("EVAL", ficha.getEvaluacion()));

        HBox transRow = new HBox(15);
        transRow.setStyle("-fx-border-color: #2a2d3a; -fx-border-width: 1 0 0 0; -fx-padding: 12 0 0 0;");

        int vistasCount = ficha.getTransversalesVistas() != null ? ficha.getTransversalesVistas().size() : 0;
        FontIcon vIcon = new FontIcon("fas-check-circle");
        vIcon.setIconColor(Color.web("#39A900"));
        Label lblVistas = new Label(vistasCount + " Vistas", vIcon);
        lblVistas.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 10px; -fx-font-weight: bold;");

        List<String> faltList = ficha.getTransversalesFaltantes();
        int faltCount = (faltList != null) ? faltList.size() : 0;
        FontIcon fIcon = new FontIcon(faltCount > 0 ? "fas-exclamation-triangle" : "fas-check-circle");
        fIcon.setIconColor(Color.web(faltCount > 0 ? "#e24b4a" : "#8b92a5"));
        Label lblFalt = new Label(faltCount + " Faltantes", fIcon);
        lblFalt.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 10px; -fx-font-weight: bold;");

        transRow.getChildren().addAll(lblVistas, lblFalt);

        card.getChildren().addAll(topRow, title, badgesRow, instBox, datesRow, acadRow, transRow);
        return card;
    }

    private Label createInstLabel(String role, String name) {
        FontIcon instIcon = new FontIcon("fas-user");
        instIcon.setIconColor(Color.web("#8b92a5"));
        instIcon.setIconSize(10);

        String cleanName = (name == null || name.isBlank()) ? "Sin Asignar" : name;
        Label lbl = new Label(role + ": " + cleanName, instIcon);
        lbl.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 10px;");
        return lbl;
    }

    private VBox createDataBox(String title, String value) {
        VBox box = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #8b92a5; -fx-font-size: 8px; -fx-font-weight: bold;");
        Label lblValue = new Label((value == null || value.isBlank()) ? "—" : value);
        lblValue.setStyle("-fx-text-fill: #e8eaf0; -fx-font-size: 10px; -fx-font-weight: bold;");
        box.getChildren().addAll(lblTitle, lblValue);
        box.setStyle("-fx-background-color: #0f1117; -fx-padding: 6 8; -fx-background-radius: 8;");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String getStatusStyle(String status) {
        if (status.toLowerCase().contains("completa") || status.toLowerCase().contains("ok")) {
            return "-fx-background-color: #1c3a12; -fx-text-fill: #5ed01a; -fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 10px; -fx-font-weight: bold;";
        } else if (status.toLowerCase().contains("error") || status.toLowerCase().contains("crítico")) {
            return "-fx-background-color: #2d1515; -fx-text-fill: #e24b4a; -fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 10px; -fx-font-weight: bold;";
        } else {
            return "-fx-background-color: #2a2010; -fx-text-fill: #d97706; -fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 10px; -fx-font-weight: bold;";
        }
    }
}