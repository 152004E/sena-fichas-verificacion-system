package Model;

/**
 * Representa un programa de formación SENA.
 * Mapeado desde Hoja6 del Excel.
 */
public class Programa {

    private String nombre;        // Col A: PROGRAMA
    private String nivel;         // Col B: NIVEL (TÉCNICO / TECNÓLOGO)
    private int codigo;           // Col C: COD PRO
    private int version;          // Col D: VER
    private String transversales; // Cols F-U: siglas de transversales requeridas (TIC, MAT, COM...)

    public Programa() {}

    public Programa(String nombre, String nivel, int codigo, int version, String transversales) {
        this.nombre = nombre;
        this.nivel = nivel;
        this.codigo = codigo;
        this.version = version;
        this.transversales = transversales;
    }

    // ─── Getters ────────────────────────────────────────────────
    public String getNombre() { return nombre; }
    public String getNivel() { return nivel; }
    public int getCodigo() { return codigo; }
    public int getVersion() { return version; }
    public String getTransversales() { return transversales; }

    // ─── Setters ────────────────────────────────────────────────
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public void setCodigo(int codigo) { this.codigo = codigo; }
    public void setVersion(int version) { this.version = version; }
    public void setTransversales(String transversales) { this.transversales = transversales; }

    @Override
    public String toString() {
        return nombre + " (" + nivel + ")";
    }
}