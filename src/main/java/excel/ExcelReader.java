package excel;

import Model.Ficha;
import Model.EstadoFicha;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelReader {

    // Columnas (0-based) donde están las transversales VISTAS en Hoja7
    // Cada índice es la columna de la materia; la siguiente (índice+1) es el
    // instructor
    private static final int[] COLS_TRANSVERSALES_VISTAS = {
            24, 26, 28, 30, 32, 36, 40, 42, 44,
            46, 48, 50, 52, 54, 56, 58, 60, 63, 65
    };

    // Columna (0-based) de "TODAS" las transversales del programa
    private static final int COL_TODAS = 16; // columna Q

    public List<Ficha> leerFichasDesdeExcel(File archivo) throws Exception {
        List<Ficha> listaFichas = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Map<Integer, EstadoFicha> estadosMap = new HashMap<>();
            Map<Integer, String> fechaFinLecMap = new HashMap<>();
            Map<Integer, String> fechaFinMap = new HashMap<>();
            extraerDatosPE04(workbook, estadosMap, fechaFinLecMap, fechaFinMap);

            Sheet sheet = workbook.getSheet("Hoja7");
            if (sheet == null)
                throw new Exception("No se encontró la Hoja7 en el Excel");

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String numFichaStr = formatter.formatCellValue(row.getCell(0), evaluator).trim();
                if (numFichaStr.isEmpty() || numFichaStr.equals("FICHA"))
                    continue;

                int numFicha;
                try {
                    numFicha = Integer.parseInt(numFichaStr);
                } catch (NumberFormatException e) {
                    System.err.println("Fila " + i + " ignorada, número inválido: " + numFichaStr);
                    continue;
                }

                Ficha ficha = new Ficha();
                ficha.setNumero(numFicha);
                ficha.setNivel(formatter.formatCellValue(row.getCell(1)));

                String aprendicesStr = formatter.formatCellValue(row.getCell(2)).trim();
                ficha.setAprendices(aprendicesStr.isEmpty() ? 0 : Integer.parseInt(aprendicesStr));

                ficha.setPrograma(formatter.formatCellValue(row.getCell(3)));

                String fechaInicio = formatter.formatCellValue(row.getCell(4), evaluator).trim();
                ficha.setFechaInicio(fechaInicio);
                ficha.setFechaFinLec(fechaFinLecMap.getOrDefault(numFicha, ""));
                ficha.setFechaFin(fechaFinMap.getOrDefault(numFicha, ""));

                ficha.setInstructorTecnico2025(formatter.formatCellValue(row.getCell(12)));
                ficha.setInstructorBilinguismo(formatter.formatCellValue(row.getCell(14)));
                ficha.setInstructorTecnico2026(formatter.formatCellValue(row.getCell(15)));

                // ── 1. Leer "TODAS" desde col 16 (separadas por espacios o saltos) ──
                String todasRaw = formatter.formatCellValue(row.getCell(COL_TODAS), evaluator).trim();
                Set<String> todasSet = parsearTransversalesTexto(todasRaw);

                // ── 2. Leer transversales VISTAS desde los índices exactos ──
                Map<String, String> vistas = new LinkedHashMap<>();
                for (int col : COLS_TRANSVERSALES_VISTAS) {
                    Cell celdaMateria = row.getCell(col);
                    Cell celdaInstructor = row.getCell(col + 1);
                    if (celdaMateria == null)
                        continue;

                    String materia = formatter.formatCellValue(celdaMateria).trim();
                    String instructor = (celdaInstructor != null)
                            ? formatter.formatCellValue(celdaInstructor).trim()
                            : "";
                    if (!materia.isEmpty()) {
                        vistas.put(materia, instructor);
                    }
                }
                ficha.setTransversalesVistas(vistas);

                // ── 3. Calcular FALTANTES = TODAS - VISTAS (replicamos la fórmula Excel) ──
                List<String> faltantes = new ArrayList<>();
                for (String transversal : todasSet) {
                    // Comparación case-insensitive + trim para evitar falsos positivos
                    boolean yaVista = vistas.keySet().stream()
                            .anyMatch(v -> v.equalsIgnoreCase(transversal));
                    if (!yaVista) {
                        faltantes.add(transversal);
                    }
                }
                ficha.setTransversalesFaltantes(faltantes);

                ficha.setTrimestre(calcularTrimestre(fechaInicio));
                ficha.setAcuerdo(calcularAcuerdo(fechaInicio));
                ficha.setEvaluacion(calcularEvaluacion(fechaInicio));
                ficha.setEstado(estadosMap.getOrDefault(numFicha, EstadoFicha.DESCONOCIDO));

                listaFichas.add(ficha);
            }
        }
        return listaFichas;
    }

    /**
     * Parsea el contenido de la celda "TODAS" a un Set<String>.
     * Maneja separadores: salto de línea, coma, punto y coma o espacio múltiple.
     * Usando Set para eliminar duplicados automáticamente.
     */
    private Set<String> parsearTransversalesTexto(String raw) {
        Set<String> resultado = new LinkedHashSet<>();
        if (raw == null || raw.isBlank())
            return resultado;

        // Separar por espacio, salto de línea, coma o punto y coma
        String[] partes = raw.split("[\\s,;]+");
        for (String parte : partes) {
            String limpia = parte.trim();
            if (!limpia.isEmpty()) {
                resultado.add(limpia);
            }
        }
        return resultado;
    }

    // ── El resto del archivo sin cambios ────────────────────────────────────────

    private void extraerDatosPE04(
            Workbook workbook,
            Map<Integer, EstadoFicha> estadosMap,
            Map<Integer, String> fechaFinLecMap,
            Map<Integer, String> fechaFinMap) {

        Sheet sheet = workbook.getSheet("PE04");
        if (sheet == null) {
            System.err.println("⚠️ Hoja PE04 no encontrada");
            return;
        }

        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            String numStr = formatter.formatCellValue(row.getCell(0), evaluator).trim();
            if (numStr.isEmpty() || numStr.equals("FICHA"))
                continue;

            try {
                int numFicha = Integer.parseInt(numStr);
                estadosMap.put(numFicha,
                        EstadoFicha.fromString(formatter.formatCellValue(row.getCell(6), evaluator)));
                fechaFinLecMap.put(numFicha, obtenerFecha(row.getCell(4), formatter, evaluator));
                fechaFinMap.put(numFicha, obtenerFecha(row.getCell(5), formatter, evaluator));
            } catch (NumberFormatException e) {
                System.err.println("Fila " + i + " en PE04 ignorada: " + numStr);
            }
        }

        System.out.println("✅ PE04 procesado: " + estadosMap.size() + " fichas encontradas");
    }

    private String calcularTrimestre(String fechaInicio) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date fecha = sdf.parse(fechaInicio);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fecha);
            Calendar hoy = Calendar.getInstance();

            long mesesTotales = (hoy.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12L
                    + (hoy.get(Calendar.MONTH) - cal.get(Calendar.MONTH));
            return "Trimestre " + Math.min(8, (mesesTotales / 3) + 1);
        } catch (Exception e) {
            return "Trimestre 1";
        }
    }

    private String calcularAcuerdo(String fechaInicio) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date fecha = sdf.parse(fechaInicio);
            Calendar corte = Calendar.getInstance();
            corte.set(2024, Calendar.NOVEMBER, 20);
            return fecha.before(corte.getTime()) ? "ACUERDO 007" : "ACUERDO 009";
        } catch (Exception e) {
            return "ACUERDO 009";
        }
    }

    private String calcularEvaluacion(String fechaInicio) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date fecha = sdf.parse(fechaInicio);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fecha);
            Calendar hoy = Calendar.getInstance();

            long meses = (hoy.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12L
                    + (hoy.get(Calendar.MONTH) - cal.get(Calendar.MONTH));

            if (meses < 1)
                return "INDUCCION";
            if (meses <= 4)
                return "ANALISIS";
            if (meses <= 9)
                return "PLANEACION";
            if (meses <= 13)
                return "EJECUCION";
            if (meses <= 18)
                return "EVALUACION";
            return "NO ESTA EN LECTIVA";
        } catch (Exception e) {
            return "DESCONOCIDO";
        }
    }

    private String obtenerFecha(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null)
            return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell))
                        return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
                    return formatter.formatCellValue(cell, evaluator).trim();
                case STRING:
                    return cell.getStringCellValue().trim();
                case FORMULA:
                    return formatter.formatCellValue(cell, evaluator).trim();
                default:
                    return "";
            }
        } catch (Exception e) {
            return formatter.formatCellValue(cell, evaluator).trim();
        }
    }
}