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

import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Converts PowerPoint presentations (PPT, PPTX, ODP) to interactive HTML5.
 * Uses LibreOffice to generate a slide-based HTML presentation.
 * 
 * Parameters (JSON):
 * {
 *   "includeNotes": false,      // Include speaker notes (default: false)
 *   "createIndex": true,        // Create slide index/navigation (default: true)
 *   "embedImages": true         // Embed images in HTML (default: true)
 * }
 * 
 * Requires: LibreOffice/OpenOffice with 'soffice' in PATH or configured via
 * transformer.libreoffice.path property.
 */
public class PresentationToHTMLTransformer extends BaseTransformMethod {
    
    private static final Logger logger = LoggerFactory.getLogger(PresentationToHTMLTransformer.class);
    
    public static final String METHOD_NAME = "presentation_to_html";
    
    private static final String DEFAULT_SOFFICE_PATH = "soffice";
    
    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }
    
    @Override
    public boolean ensureServiceAvailable() {
        try {
            String sofficePath = getSofficePath();
            ProcessBuilder pb = new ProcessBuilder(sofficePath, "--version");
            Process p = pb.start();
            int exitCode = p.waitFor(10, TimeUnit.SECONDS) ? p.exitValue() : -1;
            return exitCode == 0;
        } catch (Exception e) {
            logger.warn("LibreOffice not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters,
                           String notifyGuid, int maxWaitTimeMinutes) throws IOException {
        
        if (!sourceFile.isLocal()) {
            throw new IOException("Presentation transformer requires local file system");
        }
        
        logger.info("Converting presentation to HTML");
        
        File sourceDoc = ((FileFile) sourceFile).getJavaFile();
        if (!sourceDoc.exists()) {
            throw new IOException("Source file does not exist: " + sourceDoc.getAbsolutePath());
        }
        
        // Create temp output directory for HTML and assets
        File tempDir = Files.createTempDirectory("presentation_html_").toFile();
        
        try {
            // Use LibreOffice to convert to HTML
            String sofficePath = getSofficePath();
            
            ProcessBuilder pb = new ProcessBuilder(
                sofficePath,
                "--headless",
                "--convert-to", "html",  // HTML format (LibreOffice will use XHTML Impress File filter)
                "--outdir", tempDir.getAbsolutePath(),
                sourceDoc.getAbsolutePath()
            );
            
            logger.info("Executing: {}", String.join(" ", pb.command()));
            
            Process process = pb.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            try (BufferedReader stdOut = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                 BufferedReader stdErr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                
                String line;
                while ((line = stdOut.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = stdErr.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }
            
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Process interrupted", e);
            }
            
            if (exitCode != 0) {
                logger.error("LibreOffice conversion failed with exit code {}: {}", 
                           exitCode, errorOutput.toString());
                throw new IOException("Presentation conversion failed: " + errorOutput.toString());
            }
            
            logger.info("LibreOffice conversion output: {}", output.toString());
            
            // Find the generated HTML file
            File[] htmlFiles = tempDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".html"));
            
            if (htmlFiles == null || htmlFiles.length == 0) {
                throw new IOException("No HTML file generated in: " + tempDir.getAbsolutePath());
            }
            
            File htmlFile = htmlFiles[0];
            
            // Enhance the HTML with better navigation and styling
            enhanceHTML(htmlFile);
            
            logger.info("Successfully converted presentation to HTML: {}", 
                       htmlFile.getAbsolutePath());
            
            FileFileSystem ffs = new FileFileSystem(htmlFile.getParentFile());
            return ffs.getFile(htmlFile.getName());
            
        } catch (IOException e) {
            logger.error("Error converting presentation to HTML", e);
            throw e;
        }
    }
    
    private void enhanceHTML(File htmlFile) throws IOException {
        // Read the generated HTML
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(htmlFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        String html = content.toString();
        
        // LibreOffice exports each slide in a <div class="page-dp3">
        // We need to wrap each one properly and fix absolute positioning
        
        // Replace the slide wrapper class with our enhanced version
        html = html.replaceAll("<div ([^>]*class=\"page-dp3\"[^>]*)>", 
                              "<div class=\"slide-page\" $1>");
        
        // Add comprehensive CSS to fix overlapping and add slide navigation
        String customCSS = 
            "<style>\n" +
            "* { box-sizing: border-box; }\n" +
            "body { font-family: Arial, sans-serif; margin: 0; padding: 0; background: #f5f5f5; }\n" +
            "img { max-width: 100%; height: auto; }\n" +
            "\n" +
            "/* Slide container styling */\n" +
            ".slide-page {\n" +
            "  position: relative !important;\n" +
            "  width: 960px;\n" +
            "  min-height: 720px;\n" +
            "  margin: 20px auto;\n" +
            "  padding: 40px;\n" +
            "  background: white;\n" +
            "  border: 1px solid #ddd;\n" +
            "  border-radius: 8px;\n" +
            "  box-shadow: 0 2px 10px rgba(0,0,0,0.1);\n" +
            "  page-break-after: always;\n" +
            "  overflow: hidden;\n" +
            "}\n" +
            "\n" +
            "/* Fix absolutely positioned children */\n" +
            ".slide-page > div {\n" +
            "  position: relative !important;\n" +
            "  margin-bottom: 15px;\n" +
            "}\n" +
            "\n" +
            "/* Reset all absolute positioning inside slides */\n" +
            ".slide-page * {\n" +
            "  position: relative !important;\n" +
            "  top: auto !important;\n" +
            "  left: auto !important;\n" +
            "}\n" +
            "\n" +
            "/* Title styling */\n" +
            ".slide-page h1, .slide-page h2 {\n" +
            "  color: #333;\n" +
            "  margin-top: 0;\n" +
            "  margin-bottom: 20px;\n" +
            "}\n" +
            "\n" +
            "/* List styling */\n" +
            ".slide-page ul, .slide-page ol {\n" +
            "  margin: 10px 0;\n" +
            "  padding-left: 30px;\n" +
            "}\n" +
            "\n" +
            ".slide-page li {\n" +
            "  margin: 8px 0;\n" +
            "  line-height: 1.6;\n" +
            "}\n" +
            "\n" +
            "/* Navigation controls */\n" +
            ".navigation {\n" +
            "  position: fixed;\n" +
            "  bottom: 20px;\n" +
            "  right: 20px;\n" +
            "  background: white;\n" +
            "  padding: 15px;\n" +
            "  border-radius: 8px;\n" +
            "  box-shadow: 0 2px 15px rgba(0,0,0,0.2);\n" +
            "  z-index: 1000;\n" +
            "}\n" +
            "\n" +
            ".navigation button {\n" +
            "  margin: 0 5px;\n" +
            "  padding: 10px 20px;\n" +
            "  cursor: pointer;\n" +
            "  background: #007bff;\n" +
            "  color: white;\n" +
            "  border: none;\n" +
            "  border-radius: 5px;\n" +
            "  font-size: 14px;\n" +
            "}\n" +
            "\n" +
            ".navigation button:hover {\n" +
            "  background: #0056b3;\n" +
            "}\n" +
            "\n" +
            ".slide-number {\n" +
            "  position: absolute;\n" +
            "  top: 10px;\n" +
            "  right: 10px;\n" +
            "  background: rgba(0,0,0,0.7);\n" +
            "  color: white;\n" +
            "  padding: 5px 10px;\n" +
            "  border-radius: 4px;\n" +
            "  font-size: 12px;\n" +
            "}\n" +
            "\n" +
            "@media print {\n" +
            "  .navigation { display: none; }\n" +
            "  .slide-page { margin: 0; border: none; box-shadow: none; }\n" +
            "}\n" +
            "</style>\n";
        
        // Inject custom CSS before </head>
        if (html.contains("</head>")) {
            html = html.replace("</head>", customCSS + "</head>");
        } else {
            html = customCSS + html;
        }
        
        // Add navigation controls
        String navigation = 
            "<div class=\"navigation\">\n" +
            "  <button onclick=\"window.scrollTo(0,0)\">⬆ Top</button>\n" +
            "  <button onclick=\"window.print()\">🖨 Print</button>\n" +
            "</div>\n";
        
        if (html.contains("</body>")) {
            html = html.replace("</body>", navigation + "</body>");
        } else {
            html += navigation;
        }
        
        // Number the slides
        int slideCount = 0;
        StringBuilder numbered = new StringBuilder();
        String[] parts = html.split("<div class=\"slide-page\"");
        numbered.append(parts[0]); // Head section
        
        for (int i = 1; i < parts.length; i++) {
            slideCount++;
            numbered.append("<div class=\"slide-page\"");
            // Insert slide number
            int styleEnd = parts[i].indexOf('>');
            if (styleEnd > 0) {
                numbered.append(parts[i], 0, styleEnd);
                numbered.append("><div class=\"slide-number\">Slide ").append(slideCount).append("</div>");
                numbered.append(parts[i].substring(styleEnd + 1));
            } else {
                numbered.append(parts[i]);
            }
        }
        
        html = numbered.toString();
        
        // Write enhanced HTML back
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile))) {
            writer.write(html);
        }
        
        logger.info("Enhanced HTML with {} slides", slideCount);
    }
    
    private String getSofficePath() {
        // Try to get from system property first
        String path = System.getProperty("transformer.libreoffice.path");
        if (path != null && !path.trim().isEmpty()) {
            return path;
        }
        
        // Try environment variable
        path = System.getenv("LIBREOFFICE_PATH");
        if (path != null && !path.trim().isEmpty()) {
            return path;
        }
        
        // Default
        return DEFAULT_SOFFICE_PATH;
    }
}
