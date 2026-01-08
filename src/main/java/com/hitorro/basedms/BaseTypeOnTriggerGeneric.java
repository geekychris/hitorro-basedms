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

import com.hitorro.base.objects.BaseDMSService;
import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.util.typesystem.OnTrigger;
import com.hitorro.util.typesystem.TypeIntf;

/**
 */
public class BaseTypeOnTriggerGeneric implements OnTrigger {
    public boolean execute(OnTrigger.TriggerType key, TypeIntf type, Object bt) {
        if (!BaseDMSService.s_initialized) {
            return false;
        }

        if (bt instanceof GuidBaseType) {
            GuidBaseType so = (GuidBaseType) bt;

            if (key == OnTrigger.TriggerType.BeforePersist) {
                if (so.getGuid() == null) {
                    so.setGuid(GuidBaseType.computeGuid(type));
                }

            }
        }
        return true;
    }

    public String getName() {
        return "SysObject OnTrigger (VersionableObject builtin)";
    }
}