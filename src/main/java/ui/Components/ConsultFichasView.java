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

    // Referencias a los botones para actualizar contadores
    private ToggleButton toggleEjecucion;
    private ToggleButton toggleFinalizado;
    private ToggleButton toggleTecnologo;
    private ToggleButton toggleTecnico;

    // Vistas y toggle de modo
    private TableView<Ficha> tableView;
    private ListView<Ficha> cardsView;
    private ToggleButton btnVistaTabla;
    private ToggleButton btnVistaTarjetas;

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
        cargarDatosGuardados();
    }

    private void cambiarVista(String vista) {
        boolean mostrarTabla = "tabla".equals(vista);
        boolean mostrarTarjetas = "tarjetas".equals(vista);

        tableView.setVisible(mostrarTabla);
        tableView.setManaged(mostrarTabla);

        if (cardsView != null) {
            cardsView.setVisible(mostrarTarjetas);
            cardsView.setManaged(mostrarTarjetas);
        }

        btnVistaTabla.setSelected(mostrarTabla);
        btnVistaTarjetas.setSelected(mostrarTarjetas);

        btnVistaTabla.setStyle(getViewToggleStyle(mostrarTabla));
        btnVistaTarjetas.setStyle(getViewToggleStyle(mostrarTarjetas));
    }

    private String getViewToggleStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #39A900; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-padding: 6 12 6 12; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;";
        } else {
            return "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #8b92a5; " +
                    "-fx-background-radius: 6; -fx-padding: 6 12 6 12; " +
                    "-fx-font-size: 11px; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6;";
        }
    }

    private void cargarDatosGuardados() {
        try (DatabaseManager db = new DatabaseManager()) {
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
                f.setTransversalesFaltantes(rs.getString("transversales_faltantes"));
                f.setTrimestre(rs.getString("trimestre"));
                f.setAcuerdo(rs.getString("acuerdo"));
                f.setEvaluacion(rs.getString("evaluacion"));
                f.setEstado(EstadoFicha.fromString(rs.getString("estado")));
                listaFichas.add(f);
            }

            fichas.setAll(listaFichas);
            if (!listaFichas.isEmpty()) {
                estadoLabel.setText("✓ " + listaFichas.size() + " fichas cargadas desde BD");
                actualizarContadores();
            }

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
        // Header row: estado + botón cargar + toggle vistas
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

        // Toggle de vistas
        Label lblVista = new Label("VISTA:");
        lblVista.setStyle("-fx-font-size: 9px; -fx-text-fill: #4b5263; -fx-letter-spacing: 0.08em;");

        btnVistaTabla = new ToggleButton("📊 Tabla");
        btnVistaTarjetas = new ToggleButton("🎴 Tarjetas");

        btnVistaTabla.setSelected(true);
        btnVistaTabla.setStyle(getViewToggleStyle(true));
        btnVistaTarjetas.setStyle(getViewToggleStyle(false));

        btnVistaTabla.setOnAction(e -> cambiarVista("tabla"));
        btnVistaTarjetas.setOnAction(e -> cambiarVista("tarjetas"));

        HBox vistaToggles = new HBox(6, btnVistaTabla, btnVistaTarjetas);
        vistaToggles.setAlignment(Pos.CENTER);
        vistaToggles.setStyle("-fx-padding: 0 0 0 12;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox headerRow = new HBox(12, estadoLabel, spacer1, lblVista, vistaToggles, btnCargar);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 16, 10, 16));

        // Tabla
        tableView = buildTable();
        filtro = new FilteredList<>(fichas, p -> true);
        tableView.setItems(filtro);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Vista de tarjetas
        cardsView = buildCardsView();
        cardsView.setItems(filtro);
        cardsView.setVisible(false);
        cardsView.setManaged(false);

        VBox section = new VBox(10, headerRow, tableView, cardsView);
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

        table.getColumns().addAll(
                colNum, colProg, colNivel,
                colInicio, colFinLec, colFin,
                colEstado, colTrimestre, colAcuerdo, colEvaluacion,
                colI25, colBil, colI26, colApr,
                colDetalle // 👈 NUEVA COLUMNA
        );

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
                TablePosition<Ficha, ?> pos = table.getFocusModel().getFocusedCell();
                if (pos != null) {
                    Object cellValue = pos.getTableColumn().getCellData(ficha);
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
                        .append(ficha.getTransversalesFaltantes());

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

    // ── Vista de Lista ───────────────────────────────────────────
    private VBox buildListView() {
        VBox listContainer = new VBox(8);
        listContainer.setStyle("-fx-background-color: #13161f; -fx-background-radius: 10;" +
                      "-fx-border-color: #2a2d3a; -fx-border-radius: 10; -fx-border-width: 0.5;");

        VBox list = new VBox(8);
        list.setPadding(new Insets(8));

        for (Ficha f : fichas) {
            list.getChildren().add(buildListItem(f));
        }

        fichas.addListener((javafx.collections.ListChangeListener.Change<? extends Ficha> c) -> {
            list.getChildren().clear();
            for (Ficha f : fichas) {
                list.getChildren().add(buildListItem(f));
            }
        });

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        scroll.setPadding(new Insets(8));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        listContainer.getChildren().add(scroll);
        return listContainer;
    }

    private HBox buildListItem(Ficha f) {
        Label num = new Label(String.valueOf(f.getNumero()));
        num.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #39A900;" +
                     "-fx-min-width: 60; -fx-alignment: center-left;");

        VBox info = new VBox(4);
        Label prog = new Label(f.getPrograma());
        prog.setStyle("-fx-font-size: 12px; -fx-text-fill: #e8eaf0;");
        Label nivel = new Label(f.getNivel() + " • " + f.getEstado().getLabel());
        nivel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
        info.getChildren().addAll(prog, nivel);
        VBox.setVgrow(info, Priority.ALWAYS);

        Label fechas = new Label(f.getFechaInicio() + " → " + f.getFechaFin());
        fechas.setStyle("-fx-font-size: 10px; -fx-text-fill: #8b92a5;");

        FontIcon iconEye = new FontIcon("fas-eye");
        iconEye.setIconSize(14);
        iconEye.setIconColor(Color.WHITE);
        Button btnVer = new Button("Ver", iconEye);
        btnVer.setStyle("-fx-background-color: #1e2230; -fx-text-fill: #c8ccd8;" +
                        "-fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;" +
                        "-fx-font-size: 11px; -fx-border-color: #2a2d3a; -fx-border-radius: 6;");
        btnVer.setOnAction(e -> mostrarModalDetalle(f));

        HBox item = new HBox(12, num, info, fechas, btnVer);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-color: #0a0c12; -fx-background-radius: 8;" +
                      "-fx-border-color: #2a2d3a; -fx-border-radius: 8; -fx-border-width: 0.5;");
        item.setAlignment(Pos.CENTER_LEFT);

        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-background-color: #1e2230; -fx-background-radius: 8;" +
                "-fx-border-color: #39A900; -fx-border-radius: 8; -fx-border-width: 0.5;"));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-background-color: #0a0c12; -fx-background-radius: 8;" +
                "-fx-border-color: #2a2d3a; -fx-border-radius: 8; -fx-border-width: 0.5;"));

        HBox.setHgrow(info, Priority.ALWAYS);
        return item;
    }

    // ── Vista de Tarjetas (Dashboard) ────────────────────────────
    private ListView<Ficha> buildCardsView() {
        ListView<Ficha> cardsList = new ListView<>();
        cardsList.setStyle(
                "-fx-background-color: #13161f; -fx-background-radius: 10; " +
                "-fx-border-color: #2a2d3a; -fx-border-radius: 10; -fx-border-width: 0.5; " +
                "-fx-control-inner-background: transparent;");
        cardsList.setPlaceholder(buildPlaceholder());
        cardsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Ficha ficha, boolean empty) {
                super.updateItem(ficha, empty);
                if (empty || ficha == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox card = buildCard(ficha);
                    card.setMaxWidth(Double.MAX_VALUE);
                    setGraphic(card);
                    setText(null);
                }
                setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            }
        });
        cardsList.setFocusTraversable(false);
        return cardsList;
    }

    private VBox buildCard(Ficha f) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #0a0c12; -fx-background-radius: 12;" +
                "-fx-border-color: #2a2d3a; -fx-border-radius: 12; -fx-border-width: 0.5;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");

        // Header: número y estado
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label numLabel = new Label("Ficha #" + f.getNumero());
        numLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #39A900;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label estadoBadge = new Label(f.getEstado().getLabel());
        String badgeStyle;
        if (f.getEstado().getLabel().toLowerCase().contains("completa") ||
            f.getEstado().getLabel().toLowerCase().contains("ok")) {
            badgeStyle = "-fx-background-color: #1c3a12; -fx-text-fill: #5ed01a; -fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px;";
        } else if (f.getEstado().getLabel().toLowerCase().contains("error") ||
                   f.getEstado().getLabel().toLowerCase().contains("crítico")) {
            badgeStyle = "-fx-background-color: #2d1515; -fx-text-fill: #e24b4a; -fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px;";
        } else {
            badgeStyle = "-fx-background-color: #2a2010; -fx-text-fill: #d97706; -fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px;";
        }
        estadoBadge.setStyle(badgeStyle);

        header.getChildren().addAll(numLabel, spacer, estadoBadge);

        // Info grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(8);
        infoGrid.setVgap(8);

        int row = 0;
        infoGrid.add(createInfoLabel("Programa:", f.getPrograma(), true), 0, row++);
        infoGrid.add(createInfoLabel("Nivel:", f.getNivel(), false), 0, row++);
        infoGrid.add(createInfoLabel("Inicio:", f.getFechaInicio(), false), 0, row++);
        infoGrid.add(createInfoLabel("Fin Lec:", f.getFechaFinLec(), false), 0, row++);
        infoGrid.add(createInfoLabel("Fin:", f.getFechaFin(), false), 0, row++);
        infoGrid.add(createInfoLabel("Trimestre:", f.getTrimestre(), false), 0, row++);
        infoGrid.add(createInfoLabel("Aprendices:", String.valueOf(f.getAprendices()), false), 0, row++);

        // Instructores
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #2a2d3a; -fx-padding: 2 0;");
        infoGrid.add(sep1, 0, row++);
        GridPane.setColumnSpan(sep1, 1);

        infoGrid.add(createInfoLabel("Instructor 2025:", f.getInstructorTecnico2025(), false), 0, row++);
        infoGrid.add(createInfoLabel("Bilingüismo:", f.getInstructorBilinguismo(), false), 0, row++);
        infoGrid.add(createInfoLabel("Instructor 2026:", f.getInstructorTecnico2026(), false), 0, row++);

        // Botones de acción
        Button btnVer = new Button("👁 Ver Detalles");
        btnVer.setMaxWidth(Double.MAX_VALUE);
        btnVer.setStyle(
                "-fx-background-color: #39A900; -fx-text-fill: white;" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 12;");
        btnVer.setOnAction(e -> mostrarModalDetalle(f));

        card.getChildren().addAll(header, infoGrid, btnVer);
        return card;
    }

    private VBox createInfoLabel(String label, String value, boolean highlight) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 9px; -fx-text-fill: #4b5263; -fx-letter-spacing: 0.05em;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (highlight ? "#39A900" : "#e8eaf0") + ";" +
                     "-fx-wrap-text: true;");
        val.setWrapText(true);
        box.getChildren().addAll(lbl, val);
        return box;
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

}