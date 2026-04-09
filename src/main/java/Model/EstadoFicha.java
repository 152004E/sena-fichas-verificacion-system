package Model;

public enum EstadoFicha {
    EN_EJECUCION("En ejecucion"),
    TERMINADA("Terminada por fecha"),
    DESCONOCIDO("Desconocido");

    private final String label;

    EstadoFicha(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EstadoFicha fromString(String texto) {
        if (texto == null) return DESCONOCIDO;
        for (EstadoFicha e : values()) {
            if (e.label.equalsIgnoreCase(texto.trim())) return e;
        }
        return DESCONOCIDO;
    }
}