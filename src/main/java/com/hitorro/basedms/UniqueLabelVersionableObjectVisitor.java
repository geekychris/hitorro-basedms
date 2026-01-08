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
package com.hitorro.basedms;

import com.hitorro.base.objects.VersionableObject;

/**
 * <p/>
 * Visit a system object version tree and ensure that one and only one version can have a specific "domain","value"
 * pair.  This is used by domains that have a unique across versions flag set to true.
 */
public class UniqueLabelVersionableObjectVisitor implements VersionableObjectVisitor {
    private VersionableObject m_me;
    private String m_domain;
    private String m_value;

    public UniqueLabelVersionableObjectVisitor(VersionableObject me, String domain, String value) {
        m_me = me;

        m_domain = domain;
        m_value = value;
    }

    public boolean visit(VersionableObject visit) {
        if (visit.getGuid().equals(m_me.getGuid())) {
            // do nothing this is the version that just took on the label
        }
        return !visit.removeCategory(m_domain, m_value);
    }
}
