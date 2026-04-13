package excel;

import Model.Ficha;
import Model.EstadoFicha;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelReader {

    // Método para leer la Hoja7 (Datos técnicos e instructores)
    public List<Ficha> leerFichasDesdeExcel(File archivo) throws Exception {
        List<Ficha> listaFichas = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
                Workbook workbook = new XSSFWorkbook(fis)) {

            // 1. Primero mapeamos los estados desde PE04 para tenerlos listos
            Map<Integer, EstadoFicha> estadosMap = extraerEstadosPE04(workbook);

            // 2. Leemos la Hoja7 (Información principal)
            Sheet sheet = workbook.getSheet("Hoja7");
            if (sheet == null)
                throw new Exception("No se encontró la Hoja7 en el Excel");

            DataFormatter formatter = new DataFormatter();

            // Empezamos en la fila 2 (fila 0 = encabezado, fila 1 = encabezado duplicado
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String numFichaStr = formatter.formatCellValue(row.getCell(0));
                if (numFichaStr == null || numFichaStr.isEmpty() || numFichaStr.equals("FICHA"))
                    continue;

                Ficha ficha = new Ficha();
                ficha.setNumero(Integer.parseInt(numFichaStr));
                ficha.setNivel(formatter.formatCellValue(row.getCell(1)));

                String aprendices = formatter.formatCellValue(row.getCell(2));
                ficha.setAprendices(aprendices.isEmpty() ? 0 : Integer.parseInt(aprendices));

                ficha.setPrograma(formatter.formatCellValue(row.getCell(3)));
                ficha.setFechaInicio(formatter.formatCellValue(row.getCell(4)));
                ficha.setFechaFinLec(formatter.formatCellValue(row.getCell(5)));
                ficha.setFechaFin(formatter.formatCellValue(row.getCell(6)));

                // Mapeo de instructores (Columnas M, O, P en tu Excel suelen ser 12, 14, 15)
                ficha.setInstructorTecnico2025(formatter.formatCellValue(row.getCell(12)));
                ficha.setInstructorBilinguismo(formatter.formatCellValue(row.getCell(14)));
                ficha.setInstructorTecnico2026(formatter.formatCellValue(row.getCell(15)));
                ficha.setTransversalesFaltantes(formatter.formatCellValue(row.getCell(16)));
                String fechaInicio = formatter.formatCellValue(row.getCell(4));
                ficha.setTrimestre(calcularTrimestre(fechaInicio));
                ficha.setAcuerdo(calcularAcuerdo(fechaInicio));
                ficha.setEvaluacion(calcularEvaluacion(fechaInicio));

                // Asignamos el estado que sacamos de la otra hoja
                ficha.setEstado(estadosMap.getOrDefault(ficha.getNumero(), EstadoFicha.DESCONOCIDO));

                listaFichas.add(ficha);
            }
        }
        return listaFichas;
    }

    // Método auxiliar para leer estados de la hoja PE04
    private Map<Integer, EstadoFicha> extraerEstadosPE04(Workbook workbook) {
        Map<Integer, EstadoFicha> estados = new HashMap<>();
        Sheet sheet = workbook.getSheet("PE04");
        if (sheet == null)
            return estados;

        DataFormatter formatter = new DataFormatter();
        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            String numStr = formatter.formatCellValue(row.getCell(0)); // Col A: FICHA
            String estadoStr = formatter.formatCellValue(row.getCell(6)); // Col G: ESTADO

            if (numStr != null && !numStr.isEmpty() && !numStr.equals("FICHA")) {
                try {
                    int numFicha = Integer.parseInt(numStr);
                    EstadoFicha estado = EstadoFicha.fromString(estadoStr);
                    estados.put(numFicha, estado);
                } catch (NumberFormatException e) {
                    System.err.println("Error al parsear número en PE04: " + numStr);
                }
            }
        }
        return estados;
    }

    private String calcularTrimestre(String fechaInicio) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date fecha = sdf.parse(fechaInicio);
            java.util.Date hoy = new java.util.Date();

            long meses = (hoy.getTime() - fecha.getTime()) / (1000 * 60 * 60 * 24 * 30);
            int trimestre = (int) Math.min(8, (meses / 3) + 1);
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
            cal.set(2024, 10, 20); // 20 de noviembre 2024

            if (fecha.before(cal.getTime())) {
                return "ACUERDO 007";
            } else {
                return "ACUERDO 009";
            }
        } catch (Exception e) {
            return "ACUERDO 009";
        }
    }

    private String calcularEvaluacion(String fechaInicio) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date fecha = sdf.parse(fechaInicio);
            java.util.Date hoy = new java.util.Date();

            long meses = (hoy.getTime() - fecha.getTime()) / (1000 * 60 * 60 * 24 * 30);

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
}