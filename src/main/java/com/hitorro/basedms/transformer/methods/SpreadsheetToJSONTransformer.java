/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.basedms.transformer.methods;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Converts Excel spreadsheets (XLS, XLSX, CSV) to JSON format.
 * 
 * Parameters (JSON):
 * {
 *   "sheetIndex": 0,              // Which sheet to export (default: 0 = first sheet)
 *   "sheetName": "Sheet1",        // Or specify by name (overrides sheetIndex)
 *   "hasHeaders": true,           // First row contains headers (default: true)
 *   "format": "array",            // Output format: "array", "ndjson", "object" (default: "array")
 *   "includeMetadata": false,     // Include sheet metadata (default: false)
 *   "emptyValue": null,           // Value for empty cells (default: null)
 *   "dateFormat": "yyyy-MM-dd"    // Format for date cells (default: ISO date)
 * }
 * 
 * Output formats:
 * - "array": [{"col1": "val1", "col2": "val2"}, {...}]
 * - "ndjson": Newline-delimited JSON (one object per line)
 * - "object": {"data": [...], "metadata": {...}}
 */
public class SpreadsheetToJSONTransformer extends BaseTransformMethod {
    
    private static final Logger logger = LoggerFactory.getLogger(SpreadsheetToJSONTransformer.class);
    
    public static final String METHOD_NAME = "spreadsheet_to_json";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }
    
    @Override
    public boolean ensureServiceAvailable() {
        try {
            // Check if Apache POI classes are available
            Class.forName("org.apache.poi.ss.usermodel.Workbook");
            return true;
        } catch (ClassNotFoundException e) {
            logger.error("Apache POI not available for spreadsheet conversion");
            return false;
        }
    }
    
    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters,
                           String notifyGuid, int maxWaitTimeMinutes) throws IOException {
        
        if (!sourceFile.isLocal()) {
            throw new IOException("Spreadsheet transformer requires local file system");
        }
        
        logger.info("Converting spreadsheet to JSON");
        
        // Parse parameters
        SpreadsheetOptions options = parseParameters(parameters);
        
        // Open workbook
        Workbook workbook = openWorkbook(sourceFile);
        
        try {
            // Get the sheet to process
            Sheet sheet = getSheet(workbook, options);
            
            if (sheet == null) {
                throw new IllegalArgumentException(
                    "Sheet not found: " + (options.sheetName != null ? 
                        options.sheetName : "index " + options.sheetIndex)
                );
            }
            
            logger.info("Processing sheet: {} ({} rows)", 
                       sheet.getSheetName(), sheet.getPhysicalNumberOfRows());
            
            // Convert sheet to JSON
            String jsonOutput = convertSheetToJSON(sheet, options);
            
            // Write output
            File outputFile = Files.createTempFile("spreadsheet_", ".json").toFile();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
                writer.write(jsonOutput);
            }
            
            logger.info("Spreadsheet converted to JSON: {}", outputFile.getAbsolutePath());
            
            FileFileSystem ffs = new FileFileSystem(outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());
            
        } finally {
            workbook.close();
        }
    }
    
    private Workbook openWorkbook(BaseFile file) throws IOException {
        File javaFile = file.isLocal() ? ((com.hitorro.util.basefile.fs.file.FileFile) file).getJavaFile() : null;
        if (javaFile == null) throw new IOException("File must be local");
        String filename = javaFile.getPath().toLowerCase();
        
        try (FileInputStream fis = new FileInputStream(javaFile)) {
            if (filename.endsWith(".xlsx")) {
                return new XSSFWorkbook(fis);
            } else if (filename.endsWith(".xls")) {
                return new HSSFWorkbook(fis);
            } else {
                // Try both formats
                try {
                    return new XSSFWorkbook(fis);
                } catch (Exception e) {
                    return new HSSFWorkbook(fis);
                }
            }
        }
    }
    
    private Sheet getSheet(Workbook workbook, SpreadsheetOptions options) {
        if (options.sheetName != null) {
            return workbook.getSheet(options.sheetName);
        } else {
            return workbook.getSheetAt(options.sheetIndex);
        }
    }
    
    private String convertSheetToJSON(Sheet sheet, SpreadsheetOptions options) throws IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        
        int startRow = 0;
        
        // Extract headers if present
        if (options.hasHeaders) {
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    String header = getCellValueAsString(cell, options);
                    headers.add(header != null && !header.trim().isEmpty() ? 
                               header : "Column" + cell.getColumnIndex());
                }
            }
            startRow = 1;
        } else {
            // Generate column headers (Column0, Column1, etc.)
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getLastCellNum(); i++) {
                    headers.add("Column" + i);
                }
            }
        }
        
        // Process data rows
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Map<String, Object> rowData = new LinkedHashMap<>();
            boolean hasData = false;
            
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                Object value = getCellValue(cell, options);
                
                if (value != null) {
                    hasData = true;
                }
                
                rowData.put(headers.get(j), value != null ? value : options.emptyValue);
            }
            
            // Only add rows that have at least one non-empty cell
            if (hasData) {
                rows.add(rowData);
            }
        }
        
        // Format output
        return formatOutput(rows, sheet, options);
    }
    
    private Object getCellValue(Cell cell, SpreadsheetOptions options) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
                
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat(options.dateFormat);
                    return sdf.format(date);
                } else {
                    double value = cell.getNumericCellValue();
                    // Return as integer if it's a whole number
                    if (value == (long) value) {
                        return (long) value;
                    }
                    return value;
                }
                
            case BOOLEAN:
                return cell.getBooleanCellValue();
                
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e2) {
                        return cell.toString();
                    }
                }
                
            case BLANK:
                return null;
                
            default:
                return cell.toString();
        }
    }
    
    private String getCellValueAsString(Cell cell, SpreadsheetOptions options) {
        Object value = getCellValue(cell, options);
        return value != null ? value.toString() : "";
    }
    
    private String formatOutput(List<Map<String, Object>> rows, Sheet sheet, 
                               SpreadsheetOptions options) throws IOException {
        switch (options.format.toLowerCase()) {
            case "ndjson":
                // Newline-delimited JSON
                StringBuilder ndjson = new StringBuilder();
                for (Map<String, Object> row : rows) {
                    ndjson.append(objectMapper.writeValueAsString(row)).append("\n");
                }
                return ndjson.toString();
                
            case "object":
                // JSON object with metadata
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("data", rows);
                
                if (options.includeMetadata) {
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("sheetName", sheet.getSheetName());
                    metadata.put("rowCount", rows.size());
                    metadata.put("columnCount", rows.isEmpty() ? 0 : rows.get(0).size());
                    output.put("metadata", metadata);
                }
                
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
                
            case "array":
            default:
                // Simple JSON array
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
        }
    }
    
    private SpreadsheetOptions parseParameters(String parameters) {
        SpreadsheetOptions options = new SpreadsheetOptions();
        
        if (parameters != null && !parameters.trim().isEmpty()) {
            try {
                // Simple JSON parsing
                if (parameters.contains("\"sheetIndex\":")) {
                    String indexStr = parameters.replaceAll(".*\"sheetIndex\":\\s*(\\d+).*", "$1");
                    try {
                        options.sheetIndex = Integer.parseInt(indexStr);
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                }
                
                if (parameters.contains("\"sheetName\":")) {
                    String name = parameters.replaceAll(".*\"sheetName\":\\s*\"([^\"]+)\".*", "$1");
                    if (!name.equals(parameters)) {
                        options.sheetName = name;
                    }
                }
                
                options.hasHeaders = !parameters.contains("\"hasHeaders\":false");
                options.includeMetadata = parameters.contains("\"includeMetadata\":true");
                
                if (parameters.contains("\"format\":")) {
                    String format = parameters.replaceAll(".*\"format\":\\s*\"([^\"]+)\".*", "$1");
                    if (!format.equals(parameters)) {
                        options.format = format;
                    }
                }
                
                if (parameters.contains("\"dateFormat\":")) {
                    String dateFormat = parameters.replaceAll(".*\"dateFormat\":\\s*\"([^\"]+)\".*", "$1");
                    if (!dateFormat.equals(parameters)) {
                        options.dateFormat = dateFormat;
                    }
                }
                
            } catch (Exception e) {
                logger.warn("Error parsing parameters, using defaults: {}", e.getMessage());
            }
        }
        
        return options;
    }
    
    private static class SpreadsheetOptions {
        int sheetIndex = 0;
        String sheetName = null;
        boolean hasHeaders = true;
        String format = "array";
        boolean includeMetadata = false;
        Object emptyValue = null;
        String dateFormat = "yyyy-MM-dd";
    }
}
