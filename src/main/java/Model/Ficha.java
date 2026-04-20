package Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Ficha {

    private int numero;
    private String nivel;
    private int aprendices;
    private String programa;
    private String fechaInicio;
    private String fechaFinLec;
    private String fechaFin;
    private String instructorTecnico2025;
    private String instructorBilinguismo;
    private String instructorTecnico2026;

    // ✅ CAMBIO: de String a List<String>
    private List<String> transversalesFaltantes = new ArrayList<>();

    private EstadoFicha estado;
    private String trimestre;
    private String acuerdo;
    private String evaluacion;

    // Este ya era Map<String, String>, se confirma
    private Map<String, String> transversalesVistas = new LinkedHashMap<>();

    public Ficha() {}

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
    public List<String> getTransversalesFaltantes() { return transversalesFaltantes; }
    public EstadoFicha getEstado() { return estado; }
    public String getTrimestre() { return trimestre; }
    public String getAcuerdo() { return acuerdo; }
    public String getEvaluacion() { return evaluacion; }
    public Map<String, String> getTransversalesVistas() { return transversalesVistas; }

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
    public void setTransversalesFaltantes(List<String> v) { this.transversalesFaltantes = v; }
    public void setEstado(EstadoFicha estado) { this.estado = estado; }
    public void setTrimestre(String trimestre) { this.trimestre = trimestre; }
    public void setAcuerdo(String acuerdo) { this.acuerdo = acuerdo; }
    public void setEvaluacion(String evaluacion) { this.evaluacion = evaluacion; }
    public void setTransversalesVistas(Map<String, String> v) { this.transversalesVistas = v; }

    // ─── Lógica de validación ────────────────────────────────────
    public boolean tieneTransversalesFaltantes() {
        return transversalesFaltantes != null && !transversalesFaltantes.isEmpty();
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