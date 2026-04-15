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


import com.hitorro.util.core.Log;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HibernateUtil {
    private static Object s_lockObject = new Object();
    private static SessionFactory s_session;
    private static Map<String, SessionFactory> s_map = new HashMap<String, SessionFactory>();

    public static void setSessionFactory(String key, Configuration conf, boolean isDefault) {
        // Skip building if already set (e.g., by Spring Boot's DMSAutoConfiguration bridge)
        if (isDefault && s_session != null) {
            Log.util.info("SessionFactory already set (Spring Boot bridge) - skipping native build for key: %s", key);
            return;
        }
        if (s_map.containsKey(key)) {
            Log.util.info("SessionFactory already set for key: %s - skipping native build", key);
            return;
        }
        try {
            SessionFactory sf = conf.buildSessionFactory();
            s_map.put(key, sf);
            if (isDefault) {
                s_session = sf;
            }
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            Log.util.error("Initial SessionFactory creation failed. %s %e", ex, ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Statistics getStatistics() {
        return s_session.getStatistics();
    }

    public static SessionFactory getSessionFactory() {
        return s_session;
    }

    public static SessionFactory getSessionFactory(String key) {
        return s_map.get(key);
    }

    public static void setUsername(Properties props, String username, String password) {
        props.put("hibernate.connection.username", username);
        props.put("hibernate.connection.password", password);
    }

    public static void setCreateMode(Properties props, String createMode) {
        props.put("hibernate.hbm2ddl.auto", createMode);
    }

    public static void setUrl(Properties props, String url) {
        props.put("hibernate.connection.url", url);
    }

    public static Session getNonThreadBasedSession(Interceptor intercepter) {
        return getSessionFactory().withOptions().interceptor(intercepter).openSession();
    }

    public static Session getNonThreadBasedSession() {
        return getSessionFactory().openSession();
    }

    public static Session getNonThreadBasedSession(String key) {
        SessionFactory sf = getSessionFactory(key);
        if (sf == null) {
            return null;
        }
        return sf.openSession();
    }

    public static Session getNonThreadBasedSession(String key, Interceptor intercepter) {
        SessionFactory sf = getSessionFactory(key);

        if (sf == null) {
            return null;
        }
        return sf.withOptions().interceptor(intercepter).openSession();
    }
}
