package excel;

import java.io.File;
import java.io.IOException;

public interface ExcelSource {
    File obtenerArchivo() throws IOException;
}
