package excel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalExcelSource implements ExcelSource {

    private final Path path;

    public LocalExcelSource(Path path) {
        this.path = path;
    }

    @Override
    public File obtenerArchivo() throws IOException {
        if (path == null) {
            throw new IOException("Ruta de archivo no especificada");
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("Archivo no encontrado: " + path);
        }
        return path.toFile();
    }
}
