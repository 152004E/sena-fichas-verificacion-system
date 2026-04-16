package ui.Components;

import database.DatabaseManager;
import service.SofiaImportService;
import service.SofiaLoginService;
import service.SofiaReporteService;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Vista de descarga masiva de reportes de aprendices desde SOFIA Plus.
 *
 * Flujo de uso:
 *   1. Usuario ingresa credenciales SOFIA
 *   2. Hace click en "Conectar"
 *   3. Elige carpeta de destino
 *   4. Inicia la descarga masiva
 *   5. Ve el progreso en tiempo real por cada ficha
 *   6. Al finalizar, importa automáticamente los XLS a la base de datos
 */
public class DescargaMasivaView extends VBox {

    // ── Servicios ────────────────────────────────────────────────
    private SofiaLoginService  loginService;
    private SofiaReporteService reporteService;
    private SofiaImportService  importService;
    private ExecutorService executor;

    // ── UI ───────────────────────────────────────────────────────
    private TextField    txtUsuario;
    private TextField    txtCarpeta;
    private VBox logBox;
    private PasswordField txtContrasena;
    private TextField    txtContrasenaVisible;
    private Button       btnConectar;
    private Label        lblEstadoLogin;
    private boolean      mostrarContrasena = false;

    private Button       btnCarpeta;
    private Button       btnDescargar;
    private Button       btnCancelar;

    private ProgressBar  progressBar;
    private Label        lblProgreso;
    private Label        lblContador;
    private TextArea     logArea;
    private ScrollPane   logScroll;

    private Path carpetaDestino;
    private volatile boolean cancelado = false;

    // ── Estilos ──────────────────────────────────────────────────
    private static final String DARK     = "-fx-background-color: #0f1117;";
    private static final String CARD     = "-fx-background-color: #13161f; -fx-background-radius: 12;" +
                                           "-fx-border-color: #2a2d3a; -fx-border-radius: 12; -fx-border-width: 0.5;";
    private static final String GREEN    = "#39A900";
    private static final String RED      = "#e24b4a";
    private static final String YELLOW   = "#d97706";
    private static final String BLUE     = "#378add";
    private static final String SUBTEXT  = "-fx-text-fill: #6b7280; -fx-font-size: 11px;";

    public DescargaMasivaView() {
        setStyle(DARK);
        setPadding(new Insets(16));
        setSpacing(14);
        VBox.setVgrow(this, Priority.ALWAYS);
        executor = Executors.newSingleThreadExecutor();
        build();
    }

    // ── Construcción de la UI ────────────────────────────────────

    private void build() {
        // Header
        Label title = new Label("Descarga Masiva de Aprendices");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");
        Label sub = new Label("Descarga automática de reportes XLS desde SOFIA Plus para todas las fichas");
        sub.setStyle(SUBTEXT);

        // Cuerpo: 2 columnas (login + config)
        HBox row1 = new HBox(14, buildCardLogin(), buildCardConfiguracion());
        HBox.setHgrow(row1.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(row1.getChildren().get(1), Priority.ALWAYS);

        // Progreso
        VBox cardProgreso = buildCardProgreso();

        // Aviso técnico
        VBox aviso = buildAviso();

        getChildren().addAll(new VBox(3, title, sub), row1, cardProgreso, aviso);
    }

    // ── Card: Login ──────────────────────────────────────────────

    private VBox buildCardLogin() {
        Label t = new Label("① Credenciales SOFIA Plus");
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label lUser = fieldLabel("USUARIO");
        txtUsuario = styledField("tu.usuario@sena.edu.co");

        Label lPass = fieldLabel("CONTRASEÑA");
        txtContrasena = new PasswordField();
        txtContrasena.setPromptText("••••••••");
        applyFieldStyle(txtContrasena);

        txtContrasenaVisible = new TextField();
        txtContrasenaVisible.setPromptText("••••••••");
        applyFieldStyle(txtContrasenaVisible);
        txtContrasenaVisible.setVisible(false);
        txtContrasenaVisible.setManaged(false);

        Button btnOjo = new Button("👁");
        btnOjo.setPrefSize(36, 36);
        btnOjo.setStyle(
                "-fx-background-color: #1e2230; -fx-text-fill: #c8ccd8;" +
                "-fx-font-size: 16px; -fx-background-radius: 8; -fx-cursor: hand;" +
                "-fx-border-color: #2a2d3a; -fx-border-radius: 8; -fx-padding: 4;");
        btnOjo.setOnAction(e -> {
            mostrarContrasena = !mostrarContrasena;
            if (mostrarContrasena) {
                txtContrasenaVisible.setText(txtContrasena.getText());
                txtContrasena.setVisible(false);
                txtContrasena.setManaged(false);
                txtContrasenaVisible.setVisible(true);
                txtContrasenaVisible.setManaged(true);
            } else {
                txtContrasena.setText(txtContrasenaVisible.getText());
                txtContrasenaVisible.setVisible(false);
                txtContrasenaVisible.setManaged(false);
                txtContrasena.setVisible(true);
                txtContrasena.setManaged(true);
            }
        });

        lblEstadoLogin = new Label("Sin conectar");
        lblEstadoLogin.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        btnConectar = greenButton("🔗  Conectar a SOFIA");
        btnConectar.setMaxWidth(Double.MAX_VALUE);
        btnConectar.setOnAction(e -> conectar());

        HBox passRow = new HBox(8, txtContrasena, txtContrasenaVisible, btnOjo);
        HBox.setHgrow(txtContrasena, Priority.ALWAYS);
        HBox.setHgrow(txtContrasenaVisible, Priority.ALWAYS);

        VBox card = new VBox(10,
                t,
                new VBox(4, lUser, txtUsuario),
                new VBox(4, lPass, passRow),
                lblEstadoLogin,
                btnConectar);
        card.setPadding(new Insets(16));
        card.setStyle(CARD);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── Card: Configuración ──────────────────────────────────────

    private VBox buildCardConfiguracion() {
        Label t = new Label("② Configuración de descarga");
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        Label lCarpeta = fieldLabel("CARPETA DE DESTINO");
        txtCarpeta = styledField("Selecciona una carpeta...");
        txtCarpeta.setEditable(false);

        btnCarpeta = new Button("📁  Explorar");
        btnCarpeta.setStyle(
                "-fx-background-color: #1e2230; -fx-text-fill: #c8ccd8;" +
                "-fx-font-size: 12px; -fx-background-radius: 8; -fx-cursor: hand;" +
                "-fx-border-color: #2a2d3a; -fx-border-radius: 8; -fx-padding: 7 12;");
        btnCarpeta.setOnAction(e -> elegirCarpeta());

        HBox carpetaRow = new HBox(8, txtCarpeta, btnCarpeta);
        HBox.setHgrow(txtCarpeta, Priority.ALWAYS);
        carpetaRow.setAlignment(Pos.CENTER_LEFT);

        // Info de fichas en BD
        Label lFichas = fieldLabel("FICHAS EN BASE DE DATOS");
        int totalFichas = contarFichasEnBD();
        Label lblFichas = new Label(totalFichas > 0
                ? totalFichas + " fichas cargadas — se descargarán " + totalFichas + " reportes"
                : "No hay fichas en BD. Carga un Excel primero.");
        lblFichas.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                (totalFichas > 0 ? GREEN : RED) + ";");

        // Delay entre requests
        Label lDelay = fieldLabel("PAUSA ENTRE REQUESTS (ms)");
        Slider sliderDelay = new Slider(500, 5000, 2000);
        sliderDelay.setShowTickLabels(true);
        sliderDelay.setShowTickMarks(true);
        sliderDelay.setMajorTickUnit(1000);
        sliderDelay.setStyle("-fx-accent: " + GREEN + ";");
        Label lblDelayVal = new Label("2000 ms");
        lblDelayVal.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b92a5;");
        sliderDelay.valueProperty().addListener((obs, o, n) ->
                lblDelayVal.setText((int) n.doubleValue() + " ms"));

        btnDescargar = greenButton("⬇  Iniciar Descarga Masiva");
        btnDescargar.setMaxWidth(Double.MAX_VALUE);
        btnDescargar.setDisable(true);
        btnDescargar.setOnAction(e -> iniciarDescarga((long) sliderDelay.getValue()));

        btnCancelar = new Button("✕  Cancelar");
        btnCancelar.setStyle(
                "-fx-background-color: #2d1515; -fx-text-fill: " + RED + ";" +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8;" +
                "-fx-cursor: hand; -fx-padding: 8 14;");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.setDisable(true);
        btnCancelar.setOnAction(e -> cancelar());

        HBox botonesRow = new HBox(8, btnDescargar, btnCancelar);
        HBox.setHgrow(btnDescargar, Priority.ALWAYS);

        VBox card = new VBox(10,
                t,
                new VBox(4, lCarpeta, carpetaRow),
                new VBox(4, lFichas, lblFichas),
                new VBox(4, lDelay, sliderDelay, lblDelayVal),
                botonesRow);
        card.setPadding(new Insets(16));
        card.setStyle(CARD);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── Card: Progreso ───────────────────────────────────────────

    private VBox buildCardProgreso() {
        Label t = new Label("③ Progreso de descarga");
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e8eaf0;");

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(10);
        progressBar.setStyle("-fx-accent: " + GREEN + "; -fx-background-color: #1e2230; -fx-background-radius: 5;");

        lblProgreso = new Label("Esperando inicio...");
        lblProgreso.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b92a5;");

        lblContador = new Label("0 / 0");
        lblContador.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        HBox infoRow = new HBox();
        infoRow.getChildren().addAll(lblProgreso, spacer(), lblContador);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        // Log de actividad
logBox = new VBox(4);
logBox.setStyle("-fx-background-color: #0a0c12; -fx-padding: 8;");

logScroll = new ScrollPane(logBox);
logScroll.setFitToWidth(true);
logScroll.setPrefHeight(180);
logScroll.setStyle(
        "-fx-background-color: #0a0c12; -fx-background: #0a0c12;" +
        "-fx-border-color: #2a2d3a; -fx-border-radius: 8; -fx-background-radius: 8;");

VBox card = new VBox(10, t, infoRow, progressBar, logScroll);
card.setPadding(new Insets(16));
card.setStyle(CARD);
return card;
    }

    // ── Aviso técnico ────────────────────────────────────────────

    private VBox buildAviso() {
        Label icon = new Label("ℹ");
        icon.setStyle("-fx-text-fill: " + BLUE + "; -fx-font-size: 14px;");
        Label txt = new Label(
                "Los IDs de los campos JSF (CAMPO_FICHA, BTN_SELECCIONAR, BTN_GENERAR) en SofiaReporteService.java " +
                "pueden necesitar ajuste según la versión actual de SOFIA Plus. Si una descarga falla con error 'ViewState vacío' " +
                "o no descarga el archivo, abre DevTools → Network → Payload en el paso que falla y compara los campos con las constantes del servicio.");
        txt.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b92a5;");
        txt.setWrapText(true);
        HBox row = new HBox(10, icon, txt);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle(
                "-fx-background-color: #0d1929; -fx-background-radius: 10;" +
                "-fx-border-color: " + BLUE + "33; -fx-border-radius: 10; -fx-border-width: 0.5;");
        VBox v = new VBox(row);
        return v;
    }

    // ── Acciones ─────────────────────────────────────────────────

    private void conectar() {
        String usuario = txtUsuario.getText().trim();
        String pass    = mostrarContrasena
            ? txtContrasenaVisible.getText()
            : txtContrasena.getText();
        if (usuario.isEmpty() || pass.isEmpty()) {
            setEstadoLogin("Ingresa usuario y contraseña", RED);
            return;
        }

        btnConectar.setDisable(true);
        setEstadoLogin("Conectando...", YELLOW);

        executor.submit(() -> {
            try {
                loginService    = new SofiaLoginService();
                reporteService  = new SofiaReporteService(loginService);
                loginService.login(usuario, pass);

                Platform.runLater(() -> {
                    setEstadoLogin("✓ Conectado correctamente", GREEN);
                    btnConectar.setDisable(false);
                    btnDescargar.setDisable(carpetaDestino == null);
                    agregarLog("✓ Login exitoso en SOFIA Plus", GREEN);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setEstadoLogin("✗ " + ex.getMessage(), RED);
                    btnConectar.setDisable(false);
                    agregarLog("✗ Error de login: " + ex.getMessage(), RED);
                });
            }
        });
    }

    private void elegirCarpeta() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta destino");
        Stage stage = (Stage) getScene().getWindow();
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            carpetaDestino = dir.toPath();
            txtCarpeta.setText(dir.getAbsolutePath());
            if (loginService != null && loginService.isLoggedIn()) {
                btnDescargar.setDisable(false);
            }
        }
    }

    private void iniciarDescarga(long delayMs) {
        if (reporteService == null || !loginService.isLoggedIn()) {
            agregarLog("✗ Conecta a SOFIA Plus primero.", RED);
            return;
        }
        if (carpetaDestino == null) {
            agregarLog("✗ Elige una carpeta de destino.", RED);
            return;
        }

        List<Integer> fichas = cargarNumerosFichasDeBD();
        if (fichas.isEmpty()) {
            agregarLog("✗ No hay fichas en la BD. Carga un Excel primero.", RED);
            return;
        }

        // Crear un nuevo ExecutorService limpio para esta descarga
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = Executors.newSingleThreadExecutor();

        cancelado = false;
        reporteService.setCancelado(false);
        btnDescargar.setDisable(true);
        btnCancelar.setDisable(false);
        logBox.getChildren().clear();

        int total = fichas.size();
        agregarLog("▶ Iniciando descarga de " + total + " fichas...", BLUE);

        // Callback de progreso de descarga
        reporteService.setProgressCallback((ficha, actual, totalCb, msg, exito) ->
                Platform.runLater(() -> {
                    double pct = total > 0 ? (double) actual / total : 0;
                    progressBar.setProgress(pct);
                    lblProgreso.setText(msg);
                    lblContador.setText(actual + " / " + total);
                    agregarLog((exito ? "  " : "  ") + msg, exito ? GREEN : RED);
                    logScroll.setVvalue(1.0);
                }));

        executor.submit(() -> {
            // ── FASE 1: DESCARGA ──────────────────────────────────
            Map<Integer, Path> resultados = reporteService.descargarMasivo(fichas, carpetaDestino, delayMs);
            long ok  = resultados.values().stream().filter(p -> p != null).count();
            long err = resultados.values().stream().filter(p -> p == null).count();

            if (cancelado) {
                Platform.runLater(() -> {
                    lblProgreso.setText("Descarga cancelada");
                    agregarLog("─────────────────────────────────", "#2a2d3a");
                    agregarLog("✓ Completadas: " + ok + "  |  ✗ Errores: " + err + "  |  ⏹ Canceladas: " + (total - ok - err), YELLOW);
                    btnDescargar.setDisable(false);
                    btnCancelar.setDisable(true);
                });
                return;
            }

            // ── FASE 2: IMPORTACIÓN AUTOMÁTICA ───────────────────
            Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                lblProgreso.setText("Descarga completada. Iniciando importación...");
                agregarLog("─────────────────────────────────", "#2a2d3a");
                agregarLog("✓ Descarga completada: " + ok + "/" + total + " archivos", ok == total ? GREEN : YELLOW);
                agregarLog("▶ Iniciando importación a base de datos...", BLUE);
            });

            // Importar archivos descargados
            importService = new SofiaImportService();
            importService.setProgressCallback((archivo, nFichas, exito, msg) ->
                Platform.runLater(() -> {
                    agregarLog("  " + archivo + ": " + msg, exito ? GREEN : RED);
                    logScroll.setVvalue(1.0);
                }));

            int totalImportados = importService.importarDesdeCarpeta(carpetaDestino);

            // ── FINALIZACIÓN ─────────────────────────────────────
            Platform.runLater(() -> {
                agregarLog("─────────────────────────────────", "#2a2d3a");
                if (totalImportados > 0) {
                    agregarLog("✅ PROCESO COMPLETADO", GREEN);
                    agregarLog("📊 Fichas importadas: " + totalImportados, GREEN);
                    lblProgreso.setText("Importación completada: " + totalImportados + " fichas");
                } else {
                    agregarLog("⚠️ Importación completada sin datos nuevos", YELLOW);
                    lblProgreso.setText("Importación completada");
                }
                agregarLog("📁 Archivos en: " + carpetaDestino, BLUE);
                btnDescargar.setDisable(false);
                btnCancelar.setDisable(true);

                // Actualizar contador de fichas en BD
                int nuevoTotal = contarFichasEnBD();
                agregarLog("📈 Total fichas en BD: " + nuevoTotal, BLUE);
            });
        });
    }

    private void cancelar() {
        reporteService.setCancelado(true);
        cancelado = true;
        agregarLog("⏹ Cancelando descarga...", YELLOW);
        btnCancelar.setDisable(true);
        // NO hacer shutdownNow(), solo esperar a que la tarea respete el flag
    }

    // ── Helpers de BD ────────────────────────────────────────────

private int contarFichasEnBD() {
    try (DatabaseManager db = new DatabaseManager()) {
        db.conectar();
        return db.contarFichas();
    } catch (Exception e) { 
        return 0; 
    }
}

private List<Integer> cargarNumerosFichasDeBD() {
    List<Integer> lista = new ArrayList<>();
    try (DatabaseManager db = new DatabaseManager()) {
        db.conectar();
        ResultSet rs = db.obtenerFichas();
        while (rs.next()) lista.add(rs.getInt("numero"));
    } catch (Exception e) {
        agregarLog("✗ Error leyendo BD: " + e.getMessage(), RED);
    }
    return lista;
}

    // ── Helpers de UI ────────────────────────────────────────────

    private void setEstadoLogin(String msg, String color) {
        lblEstadoLogin.setText(msg);
        lblEstadoLogin.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + ";");
    }

    private void agregarLog(String msg, String color) {
        
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + color + "; -fx-font-family: monospace;");
        lbl.setWrapText(true);
        logBox.getChildren().add(lbl);
        // Limitar a 200 líneas
        if (logBox.getChildren().size() > 200)
            logBox.getChildren().remove(0);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 9px; -fx-text-fill: #4b5263; -fx-letter-spacing: 0.08em;");
        return l;
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        applyFieldStyle(f);
        return f;
    }

    private void applyFieldStyle(Control f) {
        f.setStyle(
                "-fx-background-color: #0a0c12; -fx-text-fill: #e8eaf0;" +
                "-fx-prompt-text-fill: #4b5263; -fx-background-radius: 8;" +
                "-fx-border-color: #2a2d3a; -fx-border-radius: 8;" +
                "-fx-font-size: 12px; -fx-padding: 7 10;");
        f.setMaxWidth(Double.MAX_VALUE);
    }

    private Button greenButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: " + GREEN + "; -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 14;");
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: #2d8a00; -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 14;"));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: " + GREEN + "; -fx-text-fill: white;" +
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 14;"));
        return b;
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
