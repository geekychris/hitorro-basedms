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

import com.hitorro.basedms.transformer.Log;
import com.hitorro.basedms.transformer.TransformMethod;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.StringProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Extract text from PDF documents using pdftotext (poppler-utils)
 * Supports layout preservation, page ranges, and encoding options
 */
public class PDFToTextTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "pdf_to_text";

    private static final StringProperty PdfToTextPath = new StringProperty(
            "transformer.pdftotext.path",
            "Path to pdftotext executable",
            "pdftotext");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = PdfToTextPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "-v" });
            int exitCode = p.waitFor();
            return exitCode == 0 || exitCode == 1 || exitCode == 99; // pdftotext returns various codes for -v
        } catch (Exception e) {
            Log.transformer.warn("pdftotext not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("PDF to text conversion requires local file system");
        }

        File sourcePdf = ((FileFile) sourceFile).getJavaFile();
        if (!sourcePdf.exists()) {
            throw new IOException("Source PDF file does not exist: " + sourcePdf.getAbsolutePath());
        }

        // Parse parameters: layout=true,encoding=utf-8,firstpage=1,lastpage=10,eol=unix
        boolean layout = Boolean.parseBoolean(getParameter(parameters, "layout", "false"));
        String encoding = getParameter(parameters, "encoding", "UTF-8");
        String firstPage = getParameter(parameters, "firstpage", null);
        String lastPage = getParameter(parameters, "lastpage", null);
        String eol = getParameter(parameters, "eol", "unix"); // unix, dos, mac

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("pdf_text_%s.txt", id));

        try {
            // Build pdftotext command
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(PdfToTextPath.apply());

            // Add layout option
            if (layout) {
                command.add("-layout");
            }

            // Add encoding
            command.add("-enc");
            command.add(encoding);

            // Add EOL style
            if ("dos".equalsIgnoreCase(eol)) {
                command.add("-eol");
                command.add("dos");
            } else if ("mac".equalsIgnoreCase(eol)) {
                command.add("-eol");
                command.add("mac");
            } else {
                command.add("-eol");
                command.add("unix");
            }

            // Add page range
            if (firstPage != null && !firstPage.isEmpty()) {
                command.add("-f");
                command.add(firstPage);
            }
            if (lastPage != null && !lastPage.isEmpty()) {
                command.add("-l");
                command.add(lastPage);
            }

            // Add input and output files
            command.add(sourcePdf.getAbsolutePath());
            command.add(outputFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Log.transformer.info("Executing: %s", String.join(" ", command));

            Process process = pb.start();

            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    Log.transformer.debug("pdftotext: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("pdftotext failed with exit code %d: %s", exitCode, output.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("Output file was not created: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully extracted text from PDF: %s", outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF text extraction interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}
