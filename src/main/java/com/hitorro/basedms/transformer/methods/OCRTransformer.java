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
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Perform OCR (Optical Character Recognition) on images using Tesseract
 * Supports multiple output formats: text, PDF, HOCR
 */
public class OCRTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "ocr";

    private static final StringProperty TesseractPath = new StringProperty(
            "transformer.tesseract.path",
            "Path to tesseract executable",
            "tesseract");

    private static final StringProperty DefaultLanguage = new StringProperty(
            "transformer.ocr.language",
            "Default OCR language",
            "eng");

    private static final IntegerProperty DefaultDPI = new IntegerProperty(
            "transformer.ocr.dpi",
            "Default DPI for OCR",
            300);

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = TesseractPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "--version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("Tesseract OCR not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("OCR requires local file system");
        }

        File sourceImage = ((FileFile) sourceFile).getJavaFile();
        if (!sourceImage.exists()) {
            throw new IOException("Source image file does not exist: " + sourceImage.getAbsolutePath());
        }

        // Parse parameters: lang=eng,output=txt|pdf|hocr,psm=3,dpi=300
        String lang = getParameter(parameters, "lang", DefaultLanguage.apply());
        String output = getParameter(parameters, "output", "txt");
        String psm = getParameter(parameters, "psm", "3"); // Page segmentation mode
        int dpi = Integer.parseInt(getParameter(parameters, "dpi", String.valueOf(DefaultDPI.apply())));

        // Validate output format
        String outputFormat;
        String extension;
        switch (output.toLowerCase()) {
            case "pdf":
                outputFormat = "pdf";
                extension = "pdf";
                break;
            case "hocr":
                outputFormat = "hocr";
                extension = "hocr";
                break;
            case "txt":
            case "text":
            default:
                outputFormat = "txt";
                extension = "txt";
                break;
        }

        // Create temp output file (tesseract adds extension automatically)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        String outputBase = Fmt.S("ocr_%s", id);
        File outputBaseFile = new File(tempDir, outputBase);
        File outputFile = new File(tempDir, outputBase + "." + extension);

        try {
            // Build tesseract command
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(TesseractPath.apply());
            command.add(sourceImage.getAbsolutePath());
            command.add(outputBaseFile.getAbsolutePath()); // Tesseract adds extension

            // Add language
            command.add("-l");
            command.add(lang);

            // Add page segmentation mode
            command.add("--psm");
            command.add(psm);

            // Add DPI if not default
            if (dpi != 300) {
                command.add("--dpi");
                command.add(String.valueOf(dpi));
            }

            // Add output format
            command.add(outputFormat);

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
                    Log.transformer.debug("Tesseract: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("Tesseract OCR failed with exit code %d: %s",
                        exitCode, processOutput.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("OCR output file was not created: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully performed OCR (lang=%s, format=%s): %s",
                    lang, outputFormat, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to perform OCR: " + e.getMessage(), e);
        }
    }
}
