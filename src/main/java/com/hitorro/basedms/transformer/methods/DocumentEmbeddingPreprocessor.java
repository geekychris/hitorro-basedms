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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

/**
 * Preprocesses text documents for embedding generation.
 * Cleans and normalizes text to improve vector embedding quality:
 * - Removes extra whitespace
 * - Normalizes line breaks
 * - Removes headers/footers patterns
 * - Removes special characters (optional)
 * - Converts to lowercase (optional)
 * - Removes URLs (optional)
 * - Removes email addresses (optional)
 * 
 * Parameters (JSON):
 * {
 *   "lowercase": true/false,        // Convert to lowercase (default: false)
 *   "removeUrls": true/false,       // Remove URLs (default: true)
 *   "removeEmails": true/false,     // Remove email addresses (default: true)
 *   "removeSpecialChars": true/false, // Remove special characters (default: false)
 *   "maxLineLength": 1000,          // Max line length before wrapping (default: 0 = no limit)
 *   "removeHeaders": true/false     // Remove common header/footer patterns (default: true)
 * }
 */
public class DocumentEmbeddingPreprocessor extends BaseTransformMethod {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentEmbeddingPreprocessor.class);
    
    public static final String METHOD_NAME = "embedding_preprocessor";
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[^\\s]+|www\\.[^\\s]+",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HEADER_FOOTER_PATTERN = Pattern.compile(
        "^(Page \\d+|\\d+ of \\d+|Chapter \\d+|Section \\d+)$",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }
    
    @Override
    public boolean ensureServiceAvailable() {
        // No external dependencies required
        return true;
    }
    
    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters,
                           String notifyGuid, int maxWaitTimeMinutes) throws IOException {
        
        if (!sourceFile.isLocal()) {
            throw new IOException("Embedding preprocessor requires local file system");
        }
        
        logger.info("Preprocessing text for embeddings");
        
        // Parse parameters
        PreprocessorOptions options = parseParameters(parameters);
        
        // Read source text
        String text = readTextFile(sourceFile);
        
        // Apply preprocessing steps
        text = preprocessText(text, options);
        
        // Write output
        File outputFile = Files.createTempFile("preprocessed_", ".txt").toFile();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(text);
        }
        
        logger.info("Preprocessed text saved: {} ({} chars)", 
                   outputFile.getAbsolutePath(), text.length());
        
        FileFileSystem ffs = new FileFileSystem(outputFile.getParentFile());
        return ffs.getFile(outputFile.getName());
    }
    
    private String readTextFile(BaseFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        File javaFile = file.isLocal() ? ((com.hitorro.util.basefile.fs.file.FileFile) file).getJavaFile() : null;
        if (javaFile == null) throw new IOException("File must be local");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(javaFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private String preprocessText(String text, PreprocessorOptions options) {
        // Remove headers/footers
        if (options.removeHeaders) {
            text = HEADER_FOOTER_PATTERN.matcher(text).replaceAll("");
        }
        
        // Remove URLs
        if (options.removeUrls) {
            text = URL_PATTERN.matcher(text).replaceAll("[URL]");
        }
        
        // Remove emails
        if (options.removeEmails) {
            text = EMAIL_PATTERN.matcher(text).replaceAll("[EMAIL]");
        }
        
        // Remove special characters (keep letters, numbers, basic punctuation)
        if (options.removeSpecialChars) {
            text = text.replaceAll("[^a-zA-Z0-9\\s.,!?;:()\\[\\]\"'-]", "");
        }
        
        // Normalize whitespace
        text = text.replaceAll("\\s+", " ");  // Multiple spaces to single space
        text = text.replaceAll("\\n\\s*\\n\\s*\\n+", "\n\n");  // Multiple newlines to double newline
        
        // Wrap long lines
        if (options.maxLineLength > 0) {
            text = wrapText(text, options.maxLineLength);
        }
        
        // Convert to lowercase
        if (options.lowercase) {
            text = text.toLowerCase();
        }
        
        // Trim and clean
        text = text.trim();
        
        return text;
    }
    
    private String wrapText(String text, int maxLength) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            if (line.length() <= maxLength) {
                result.append(line).append("\n");
            } else {
                // Wrap long line at word boundaries
                String[] words = line.split("\\s+");
                StringBuilder currentLine = new StringBuilder();
                
                for (String word : words) {
                    if (currentLine.length() + word.length() + 1 > maxLength) {
                        if (currentLine.length() > 0) {
                            result.append(currentLine.toString().trim()).append("\n");
                            currentLine = new StringBuilder();
                        }
                    }
                    currentLine.append(word).append(" ");
                }
                
                if (currentLine.length() > 0) {
                    result.append(currentLine.toString().trim()).append("\n");
                }
            }
        }
        
        return result.toString();
    }
    
    private PreprocessorOptions parseParameters(String parameters) {
        PreprocessorOptions options = new PreprocessorOptions();
        
        if (parameters != null && !parameters.trim().isEmpty()) {
            try {
                // Simple JSON parsing (could use Jackson if available)
                options.lowercase = parameters.contains("\"lowercase\":true");
                options.removeUrls = !parameters.contains("\"removeUrls\":false");
                options.removeEmails = !parameters.contains("\"removeEmails\":false");
                options.removeSpecialChars = parameters.contains("\"removeSpecialChars\":true");
                options.removeHeaders = !parameters.contains("\"removeHeaders\":false");
                
                // Parse maxLineLength
                if (parameters.contains("\"maxLineLength\":")) {
                    String maxLenStr = parameters.replaceAll(".*\"maxLineLength\":\\s*(\\d+).*", "$1");
                    try {
                        options.maxLineLength = Integer.parseInt(maxLenStr);
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                }
            } catch (Exception e) {
                logger.warn("Error parsing parameters, using defaults: {}", e.getMessage());
            }
        }
        
        return options;
    }
    
    private static class PreprocessorOptions {
        boolean lowercase = false;
        boolean removeUrls = true;
        boolean removeEmails = true;
        boolean removeSpecialChars = false;
        boolean removeHeaders = true;
        int maxLineLength = 0;
    }
}
