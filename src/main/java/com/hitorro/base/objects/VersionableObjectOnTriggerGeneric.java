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
package com.hitorro.base.objects;

import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.basedms.StoreUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.OnTrigger;
import com.hitorro.util.typesystem.TypeIntf;

import java.util.Date;

/**
 */
public class VersionableObjectOnTriggerGeneric implements OnTrigger {
    public boolean execute(OnTrigger.TriggerType key, TypeIntf type, Object bt) {
        if (!BaseDMSService.s_initialized) {
            return false;
        }

        if (bt instanceof VersionableObject) {
            VersionableObject so = (VersionableObject) bt;
            if (key == OnTrigger.TriggerType.BeforePersist) {
                so.setModifiedDate(new Date());
                if (so.getGuid() == null) {
                    so.setGuid(GuidBaseType.computeGuid(type));
                }
                if (so.getCanonicalGuid() == null) {
                    // Since we are on a before persist, newing up a branch or a major/minor will of set
                    // the canonical guid before we get to fire this trigger.
                    so.setCanonicalGuid(so.getGuid());
                    so.setCanonical(so);
                    if (StringUtil.nullOrEmptyOrBlankString(so.getIndexName())) {
                        Store store = StoreUtil.getDefaultStore();
                        if (store != null) {
                            so.setIndexName(store.getName());
                        }
                    }
                }
            }
            /*else if (key == OnTrigger.TriggerType.BeforeSave)
            {
                so.setModifiedDate(new Date());
            }*/
        }
        return true;
    }

    public String getName() {
        return "SysObject OnTrigger (VersionableObject builtin)";
    }
}
