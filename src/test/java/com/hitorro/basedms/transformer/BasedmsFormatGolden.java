package com.hitorro.basedms.transformer;

import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.jvs.ConversionContext;
import com.hitorro.basedms.jvs.converters.VersionableObjectConverter;
import com.hitorro.basedms.transformer.methods.PDFTemplateTransformer;
import com.hitorro.basedms.transformer.methods.SpreadsheetToJSONTransformer;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.objects.EmbeddableDomainValue;
import com.hitorro.util.core.valuemap.DomainValueIntf;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The JSON basedms produces and the JSON it reads, with no Jackson types in this source, so the
 * same code runs on the published 3.1.2 jar (Jackson 2; hitorro-j3 tools/Jackson2BasedmsFormatGolden
 * prints {@link #compute}) and on 4.0.0 (Jackson2FormatCompatTest): spreadsheet-to-JSON renditions,
 * the FDF the PDF template transformer writes from JSON variables, and DMS-to-JVS documents.
 */
public final class BasedmsFormatGolden {

    private BasedmsFormatGolden() {
    }

    static final Map<String, String> out = new LinkedHashMap<>();

    static void p(String k, Object v) {
        out.put(k, String.valueOf(v).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r"));
    }

    static String error(Throwable t) {
        while (t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null) t = t.getCause();
        String n = t.getClass().getName();
        return "!" + (n.startsWith("com.fasterxml.jackson") || n.startsWith("tools.jackson") ? "jackson" : t.getClass().getSimpleName());
    }

    static Path workbook(Path dir) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Data \u00e9");
            CreationHelper h = wb.getCreationHelper();
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(h.createDataFormat().getFormat("yyyy-mm-dd"));
            Object[][] rows = {
                    {"name", "count", "ratio", "flag", "when", "formula", "note"},
                    {"caf\u00e9 \uD83D\uDE00", 3.0, 1.5, true, new GregorianCalendar(2024, Calendar.FEBRUARY, 29).getTime(), "B2*2", "\"quoted\" {x}"},
                    {"big", 9007199254740993.0, 0.1, false, null, "A3&\"!\"", null},
                    {"exp", 1e20, -0.0, null, null, "1/0", "line\nbreak"},
                    {null, null, null, null, null, null, null},
                    {"tiny", 1e-7, 123456789.125, true, null, null, ""},
            };
            for (int r = 0; r < rows.length; r++) {
                Row row = s.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    Object v = rows[r][c];
                    if (v == null) continue;
                    Cell cell = row.createCell(c);
                    if (r > 0 && c == 5) cell.setCellFormula((String) v);
                    else if (v instanceof String) cell.setCellValue((String) v);
                    else if (v instanceof Double) cell.setCellValue((Double) v);
                    else if (v instanceof Boolean) cell.setCellValue((Boolean) v);
                    else if (v instanceof Date) {
                        cell.setCellValue((Date) v);
                        cell.setCellStyle(date);
                    }
                }
            }
            Path f = dir.resolve("book.xlsx");
            try (FileOutputStream o = new FileOutputStream(f.toFile())) {
                wb.write(o);
            }
            return f;
        }
    }

    static String read(BaseFile f) throws Exception {
        File jf = ((FileFile) f).getJavaFile();
        String s = new String(Files.readAllBytes(jf.toPath()), StandardCharsets.UTF_8);
        jf.delete();
        return s;
    }

    public static Map<String, String> compute(Path dir) throws Exception {
        out.clear();
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            Path book = workbook(dir);
            BaseFile src = new FileFileSystem(dir.toFile()).getFile(book.getFileName().toString());
            String[] sheetArgs = {null, "", "{\"format\":\"array\"}", "{\"format\":\"ndjson\"}",
                    "{\"format\":\"object\",\"includeMetadata\":true}", "{\"format\":\"object\"}",
                    "{\"hasHeaders\":false,\"format\":\"ndjson\"}", "{\"sheetName\":\"Data \u00e9\",\"dateFormat\":\"dd/MM/yyyy\"}",
                    "{\"sheetIndex\": 3}"};
            for (int i = 0; i < sheetArgs.length; i++) {
                final String a = sheetArgs[i];
                String v;
                try {
                    v = read(new SpreadsheetToJSONTransformer().convert(src, "id", a, null, 1));
                } catch (Throwable t) {
                    v = error(t);
                }
                p("sheet." + i, v);
            }

            // FDF from JSON template variables (escaping, every node type, the CSV fallback, bad JSON)
            String[] fdf = {null, "", "a=1,b=(x),flatten=true,_template_path=/t", "{}",
                    "{\"name\":\"Ann (A) \\\\ B\",\"n\":1.10,\"i\":12345678901234567890,\"e\":1e2,\"t\":true,\"z\":null,"
                            + "\"o\":{\"x\":1},\"arr\":[1,2],\"u\":\"\u00e9\uD83D\uDE00\",\"flatten\":true,\"_template_path\":\"/x\"}",
                    "{\"variables\":{\"a\":\"1\",\"b\":2},\"flatten\":false}", "{\"variables\":[1,2]}", "{\"variables\":\"str\"}",
                    "{bad", "{\"a\":1} trailing", "{\"a\":1,\"a\":2}", "[1,2]", "\"just a string\"", "  {\"sp\":\" x \"}"};
            Method gen = PDFTemplateTransformer.class.getDeclaredMethod("generateFdf", String.class, String.class, File.class);
            gen.setAccessible(true);
            for (int i = 0; i < fdf.length; i++) {
                File f = dir.resolve("out" + i + ".fdf").toFile();
                String v;
                try {
                    gen.invoke(new PDFTemplateTransformer(), fdf[i], "", f);
                    v = new String(Files.readAllBytes(f.toPath()), StandardCharsets.ISO_8859_1);
                } catch (Throwable t) {
                    v = error(t);
                }
                p("fdf." + i, v);
            }

            // DMS object -> JVS (no type system configured here, so the untyped path)
            VersionableObjectConverter conv = new VersionableObjectConverter();
            VersionableObject vo = new VersionableObject();
            vo.setGuid("vo:1");
            vo.setNote("note \u00e9");
            vo.setCreator("chris");
            vo.setRealm("r");
            vo.setAuthoredDate(new Date(1700000000000L));
            for (String clock : new String[]{"creationDate", "modifiedDate"}) {   // otherwise the time of the run
                java.lang.reflect.Field f = VersionableObject.class.getDeclaredField(clock);
                f.setAccessible(true);
                f.set(vo, new Date(1700000001000L));
            }
            Set<DomainValueIntf> cats = new LinkedHashSet<>();
            cats.add(new EmbeddableDomainValue("topic", "food"));
            vo.setCategories(cats);
            String v;
            try {
                v = String.valueOf(conv.convert(vo, ConversionContext.withDefaults()).getJsonNode());
            } catch (Throwable t) {
                v = error(t) + (t.getCause() != null ? " cause " + error(t.getCause()) : "");
            }
            p("jvs.versionable", v);
        } finally {
            TimeZone.setDefault(previous);
        }
        return out;
    }

    public static void main(String[] a) throws Exception {
        for (Map.Entry<String, String> e : compute(Files.createTempDirectory("bdfmt")).entrySet()) {
            System.out.println(e.getKey() + "=" + e.getValue());
        }
    }
}
