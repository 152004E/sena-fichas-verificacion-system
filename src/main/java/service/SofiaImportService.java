package service;

import Model.Ficha;
import database.DatabaseManager;
import excel.ExcelReader;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Importa archivos XLS descargados desde SOFIA Plus a la base de datos.
 *
 * Flujo:
 *   1. Lee todos los archivos .xls en una carpeta
 *   2. Extrae las fichas de cada archivo con ExcelReader
 *   3. Inserta o actualiza cada ficha en la BD (upsert por número)
 *   4. Reporta progreso por callback
 */
public class SofiaImportService {

    private final ExcelReader excelReader;
    private ProgressCallback callback;

    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * @param archivo  nombre del archivo procesado
         * @param fichas   cantidad de fichas importadas desde este archivo
         * @param exito    true = OK, false = error
         * @param mensaje  descripción del resultado
         */
        void onProgreso(String archivo, int fichas, boolean exito, String mensaje);
    }

    public SofiaImportService() {
        this.excelReader = new ExcelReader();
    }

    public void setProgressCallback(ProgressCallback cb) {
        this.callback = cb;
    }

    /**
     * Importa todos los archivos .xls de una carpeta a la base de datos.
     *
     * @param carpeta  carpeta con archivos .xls descargados
     * @return cantidad total de fichas importadas
     */
    public int importarDesdeCarpeta(Path carpeta) {
        if (!carpeta.toFile().exists()) {
            notificar("ERROR", 0, false, "La carpeta no existe: " + carpeta);
            return 0;
        }

        File[] archivosXls = carpeta.toFile()
                .listFiles((dir, name) -> name.toLowerCase().endsWith(".xls"));

        if (archivosXls == null || archivosXls.length == 0) {
            notificar("INFO", 0, false, "No se encontraron archivos .xls en " + carpeta);
            return 0;
        }

        int totalImportados = 0;
        int archivosProcesados = 0;
        int archivosFallidos = 0;

        try (DatabaseManager db = new DatabaseManager()) {
            db.conectar();

            for (File archivo : archivosXls) {
                try {
                    System.out.println("📄 Procesando: " + archivo.getName());

                    // Leer fichas del Excel
                    List<Ficha> fichas = excelReader.leerFichasDesdeExcel(archivo);

                    if (fichas.isEmpty()) {
                        notificar(archivo.getName(), 0, false, "⚠ Sin datos válidos");
                        archivosFallidos++;
                        continue;
                    }

                    // Insertar/actualizar cada ficha
                    int insertados = 0;
                    int actualizados = 0;

                    for (Ficha ficha : fichas) {
                        if (db.existeFicha(ficha.getNumero())) {
                            db.upsertFicha(ficha);
                            actualizados++;
                        } else {
                            db.insertarFicha(ficha);
                            insertados++;
                        }
                    }

                    totalImportados += fichas.size();
                    archivosProcesados++;

                    String msg = String.format("✓ %d fichas (%d nuevas, %d actualizadas)",
                            fichas.size(), insertados, actualizados);
                    notificar(archivo.getName(), fichas.size(), true, msg);

                } catch (Exception e) {
                    archivosFallidos++;
                    notificar(archivo.getName(), 0, false, "✗ Error: " + e.getMessage());
                    System.err.println("Error procesando " + archivo.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            db.desconectar();

        } catch (Exception e) {
            notificar("ERROR", 0, false, "Error general: " + e.getMessage());
            System.err.println("Error en importación masiva: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("✅ Importación completada: " + totalImportados + " fichas totales");
        System.out.println("   Archivos procesados: " + archivosProcesados +
                          " | Fallidos: " + archivosFallidos);

        return totalImportados;
    }

    private void notificar(String archivo, int fichas, boolean exito, String msg) {
        if (callback != null) {
            callback.onProgreso(archivo, fichas, exito, msg);
        }
    }
}
