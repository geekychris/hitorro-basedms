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

import com.hitorro.base.objects.User;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.map.MapUtil;

/**
 * For reading a CSV file that will populate user objects.
 */
public class UserCSVConsumer extends CSVHibernateLoaderConsumer<User> {
    public static final String UserNameColumn = "username";
    public static final String DisplayNameColumn = "displayname";
    public static final String PasswordColumn = "password";
    public static final String EMailAddressColumn = "emailaddress";
    public static final String ReamlColumn = "realm";
    private static final String[][] Keys = {{"username", "name"}};

    public void start() {
    }

    public Class getPersistingClass() {
        return User.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, User user, boolean existsAlready) {
        String userName = MapUtil.getColumnFromColumMap(UserNameColumn, m_headerMap, row);
        String displayName = MapUtil.getColumnFromColumMap(DisplayNameColumn, m_headerMap, row);
        String password = MapUtil.getColumnFromColumMap(PasswordColumn, m_headerMap, row);
        String email = MapUtil.getColumnFromColumMap(EMailAddressColumn, m_headerMap, row);
        String realm = MapUtil.getColumnFromColumMap(ReamlColumn, m_headerMap, row);


        user.setName(userName);
        user.setDisplayName(displayName);
        user.setEmailAddress(email);
        user.setAdapterSource(adapterSource);
        user.setRealm(realm);
        // the existence of a password indicates that the user can log in - but isn't actually a password yet
        // todo chris - need to make passwords real
        if (password != null && password.length() > 0) {
            user.setPassword(password);
        } else {
            user.setPassword(null);
        }
        this.saveOrUpdate(existsAlready, user);
        return true;
    }

    public void done() {
    }

}
