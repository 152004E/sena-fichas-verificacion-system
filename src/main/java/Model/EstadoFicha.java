package Model;

public enum EstadoFicha {
    EN_EJECUCION("En ejecución"),
    TERMINADA("Finalizado"),
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

        String normalized = normalize(texto);
        if (normalized.contains("ejecucion") || normalized.contains("en ejecucion")) {
            return EN_EJECUCION;
        }
        if (normalized.contains("finalizado") || normalized.contains("terminada") || normalized.contains("terminado")) {
            return TERMINADA;
        }
        return DESCONOCIDO;
    }

    private static String normalize(String texto) {
        String normalized = java.text.Normalizer.normalize(texto.trim(), java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase();
    }
}