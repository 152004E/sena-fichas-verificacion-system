package service;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descarga el reporte XLS de aprendices desde SOFIA Plus.
 *
 * FLUJO (6 pasos obligatorios):
 *   Paso 0 — GET  /sofia/home/principal.faces      → inicializa UIViewRoot de la sesión
 *   Paso 1 — GET  /reporteAprendices.faces         → HTML con ViewState + conversationContext
 *   Paso 2 — GET  /modalFicha.faces                → modal inicial con nuevo conversationContext
 *   Paso 3 — POST /modalFicha.faces (búsqueda)    → POST primera búsqueda de ficha
 *   Paso 4 — POST /modalFicha.faces (selección)   → seleccionar ficha (cierra modal)
 *   Paso 5 — POST /reporteAprendices.faces         → descargar XLS
 *
 * ⚠️  IMPORTANTE PARA EL MANTENEDOR:
 *   Los nombres de los campos JSF (form ID, botón ID) y los query parameters
 *   (conversationContext, centro, estado2) pueden cambiar si SOFIA actualiza
 *   su versión. Si algo falla, inspecciona el HTML con DevTools.
 */
public class SofiaReporteService {

    private static final String BASE_URL    = "http://senasofiaplus.edu.co";
    private static final String HOME_URL = BASE_URL + "/sofia/home/principal.faces";
    private static final String REPORTE_URL_BASE = BASE_URL +
            "/sofia/ejecucionformacion/reportes/reporteAprendices.faces";
    private static final String MODAL_URL_BASE = BASE_URL +
            "/sofia/fwk-webcommon/ficha/modalFicha.faces";

    // Query params siempre iguales en el modal
    private static final String MODAL_QUERY_PARAMS = "?centro=9405&estado2=7,8,12&conversationContext=";

    // ── Constantes JSF Modal ──
    private static final String CAMPO_FICHA_MODAL = "form:codigoFichaITX";
    private static final String BTN_BUSCAR_MODAL = "form:buscarCBT";

    // ── Constantes JSF Reporte ──
    private static final String CAMPO_FICHA_REPORTE = "inputFichaCaracterizacionPrograma";
    private static final String HI_CAMPO_FICHA_REPORTE = "hi_inputFichaCaracterizacionPrograma";
    private static final String VALOR_CAMPO = "valorCampo";
    private static final String BTN_GENERAR_REPORTE = "frmForma1:btnConsultar";

    // ── Estado interno ───────────────────────────────────────────
    private final SofiaLoginService loginService;
    private ProgressCallback        callback;
    private volatile boolean        cancelado = false;

    // ── Constructor ──────────────────────────────────────────────
    public SofiaReporteService(SofiaLoginService loginService) {
        this.loginService = loginService;
    }

    // ── Interfaz de progreso ─────────────────────────────────────
    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * @param ficha    número de ficha actual
         * @param actual   índice 1-based
         * @param total    total de fichas
         * @param mensaje  descripción del paso
         * @param exito    true = OK, false = error
         */
        void onProgreso(int ficha, int actual, int total, String mensaje, boolean exito);
    }

    public void setProgressCallback(ProgressCallback cb) { this.callback = cb; }

    public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }

    // ── API pública ──────────────────────────────────────────────

    /**
     * Descarga el XLS de aprendices para una sola ficha.
     *
     * @param numeroFicha  número de la ficha SENA
     * @param dirDestino   carpeta donde guardar el archivo
     * @return Path del archivo descargado
     */
    public Path descargarReporteFicha(int numeroFicha, Path dirDestino) throws Exception {
        notificar(numeroFicha, 0, 1, "Iniciando descarga...", true);

        // ── Paso 0: Navegar a HOME para asegurar sesión en contexto de la app ──
        notificar(numeroFicha, 0, 1, "Paso 0/6 — Inicializando sesión en la aplicación...", true);
        System.out.println("DEBUG Paso 0: GET HOME " + HOME_URL);
        // CORRECCIÓN: Debe ser GET, no POST. Un POST vacío a HOME genera un error JSF
        // que corrompe el ViewState de la sesión. El GET inicializa el UIViewRoot correctamente.
        String htmlHome = loginService.doGet(HOME_URL);
        System.out.println("DEBUG Paso 0: HOME status ok, body length=" + htmlHome.length());
        System.out.println("DEBUG Paso 0 fragment: " + htmlHome.substring(0, Math.min(200, htmlHome.length())));

        // ── Pausa para que se estabilice la sesión ───────────────────────────
        Thread.sleep(2000);

        // ── Paso 1: GET página del reporte ───────────────────────
        notificar(numeroFicha, 0, 1, "Paso 1/6 — Cargando página de reportes...", true);
        String urlReportePaso1 = REPORTE_URL_BASE + "?menId=79&fwkmenu=si";
        System.out.println("DEBUG Paso 1: GET " + urlReportePaso1);
        String htmlReporte = loginService.doGet(urlReportePaso1);
        System.out.println("DEBUG Paso 1: Response length = " + htmlReporte.length() + " chars");
        
        // Detectar si redirige a login (sesión aún inválida)
        if (htmlReporte.contains("josso_login") || htmlReporte.contains("Redirects the user")) {
            System.out.println("WARN Paso 1: Sesión rechazada, reiniciando...");
            System.out.println("HTML: " + htmlReporte.substring(0, Math.min(300, htmlReporte.length())));
            // Reintento: GET a HOME para re-inicializar el UIViewRoot
            System.out.println("DEBUG Reintento: visitando HOME (GET) otra vez...");
            loginService.doGet(HOME_URL);
            Thread.sleep(1500);
            htmlReporte = loginService.doGet(urlReportePaso1);
        }
        
        String viewState1 = SofiaLoginService.extraerViewState(htmlReporte);
        System.out.println("DEBUG Paso 1: ViewState encontrado = " + (!viewState1.isEmpty()));
        String conversationContext1 = extraerConversationContext(htmlReporte);
        System.out.println("DEBUG Paso 1: ConversationContext = " + conversationContext1);

        if (viewState1.isEmpty()) {
            System.out.println("DEBUG Paso 1: HTML fragment: " + htmlReporte.substring(0, Math.min(500, htmlReporte.length())));
            throw new Exception("ViewState vacío en reporte. La sesión puede haber expirado.");
        }

        // ── Paso 2: GET modal inicial ────────────────────────────
        notificar(numeroFicha, 0, 1, "Paso 2/6 — Abriendo modal de fichas...", true);
        String urlModal = MODAL_URL_BASE + MODAL_QUERY_PARAMS + conversationContext1;
        System.out.println("DEBUG Paso 2: GET " + urlModal);
        String htmlModal = loginService.doGet(urlModal);
        String viewState2 = SofiaLoginService.extraerViewState(htmlModal);
        String conversationContext2 = extraerConversationContext(htmlModal);
        System.out.println("DEBUG Paso 2: viewState2 ok=" + !viewState2.isEmpty() + ", ctx2='" + conversationContext2 + "'");
        if (viewState2.isEmpty()) {
            System.out.println("DEBUG Paso 2 HTML fragment: " + htmlModal.substring(0, Math.min(500, htmlModal.length())));
            throw new Exception("ViewState vacío en modal (Paso 2). Verifica el parámetro 'centro' en MODAL_QUERY_PARAMS.");
        }

        // ── Paso 3: POST primer búsqueda en modal ────────────────
        notificar(numeroFicha, 0, 1, "Paso 3/6 — Buscando ficha " + numeroFicha + "...", true);
        String urlModalPost = MODAL_URL_BASE + "?conversationContext=" + conversationContext2;
        String htmlBusqueda = buscarFichaEnModal(numeroFicha, viewState2, urlModalPost);
        String viewState3 = SofiaLoginService.extraerViewState(htmlBusqueda);
        // conversationContext del modal después de búsqueda — el POST de selección usa la misma URL
        extraerConversationContext(htmlBusqueda); // registrado en log, resultado descartado intencionalmente

        // ── Paso 4: POST seleccionar ficha (confirmar búsqueda) ───
        notificar(numeroFicha, 0, 1, "Paso 4/6 — Seleccionando ficha...", true);
        String htmlSeleccion = seleccionarFichaEnModal(numeroFicha, viewState3, urlModalPost);
        String viewState4 = SofiaLoginService.extraerViewState(htmlSeleccion);
        String conversationContext4 = extraerConversationContext(htmlSeleccion);

        // ── Paso 5: POST generar y descargar reporte ─────────────
        notificar(numeroFicha, 0, 1, "Paso 5/6 — Descargando XLS...", true);
        String urlReportePost = REPORTE_URL_BASE + "?conversationContext=" + conversationContext4;
        byte[] xlsBytes = generarReporte(numeroFicha, viewState4, urlReportePost, htmlSeleccion);

        if (xlsBytes == null || xlsBytes.length < 512) {
            throw new Exception("Respuesta vacía o inválida del servidor para ficha " + numeroFicha);
        }

        // ── Guardar archivo ──────────────────────────────────────
        Files.createDirectories(dirDestino);
        String nombreArchivo = "Aprendices_Ficha_" + numeroFicha + ".xls";
        Path destino = dirDestino.resolve(nombreArchivo);
        Files.write(destino, xlsBytes);

        notificar(numeroFicha, 1, 1, "✓ Guardado: " + nombreArchivo, true);
        return destino;
    }

    /**
     * Descarga reportes XLS para una lista de fichas.
     * Continúa aunque alguna falle. Respeta un delay entre requests.
     *
     * @param fichas      lista de números de ficha
     * @param dirDestino  carpeta destino
     * @param delayMs     milisegundos entre requests (recomendado: 1500–3000)
     * @return mapa ficha → Path (null si falló)
     */
    public Map<Integer, Path> descargarMasivo(
            java.util.List<Integer> fichas,
            Path dirDestino,
            long delayMs) {

        Map<Integer, Path> resultados = new LinkedHashMap<>();
        int total = fichas.size();

        for (int i = 0; i < total; i++) {
            // Verificar si se solicitó cancelación
            if (cancelado) {
                System.out.println("⏹  Descarga cancelada en ficha " + (i + 1) + "/" + total);
                notificar(0, i, total, "⏹ Descarga cancelada por el usuario", false);
                break;
            }

            int ficha = fichas.get(i);
            try {
                Path archivo = descargarReporteFicha(ficha, dirDestino);
                resultados.put(ficha, archivo);
                notificar(ficha, i + 1, total,
                        "✓ Ficha " + ficha + " descargada (" + (i + 1) + "/" + total + ")", true);
            } catch (Exception e) {
                resultados.put(ficha, null);
                notificar(ficha, i + 1, total,
                        "✗ Error ficha " + ficha + ": " + e.getMessage(), false);
                System.err.println("Error ficha " + ficha + ": " + e.getMessage());
            }

            // Pausa entre requests para no sobrecargar el servidor
            if (i < total - 1) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long exitosos = resultados.values().stream().filter(p -> p != null).count();
        System.out.println("✅ Descarga masiva terminada: " + exitosos + "/" + total + " exitosas");
        return resultados;
    }

    // ── Pasos internos ───────────────────────────────────────────

    /**
     * Extrae el conversationContext del HTML.
     * Busca: ?conversationContext=X donde X puede ser [a-z]
     */
    private static String extraerConversationContext(String html) {
        // CORRECCIÓN: Acepta alfanumérico ([a-zA-Z0-9]+). La versión anterior solo aceptaba
        // letras minúsculas ([a-z]+), lo que hacía que contextos como "a1" o "b3" no se
        // capturaran y el fallback "a" causara el Error 495 / ViewState vacío.
        Pattern p = Pattern.compile("conversationContext=([a-zA-Z0-9]+)");
        Matcher m = p.matcher(html);
        if (m.find()) {
            String ctx = m.group(1);
            System.out.println("DEBUG conversationContext extraído: '" + ctx + "'");
            return ctx;
        }
        System.out.println("WARN conversationContext no encontrado en HTML, usando fallback 'a'");
        return "a"; // fallback — si esto aparece en logs, revisar el HTML del paso anterior
    }

    /**
     * Paso 3: POST primera búsqueda de ficha en el modal.
     * 
     * @param numeroFicha número de ficha a buscar
     * @param viewState ViewState del modal
     * @param urlModal URL del modal con conversationContext
     * @return HTML con resultados de búsqueda
     */
    private String buscarFichaEnModal(int numeroFicha, String viewState, String urlModal) 
            throws Exception {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("form:hiddenFocoPagina",      "");
        campos.put(CAMPO_FICHA_MODAL,            String.valueOf(numeroFicha));
        campos.put("form:departamentoSOM",       "");
        campos.put("fechaInicialICL",            "");
        campos.put("form:jornadaSOM",            "");
        campos.put("fechaFinalICL",              "");
        campos.put(BTN_BUSCAR_MODAL,             "Consultar");  // ⚠️ Texto exacto
        campos.put("form_SUBMIT",                "1");
        campos.put("form:_link_hidden_",         "");
        campos.put("form:_idcl",                 "");
        campos.put("javax.faces.ViewState",     viewState);

        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", urlModal);
        headers.put("Origin",  "http://senasofiaplus.edu.co");

        String rawBody = SofiaLoginService.buildFormBody(campos);
        byte[] respBytes = loginService.doPostBytes(urlModal, rawBody, headers);
        return new String(respBytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Paso 4: POST para seleccionar la ficha encontrada (cierra modal).
     * 
     * @param numeroFicha número de ficha
     * @param viewState ViewState de la búsqueda anterior
     * @param urlModal URL del modal con conversationContext
     * @return HTML después de seleccionar (modal cerrado)
     */
    private String seleccionarFichaEnModal(int numeroFicha, String viewState, String urlModal)
            throws Exception {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("form:hiddenFocoPagina",      "");
        campos.put(BTN_BUSCAR_MODAL,             "");  // Vacío en la segunda búsqueda
        campos.put(CAMPO_FICHA_MODAL,            String.valueOf(numeroFicha));
        campos.put("form:departamentoSOM",       "");
        campos.put("fechaInicialICL",            "");
        campos.put("form:jornadaSOM",            "");
        campos.put("fechaFinalICL",              "");
        campos.put("form_SUBMIT",                "1");
        campos.put("form:_link_hidden_",         "");
        campos.put("javax.faces.ViewState",     viewState);

        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", urlModal);
        headers.put("Origin",  "http://senasofiaplus.edu.co");

        String rawBody = SofiaLoginService.buildFormBody(campos);
        byte[] respBytes = loginService.doPostBytes(urlModal, rawBody, headers);
        return new String(respBytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Paso 5: POST para generar y descargar el reporte XLS.
     * 
     * @param numeroFicha número de ficha
     * @param viewState ViewState del paso anterior (modal)
     * @param urlReporte URL con conversationContext actualizado
     * @param htmlModalRespuesta HTML de la respuesta modal para extraer descripción
     * @return bytes del archivo XLS
     */
    private byte[] generarReporte(int numeroFicha, String viewState, String urlReporte, 
                                  String htmlModalRespuesta) throws Exception {
        // Extraer la descripción de la ficha del HTML modal
        // Formato esperado: "Ficha 3334496 (OPERACIONES COMERCIALES EN RETAIL)"
        String descripcionFicha = extraerDescripcionFicha(htmlModalRespuesta, numeroFicha);

        Map<String, String> campos = new LinkedHashMap<>();
        campos.put(VALOR_CAMPO,                  "frmForma1:" + CAMPO_FICHA_REPORTE);
        campos.put(HI_CAMPO_FICHA_REPORTE,       String.valueOf(numeroFicha));
        campos.put(CAMPO_FICHA_REPORTE,          descripcionFicha);
        campos.put(BTN_GENERAR_REPORTE,          "Generar Reporte");
        campos.put("frmForma1_SUBMIT",           "1");
        campos.put("frmForma1:_link_hidden_",    "");
        campos.put("frmForma1:_idcl",            "");
        campos.put("javax.faces.ViewState",     viewState);

        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", REPORTE_URL_BASE + "?menId=79&fwkmenu=si");
        headers.put("Origin",  "http://senasofiaplus.edu.co");

        String rawBody = SofiaLoginService.buildFormBody(campos);
        return loginService.doPostBytes(urlReporte, rawBody, headers);
    }

    /**
     * Extrae la descripción de la ficha del HTML modal.
     * Busca algo como: "Ficha 3334496 (OPERACIONES COMERCIALES EN RETAIL)"
     */
    private static String extraerDescripcionFicha(String html, int numeroFicha) {
        Pattern p = Pattern.compile("Ficha " + numeroFicha + " \\(([^)]+)\\)");
        Matcher m = p.matcher(html);
        if (m.find()) {
            String programa = m.group(1);
            return "Ficha " + numeroFicha + " (" + programa + ")";
        }
        // Fallback si no encuentra la descripción completa
        return "Ficha " + numeroFicha;
    }

    private void notificar(int ficha, int actual, int total, String msg, boolean exito) {
        if (callback != null) {
            callback.onProgreso(ficha, actual, total, msg, exito);
        }
    }
}