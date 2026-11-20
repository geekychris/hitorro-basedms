package com.hitorro.basedms.session;

import com.hitorro.base.objects.Container;
import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.db.HTIntegrator;
import com.hitorro.basedms.db.HibernateUtil;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/**
 * One H2 (in memory) SessionFactory for the Hibernate tests, registered as HibernateUtil's
 * default factory, which DMSSession uses. HibernateUtil keeps its default for the life of the
 * JVM, so every test class shares this one.
 */
public final class TestSessionFactory {

    static final String KEY = "hibernate7-test";
    static final Class<?>[] ENTITIES = {
            ProbeEntity.class, VersionableObject.class, Container.class, Content.class,
    };

    private TestSessionFactory() {
    }

    public static synchronized SessionFactory get() {
        SessionFactory sf = HibernateUtil.getSessionFactory(KEY);
        if (sf != null) {
            return sf;
        }
        Configuration cfg = new Configuration(
                new BootstrapServiceRegistryBuilder().applyIntegrator(new HTIntegrator()).build());
        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:basedms-hibernate7;DB_CLOSE_DELAY=-1");
        cfg.setProperty("hibernate.connection.username", "sa");
        cfg.setProperty("hibernate.connection.password", "");
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        for (Class<?> c : ENTITIES) {
            cfg.addAnnotatedClass(c);
        }
        HibernateUtil.setSessionFactory(KEY, cfg, true);
        sf = HibernateUtil.getSessionFactory(KEY);
        if (sf != HibernateUtil.getSessionFactory()) {
            throw new IllegalStateException("another default SessionFactory is already registered");
        }
        return sf;
    }

    /** A DMSSession on the test factory (the constructor is package-private). */
    public static DMSSession newSession(boolean cache) {
        get();
        return new DMSSession(cache);
    }
}
