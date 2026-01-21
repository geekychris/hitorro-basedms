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
 * Compress PDF files using Ghostscript to reduce file size
 * Supports multiple quality presets and grayscale conversion
 */
public class PDFCompressTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "pdf_compress";

    private static final StringProperty GhostscriptPath = new StringProperty(
            "transformer.ghostscript.path",
            "Path to Ghostscript executable",
            "gs");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = GhostscriptPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "--version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("Ghostscript not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("PDF compression requires local file system");
        }

        File sourcePdf = ((FileFile) sourceFile).getJavaFile();
        if (!sourcePdf.exists()) {
            throw new IOException("Source PDF file does not exist: " + sourcePdf.getAbsolutePath());
        }

        // Parse parameters:
        // quality=screen|ebook|printer,grayscale=true|false,compatibility=1.4
        String quality = getParameter(parameters, "quality", "ebook");
        boolean grayscale = Boolean.parseBoolean(getParameter(parameters, "grayscale", "false"));
        String compatibility = getParameter(parameters, "compatibility", "1.4");

        // Validate quality setting
        String pdfSettings;
        switch (quality.toLowerCase()) {
            case "screen":
                pdfSettings = "screen"; // 72 dpi, smallest file
                break;
            case "ebook":
                pdfSettings = "ebook"; // 150 dpi, balanced
                break;
            case "printer":
                pdfSettings = "printer"; // 300 dpi, high quality
                break;
            default:
                pdfSettings = "ebook";
                Log.transformer.warn("Unknown quality setting '%s', using 'ebook'", quality);
        }

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("pdf_compressed_%s.pdf", id));

        try {
            // Build Ghostscript command
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(GhostscriptPath.apply());
            command.add("-dNOPAUSE");
            command.add("-dBATCH");
            command.add("-dSAFER");
            command.add("-sDEVICE=pdfwrite");
            command.add(Fmt.S("-dCompatibilityLevel=%s", compatibility));
            command.add(Fmt.S("-dPDFSETTINGS=/%s", pdfSettings));

            // Add grayscale conversion if requested
            if (grayscale) {
                command.add("-sColorConversionStrategy=Gray");
                command.add("-dProcessColorModel=/DeviceGray");
            }

            // Compression options
            command.add("-dCompressFonts=true");
            command.add("-dSubsetFonts=true");
            command.add("-dCompressPages=true");
            command.add("-dEmbedAllFonts=true");

            // Output file
            command.add(Fmt.S("-sOutputFile=%s", outputFile.getAbsolutePath()));
            command.add(sourcePdf.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Log.transformer.info("Executing: %s", String.join(" ", command));

            Process process = pb.start();

            // Capture output for debugging
            StringBuilder processOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                    Log.transformer.debug("Ghostscript: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("PDF compression failed with exit code %d: %s",
                        exitCode, processOutput.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("Compressed PDF was not created: " + outputFile.getAbsolutePath());
            }

            // Log compression ratio
            long originalSize = sourcePdf.length();
            long compressedSize = outputFile.length();
            double ratio = (1.0 - ((double) compressedSize / originalSize)) * 100;

            Log.transformer.info("Successfully compressed PDF (quality=%s, grayscale=%s): %s",
                    pdfSettings, grayscale, outputFile.getAbsolutePath());
            Log.transformer.info("Compression: %d KB → %d KB (%.1f%% reduction)",
                    originalSize / 1024, compressedSize / 1024, ratio);

            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF compression interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to compress PDF: " + e.getMessage(), e);
        }
    }
}
