package com.hitorro.basedms.session;

import com.hitorro.base.typesystem.GuidBaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Test entity for the Hibernate 7 session tests. It counts how often HiTorro's save/persist
 * callbacks reach it: the listeners call GuidBaseType.ensureGuidBeforePersist reflectively,
 * which dispatches to this override. The count is a column, so a callback that ran on the
 * caller's instance before a merge copied its state shows up in the database.
 */
@Entity
@Table(name = "probe_entity")
public class ProbeEntity extends GuidBaseType {

    @Column(name = "name")
    private String name;

    @Column(name = "callbackCount")
    private int callbackCount;

    public ProbeEntity() {
    }

    public ProbeEntity(String guid, String name) {
        this.guid = guid;
        this.name = name;
    }

    @Override
    public int getSerializationVersion() {
        return 1;
    }

    @Override
    protected void ensureGuidBeforePersist() {
        callbackCount++;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCallbackCount() {
        return callbackCount;
    }
}
