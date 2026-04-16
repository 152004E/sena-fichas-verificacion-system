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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maneja login y sesión en SOFIA Plus.
 *
 * Flujo JOSSO (SSO de JBoss):
 * 1. GET /login/login.faces → extrae campos del form + cookies
 * 2. POST /josso/signon/login.do → envía credenciales → redirect
 * 3. Sigue redirects hasta la app → sesión lista
 */
public class SofiaLoginService {

    private static final String BASE_URL = "http://senasofiaplus.edu.co";
    private static final String LOGIN_URL = BASE_URL + "/sofia/login/login.faces";
    private static final String JOSSO_URL = BASE_URL + "/josso/signon/login.do";

    private final CookieManager cookieManager;
    private final HttpClient httpClient;
    private boolean loggedIn = false;

    public SofiaLoginService() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ── API pública ──────────────────────────────────────────────

    /** Inicia sesión con las credenciales dadas. Lanza excepción si falla. */
    public void login(String usuario, String contrasena) throws Exception {
        loggedIn = false;
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("🔐 INICIANDO LOGIN - Usuario: " + usuario);
        System.out.println("═══════════════════════════════════════════════");

        // 1. GET para obtener cookies iniciales
        System.out.println("📡 Paso 1: GET login page: " + LOGIN_URL);
        String loginPageHtml = doGet(LOGIN_URL);
        System.out.println("📄 Response length: " + loginPageHtml.length() + " chars");

        // Log de cookies obtenidas
        System.out.println("🍪 Cookies después del GET:");
        for (HttpCookie c : cookieManager.getCookieStore().getCookies()) {
            System.out.println("   • " + c.getName() + " = " + c.getValue() + " (domain: " + c.getDomain() + ")");
        }

        // 2. Extraer campos ocultos del form JSF/JOSSO
        Map<String, String> formFields = extraerCamposForm(loginPageHtml);
        System.out.println("📝 Campos del form encontrados: " + formFields.keySet());

        // 3. Agregar credenciales
        formFields.put("josso_username", usuario);
        formFields.put("josso_password", contrasena);
        formFields.put("josso_cmd", "login");
        System.out.println("🔑 Credenciales agregadas al form");

        // 4. POST al endpoint JOSSO
        String postUrl = formFields.getOrDefault("action", JOSSO_URL);
        if (!postUrl.startsWith("http"))
            postUrl = BASE_URL + postUrl;
        System.out.println("📡 Paso 2: POST a JOSSO: " + postUrl);
        String respBody = doPost(postUrl, formFields);
        System.out.println("📄 Response length: " + respBody.length() + " chars");

        // Después del POST a JOSSO
        System.out.println("📡 Paso 3: Navegando a HOME para completar handshake JOSSO...");
        Thread.sleep(1500); // Dale tiempo al SSO
        String homeHtml = doGetConVerificacion(BASE_URL + "/sofia/home/principal.faces");
        System.out.println("📄 HOME response length: " + homeHtml.length());

        // Verificar si realmente entramos
        if (homeHtml.contains("josso_login") || homeHtml.contains("Redirects the user")) {
            System.out.println("❌ No se pudo establecer sesión en /sofia/");
            throw new Exception("JOSSO handshake incompleto. Sesión no establecida.");
        }

        System.out.println("✅ Sesión establecida en la aplicación");
        loggedIn = true;

        // Log de cookies después del POST
        System.out.println("🍪 Cookies después del POST:");
        for (HttpCookie c : cookieManager.getCookieStore().getCookies()) {
            System.out.println("   • " + c.getName() + " = " + c.getValue() + " (domain: " + c.getDomain() + ")");
        }

        // 5. Verificar que realmente ingresamos
        System.out.println("🔍 Verificando login...");
        if (respBody.contains("josso_username") || respBody.contains("Usuario o contraseña")) {
            System.out.println("❌ Login fallido - el response contiene campos de login");
            throw new Exception("Credenciales incorrectas o login fallido.");
        }

        // Verificar si hay redirección o contenido de aplicación
        if (respBody.contains("/sofia/josso_login/") || respBody.contains("Redirects the user")) {
            System.out.println("⚠️  Response contiene redirección a login:");
            System.out.println(respBody.substring(0, Math.min(500, respBody.length())));
            // No lanzar excepción aquí, puede ser normal si hay redirects
        }

        loggedIn = true;
        System.out.println("✅ Login exitoso en SOFIA Plus");
        System.out.println("═══════════════════════════════════════════════");
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }

    /** Devuelve todas las cookies actuales como header Cookie */
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

    public String doGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
        return resp.body();
    }

    public String doGetConVerificacion(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-CO,es;q=0.9")
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = httpClient.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));

        // Log de URL final para diagnóstico
        System.out.println("DEBUG URL final tras redirects: " + resp.uri());
        System.out.println("DEBUG Status code: " + resp.statusCode());
        return resp.body();
    }

    public String doPost(String url, Map<String, String> fields) throws Exception {
        String body = buildFormBody(fields);
        // Para POST a HOME después del login, el Referer debe ser /sofia-public/
        String referer = url.contains("/sofia/home/") ? BASE_URL + "/sofia-public/" : LOGIN_URL;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml,*/*")
                .header("Referer", referer)
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<String> resp = httpClient.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.ISO_8859_1));
        return resp.body();
    }

    public byte[] doPostBytes(String url, String rawBody, Map<String, String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(rawBody, StandardCharsets.ISO_8859_1))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .timeout(Duration.ofSeconds(60));
        headers.forEach(builder::header);
        HttpResponse<byte[]> resp = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        return resp.body();
    }

    // ── Utilidades ───────────────────────────────────────────────

    /** Extrae campos ocultos de un form HTML */
    private Map<String, String> extraerCamposForm(String html) {
        Map<String, String> fields = new HashMap<>();

        // Buscar action del form
        Pattern actionPat = Pattern.compile("<form[^>]+action=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher actionM = actionPat.matcher(html);
        if (actionM.find())
            fields.put("action", actionM.group(1));

        // Buscar inputs hidden
        Pattern inputPat = Pattern.compile("<input[^>]+type=[\"']hidden[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
        Pattern namePat = Pattern.compile("name=[\"']([^\"']+)[\"']");
        Pattern valPat = Pattern.compile("value=[\"']([^\"']*)[\"']");
        Matcher inputM = inputPat.matcher(html);
        while (inputM.find()) {
            String tag = inputM.group();
            Matcher nm = namePat.matcher(tag);
            Matcher vm = valPat.matcher(tag);
            if (nm.find()) {
                String name = nm.group(1);
                String val = vm.find() ? vm.group(1) : "";
                fields.put(name, val);
            }
        }
        return fields;
    }

    /** Extrae ViewState de HTML JSF */
    public static String extraerViewState(String html) {
        Pattern p = Pattern.compile("id=[\"']javax\\.faces\\.ViewState[\"'][^>]*value=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find())
            return m.group(1);

        // Orden alternativo de atributos
        Pattern p2 = Pattern.compile("name=[\"']javax\\.faces\\.ViewState[\"'][^>]*value=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(html);
        if (m2.find())
            return m2.group(1);

        return "";
    }

    /** Construye cuerpo application/x-www-form-urlencoded */
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
}