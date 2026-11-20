package com.hitorro.base.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Container queries without a select clause get an explicit result type on Hibernate 7. */
class ContainerQueryTest {

    @Test
    void detectsSelectClause() {
        assertFalse(Container.hasSelectClause("from com.hitorro.base.objects.VersionableObject s join s.containers c where c.id = :id"));
        assertFalse(Container.hasSelectClause("  FROM Document d"));
        assertTrue(Container.hasSelectClause("select s from VersionableObject s join s.containers c"));
        assertTrue(Container.hasSelectClause("  SELECT s, c from VersionableObject s join s.containers c"));
        assertFalse(Container.hasSelectClause(null));
    }
}
