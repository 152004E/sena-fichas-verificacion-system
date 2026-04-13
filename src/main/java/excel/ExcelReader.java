package excel;

import Model.Ficha;
import Model.EstadoFicha;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelReader {

    public List<Ficha> leerFichasDesdeExcel(File archivo) throws Exception {
        List<Ficha> listaFichas = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
                Workbook workbook = new XSSFWorkbook(fis)) {

            // 1. Extraer estados, fechaFinLec y fechaFin desde PE04
            Map<Integer, EstadoFicha> estadosMap = new HashMap<>();
            Map<Integer, String> fechaFinLecMap = new HashMap<>();
            Map<Integer, String> fechaFinMap = new HashMap<>();
            extraerDatosPE04(workbook, estadosMap, fechaFinLecMap, fechaFinMap);

            // 2. Leer Hoja7
            Sheet sheet = workbook.getSheet("Hoja7");
            if (sheet == null)
                throw new Exception("No se encontró la Hoja7 en el Excel");

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String numFichaStr = formatter.formatCellValue(row.getCell(0), evaluator);
                if (numFichaStr == null || numFichaStr.isEmpty() || numFichaStr.equals("FICHA"))
                    continue;

                int numFicha;
                try {
                    numFicha = Integer.parseInt(numFichaStr.trim());
                    System.out.println("Ficha Hoja7: " + numFicha);
                } catch (NumberFormatException e) {
                    System.err.println("Fila " + i + " ignorada, número inválido: " + numFichaStr);
                    continue;
                }

                Ficha ficha = new Ficha();
                ficha.setNumero(numFicha);
                ficha.setNivel(formatter.formatCellValue(row.getCell(1)));

                String aprendices = formatter.formatCellValue(row.getCell(2)).trim();
                ficha.setAprendices(aprendices.isEmpty() ? 0 : Integer.parseInt(aprendices));

                ficha.setPrograma(formatter.formatCellValue(row.getCell(3)));

                String fechaInicio = formatter.formatCellValue(row.getCell(4), evaluator).trim();
                ficha.setFechaInicio(fechaInicio);

                ficha.setFechaFinLec(fechaFinLecMap.getOrDefault(numFicha, ""));
                ficha.setFechaFin(fechaFinMap.getOrDefault(numFicha, ""));

                // Instructores
                ficha.setInstructorTecnico2025(formatter.formatCellValue(row.getCell(12)));
                ficha.setInstructorBilinguismo(formatter.formatCellValue(row.getCell(14)));
                ficha.setInstructorTecnico2026(formatter.formatCellValue(row.getCell(15)));

                // Transversales FALTANTES → col 16 (separadas por espacios)
                ficha.setTransversalesFaltantes(formatter.formatCellValue(row.getCell(16)).trim());

                // Transversales VISTAS → pares (materia, instructor) en cols 32-65
                Map<String, String> vistas = new LinkedHashMap<>();
                for (int col = 32; col <= 64; col += 2) {
                    String materia = formatter.formatCellValue(row.getCell(col)).trim();
                    String instructor = formatter.formatCellValue(row.getCell(col + 1)).trim();
                    if (!materia.isEmpty()) {
                        vistas.put(materia, instructor);
                    }
                }
                ficha.setTransversalesVistas(vistas);

                // Campos calculados
                ficha.setTrimestre(calcularTrimestre(fechaInicio));
                ficha.setAcuerdo(calcularAcuerdo(fechaInicio));
                ficha.setEvaluacion(calcularEvaluacion(fechaInicio));

                // Estado desde PE04
                ficha.setEstado(estadosMap.getOrDefault(numFicha, EstadoFicha.DESCONOCIDO));

                listaFichas.add(ficha);
            }
        }
        return listaFichas;
    }

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

                String estadoStr = formatter.formatCellValue(row.getCell(6), evaluator);
                estadosMap.put(numFicha, EstadoFicha.fromString(estadoStr));

                String finLec = obtenerFecha(row.getCell(4), formatter, evaluator);
                fechaFinLecMap.put(numFicha, finLec);

                String finTotal = obtenerFecha(row.getCell(5), formatter, evaluator);
                fechaFinMap.put(numFicha, finTotal);
                System.out.println("Ficha PE04: " + numFicha + " | Fin: " + finTotal);

            } catch (NumberFormatException e) {
                System.err.println("Fila " + i + " en PE04 ignorada: " + numStr);
            }
        }

        System.out.println("✅ PE04 procesado: " + estadosMap.size() + " fichas encontradas");
    }

    private String calcularTrimestre(String fechaInicio) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date fecha = sdf.parse(fechaInicio);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(fecha);
            java.util.Calendar hoy = java.util.Calendar.getInstance();

            int años = hoy.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR);
            int meses = hoy.get(java.util.Calendar.MONTH) - cal.get(java.util.Calendar.MONTH);
            long mesesTotales = años * 12L + meses;

            int trimestre = (int) Math.min(8, (mesesTotales / 3) + 1);
            return "Trimestre " + trimestre;
        } catch (Exception e) {
            return "Trimestre 1";
        }
    }

    private String calcularAcuerdo(String fechaInicio) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date fecha = sdf.parse(fechaInicio);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(2024, 10, 20);
            return fecha.before(cal.getTime()) ? "ACUERDO 007" : "ACUERDO 009";
        } catch (Exception e) {
            return "ACUERDO 009";
        }
    }

    private String calcularEvaluacion(String fechaInicio) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date fecha = sdf.parse(fechaInicio);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(fecha);
            java.util.Calendar hoy = java.util.Calendar.getInstance();

            int años = hoy.get(java.util.Calendar.YEAR) - cal.get(java.util.Calendar.YEAR);
            int meses = hoy.get(java.util.Calendar.MONTH) - cal.get(java.util.Calendar.MONTH);
            long mesesTotales = años * 12L + meses;

            if (mesesTotales < 1)  return "INDUCCION";
            if (mesesTotales <= 4)  return "ANALISIS";
            if (mesesTotales <= 9)  return "PLANEACION";
            if (mesesTotales <= 13) return "EJECUCION";
            if (mesesTotales <= 18) return "EVALUACION";
            return "NO ESTA EN LECTIVA";
        } catch (Exception e) {
            return "DESCONOCIDO";
        }
    }

    private String obtenerFecha(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        java.util.Date date = cell.getDateCellValue();
                        return new SimpleDateFormat("dd/MM/yyyy").format(date);
                    }
                    return formatter.formatCellValue(cell, evaluator).trim();
                case STRING:
                    return cell.getStringCellValue().trim();
                case FORMULA:
                    return formatter.formatCellValue(cell, evaluator).trim();
                default:
                    return "";
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error leyendo fecha: " + e.getMessage());
            return formatter.formatCellValue(cell, evaluator).trim();
        }
    }
}