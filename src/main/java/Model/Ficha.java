package Model;

/**
 * Representa una ficha SENA.
 * Columnas mapeadas desde Hoja7 y PE04 del Excel.
 */
public class Ficha {

    private int numero;           // Col A: FICHA
    private String nivel;         // Col B: NIVEL (TECNICO / TECNÓLOGO)
    private int aprendices;       // Col C: APRENDICES
    private String programa;      // Col D: PROGRAMA
    private String fechaInicio;   // Col E: F_INI
    private String fechaFinLec;   // Col F: F_FIN_LEC
    private String fechaFin;      // Col G: F_FIN
    private String instructorTecnico2025; // Col M: INSTRUCTOR TECNICO 2025
    private String instructorBilinguismo; // Col O: INSTRUCTOR BILINGUISMO 2025
    private String instructorTecnico2026; // Col P: INSTRUCTOR TECNICO 2026
    private String transversalesFaltantes; // Col Q: TODAS (transversales que faltan)
    private EstadoFicha estado;   // Calculado desde PE04

    public Ficha() {}

    public Ficha(int numero, String nivel, int aprendices, String programa,
                 String fechaInicio, String fechaFinLec, String fechaFin,
                 String instructorTecnico2025, String instructorBilinguismo,
                 String instructorTecnico2026, String transversalesFaltantes,
                 EstadoFicha estado) {
        this.numero = numero;
        this.nivel = nivel;
        this.aprendices = aprendices;
        this.programa = programa;
        this.fechaInicio = fechaInicio;
        this.fechaFinLec = fechaFinLec;
        this.fechaFin = fechaFin;
        this.instructorTecnico2025 = instructorTecnico2025;
        this.instructorBilinguismo = instructorBilinguismo;
        this.instructorTecnico2026 = instructorTecnico2026;
        this.transversalesFaltantes = transversalesFaltantes;
        this.estado = estado;
    }

    // ─── Getters ────────────────────────────────────────────────
    public int getNumero() { return numero; }
    public String getNivel() { return nivel; }
    public int getAprendices() { return aprendices; }
    public String getPrograma() { return programa; }
    public String getFechaInicio() { return fechaInicio; }
    public String getFechaFinLec() { return fechaFinLec; }
    public String getFechaFin() { return fechaFin; }
    public String getInstructorTecnico2025() { return instructorTecnico2025; }
    public String getInstructorBilinguismo() { return instructorBilinguismo; }
    public String getInstructorTecnico2026() { return instructorTecnico2026; }
    public String getTransversalesFaltantes() { return transversalesFaltantes; }
    public EstadoFicha getEstado() { return estado; }

    // ─── Setters ────────────────────────────────────────────────
    public void setNumero(int numero) { this.numero = numero; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public void setAprendices(int aprendices) { this.aprendices = aprendices; }
    public void setPrograma(String programa) { this.programa = programa; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFinLec(String fechaFinLec) { this.fechaFinLec = fechaFinLec; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }
    public void setInstructorTecnico2025(String v) { this.instructorTecnico2025 = v; }
    public void setInstructorBilinguismo(String v) { this.instructorBilinguismo = v; }
    public void setInstructorTecnico2026(String v) { this.instructorTecnico2026 = v; }
    public void setTransversalesFaltantes(String v) { this.transversalesFaltantes = v; }
    public void setEstado(EstadoFicha estado) { this.estado = estado; }

    // ─── Lógica de validación ────────────────────────────────────
    public boolean tieneTransversalesFaltantes() {
        return transversalesFaltantes != null && !transversalesFaltantes.trim().isEmpty();
    }

    public boolean tieneInstructorTecnico() {
        return (instructorTecnico2025 != null && !instructorTecnico2025.trim().isEmpty())
            || (instructorTecnico2026 != null && !instructorTecnico2026.trim().isEmpty());
    }

    @Override
    public String toString() {
        return "Ficha{" + numero + ", " + programa + ", " + estado + "}";
    }
}