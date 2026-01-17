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
package com.hitorro.base.typesystem.listeners;

import com.hitorro.base.typesystem.GuidBaseType;
import com.hitorro.util.typesystem.OnTrigger;
import com.hitorro.util.typesystem.TypeManager;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;

import java.lang.reflect.Method;

/**
 */
public class HTHibernateOnSaveListener implements SaveOrUpdateEventListener {
    public void onSaveOrUpdate(SaveOrUpdateEvent saveOrUpdateEvent) throws HibernateException {
        Object o = saveOrUpdateEvent.getObject();
        
        // Manually trigger @PrePersist methods since DMSSession bypasses JPA
        invokePrePersistCallbacks(o);
        
        TypeManager.executeTrigger(OnTrigger.TriggerType.BeforeSave, o, true);
    }
    
    /**
     * Manually invoke @PrePersist callbacks since Hibernate Session API doesn't trigger them.
     * This ensures GUID initialization and other pre-persist logic runs correctly.
     */
    private void invokePrePersistCallbacks(Object entity) {
        if (entity == null) return;
        
        // For GuidBaseType, call ensureGuidBeforePersist() directly
        if (entity instanceof GuidBaseType) {
            try {
                Method method = GuidBaseType.class.getDeclaredMethod("ensureGuidBeforePersist");
                method.setAccessible(true);
                method.invoke(entity);
            } catch (Exception e) {
                // Ignore - guid will be generated via getter if needed
            }
        }
    }
}