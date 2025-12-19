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
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * User role. A role is a set of Permissions, which together describe the capabilities of a User. By convention, a role
 * names starts with a capital letter.
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Role,
        isView = false,
        isPersisted = true,
        schemaVersion = Role.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class)
@UiTypeProperties(name = "Role", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = Role.RoleListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = Role.RoleEditView.class)}
)
public class Role extends VersionableObject {
    public static final int SerializationVersion = 1;
    private String name;
    private String description;
    private Set<Permission> permissions;

    public Role() {
        permissions = new HashSet<Permission>();
    }

    /**
     * Fetch a Role by its name.
     *
     * @param session the dms session
     * @param name    the userName of the user to fetch
     * @return the Role, which will be null if not found
     */
    public static Role getRoleForName(BaseSession session, String name) {
        return (Role) session.getObject(Role.class, "where name = :a", name);
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Role other = (Role) orig;
        name = other.name;
        description = other.description;

        // permissions
        HashSet<Permission> temp = new HashSet<Permission>();
        // have to copy this way because of hibernate
        for (Permission obj : other.permissions) {
            temp.add(obj);
        }
        permissions = temp;
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

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> val) {
        permissions = val;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(name);
        os.writeString(description);
        os.writeSetOfBaseType(permissions);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                name = os.readString();
                description = os.readString();
                os.readSetOfHTSerializable(permissions);
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "RoleListView",
            isView = true,
            isPersisted = false,
            schemaVersion = Role.SerializationVersion)
    public abstract static class RoleListView {
        public abstract String getName();

        public abstract String getDescription();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "RoleEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = Role.SerializationVersion)
    public abstract static class RoleEditView {
        public abstract String getName();

        public abstract String getDescription();

        public abstract int getState();
    }

}
