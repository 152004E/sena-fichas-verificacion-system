package ui;

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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.List;

public class MainWindow {

    private final ObservableList<Ficha> fichas = FXCollections.observableArrayList();

    public void show(Stage stage) {
        // ── Título ──────────────────────────────────────────────
        Label titulo = new Label("🚀 SENA Fichas Manager");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #39A900;");

        Label subtitulo = new Label("Sistema de Validación y Gestión de Fichas");
        subtitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        Label estado = new Label("✅ Selecciona un archivo Excel para iniciar la validación local");
        estado.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");

        Button btnCargar = new Button("📂 Cargar Excel local");
        btnCargar.setStyle(
            "-fx-background-color: #39A900; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-cursor: hand;"
        );

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar por número, programa, nivel o estado...");
        searchField.setMinWidth(350);

        TableView<Ficha> tablaFichas = crearTablaFichas();
        FilteredList<Ficha> filtro = new FilteredList<>(fichas, p -> true);
        tablaFichas.setItems(filtro);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String termino = newValue == null ? "" : newValue.trim().toLowerCase();
            filtro.setPredicate(ficha -> {
                if (termino.isEmpty()) return true;
                return String.valueOf(ficha.getNumero()).contains(termino)
                    || safe(ficha.getPrograma()).contains(termino)
                    || safe(ficha.getNivel()).contains(termino)
                    || safe(ficha.getEstado().getLabel()).contains(termino);
            });
        });

        btnCargar.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Seleccionar archivo Excel");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls")
            );
            File archivo = chooser.showOpenDialog(stage);
            if (archivo != null) {
                try {
                    DatabaseManager db = new DatabaseManager();
                    SyncService sync = new SyncService(db);
                    List<Ficha> lista = sync.ejecutarSincronizacion(new LocalExcelSource(archivo.toPath()));
                    fichas.setAll(lista);
                    estado.setText("✅ Cargado y sincronizado: " + archivo.getName() + " (" + lista.size() + " fichas)");
                } catch (Exception ex) {
                    estado.setText("❌ Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        HBox header = new HBox(20, btnCargar, searchField);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 20, 20));

        VBox topContainer = new VBox(10, titulo, subtitulo, estado, header);
        topContainer.setPadding(new Insets(20, 20, 10, 20));

        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(tablaFichas);
        root.setStyle("-fx-background-color: #f5f5f5;");

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("SENA Fichas Manager");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    private TableView<Ficha> crearTablaFichas() {
        TableView<Ficha> table = new TableView<>();

        TableColumn<Ficha, Integer> colNumero = new TableColumn<>("Ficha");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setPrefWidth(100);

        TableColumn<Ficha, String> colPrograma = new TableColumn<>("Programa");
        colPrograma.setCellValueFactory(new PropertyValueFactory<>("programa"));
        colPrograma.setPrefWidth(230);

        TableColumn<Ficha, String> colNivel = new TableColumn<>("Nivel");
        colNivel.setCellValueFactory(new PropertyValueFactory<>("nivel"));
        colNivel.setPrefWidth(130);

        TableColumn<Ficha, Integer> colAprendices = new TableColumn<>("Aprendices");
        colAprendices.setCellValueFactory(new PropertyValueFactory<>("aprendices"));
        colAprendices.setPrefWidth(110);

        TableColumn<Ficha, String> colInicio = new TableColumn<>("Fecha inicio");
        colInicio.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        colInicio.setPrefWidth(120);

        TableColumn<Ficha, String> colFinLectiva = new TableColumn<>("Fecha fin lectiva");
        colFinLectiva.setCellValueFactory(new PropertyValueFactory<>("fechaFinLec"));
        colFinLectiva.setPrefWidth(120);

        TableColumn<Ficha, String> colFin = new TableColumn<>("Fecha fin");
        colFin.setCellValueFactory(new PropertyValueFactory<>("fechaFin"));
        colFin.setPrefWidth(120);

        TableColumn<Ficha, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEstado().getLabel()));
        colEstado.setPrefWidth(140);

        TableColumn<Ficha, String> colInstructor2025 = new TableColumn<>("Instructor 2025");
        colInstructor2025.setCellValueFactory(new PropertyValueFactory<>("instructorTecnico2025"));
        colInstructor2025.setPrefWidth(180);

        TableColumn<Ficha, String> colBilinguismo = new TableColumn<>("Bilinguismo");
        colBilinguismo.setCellValueFactory(new PropertyValueFactory<>("instructorBilinguismo"));
        colBilinguismo.setPrefWidth(180);

        TableColumn<Ficha, String> colInstructor2026 = new TableColumn<>("Instructor 2026");
        colInstructor2026.setCellValueFactory(new PropertyValueFactory<>("instructorTecnico2026"));
        colInstructor2026.setPrefWidth(180);

        TableColumn<Ficha, String> colTransversales = new TableColumn<>("Transversales faltantes");
        colTransversales.setCellValueFactory(new PropertyValueFactory<>("transversalesFaltantes"));
        colTransversales.setPrefWidth(220);

        table.getColumns().addAll(
            colNumero,
            colPrograma,
            colNivel,
            colAprendices,
            colInicio,
            colFinLectiva,
            colFin,
            colEstado,
            colInstructor2025,
            colBilinguismo,
            colInstructor2026,
            colTransversales
        );

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
