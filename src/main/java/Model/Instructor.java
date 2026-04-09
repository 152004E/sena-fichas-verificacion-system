package Model;

/**
 * Representa un instructor SENA.
 * Nombres extraídos de columnas de instructores en Hoja7 y Hoja4.
 */
public class Instructor {

    private int id;
    private String nombre;        // Nombre completo del instructor
    private String tipo;          // TECNICO, BILINGUISMO, BIENVENIDA, INDUCCION

    public Instructor() {}

    public Instructor(int id, String nombre, String tipo) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    // ─── Getters ────────────────────────────────────────────────
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }

    // ─── Setters ────────────────────────────────────────────────
    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return nombre + " [" + tipo + "]";
    }
}