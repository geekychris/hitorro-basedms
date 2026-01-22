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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.basedms.transformer.ai.AIServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uses AI/LLM to generate summaries of text documents.
 * Produces JSON output with summary, key points, and metadata.
 * 
 * Parameters (JSON):
 * {
 *   "maxLength": 200,           // Max summary length in words (default: 200)
 *   "format": "json",           // Output format: "json" or "text" (default: "json")
 *   "includeKeyPoints": true,   // Extract key points (default: true)
 *   "language": "english"       // Target language (default: english)
 * }
 * 
 * Requires: AI service enabled (hitorro.ai.enabled=true) with Ollama running.
 * 
 * JSON Output format:
 * {
 *   "summary": "Brief summary text...",
 *   "keyPoints": ["point 1", "point 2", ...],
 *   "wordCount": 150,
 *   "sourceLength": 5000,
 *   "compressionRatio": 0.03
 * }
 */
public class DocumentSummarizerTransformer extends BaseTransformMethod {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentSummarizerTransformer.class);
    
    public static final String METHOD_NAME = "document_summarizer";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }
    
    @Override
    public boolean ensureServiceAvailable() {
        if (!AIServiceRegistry.isAvailable()) {
            logger.warn("AI service not available for document summarization");
            return false;
        }
        return true;
    }
    
    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters,
                           String notifyGuid, int maxWaitTimeMinutes) throws IOException {
        
        if (!sourceFile.isLocal()) {
            throw new IOException("Summarizer requires local file system");
        }
        
        logger.info("Generating AI summary for document");
        
        // Check AI service is available
        if (!AIServiceRegistry.isAvailable()) {
            throw new IllegalStateException("AI service not available. Enable with hitorro.ai.enabled=true");
        }
        
        // Parse parameters
        SummarizerOptions options = parseParameters(parameters);
        
        // Read source text
        String text = readTextFile(sourceFile);
        
        logger.info("Generating summary (max {} words) for {} characters of text", 
                   options.maxLength, text.length());
        
        // Generate summary using AI
        String summary = AIServiceRegistry.getInstance().summarize(text, options.maxLength);
        
        // Optionally extract key points
        String keyPoints = null;
        if (options.includeKeyPoints) {
            logger.info("Extracting key points...");
            keyPoints = extractKeyPoints(text);
        }
        
        // Calculate metrics
        int summaryWordCount = countWords(summary);
        int sourceWordCount = countWords(text);
        double compressionRatio = (double) summaryWordCount / sourceWordCount;
        
        logger.info("Summary generated: {} words (compression ratio: {:.2%})", 
                   summaryWordCount, compressionRatio);
        
        // Format output
        String output;
        String extension;
        
        if ("text".equalsIgnoreCase(options.format)) {
            // Plain text output
            StringBuilder textOutput = new StringBuilder();
            textOutput.append("=== SUMMARY ===\n\n");
            textOutput.append(summary).append("\n\n");
            
            if (keyPoints != null) {
                textOutput.append("=== KEY POINTS ===\n\n");
                textOutput.append(keyPoints).append("\n");
            }
            
            output = textOutput.toString();
            extension = ".txt";
        } else {
            // JSON output
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summary", summary);
            
            if (keyPoints != null) {
                // Parse key points into array
                String[] points = keyPoints.split("\n");
                result.put("keyPoints", points);
            }
            
            result.put("metrics", Map.of(
                "summaryWordCount", summaryWordCount,
                "sourceWordCount", sourceWordCount,
                "compressionRatio", compressionRatio,
                "sourceLength", text.length()
            ));
            
            result.put("metadata", Map.of(
                "model", AIServiceRegistry.getInstance().getServiceName(),
                "generatedAt", System.currentTimeMillis()
            ));
            
            output = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            extension = ".json";
        }
        
        // Write output
        File outputFile = Files.createTempFile("summary_", extension).toFile();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(output);
        }
        
        logger.info("Summary saved: {}", outputFile.getAbsolutePath());
        
        FileFileSystem ffs = new FileFileSystem(outputFile.getParentFile());
        return ffs.getFile(outputFile.getName());
    }
    
    private String extractKeyPoints(String text) {
        // Ask AI to extract key points
        String prompt = "List the 5 most important key points from the text. " +
                       "Format as a numbered list.";
        
        return AIServiceRegistry.getInstance().answerQuestion(text, prompt);
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
    
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
    
    private SummarizerOptions parseParameters(String parameters) {
        SummarizerOptions options = new SummarizerOptions();
        
        if (parameters != null && !parameters.trim().isEmpty()) {
            try {
                // Simple JSON parsing
                if (parameters.contains("\"maxLength\":")) {
                    String maxLenStr = parameters.replaceAll(".*\"maxLength\":\\s*(\\d+).*", "$1");
                    try {
                        options.maxLength = Integer.parseInt(maxLenStr);
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                }
                
                if (parameters.contains("\"format\":")) {
                    String format = parameters.replaceAll(".*\"format\":\\s*\"([^\"]+)\".*", "$1");
                    if (!format.equals(parameters)) {
                        options.format = format;
                    }
                }
                
                options.includeKeyPoints = !parameters.contains("\"includeKeyPoints\":false");
                
                if (parameters.contains("\"language\":")) {
                    String language = parameters.replaceAll(".*\"language\":\\s*\"([^\"]+)\".*", "$1");
                    if (!language.equals(parameters)) {
                        options.language = language;
                    }
                }
                
            } catch (Exception e) {
                logger.warn("Error parsing parameters, using defaults: {}", e.getMessage());
            }
        }
        
        return options;
    }
    
    private static class SummarizerOptions {
        int maxLength = 200;
        String format = "json";
        boolean includeKeyPoints = true;
        String language = "english";
    }
}
