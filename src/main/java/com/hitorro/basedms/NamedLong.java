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

import com.hitorro.base.objects.NamedLongEntry;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;
import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedLong {
    private static final Map<String, NamedLong> s_namedLongs = new HashMap<String, NamedLong>();
    private int retries = 10;
    private String name;
    private long initialValue;
    private long incrementor;
    private long currentValue;
    private long capValue;
    private String description;
    private NamedLongEntry entry = null;
    private boolean isInitialized = false;

    protected NamedLong(String name, long initialValue, long incrementor, String description) {
        this.name = name;
        this.initialValue = initialValue;
        this.incrementor = incrementor;
        this.description = description;
    }

    public static NamedLong getNamedLong(String name) {
        return s_namedLongs.get(name);
    }

    public static List<NamedLong> getNamedLongs() {
        List<NamedLong> list = new ArrayList<NamedLong>();
        list.addAll(s_namedLongs.values());
        return list;
    }

    public static NamedLong registerNamedLong(String name, long initialValue, long incrementor, String description) {
        NamedLong nl = new NamedLong(name, initialValue, incrementor, description);
        s_namedLongs.put(name, nl);
        return nl;
    }

    public void delete() {
        BaseSession session = BaseSessionFactory.getFactory().getSession();
        try {
            Object o = session.getSingleObject(NamedLongEntry.class, Fmt.S("where name='%s'", name));
            if (o != null) {
                session.delete(o);
                session.commit();
            } else {
                session.rollback();
            }

        } finally {
            DMSSessionFactory.closeSession(session);
        }
    }


    private void init() {
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        try {
            entry = retrieve(session);
            if (entry == null) {
                entry = this.createNewNamedLongEntry();
                if (entry != null) {
                    currentValue = initialValue;
                    capValue = currentValue;
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackCloseSession(session);
        }
    }

    public synchronized void moveMaxTo(long newMax) {

        if (capValue < newMax) {
            long v = newMax / incrementor;
            newMax = incrementor * v;
            //moveCounterForward(newMax);
        }

    }


    public synchronized long getNextValue() {
        if (!isInitialized) {
            init();
            isInitialized = true;
        }

        if (currentValue < capValue) {
            long returnMe = currentValue;
            currentValue++;
            return returnMe;
        } else {
            incrementCounter();
            long returnMe = currentValue;
            currentValue++;
            return returnMe;

        }
    }


    private void incrementCounter() {
        boolean result = incrementCounterAux();
        for (int i = 0; i < retries; i++) {
            if (result == true) {
                break;
            }
            // we have to re-retrieve the counter since it was out of date
            result = incrementCounterAux();
        }
        if (!result) {
            Log.basedms.error("Unable to increment NamedLongEntry %s", name);
        }
    }


    private boolean incrementCounterAux() {
        // update our counters

        //StaleObjectStateException
        BaseSession session = DMSSessionFactory.getFactory().getSession();
        boolean blownUp = false;
        try {
            Log.basedms.debug("NamedLong counter: %s, current value: %s, next value: %s. enter incrementCounterAux", name, currentValue, capValue);
            entry = retrieve(session);
            this.currentValue = entry.getValue();
            this.capValue = currentValue + entry.getIncrementor();
            this.entry.setValue(capValue);
            session.saveOrUpdate(entry);
            DMSSessionFactory.getFactory().commitAndCloseSession(session);
            Log.basedms.debug("NamedLong counter: %s, current value: %s, next value: %s. exit incrementCounterAux", name, currentValue, capValue);

            return true;
        } catch (StaleStateException staleException) {
            Log.basedms.debug("NamedLong counter: %s, current value: %s, next value: %s. StaleStateException %s %e",
                    name, currentValue, capValue, staleException, staleException);
            blownUp = true;
            return false;
        } finally {
            if (blownUp) {
                Log.basedms.debug("NamedLong counter: %s, current value: %s, next value: %s. enter finally block's rollback section", name, currentValue, capValue);
                DMSSessionFactory.getFactory().rollbackCloseSession(session);
            }
        }
    }


    private NamedLongEntry retrieve(BaseSession session) {
        try {
            NamedLongEntry entry = (NamedLongEntry) session.getSingleObject(NamedLongEntry.class, Fmt.S(" where name='%s'", name));
            if (entry == null) {
                return null;
            }
            return entry;
        } catch (HibernateException e) {
            Log.basedms.error("Unable to retrieve NamedLongEntry %s", name);
        }

        return null;
    }


    private NamedLongEntry createNewNamedLongEntry() {
        BaseSession session = BaseSessionFactory.getFactory().getSession();
        try {
            NamedLongEntry entry = new NamedLongEntry();
            entry.setDescription(this.description);
            entry.setIncrementor(this.incrementor);
            entry.setValue(this.initialValue);
            entry.setName(name);
            session.saveOrUpdate(entry);
            session.commit();
            return entry;
        } catch (HibernateException e) {
            Log.basedms.error("Unable to create NamedLongEntry %s %s %e", name, e, e);
        } finally {
            DMSSessionFactory.closeSession(session);
        }
        return null;
    }
}
