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
package com.hitorro.basedms.db;

import com.hitorro.base.typesystem.listeners.HTHibernateDeleteListener;
import com.hitorro.base.typesystem.listeners.HTHibernateLoadListener;
import com.hitorro.base.typesystem.listeners.HTHibernateOnSaveListener;
import com.hitorro.base.typesystem.listeners.HTHibernatePersistListener;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Created by chris on 2/28/16.
 */
public class HTIntegrator implements Integrator, Service {

    private static final DuplicationStrategy HT_DUPLICATION_STRATEGY = new DuplicationStrategy() {
        @Override
        public boolean areMatch(Object listener, Object original) {
            return listener.getClass().equals(original.getClass());
            //&& HTIntegrator.class.isInstance( original );
        }

        @Override
        public Action getAction() {
            return Action.KEEP_ORIGINAL;
        }
    };

    @Override
    public void integrate(final Metadata metadata,
                          final SessionFactoryImplementor sessionFactory,
                          final SessionFactoryServiceRegistry serviceRegistry) {
        // XXX TODO ???? debugging added this line back in on 28th aug 2007
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);
        eventListenerRegistry.addDuplicationStrategy(HT_DUPLICATION_STRATEGY);

        SaveOrUpdateEventListener[] stack = {new HTHibernateOnSaveListener()};
        eventListenerRegistry.prependListeners(EventType.SAVE_UPDATE, stack);

        SaveOrUpdateEventListener[] s2 = {new HTHibernateOnSaveListener()};
        eventListenerRegistry.prependListeners(EventType.SAVE, s2);

        PersistEventListener[] persiststack = {new HTHibernatePersistListener()};
        eventListenerRegistry.prependListeners(EventType.PERSIST, persiststack);

        DeleteEventListener[] deleteStack = {new HTHibernateDeleteListener()};
        eventListenerRegistry.prependListeners(EventType.DELETE, deleteStack);

        PostLoadEventListener[] loadStack = {new HTHibernateLoadListener()};
        eventListenerRegistry.prependListeners(EventType.POST_LOAD, loadStack);
    }

    @Override
    public void disintegrate(final SessionFactoryImplementor sessionFactory, final SessionFactoryServiceRegistry serviceRegistry) {

    }
}