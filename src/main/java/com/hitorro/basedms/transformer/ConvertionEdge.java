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

 * Defines end points of a transformation.
 */
public class ConvertionEdge {
    private String m_sourceMimeType;
    private String m_targetMimeType;

    private String m_transformerName;
    private String m_tranformerMethod;
    private String m_methodArgs;

    public String getSourceMimeType() {
        return m_sourceMimeType;
    }

    public void setSourceMimeType(String mimeType) {
        m_sourceMimeType = mimeType;
    }

    public String getTargetMimeType() {
        return m_targetMimeType;
    }

    public void setTargetMimeType(String mimeType) {
        m_targetMimeType = mimeType;
    }

    public String getTransformerName() {
        return m_transformerName;
    }

    public void setTransformerName(String transformer) {
        m_transformerName = transformer;
    }

    public String getTransformerMethod() {
        return m_tranformerMethod;
    }

    public void setTransformerMethod(String method) {
        m_tranformerMethod = method;
    }

    public String getMethodArgs() {
        return m_methodArgs;
    }

    public void setMethodArgs(String args) {
        m_methodArgs = args;
    }

    public TransformMethod getTransformerMethodImpl() {
        return TransformerService.getService().getMethod(getTransformerMethod());
    }
}
