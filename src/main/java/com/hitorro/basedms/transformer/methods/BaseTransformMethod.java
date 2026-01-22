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

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.basedms.transformer.TransformMethod;
import com.hitorro.util.json.String2JsonMapper;

/**
 * Common base class for transformer methods providing shared functionality
 */
public abstract class BaseTransformMethod implements TransformMethod {

    private static final String2JsonMapper jsonMapper = new String2JsonMapper();

    /**
     * Helper to parse parameters in format key1=value1,key2=value2 or JSON
     */
    protected String getParameter(String parameters, String key, String defaultValue) {
        if (parameters == null || parameters.isEmpty()) {
            return defaultValue;
        }

        if (isJson(parameters)) {
            JsonNode node = jsonMapper.apply(parameters);
            if (node != null && node.has(key)) {
                JsonNode value = node.get(key);
                return value.isNull() ? defaultValue : value.asText();
            }
            return defaultValue;
        }

        for (String param : parameters.split(",")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(key)) {
                String val = parts[1].trim();
                return val;
            }
        }

        return defaultValue;
    }

    /**
     * Parses the parameters string as a JSON node
     */
    protected JsonNode getJsonParameters(String parameters) {
        if (parameters == null || parameters.isEmpty() || !isJson(parameters)) {
            return null;
        }
        return jsonMapper.apply(parameters);
    }

    private boolean isJson(String parameters) {
        String trimmed = parameters.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}
