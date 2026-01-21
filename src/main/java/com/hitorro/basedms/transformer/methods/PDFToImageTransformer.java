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
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.json.keys.StringProperty;

import java.io.File;
import java.io.IOException;

public class PDFToImageTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "pdf_to_image";

    private static final StringProperty PdfToPpmPath = new StringProperty(
            "transformer.pdftoppm.path",
            "Path to pdftoppm executable",
            "pdftoppm");

    private static final IntegerProperty DefaultDPI = new IntegerProperty(
            "transformer.pdf.image.dpi",
            "Default DPI for PDF to image conversion",
            150);

    private static final IntegerProperty DefaultQuality = new IntegerProperty(
            "transformer.pdf.image.quality",
            "JPEG quality (1-100)",
            85);

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = PdfToPpmPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "-v" });
            int exitCode = p.waitFor();
            return exitCode == 0 || exitCode == 1; // pdftoppm returns 1 for -v
        } catch (Exception e) {
            Log.transformer.warn("pdftoppm not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("PDF to image conversion requires local file system");
        }

        File sourcePdf = ((FileFile) sourceFile).getJavaFile();
        if (!sourcePdf.exists()) {
            throw new IOException("Source PDF file does not exist: " + sourcePdf.getAbsolutePath());
        }

        // Parse parameters: format=jpeg,dpi=150,quality=85,page=1
        String format = getParameter(parameters, "format", "jpeg").toLowerCase();
        int dpi = Integer.parseInt(getParameter(parameters, "dpi", String.valueOf(DefaultDPI.apply())));
        int quality = Integer.parseInt(getParameter(parameters, "quality", String.valueOf(DefaultQuality.apply())));
        int page = Integer.parseInt(getParameter(parameters, "page", "1"));

        // Determine output format flag
        String formatFlag;
        String extension;
        switch (format) {
            case "png":
                formatFlag = "-png";
                extension = "png";
                break;
            case "tiff":
            case "tif":
                formatFlag = "-tiff";
                extension = "tiff";
                break;
            case "jpeg":
            case "jpg":
            default:
                formatFlag = "-jpeg";
                extension = "jpg";
                break;
        }

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("pdf_conv_%s.%s", id, extension));
        String outputPrefix = new File(tempDir, Fmt.S("pdf_conv_%s", id)).getAbsolutePath();

        try {
            // Build pdftoppm command
            ProcessBuilder pb = new ProcessBuilder(
                    PdfToPpmPath.apply(),
                    formatFlag,
                    "-r", String.valueOf(dpi),
                    "-f", String.valueOf(page),
                    "-l", String.valueOf(page),
                    "-singlefile");

            // Add quality for JPEG
            if (format.equals("jpeg") || format.equals("jpg")) {
                pb.command().add("-jpegopt");
                pb.command().add(Fmt.S("quality=%d", quality));
            }

            pb.command().add(sourcePdf.getAbsolutePath());
            pb.command().add(outputPrefix);

            pb.redirectErrorStream(true);

            Log.transformer.info("Executing: %s", String.join(" ", pb.command()));

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("pdftoppm failed with exit code %d", exitCode));
            }

            // pdftoppm with -singlefile creates file without page number suffix
            File actualOutput = new File(outputPrefix + "." + extension);
            if (!actualOutput.exists()) {
                throw new IOException("Output file was not created: " + actualOutput.getAbsolutePath());
            }

            // Rename to expected output name
            if (!actualOutput.renameTo(outputFile)) {
                // If rename fails, try copy
                java.nio.file.Files.copy(actualOutput.toPath(), outputFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                actualOutput.delete();
            }

            Log.transformer.info("Successfully converted PDF to %s: %s", format, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF conversion interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to convert PDF to image: " + e.getMessage(), e);
        }
    }
}
