package database;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import Model.Ficha;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:data/fichas.db";
    private Connection connection;

    public void conectar() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        System.out.println("✅ Conectado a SQLite");
        crearTablas();
    }

    public void desconectar() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Conexión cerrada");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar: " + e.getMessage());
        }
    }

    private void crearTablas() throws SQLException {
        String sqlPrograma = """
                    CREATE TABLE IF NOT EXISTS PROGRAMA (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL,
                        nivel TEXT,
                        codigo INTEGER,
                        version INTEGER,
                        transversales TEXT
                    )
                """;

        String sqlFicha = """
                    CREATE TABLE IF NOT EXISTS FICHA (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        numero INTEGER NOT NULL,
                        nivel TEXT,
                        aprendices INTEGER,
                        programa TEXT,
                        fecha_inicio TEXT,
                        fecha_fin_lec TEXT,
                        fecha_fin TEXT,
                        instructor_tecnico_2025 TEXT,
                        instructor_bilinguismo TEXT,
                        instructor_tecnico_2026 TEXT,
                        transversales_faltantes TEXT,
                        transversales_vistas TEXT,
                        estado TEXT,
                        trimestre TEXT,
                        acuerdo TEXT,
                        evaluacion TEXT
                    )
                """;

        String sqlInstructor = """
                    CREATE TABLE IF NOT EXISTS INSTRUCTOR (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL,
                        tipo TEXT
                    )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlPrograma);
            stmt.execute(sqlFicha);
            stmt.execute(sqlInstructor);
            System.out.println("✅ Tablas creadas correctamente");
        }
    }

    public void insertarFicha(Ficha f) throws SQLException {
        String sql = """
                    INSERT INTO FICHA (numero, nivel, aprendices, programa,
                        fecha_inicio, fecha_fin_lec, fecha_fin,
                        instructor_tecnico_2025, instructor_bilinguismo,
                        instructor_tecnico_2026, transversales_faltantes,
                        transversales_vistas, estado,
                        trimestre, acuerdo, evaluacion)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, f.getNumero());
            ps.setString(2, f.getNivel());
            ps.setInt(3, f.getAprendices());
            ps.setString(4, f.getPrograma());
            ps.setString(5, f.getFechaInicio());
            ps.setString(6, f.getFechaFinLec());
            ps.setString(7, f.getFechaFin());
            ps.setString(8, f.getInstructorTecnico2025());
            ps.setString(9, f.getInstructorBilinguismo());
            ps.setString(10, f.getInstructorTecnico2026());
           ps.setString(11, String.join(";", f.getTransversalesFaltantes()));
            // Serializar transversalesVistas: "COMP1:INSTRUCTOR1;COMP2:INSTRUCTOR2"
            ps.setString(12, serializarVistas(f.getTransversalesVistas()));
            ps.setString(13, f.getEstado() != null ? f.getEstado().getLabel() : "Desconocido");
            ps.setString(14, f.getTrimestre());
            ps.setString(15, f.getAcuerdo());
            ps.setString(16, f.getEvaluacion());
            ps.executeUpdate();
        }
    }

    /** Serializa Map<String,String> a "KEY1:VAL1;KEY2:VAL2" */
    private String serializarVistas(Map<String, String> vistas) {
        if (vistas == null || vistas.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : vistas.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue() != null ? entry.getValue() : "");
        }
        return sb.toString();
    }

    /** Deserializa "KEY1:VAL1;KEY2:VAL2" a Map<String,String> */
    public static Map<String, String> deserializarVistas(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return map;
        for (String pair : raw.split(";")) {
            int idx = pair.indexOf(':');
            if (idx >= 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            } else {
                map.put(pair, "");
            }
        }
        return map;
    }

    public ResultSet obtenerFichas() throws SQLException {
        String sql = "SELECT * FROM FICHA";
        return connection.createStatement().executeQuery(sql);
    }

    public void limpiarFichas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM FICHA");
            System.out.println("🗑️ Fichas eliminadas");
        }
    }

    public int contarFichasPorEstado(String estado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM FICHA WHERE estado = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, estado);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int contarFichas() throws SQLException {
        String sql = "SELECT COUNT(*) FROM FICHA";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Connection getConnection() {
        return connection;
    }
}