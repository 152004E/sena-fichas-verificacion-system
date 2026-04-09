package service;

import Model.Ficha;
import database.DatabaseManager;
import excel.ExcelReader;
import excel.ExcelSource;
import java.util.List;

public class SyncService {
    private DatabaseManager db;
    private ExcelReader reader;

    public SyncService(DatabaseManager db) {
        this.db = db;
        this.reader = new ExcelReader();
    }

    public List<Ficha> ejecutarSincronizacion(ExcelSource source) throws Exception {
        // 1. Obtener el archivo desde la fuente y convertirlo en objetos Java
        List<Ficha> fichasDesdeExcel = reader.leerFichasDesdeExcel(source.obtenerArchivo());

        // 2. Conectar a la base de datos
        db.conectar();

        // 3. Limpiar los datos viejos (opcional, según tu lógica)
        db.limpiarFichas();

        // 4. Guardar las nuevas fichas una por una (o en batch)
        for (Ficha f : fichasDesdeExcel) {
            db.insertarFicha(f);
        }

        // 5. Cerrar conexión
        db.desconectar();
        
        System.out.println("Sincronización terminada con " + fichasDesdeExcel.size() + " fichas.");
        return fichasDesdeExcel;
    }
}