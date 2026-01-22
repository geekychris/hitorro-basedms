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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.basedms.transformer.Log;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.StringProperty;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 * PDF Template Transformer using pdftk to fill form fields.
 * Supports both JSON and CSV parameters.
 * JSON format: {"flatten": true, "variables": {"field1": "value1"}}
 * CSV format: flatten=true,field1=value1,field2=value2
 */
public class PDFTemplateTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "pdf_template";

    private static final StringProperty PdftkPath = new StringProperty(
            "transformer.pdftk.path",
            "Path to pdftk executable",
            "pdftk");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = PdftkPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "--version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("pdftk not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {

        File templatePdf;
        String data;

        String templatePath = getParameter(parameters, "_template_path", null);
        if (templatePath != null) {
            templatePdf = new File(templatePath);
            // Read data from source file
            data = sourceFile.readString();
        } else {
            if (!sourceFile.isLocal()) {
                throw new IOException("PDF templating requires local file system when source is the template");
            }
            templatePdf = ((FileFile) sourceFile).getJavaFile();
            data = parameters;
        }

        if (!templatePdf.exists()) {
            throw new IOException("Template PDF file does not exist: " + templatePdf.getAbsolutePath());
        }

        // Create temp files
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File fdfFile = new File(tempDir, Fmt.S("pdf_template_%s.fdf", id));
        File outputFile = new File(tempDir, Fmt.S("pdf_filled_%s.pdf", id));

        try {
            boolean flatten = Boolean.parseBoolean(getParameter(parameters, "flatten", "true"));

            // Generate FDF file
            generateFdf(data, parameters, fdfFile);

            // Build pdftk command
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(PdftkPath.apply());
            command.add(templatePdf.getAbsolutePath());
            command.add("fill_form");
            command.add(fdfFile.getAbsolutePath());
            command.add("output");
            command.add(outputFile.getAbsolutePath());
            if (flatten) {
                command.add("flatten");
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Log.transformer.info("Executing pdftk: %s", String.join(" ", command));

            Process process = pb.start();

            // Capture output
            StringBuilder processOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                    Log.transformer.debug("pdftk: %s", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(Fmt.S("pdftk failed with exit code %d: %s", exitCode, processOutput.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("Filled PDF was not created: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully filled PDF template: %s", outputFile.getAbsolutePath());

            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF templating interrupted", e);
        } finally {
            // Log FDF content for debugging
            if (fdfFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(fdfFile))) {
                    Log.transformer.debug("FDF Content for %s:", id);
                    String l;
                    while ((l = r.readLine()) != null) {
                        Log.transformer.debug("  %s", l);
                    }
                } catch (IOException e) {
                    Log.transformer.warn("Failed to log FDF content: %s", e.getMessage());
                }
                fdfFile.delete();
            }
        }
    }

    private void generateFdf(String data, String parameters, File fdfFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fdfFile), "ISO-8859-1")))) {
            writer.println("%FDF-1.2");
            writer.println("1 0 obj");
            writer.println("<<");
            writer.println("/FDF << /Fields [");

            JsonNode node = getJsonParameters(data);
            if (node != null && node.isObject()) {
                JsonNode vars = node.has("variables") ? node.get("variables") : node;
                if (vars.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = vars.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String fieldName = field.getKey();
                        if (fieldName.equalsIgnoreCase("flatten") || fieldName.equalsIgnoreCase("variables")
                                || fieldName.equalsIgnoreCase("_template_path"))
                            continue;
                        writer.println(Fmt.S("<< /V (%s) /T (%s) >>",
                                escapeFdf(field.getValue().asText()),
                                escapeFdf(fieldName)));
                    }
                }
            } else if (data != null) {
                // Fallback to CSV parameters
                for (String param : data.split(",")) {
                    String[] parts = param.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        if (key.equalsIgnoreCase("flatten") || key.equalsIgnoreCase("_template_path"))
                            continue;
                        writer.println(Fmt.S("<< /V (%s) /T (%s) >>",
                                escapeFdf(parts[1].trim()),
                                escapeFdf(key)));
                    }
                }
            }

            writer.println("] /NeedAppearances true >>");
            writer.println(">>");
            writer.println("endobj");
            writer.println("trailer");
            writer.println("<<");
            writer.println("/Root 1 0 R");
            writer.println(">>");
            writer.println("%%EOF");
        }
    }

    private String escapeFdf(String value) {
        if (value == null)
            return "";
        // Basic FDF escaping: ( becomes \(, ) becomes \), \ becomes \\
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
