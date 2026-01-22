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

/**
 * Registry for accessing the AI service implementation.
 * This follows the same pattern as TransformerService - a singleton that
 * can be set by the Spring layer but accessed from anywhere.
 */
public class AIServiceRegistry {
    
    private static AIService instance;
    
    /**
     * Set the AI service implementation (called by Spring configuration)
     * 
     * @param service The AI service implementation
     */
    public static synchronized void setInstance(AIService service) {
        instance = service;
    }
    
    /**
     * Get the AI service instance
     * 
     * @return AI service or null if not configured
     */
    public static synchronized AIService getInstance() {
        return instance;
    }
    
    /**
     * Check if an AI service is available
     * 
     * @return true if service is configured and available
     */
    public static boolean isAvailable() {
        return instance != null && instance.isAvailable();
    }
}
