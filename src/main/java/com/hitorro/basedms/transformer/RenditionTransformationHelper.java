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
package com.hitorro.basedms.transformer;

import com.hitorro.base.objects.Content;
import com.hitorro.basedms.transformer.constraints.FromConstraint;
import com.hitorro.basedms.transformer.constraints.ToConstraint;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.opers.HTPredicate;
import com.hitorro.util.core.opers.LogicalAndOperator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to find available transformations for content renditions
 */
public class RenditionTransformationHelper {
    
    /**
     * Get all available target MIME types for a given source MIME type
     * 
     * @param sourceMimeType The source MIME type (e.g., "application/pdf")
     * @return List of target MIME types that the source can be converted to
     */
    public static List<String> getAvailableTargetMimeTypes(String sourceMimeType) {
        List<String> targets = new ArrayList<>();
        
        TransformerService service = TransformerService.getService();
        if (service == null) {
            return targets;
        }
        
        ConvertionContext context = service.getConvertionContext();
        FromConstraint fromConstraint = new FromConstraint(sourceMimeType);
        
        List<ConvertionEdge> edges = context.visit(fromConstraint);
        
        for (ConvertionEdge edge : edges) {
            String targetMime = edge.getTargetMimeType();
            if (!targets.contains(targetMime)) {
                targets.add(targetMime);
            }
        }
        
        return targets;
    }
    
    /**
     * Get all available transformations for a given source MIME type
     * Returns a map of target MIME type to transformation description
     * 
     * @param sourceMimeType The source MIME type
     * @return Map of target MIME types to transformation descriptions
     */
    public static Map<String, TransformationInfo> getAvailableTransformations(String sourceMimeType) {
        Map<String, TransformationInfo> transformations = new HashMap<>();
        
        TransformerService service = TransformerService.getService();
        if (service == null) {
            return transformations;
        }
        
        ConvertionContext context = service.getConvertionContext();
        FromConstraint fromConstraint = new FromConstraint(sourceMimeType);
        
        List<ConvertionEdge> edges = context.visit(fromConstraint);
        
        for (ConvertionEdge edge : edges) {
            String targetMime = edge.getTargetMimeType();
            TransformMethod method = edge.getTransformerMethodImpl();
            
            if (method != null && method.ensureServiceAvailable()) {
                TransformationInfo info = new TransformationInfo(
                    edge.getSourceMimeType(),
                    targetMime,
                    edge.getTransformerName(),
                    edge.getTransformerMethod(),
                    edge.getMethodArgs(),
                    true
                );
                transformations.put(targetMime, info);
            }
        }
        
        return transformations;
    }
    
    /**
     * Check if a transformation from one MIME type to another is available
     * 
     * @param sourceMimeType Source MIME type
     * @param targetMimeType Target MIME type
     * @return true if transformation is available
     */
    public static boolean isTransformationAvailable(String sourceMimeType, String targetMimeType) {
        TransformerService service = TransformerService.getService();
        if (service == null) {
            return false;
        }
        
        ConvertionContext context = service.getConvertionContext();
        
        HTPredicate<ConvertionEdge> constraint = new LogicalAndOperator<>(
            new FromConstraint(sourceMimeType),
            new ToConstraint(targetMimeType)
        );
        
        List<ConvertionEdge> edges = context.visit(constraint);
        
        if (ListUtil.nullOrEmpty(edges)) {
            return false;
        }
        
        // Check if at least one edge has an available method
        for (ConvertionEdge edge : edges) {
            TransformMethod method = edge.getTransformerMethodImpl();
            if (method != null && method.ensureServiceAvailable()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the conversion edge for a specific source to target transformation
     * 
     * @param sourceMimeType Source MIME type
     * @param targetMimeType Target MIME type
     * @return ConvertionEdge if available, null otherwise
     */
    public static ConvertionEdge getConversionEdge(String sourceMimeType, String targetMimeType) {
        TransformerService service = TransformerService.getService();
        if (service == null) {
            return null;
        }
        
        ConvertionContext context = service.getConvertionContext();
        
        HTPredicate<ConvertionEdge> constraint = new LogicalAndOperator<>(
            new FromConstraint(sourceMimeType),
            new ToConstraint(targetMimeType)
        );
        
        List<ConvertionEdge> edges = context.visit(constraint);
        
        if (ListUtil.nullOrEmpty(edges)) {
            return null;
        }
        
        // Return the first edge with an available method
        for (ConvertionEdge edge : edges) {
            TransformMethod method = edge.getTransformerMethodImpl();
            if (method != null && method.ensureServiceAvailable()) {
                return edge;
            }
        }
        
        return null;
    }
    
    /**
     * Information about an available transformation
     */
    public static class TransformationInfo {
        private final String sourceMimeType;
        private final String targetMimeType;
        private final String transformerName;
        private final String methodName;
        private final String methodArgs;
        private final boolean available;
        
        public TransformationInfo(String sourceMimeType, String targetMimeType, 
                                String transformerName, String methodName, String methodArgs, boolean available) {
            this.sourceMimeType = sourceMimeType;
            this.targetMimeType = targetMimeType;
            this.transformerName = transformerName;
            this.methodName = methodName;
            this.methodArgs = methodArgs;
            this.available = available;
        }
        
        public String getSourceMimeType() { return sourceMimeType; }
        public String getTargetMimeType() { return targetMimeType; }
        public String getTransformerName() { return transformerName; }
        public String getMethodName() { return methodName; }
        public String getMethodArgs() { return methodArgs; }
        public boolean isAvailable() { return available; }
    }
}
