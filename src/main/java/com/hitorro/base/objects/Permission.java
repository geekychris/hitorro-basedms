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
package com.hitorro.base.objects;

import com.hitorro.base.typesystem.accessors.GuidAccessor;
import com.hitorro.util.auth.PermissionInterface;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import jakarta.persistence.*;

import java.io.IOException;

/**
 * User permission. A permission is a representation of the ability to do a specific bit of functionality.
 */
@Entity
@Table(name = "Permission")
@PrimaryKeyJoinColumn(name = "system_id")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Permission,
        isView = false,
        isPersisted = true,
        schemaVersion = Permission.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class)
@UiTypeProperties(name = "Permission", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = Permission.PermissionListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = Permission.PermissionEditView.class)}
)
public class Permission extends VersionableObject implements PermissionInterface {

    public static final int SerializationVersion = 1;
    // todo chris - should use a more formal cache
    // our globally cached admin permission
    private static Permission s_AdminPermission;
    
    @Column(name = "name", length = 20, nullable = false)
    @org.hibernate.annotations.Index(name = "name_idx")
    private String name;
    
    @Column(name = "description", length = 255)
    private String description;

    public Permission() {

    }

    /**
     * Fetch a Permission by its name.
     *
     * @param session the dms session
     * @param name    the userName of the user to fetch
     * @return the Permission, which will be null if not found
     */
    public static Permission getPermissionForName(BaseSession session, String name) {
        return (Permission) session.getObject(Permission.class, "where name = :a", name);
    }

    /**
     * Get the special "admin" permission.
     *
     * @param session the dms session
     * @return a copy of the admin permission (not necessarily fetched from the database)
     */
    public static Permission getAdminPermission(BaseSession session) {
        if (s_AdminPermission == null) {
            s_AdminPermission = getPermissionForName(session, "administer");
        }
        return s_AdminPermission;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Permission other = (Permission) orig;
        name = other.name;
        description = other.description;
    }

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getName() {
        return name;
    }

    public void setName(String val) {
        this.name = val;
    }

    @UiProperties(displayName = "Description", displayType = UiProperties.TextFieldDisplay, order = 20)
    public String getDescription() {
        return description;
    }

    public void setDescription(String val) {
        this.description = val;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(name);
        os.writeString(description);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                name = os.readString();
                description = os.readString();
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "PermissionListView",
            isView = true,
            isPersisted = false,
            schemaVersion = Permission.SerializationVersion)
    public abstract static class PermissionListView {
        public abstract String getName();

        public abstract String getDescription();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "PermissionEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = Permission.SerializationVersion)
    public abstract static class PermissionEditView {
        public abstract String getName();

        public abstract String getDescription();

        public abstract int getState();
    }
}
