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
 * Convert documents using LibreOffice/OpenOffice
 * Supports: doc/docx -> PDF, xls/xlsx -> PDF, ppt/pptx -> PDF, odt/ods/odp ->
 * PDF
 */
public class LibreOfficeTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "libreoffice_convert";

    private static final StringProperty LibreOfficePath = new StringProperty(
            "transformer.libreoffice.path",
            "Path to LibreOffice soffice executable",
            "soffice");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = LibreOfficePath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "--version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("LibreOffice not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("LibreOffice conversion requires local file system");
        }

        File sourceDoc = ((FileFile) sourceFile).getJavaFile();
        if (!sourceDoc.exists()) {
            throw new IOException("Source document does not exist: " + sourceDoc.getAbsolutePath());
        }

        // Parse parameters: format=pdf (default)
        String format = getParameter(parameters, "format", "pdf").toLowerCase();

        // Create temp output directory
        File tempDir = new File(System.getProperty("java.io.tmpdir"), Fmt.S("libreoffice_conv_%s", id));
        tempDir.mkdirs();

        File outputFile = null;

        try {
            // Build LibreOffice command
            // --headless: run without GUI
            // --convert-to: output format
            // --outdir: output directory
            ProcessBuilder pb = new ProcessBuilder(
                    LibreOfficePath.apply(),
                    "--headless",
                    "--convert-to", format,
                    "--outdir", tempDir.getAbsolutePath(),
                    sourceDoc.getAbsolutePath());

            pb.redirectErrorStream(true);

            Log.transformer.info("Executing: %s", String.join(" ", pb.command()));

            Process process = pb.start();

            // Capture output for debugging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    Log.transformer.debug("LibreOffice: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("LibreOffice conversion failed with exit code %d: %s",
                        exitCode, output.toString()));
            }

            // Find the output file (LibreOffice creates it with same base name + new
            // extension)
            String baseName = sourceDoc.getName();
            int lastDot = baseName.lastIndexOf('.');
            if (lastDot > 0) {
                baseName = baseName.substring(0, lastDot);
            }

            outputFile = new File(tempDir, baseName + "." + format);

            if (!outputFile.exists()) {
                // Try to find any file in the output directory
                File[] files = tempDir.listFiles();
                if (files != null && files.length > 0) {
                    outputFile = files[0];
                } else {
                    throw new IOException("Output file was not created by LibreOffice");
                }
            }

            Log.transformer.info("Successfully converted document to %s: %s", format, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LibreOffice conversion interrupted", e);
        } catch (Exception e) {
            // Clean up on error
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete();
            }
            if (tempDir.exists()) {
                tempDir.delete();
            }
            throw new IOException("Failed to convert document: " + e.getMessage(), e);
        }
    }
}
