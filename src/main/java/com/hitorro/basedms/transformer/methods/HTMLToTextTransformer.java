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
 * Extract text from HTML documents using html2text or lynx
 * Converts HTML to plain text while preserving structure
 */
public class HTMLToTextTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "html_to_text";

    private static final StringProperty Html2TextPath = new StringProperty(
            "transformer.html2text.path",
            "Path to html2text executable",
            "html2text");

    private static final IntegerProperty DefaultWidth = new IntegerProperty(
            "transformer.html.text.width",
            "Default text width",
            80);

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        // Try html2text first
        try {
            String cmd = Html2TextPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "-help" });
            if (p.waitFor() == 0) {
                return true;
            }
        } catch (Exception e) {
            // html2text not found or failed to execute
        }

        // Try lynx as fallback
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "lynx", "-version" });
            if (p.waitFor() == 0) {
                return true;
            }
        } catch (Exception e) {
            // lynx not found or failed to execute
        }

        Log.transformer.warn("Neither html2text nor lynx available for HTML to text conversion");
        return false;
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("HTML to text conversion requires local file system");
        }

        File sourceHtml = ((FileFile) sourceFile).getJavaFile();
        if (!sourceHtml.exists()) {
            throw new IOException("Source HTML file does not exist: " + sourceHtml.getAbsolutePath());
        }

        // Parse parameters: width=80,links=inline|reference
        int width = Integer.parseInt(getParameter(parameters, "width", String.valueOf(DefaultWidth.apply())));
        String links = getParameter(parameters, "links", "inline");

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("html_text_%s.txt", id));

        try {
            // Try html2text first
            boolean useHtml2Text = tryHtml2Text();

            java.util.List<String> command = new java.util.ArrayList<>();

            if (useHtml2Text) {
                // Build html2text command
                command.add(Html2TextPath.apply());
                command.add("-width");
                command.add(String.valueOf(width));
                command.add("-nobs"); // No backspaces (bold/underline)
                command.add("-ascii"); // Use plain ASCII

                if ("reference".equalsIgnoreCase(links)) {
                    command.add("-links");
                }

                command.add(sourceHtml.getAbsolutePath());
            } else {
                // Use lynx as fallback
                command.add("lynx");
                command.add("-dump");
                command.add("-width=" + width);
                command.add("-nolist"); // Don't list links at end if inline
                command.add(sourceHtml.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(outputFile);
            pb.redirectErrorStream(true);

            Log.transformer.info("Executing: %s", String.join(" ", command));

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("HTML to text conversion failed with exit code %d", exitCode));
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("Text output was not created or is empty: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully converted HTML to text (width=%d): %s",
                    width, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTML to text conversion interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to convert HTML to text: " + e.getMessage(), e);
        }
    }

    private boolean tryHtml2Text() {
        try {
            Process p = Runtime.getRuntime().exec(new String[] { Html2TextPath.apply(), "-help" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
