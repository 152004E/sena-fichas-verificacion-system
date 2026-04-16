package service;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja login y sesión en SOFIA Plus.
 *
 * Flujo JOSSO real (confirmado con DevTools del navegador):
 *
 * 1. GET http://senasofiaplus.edu.co/sofia-public/
 * → recibe cookies iniciales (JSESSIONID, cookiesession1, etc.)
 *
 * 2. POST
 * http://authpre.senasofiaplus.edu.co/josso/signon/usernamePasswordLogin.do
 * ↑ subdominio AUTH separado del app server principal
 * Campos:
 * josso_cmd = login
 * josso_username = "CC,<número_documento>"
 * josso_password = <contraseña>
 * josso_rememberme = false
 * josso_back_to = http://senasofiaplus.edu.co/sofia/josso_security_check
 * → 302 Location:
 * http://senasofiaplus.edu.co/sofia/josso_security_check?josso_assertion_id=XYZ
 *
 * 3. GET
 * http://senasofiaplus.edu.co/sofia/josso_security_check?josso_assertion_id=XYZ
 * → JOSSO valida el assertion y fija JOSSO_SESSIONID en el app server
 * → 302 Location: /sofia/home/principal.faces
 *
 * 4. GET URL final → página principal autenticada
 */
public class SofiaLoginService {

    // ── URLs ─────────────────────────────────────────────────────
    /** Servidor principal de la aplicación */
    private static final String BASE_URL = "http://senasofiaplus.edu.co";
    /**
     * Servidor de autenticación SSO — subdominio DIFERENTE (confirmado en DevTools)
     */
    private static final String AUTH_URL = "http://authpre.senasofiaplus.edu.co";

    /** Página pública con el formulario de login */
    private static final String LOGIN_PAGE = BASE_URL + "/sofia-public/";
    /** Endpoint POST del SSO — en el servidor AUTH, no en BASE */
    private static final String JOSSO_POST = AUTH_URL + "/josso/signon/usernamePasswordLogin.do";
    /** josso_back_to: URL en el app server donde JOSSO entrega el assertion_id */
    private static final String BACK_TO = BASE_URL + "/sofia/josso_security_check";

    // ── Tipo de documento por defecto (Cédula de Ciudadanía) ─────
    private static final String TIPO_DOC_DEFAULT = "CC";

    // ── Estado interno ───────────────────────────────────────────
    private final CookieManager cookieManager;
    private final HttpClient httpClient; // sigue redirects
    private final HttpClient httpClientNoRedirect; // NO sigue redirects
    private boolean loggedIn = false;

    // ── Constructor ──────────────────────────────────────────────
    public SofiaLoginService() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.httpClientNoRedirect = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ── API pública ──────────────────────────────────────────────

    /**
     * Login con tipo de documento por defecto (CC).
     * usuario = número de documento (ej: "12345678")
     */
    public void login(String usuario, String contrasena) throws Exception {
        login(TIPO_DOC_DEFAULT, usuario, contrasena);
    }

    /**
     * Login especificando tipo de documento.
     * tipoDoc: "CC", "TI", "CE", "PEP", "PPT"
     * usuario : número de documento
     */
    public void login(String tipoDoc, String usuario, String contrasena) throws Exception {
        loggedIn = false;

        // ── Paso 1: GET /sofia-public/ → obtener cookies iniciales ──
        System.out.println("══════════════════════════════════════");
        System.out.println("Paso 1: GET " + LOGIN_PAGE);
        HttpResponse<String> pageResp = doGetRaw(LOGIN_PAGE);
        System.out.println("  Status: " + pageResp.statusCode());
        System.out.println("  Cookies tras GET: " + listCookies());
        System.out.println("Cookies antes del POST:");
        cookieManager.getCookieStore().getCookies()
                .forEach(c -> System.out.println("  " + c.getDomain() + " → " + c.getName() + "=" + c.getValue())

                );

        // ── Paso 2: POST JOSSO sin seguir redirect ───────────────
        // El JS concatenarValores() construye: "CC,12345678," <- coma trailing siempre
        // (cuando sucursal esta vacio: tipoID+","+numero+","+sucursal → "CC,123,")
        String jossoUsername = tipoDoc + "," + usuario + ",";

        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("josso_cmd", "login");
        campos.put("josso_username", jossoUsername);
        campos.put("josso_rememberme", "false");
        campos.put("josso_back_to", BACK_TO);
        // Campos visuales del form — el servidor los espera tal como los envía el
        // navegador
        campos.put("select", tipoDoc);
        campos.put("ingreso", usuario);
        campos.put("josso_password", contrasena);
        campos.put("sucursal", "");

        System.out.println("Paso 2: POST " + JOSSO_POST);
        System.out.println("  josso_username: " + jossoUsername);
        String bodyDebug = buildFormBody(campos);
        System.out.println("  Body POST: " + bodyDebug);

        HttpResponse<String> jossoResp = doPostNoRedirect(JOSSO_POST, campos, LOGIN_PAGE);
        System.out.println("  Status: " + jossoResp.statusCode());

        String location = jossoResp.headers().firstValue("Location").orElse(null);
        System.out.println("  Location: " + location);

        if (location == null || location.isBlank()) {
            // Puede que las credenciales sean incorrectas o la sesión ya expiró
            // Por esto:
            String fullBody = jossoResp.body();
            System.out.println("  Body COMPLETO:\n" + fullBody);
            throw new Exception("JOSSO no devolvió Location...");
        }

        // ── Paso 3: GET josso_security_check (fija la sesión real) ──
        String securityCheckUrl = location.startsWith("http") ? location : BASE_URL + location;
        System.out.println("Paso 3: GET (security check) " + securityCheckUrl);

        // Usamos httpClient (ALWAYS) para que siga el redirect final a principal.faces
        HttpResponse<String> checkResp = doGetRaw(securityCheckUrl);
        System.out.println("  Status final: " + checkResp.statusCode());
        System.out.println("  URL final:    " + checkResp.uri());
        System.out.println("  Cookies tras security check: " + listCookies());

        // ── Paso 4: Verificar sesión ─────────────────────────────
        String body = checkResp.body();
        if (checkResp.uri().toString().contains("josso_login")
                || body.contains("josso_login")
                || body.contains("josso_username")) {
            throw new Exception(
                    "Sesión NO establecida — sigue redirigiendo a login. "
                            + "Verifica credenciales o el flujo de cookies.");
        }

        loggedIn = true;
        System.out.println("✅ Login exitoso. URL final: " + checkResp.uri());
        System.out.println("Cookies finales: " + listCookies());
        System.out.println("══════════════════════════════════════");
    }

    // ── Getters ──────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }

    /** Header Cookie listo para pegar en otras requests */
    public String getCookieHeader() {
        StringBuilder sb = new StringBuilder();
        for (HttpCookie c : cookieManager.getCookieStore().getCookies()) {
            if (sb.length() > 0)
                sb.append("; ");
            sb.append(c.getName()).append("=").append(c.getValue());
        }
        return sb.toString();
    }

    // ── HTTP helpers ─────────────────────────────────────────────

    /** GET con redirect ALWAYS — devuelve la respuesta completa */
    private HttpResponse<String> doGetRaw(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-CO,es;q=0.9,en;q=0.8")
                .timeout(Duration.ofSeconds(30))
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
    }

    /** GET conveniente que devuelve sólo el body */
    public String doGet(String url) throws Exception {
        return doGetRaw(url).body();
    }

    /** POST sin seguir redirect — para capturar el Location de JOSSO */
    private HttpResponse<String> doPostNoRedirect(
            String url, Map<String, String> fields, String referer) throws Exception {

        String body = buildFormBody(fields);

        // ── Construir cookie header manualmente para cruzar subdominios ──
        String cookieHeader = cookieManager.getCookieStore().getCookies()
                .stream()
                .map(c -> c.getName() + "=" + c.getValue())
                .collect(java.util.stream.Collectors.joining("; "));

        System.out.println("  Cookies enviadas al POST: " + cookieHeader); // debug

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .header("Accept-Language", "es-CO,es;q=0.9")
                .header("Referer", referer)
                .header("Cookie", cookieHeader) // ← ESTO es lo que faltaba
                .timeout(Duration.ofSeconds(30))
                .build();
        return httpClientNoRedirect.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
    }

    /** POST con redirect ALWAYS — para requests dentro de la app SOFIA */
    public String doPost(String url, Map<String, String> fields) throws Exception {
        String body = buildFormBody(fields);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .header("Accept-Language", "es-CO,es;q=0.9")
                .header("Referer", BASE_URL + "/sofia/")
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
        return resp.body();
    }

    /** POST que devuelve bytes (para descargar archivos) */
    public byte[] doPostBytes(String url, String rawBody, Map<String, String> extraHeaders) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(rawBody, StandardCharsets.ISO_8859_1))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .timeout(Duration.ofSeconds(60));
        if (extraHeaders != null)
            extraHeaders.forEach(builder::header);
        HttpResponse<byte[]> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        return resp.body();
    }

    // ── Utilidades ───────────────────────────────────────────────

    /** Extrae ViewState JSF del HTML */
    public static String extraerViewState(String html) {
        // Orden atributos: id primero
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("id=[\"']javax\\.faces\\.ViewState[\"'][^>]*value=[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        if (m.find())
            return m.group(1);

        // Orden atributos: name primero
        m = java.util.regex.Pattern
                .compile("name=[\"']javax\\.faces\\.ViewState[\"'][^>]*value=[\"']([^\"']+)[\"']",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        if (m.find())
            return m.group(1);

        return "";
    }

    /** Construye application/x-www-form-urlencoded */
    public static String buildFormBody(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (sb.length() > 0)
                sb.append("&");
            sb.append(encode(e.getKey())).append("=").append(encode(e.getValue()));
        }
        return sb.toString();
    }

    private static String encode(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    /** Lista cookies actuales como string legible */
    private String listCookies() {
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        if (cookies.isEmpty())
            return "(ninguna)";
        StringBuilder sb = new StringBuilder();
        for (HttpCookie c : cookies) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(c.getName()).append("=").append(c.getValue());
        }
        return sb.toString();
    }
}