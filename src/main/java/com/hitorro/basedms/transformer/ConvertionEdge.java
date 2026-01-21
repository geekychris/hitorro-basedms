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

/**
 * 
 * Defines end points of a transformation.
 */
public class ConvertionEdge {
    private String sourceMimeType;
    private String targetMimeType;

    private String transformerName;
    private String transformerClass;
    private String tranformerMethod;
    private String methodArgs;

    public String getSourceMimeType() {
        return sourceMimeType;
    }

    public void setSourceMimeType(String mimeType) {
        sourceMimeType = mimeType;
    }

    public String getTargetMimeType() {
        return targetMimeType;
    }

    public void setTargetMimeType(String mimeType) {
        targetMimeType = mimeType;
    }

    public String getTransformerName() {
        return transformerName;
    }

    public void setTransformerName(String transformer) {
        transformerName = transformer;
    }

    public String getTransformerClass() {
        return transformerClass;
    }

    public void setTransformerClass(String transformerClass) {
        this.transformerClass = transformerClass;
    }

    public String getTransformerMethod() {
        return tranformerMethod;
    }

    public void setTransformerMethod(String method) {
        tranformerMethod = method;
    }

    public String getMethodArgs() {
        return methodArgs;
    }

    public void setMethodArgs(String args) {
        this.methodArgs = args;
    }

    public TransformMethod getTransformerMethodImpl() {
        return TransformerService.getService().getMethod(getTransformerMethod());
    }
}
