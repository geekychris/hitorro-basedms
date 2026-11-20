package com.hitorro.basedms.session;

import com.hitorro.base.objects.Container;
import com.hitorro.base.objects.Content;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.db.HTIntegrator;
import com.hitorro.basedms.db.HibernateUtil;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.lang.reflect.Field;

/**
 * SessionFactories for the Hibernate tests.
 * <p>
 * The H2 (in memory) factory is registered as HibernateUtil's default factory, which DMSSession
 * uses. HibernateUtil keeps its default for the life of the JVM, so every test class shares it.
 * <p>
 * The database matrix runs the same tests against PostgreSQL and MySQL in containers:
 * {@link #open(JdbcDatabaseContainer, Class[])} builds a factory on the container and
 * {@link #useAsDefault(SessionFactory)} points HibernateUtil's default at it for the duration of
 * a test class, so DMSSession and every code path behind it run on that database.
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
        Configuration cfg = configuration("jdbc:h2:mem:basedms-hibernate7;DB_CLOSE_DELAY=-1", "sa", "", ENTITIES);
        HibernateUtil.setSessionFactory(KEY, cfg, true);
        sf = HibernateUtil.getSessionFactory(KEY);
        if (sf != HibernateUtil.getSessionFactory()) {
            throw new IllegalStateException("another default SessionFactory is already registered");
        }
        return sf;
    }

    /**
     * A Configuration as HibernateService builds one: HTIntegrator, the given entities, unquoted
     * identifiers (Hibernate's default). Every test database starts empty, so the schema is created
     * without a drop first (create-only: MySQL cannot drop foreign keys of tables that do not exist
     * yet), and creation halts on the first DDL error, which Hibernate otherwise only logs (an
     * index that fails to create would go unnoticed).
     */
    public static Configuration configuration(String url, String user, String password, Class<?>... entities) {
        Configuration cfg = new Configuration(
                new BootstrapServiceRegistryBuilder().applyIntegrator(new HTIntegrator()).build());
        cfg.setProperty("hibernate.connection.url", url);
        cfg.setProperty("hibernate.connection.username", user);
        cfg.setProperty("hibernate.connection.password", password);
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-only");
        cfg.setProperty("hibernate.hbm2ddl.halt_on_error", "true");
        for (Class<?> c : entities) {
            cfg.addAnnotatedClass(c);
        }
        return cfg;
    }

    /** A new SessionFactory on a (started) database container; the caller closes it. */
    public static SessionFactory open(JdbcDatabaseContainer<?> db, Class<?>... entities) {
        Configuration cfg = configuration(db.getJdbcUrl(), db.getUsername(), db.getPassword(),
                entities.length == 0 ? ENTITIES : entities);
        return cfg.buildSessionFactory();
    }

    /**
     * Make sf HibernateUtil's default factory (what DMSSession opens sessions on) and return the
     * previous default. Test classes run one at a time, so a class may swap the default in
     * {@code @BeforeAll} and restore it in {@code @AfterAll}.
     */
    public static synchronized SessionFactory useAsDefault(SessionFactory sf) {
        try {
            Field f = HibernateUtil.class.getDeclaredField("s_session");
            f.setAccessible(true);
            SessionFactory previous = (SessionFactory) f.get(null);
            f.set(null, sf);
            return previous;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot set HibernateUtil's default SessionFactory", e);
        }
    }

    /** A DMSSession on the default factory (the constructor is package-private). */
    public static DMSSession newSession(boolean cache) {
        if (HibernateUtil.getSessionFactory() == null) {
            get();
        }
        return new DMSSession(cache);
    }
}
