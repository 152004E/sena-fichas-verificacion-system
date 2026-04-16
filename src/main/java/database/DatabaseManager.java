package database;

import java.sql.*;
import Model.Ficha;

public class DatabaseManager implements AutoCloseable {

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

    @Override
    public void close() {
        desconectar();
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
                        instructor_tecnico_2026, transversales_faltantes, estado,
                        trimestre, acuerdo, evaluacion)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
            ps.setString(13, f.getTrimestre());
            ps.setString(14, f.getAcuerdo());
            ps.setString(15, f.getEvaluacion());
            ps.executeUpdate();
        }
    }

    /**
     * Inserta o actualiza una ficha por número (upsert).
     * Si existe, actualiza todos los campos. Si no, la inserta.
     */
    public void upsertFicha(Ficha f) throws SQLException {
        String sql = """
                    INSERT INTO FICHA (numero, nivel, aprendices, programa,
                        fecha_inicio, fecha_fin_lec, fecha_fin,
                        instructor_tecnico_2025, instructor_bilinguismo,
                        instructor_tecnico_2026, transversales_faltantes, estado,
                        trimestre, acuerdo, evaluacion)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(numero) DO UPDATE SET
                        nivel = excluded.nivel,
                        aprendices = excluded.aprendices,
                        programa = excluded.programa,
                        fecha_inicio = excluded.fecha_inicio,
                        fecha_fin_lec = excluded.fecha_fin_lec,
                        fecha_fin = excluded.fecha_fin,
                        instructor_tecnico_2025 = excluded.instructor_tecnico_2025,
                        instructor_bilinguismo = excluded.instructor_bilinguismo,
                        instructor_tecnico_2026 = excluded.instructor_tecnico_2026,
                        transversales_faltantes = excluded.transversales_faltantes,
                        estado = excluded.estado,
                        trimestre = excluded.trimestre,
                        acuerdo = excluded.acuerdo,
                        evaluacion = excluded.evaluacion
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
            ps.setString(13, f.getTrimestre());
            ps.setString(14, f.getAcuerdo());
            ps.setString(15, f.getEvaluacion());
            ps.executeUpdate();
        }
    }

    /**
     * Verifica si existe una ficha por número.
     */
    public boolean existeFicha(int numero) throws SQLException {
        String sql = "SELECT COUNT(*) FROM FICHA WHERE numero = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
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