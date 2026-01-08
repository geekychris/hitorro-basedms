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
package com.hitorro.basedms.workflow.constraints;

import com.fasterxml.jackson.databind.JsonNode;
import com.hitorro.basedms.workflow.OutstandingWorkflowItem;
import com.hitorro.util.core.HTAssert;
import com.hitorro.util.core.opers.HTPredicate;


public class OutstandingWorkFlowItemTypeConstraint implements HTPredicate<OutstandingWorkflowItem> {
    private boolean m_ignoreCase;
    private String m_term;

    public OutstandingWorkFlowItemTypeConstraint(String term, boolean ignoreCase) {
        m_ignoreCase = ignoreCase;
        m_term = term;
        if (m_ignoreCase) {
            m_term = m_term.toLowerCase();
        }
    }

    public boolean initFromMap(final JsonNode map) {
        // MUST IMPLEMENT
        HTAssert.assertThat(false, "OutstandingWorkFlowItemTypeConstraint.initFromMap not implemented");
        return false;
    }

    public void initForPass() {

    }

    public boolean test(OutstandingWorkflowItem in) {
        String s = in.getItemType();
        if (m_ignoreCase) {
            // this is expensive!
            s = s.toLowerCase();

        }
        return s.indexOf(m_term) != -1;
    }
}
