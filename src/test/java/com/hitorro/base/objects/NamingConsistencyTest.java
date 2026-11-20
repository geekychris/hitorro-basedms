package com.hitorro.base.objects;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * A Spring Boot app can run two SessionFactories on one database: Spring's (spring.jpa.*) and
 * basedms's own HibernateService, which only sees the properties DatabaseConfigProvider gives it
 * (Hibernate's defaults for naming and quoting). The example apps set Hibernate's standard physical
 * naming and the legacy-JPA implicit naming; this checks that those give basedms's entities the
 * same tables and columns as Hibernate's defaults, so the two factories agree as long as identifier
 * quoting is left off on both.
 */
class NamingConsistencyTest {

    private static TreeSet<String> schema(Map<String, Object> settings) {
        StandardServiceRegistryBuilder b = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false");
        settings.forEach(b::applySetting);
        StandardServiceRegistry registry = b.build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            for (Class<?> c : DmsSchemaTest.ALL_ENTITIES) {
                sources.addAnnotatedClass(c);
            }
            Metadata md = sources.buildMetadata();
            TreeSet<String> names = new TreeSet<>();
            for (var ns : md.getDatabase().getNamespaces()) {
                for (Table t : ns.getTables()) {
                    names.add(t.getQuotedName());
                    for (Column c : t.getColumns()) {
                        names.add(t.getQuotedName() + "." + c.getQuotedName());
                    }
                }
            }
            return names;
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void springBootNamingSettingsGiveTheSameSchemaAsHibernateDefaults() {
        TreeSet<String> hibernateService = schema(Map.of());
        TreeSet<String> spring = schema(Map.of(
                "hibernate.physical_naming_strategy", PhysicalNamingStrategyStandardImpl.class.getName(),
                "hibernate.implicit_naming_strategy", ImplicitNamingStrategyLegacyJpaImpl.class.getName()));
        assertEquals(hibernateService, spring);
    }

    /** Spring Boot's own default (camelCase to snake_case) does not match: apps must set the standard strategy. */
    @Test
    void springBootDefaultNamingDiffersFromHibernateDefaults() {
        TreeSet<String> spring = schema(Map.of("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
        assertNotEquals(schema(Map.of()), spring);
    }
}
