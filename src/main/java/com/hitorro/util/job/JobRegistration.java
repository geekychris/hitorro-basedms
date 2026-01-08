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
package com.hitorro.util.job;

import com.hitorro.util.core.classes.ClassUtil;


public class JobRegistration {
    public String _name;
    public String _displayName;
    public String _viewName;
    public String _jobClassString;
    public Class _jobClass;
    public Class _parameterClass;

    public JobRegistration(Class jobClass, String displayName, Class parameterClass, String viewName) {
        _jobClass = jobClass;
        _displayName = displayName;
        _parameterClass = parameterClass;
        _viewName = viewName;
        Job aj = getAppJob();
        if (aj != null) {
            _name = aj.getName().toLowerCase();
        }
        if (jobClass != null) {
            _jobClassString = jobClass.getCanonicalName();
        }
    }

    public Job getAppJob() {
        return (Job) ClassUtil.getInstanceSwallowError(_jobClass, Job.class);
    }
}