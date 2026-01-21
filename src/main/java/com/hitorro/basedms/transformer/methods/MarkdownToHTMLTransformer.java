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
 * Convert Markdown documents to HTML using pandoc
 * Supports standalone HTML generation with CSS and table of contents
 */
public class MarkdownToHTMLTransformer extends BaseTransformMethod {
    public static final String METHOD_NAME = "markdown_to_html";

    private static final StringProperty PandocPath = new StringProperty(
            "transformer.pandoc.path",
            "Path to pandoc executable",
            "pandoc");

    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }

    @Override
    public boolean ensureServiceAvailable() {
        try {
            String cmd = PandocPath.apply();
            Process p = Runtime.getRuntime().exec(new String[] { cmd, "--version" });
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.transformer.warn("Pandoc not available: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters, String notifyGuid,
            int maxWaitTimeMinutes)
            throws IOException {
        if (!sourceFile.isLocal()) {
            throw new IOException("Markdown to HTML conversion requires local file system");
        }

        File sourceMarkdown = ((FileFile) sourceFile).getJavaFile();
        if (!sourceMarkdown.exists()) {
            throw new IOException("Source Markdown file does not exist: " + sourceMarkdown.getAbsolutePath());
        }

        // Parse parameters:
        // standalone=true|false,toc=true|false,css=style.css,title=Document
        boolean standalone = Boolean.parseBoolean(getParameter(parameters, "standalone", "true"));
        boolean toc = Boolean.parseBoolean(getParameter(parameters, "toc", "false"));
        String css = getParameter(parameters, "css", null);
        String title = getParameter(parameters, "title", "Document");

        // Create temp output file
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File outputFile = new File(tempDir, Fmt.S("markdown_%s.html", id));

        try {
            // Build pandoc command
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(PandocPath.apply());

            // Input file
            command.add(sourceMarkdown.getAbsolutePath());

            // Output format
            command.add("-f");
            command.add("markdown");
            command.add("-t");
            command.add("html");

            // Standalone HTML document
            if (standalone) {
                command.add("--standalone");
                command.add("--metadata");
                command.add(Fmt.S("title=%s", title));
            }

            // Table of contents
            if (toc) {
                command.add("--toc");
                command.add("--toc-depth=3");
            }

            // CSS stylesheet
            if (css != null && !css.isEmpty()) {
                File cssFile = new File(css);
                if (cssFile.exists()) {
                    command.add("--css");
                    command.add(cssFile.getAbsolutePath());
                } else {
                    Log.transformer.warn("CSS file not found: %s", css);
                }
            }

            // Enable syntax highlighting for code blocks
            command.add("--highlight-style=pygments");

            // Output file
            command.add("-o");
            command.add(outputFile.getAbsolutePath());

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
                    Log.transformer.debug("Pandoc: %s", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IOException(Fmt.S("Markdown to HTML conversion failed with exit code %d: %s",
                        exitCode, processOutput.toString()));
            }

            if (!outputFile.exists()) {
                throw new IOException("HTML output was not created: " + outputFile.getAbsolutePath());
            }

            Log.transformer.info("Successfully converted Markdown to HTML (standalone=%s, toc=%s): %s",
                    standalone, toc, outputFile.getAbsolutePath());
            com.hitorro.util.basefile.fs.file.FileFileSystem ffs = new com.hitorro.util.basefile.fs.file.FileFileSystem(
                    outputFile.getParentFile());
            return ffs.getFile(outputFile.getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Markdown to HTML conversion interrupted", e);
        } catch (Exception e) {
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw new IOException("Failed to convert Markdown to HTML: " + e.getMessage(), e);
        }
    }
}
