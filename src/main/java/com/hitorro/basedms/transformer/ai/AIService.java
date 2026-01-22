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
package com.hitorro.basedms.transformer.ai;

import java.util.List;
import java.util.Map;

/**
 * Abstraction layer for AI/LLM operations used by transformers.
 * This allows transformers to be implementation-agnostic and enables
 * different AI backends (OpenAI, Claude, local models, etc.)
 */
public interface AIService {
    
    /**
     * Generate a summary of the provided text
     * 
     * @param text The text to summarize
     * @param maxLength Maximum length of summary (in words/tokens)
     * @return Summary text
     */
    String summarize(String text, int maxLength);
    
    /**
     * Answer a question about the provided text
     * 
     * @param text The context text
     * @param question The question to answer
     * @return Answer to the question
     */
    String answerQuestion(String text, String question);
    
    /**
     * Answer multiple questions about the provided text
     * 
     * @param text The context text
     * @param questions List of questions
     * @return Map of question to answer
     */
    Map<String, String> answerQuestions(String text, List<String> questions);
    
    /**
     * Extract structured information from text
     * 
     * @param text The text to analyze
     * @param schema JSON schema describing the desired structure
     * @return JSON string with extracted structured data
     */
    String extractStructuredData(String text, String schema);
    
    /**
     * Generate embeddings for text (for vector search/RAG)
     * 
     * @param text The text to embed
     * @return Array of embedding values
     */
    float[] generateEmbedding(String text);
    
    /**
     * Check if the AI service is available
     * 
     * @return true if service is ready to use
     */
    boolean isAvailable();
    
    /**
     * Get the name/type of the AI service
     * 
     * @return Service identifier (e.g., "openai-gpt4", "llama3.2", "claude-3")
     */
    String getServiceName();
}
