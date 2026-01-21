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
 * Convert PostScript and EPS files to PDF using Ghostscript (ps2pdf)
 * Supports quality settings and PDF version compatibility
 */
public class PostScriptToPDFTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "ps_to_pdf";

    private static final StringProperty Ps2PdfPath = new StringProperty(
            "transformer.ps2pdf.path",
            "Path to ps2pdf executable (Ghostscript)",
            "ps2pdf");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            // ps2pdf is a shell script wrapper around gs (ghostscript)
            // Check for ghostscript instead
            Process p = Runtime.getRuntime().exec(new String[] { "gs", "--version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("Ghostscript (ps2pdf) not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("PostScript to PDF conversion requires local file system");
        }

        File sourcePs = ((FileFile) sourceFile).getJavaFile();
        if (!sourcePs.exists()) {
            throw new IOException("Source PostScript file does not exist: " + sourcePs.getAbsolutePath());
        }

        // Parse parameters: quality=screen|ebook|printer|prepress,compatibility=1.4
        String quality = getParameter(parameters, "quality", "ebook");
        String compatibility = getParameter(parameters, "compatibility", "1.4");

        // Validate quality setting
        String pdfSettings;
        switch (quality.toLowerCase()) {
            case "screen":
                pdfSettings = "screen"; // 72 dpi, low quality
                break;
            case "ebook":
                pdfSettings = "ebook"; // 150 dpi, medium quality
                break;
            case "printer":
                pdfSettings = "printer"; // 300 dpi, high quality
                break;
            case "prepress":
                pdfSettings = "prepress"; // 300 dpi, highest quality
                break;
            default:
                pdfSettings = "ebook";
                Log.transformer.warn("Unknown quality setting '%s', using 'ebook'", quality);
        }

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("ps_conv_%s.pdf", id));

        try {
            // Build ps2pdf command
            // ps2pdf is typically a shell script, so we'll call gs directly for better
            // control
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add("gs");
            command.add("-dNOPAUSE");
            command.add("-dBATCH");
            command.add("-dSAFER");
            command.add("-sDEVICE=pdfwrite");
            command.add(Fmt.S("-dCompatibilityLevel=%s", compatibility));
            command.add(Fmt.S("-dPDFSETTINGS=/%s", pdfSettings));
            command.add(Fmt.S("-sOutputFile=%s", outputFile.getAbsolutePath()));
            command.add(sourcePs.getAbsolutePath());

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
                    Log.transformer.debug("Ghostscript: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("Ghostscript (ps2pdf) failed with exit code %d: %s",
                        exitCode, output.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("Output PDF was not created: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully converted PostScript to PDF (quality=%s): %s",
                    pdfSettings, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PostScript conversion interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to convert PostScript to PDF: " + e.getMessage(), e);
        }
    }
}
