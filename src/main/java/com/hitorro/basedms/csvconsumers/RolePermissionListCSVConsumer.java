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
package com.hitorro.basedms.csvconsumers;

import com.hitorro.base.objects.Permission;
import com.hitorro.base.objects.Role;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;

import java.util.Set;

/**
 * Load Roles from a CSV file.
 */
public class RolePermissionListCSVConsumer extends CSVHibernateLoaderConsumer<Role> {
    public static final String RoleNameColumn = "rolename";
    public static final String PermissionNameColumn = "permissionname";
    private static final String[][] Keys = {{RoleNameColumn, "name"}};

    public void start() {
    }

    public Class getPersistingClass() {
        return Role.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, Role role, boolean existsAlready) {
        String roleName = MapUtil.getColumnFromColumMap(RoleNameColumn, m_headerMap, row);
        String permissionName = MapUtil.getColumnFromColumMap(PermissionNameColumn, m_headerMap, row);

        role.setName(roleName);
        Permission perm = Permission.getPermissionForName(getSession(), permissionName);
        if (perm != null) {
            Set<Permission> perms = role.getPermissions();
            perms.add(perm);
        }

        this.saveOrUpdate(existsAlready, role);
        return true;
    }

    public void done() {
    }


}
