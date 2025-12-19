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

import com.hitorro.util.typesystem.OnTrigger;
import com.hitorro.util.typesystem.TypeManager;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;

import java.util.Set;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 17, 2006 Time: 8:10:14 AM
 */
public class HTHibernateDeleteListener implements DeleteEventListener {
    public void onDelete(DeleteEvent deleteEvent) throws HibernateException {
        Object o = deleteEvent.getObject();
        TypeManager.executeTrigger(OnTrigger.TriggerType.BeforeDelete, o, true);
    }

    @Override
    public void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException {
        Object o = event.getObject();
        TypeManager.executeTrigger(OnTrigger.TriggerType.BeforeDelete, o, true);
    }

    public void onDelete(DeleteEvent deleteEvent, Set set) throws HibernateException {
        Object o = deleteEvent.getObject();
        TypeManager.executeTrigger(OnTrigger.TriggerType.BeforeDelete, o, true);
    }
}
