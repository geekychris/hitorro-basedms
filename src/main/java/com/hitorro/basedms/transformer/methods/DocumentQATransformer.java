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
import java.util.*;

/**
 * Uses AI/LLM to answer questions about document content.
 * Implements a basic RAG (Retrieval Augmented Generation) pattern.
 * 
 * Parameters (JSON):
 * {
 *   "questions": ["What is the main topic?", "Who is the author?"],
 *   "format": "json"              // Output format: "json" or "text" (default: "json")
 * }
 * 
 * Or single question:
 * {
 *   "question": "What is the main topic?"
 * }
 * 
 * Requires: AI service enabled (hitorro.ai.enabled=true) with Ollama running.
 * 
 * JSON Output format:
 * {
 *   "answers": {
 *     "What is the main topic?": "The document discusses...",
 *     "Who is the author?": "The author is..."
 *   },
 *   "metadata": {
 *     "questionCount": 2,
 *     "documentLength": 5000,
 *     "model": "ollama-llama3.2"
 *   }
 * }
 */
public class DocumentQATransformer extends BaseTransformMethod {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentQATransformer.class);
    
    public static final String METHOD_NAME = "document_qa";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getMethodName() {
        return METHOD_NAME;
    }
    
    @Override
    public boolean ensureServiceAvailable() {
        if (!AIServiceRegistry.isAvailable()) {
            logger.warn("AI service not available for document Q&A");
            return false;
        }
        return true;
    }
    
    @Override
    public BaseFile convert(BaseFile sourceFile, String id, String parameters,
                           String notifyGuid, int maxWaitTimeMinutes) throws IOException {
        
        if (!sourceFile.isLocal()) {
            throw new IOException("Q&A transformer requires local file system");
        }
        
        logger.info("Answering questions about document");
        
        // Check AI service is available
        if (!AIServiceRegistry.isAvailable()) {
            throw new IllegalStateException("AI service not available. Enable with hitorro.ai.enabled=true");
        }
        
        // Parse parameters
        QAOptions options = parseParameters(parameters);
        
        if (options.questions.isEmpty()) {
            throw new IllegalArgumentException("No questions provided. Use 'question' or 'questions' parameter.");
        }
        
        // Read source text
        String text = readTextFile(sourceFile);
        
        logger.info("Answering {} question(s) about {} characters of text", 
                   options.questions.size(), text.length());
        
        // Get answers from AI
        Map<String, String> answers;
        
        if (options.questions.size() == 1) {
            // Single question - use simple API
            String question = options.questions.get(0);
            String answer = AIServiceRegistry.getInstance().answerQuestion(text, question);
            answers = Map.of(question, answer);
        } else {
            // Multiple questions - batch them
            answers = AIServiceRegistry.getInstance().answerQuestions(text, options.questions);
        }
        
        logger.info("Generated {} answer(s)", answers.size());
        
        // Format output
        String output;
        String extension;
        
        if ("text".equalsIgnoreCase(options.format)) {
            // Plain text output
            StringBuilder textOutput = new StringBuilder();
            textOutput.append("=== DOCUMENT Q&A ===\n\n");
            
            int i = 1;
            for (String question : options.questions) {
                textOutput.append("Q").append(i).append(": ").append(question).append("\n");
                textOutput.append("A").append(i).append(": ").append(answers.get(question)).append("\n\n");
                i++;
            }
            
            output = textOutput.toString();
            extension = ".txt";
        } else {
            // JSON output
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("answers", answers);
            
            result.put("metadata", Map.of(
                "questionCount", options.questions.size(),
                "documentLength", text.length(),
                "documentWordCount", countWords(text),
                "model", AIServiceRegistry.getInstance().getServiceName(),
                "generatedAt", System.currentTimeMillis()
            ));
            
            output = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            extension = ".json";
        }
        
        // Write output
        File outputFile = Files.createTempFile("qa_", extension).toFile();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write(output);
        }
        
        logger.info("Q&A results saved: {}", outputFile.getAbsolutePath());
        
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
    
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
    
    private QAOptions parseParameters(String parameters) {
        QAOptions options = new QAOptions();
        
        if (parameters == null || parameters.trim().isEmpty()) {
            return options;
        }
        
        try {
            // Parse JSON to extract questions
            @SuppressWarnings("unchecked")
            Map<String, Object> params = objectMapper.readValue(parameters, Map.class);
            
            // Check for single question
            if (params.containsKey("question")) {
                options.questions.add(params.get("question").toString());
            }
            
            // Check for multiple questions
            if (params.containsKey("questions")) {
                Object questionsObj = params.get("questions");
                if (questionsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> questionsList = (List<String>) questionsObj;
                    options.questions.addAll(questionsList);
                }
            }
            
            // Get format
            if (params.containsKey("format")) {
                options.format = params.get("format").toString();
            }
            
        } catch (Exception e) {
            logger.warn("Error parsing parameters: {}", e.getMessage());
            
            // Fallback: try simple string parsing
            if (parameters.contains("\"question\":")) {
                String question = parameters.replaceAll(".*\"question\":\\s*\"([^\"]+)\".*", "$1");
                if (!question.equals(parameters)) {
                    options.questions.add(question);
                }
            }
        }
        
        return options;
    }
    
    private static class QAOptions {
        List<String> questions = new ArrayList<>();
        String format = "json";
    }
}
