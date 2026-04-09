package database;

import java.sql.*;
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
                estado TEXT
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
                instructor_tecnico_2026, transversales_faltantes, estado)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
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
            ps.setString(11, f.getTransversalesFaltantes());
            ps.setString(12, f.getEstado() != null ? f.getEstado().getLabel() : "Desconocido");
            ps.executeUpdate();
        }
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

    public Connection getConnection() {
        return connection;
    }
}