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
package com.hitorro.base.typesystem.accessors;

import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.classes.ClassUtil;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import org.hibernate.query.Query;

import java.util.List;

/**
 * <p/>
 * Class to allow the retrieval of an object via guid and to generate a guid. The standard globally identifyable object
 * should use the standard:
 * <p/>
 * typecode:instanceid:id
 * <p/>
 * format, however there are some objects we want "soft links".  For instance: store:fs_store1
 */
public class GuidAccessor implements com.hitorro.util.typesystem.Accessor {
    protected static final String SysForIdQuery = "from %s e where e.%s =:id";

    protected static final String SysForIdRealmQuery = "from %s e where e.%s =:id AND (e.realm =:realm OR e.realm is null)";
    protected com.hitorro.util.typesystem.TypeManager manager;
    protected com.hitorro.util.typesystem.Type type;
    protected String softRefField[];
    protected String query;
    protected String queryRealm;
    protected String softQuery;
    protected String softQueryRealm;

    public void init(com.hitorro.util.typesystem.TypeManager manager, com.hitorro.util.typesystem.Type type, String softRefField[]) {
        this.manager = manager;
        this.type = type;
        this.softRefField = softRefField;
        initAux();
    }

    public String[] softGuidKeyParts() {
        return softRefField;
    }


    /**
     * sub class for such things as query initialization
     */
    public void initAux() {
        // Use simple class name for Hibernate 6 entity name resolution
        String entityName = type.getImplementationClass().getSimpleName();
        query = Fmt.S(SysForIdQuery, entityName, "guid");
        queryRealm = Fmt.S(SysForIdRealmQuery, entityName, "guid");


        StringBuilder b = new StringBuilder();
        b.append("from ");
        b.append(entityName);
        b.append(" e where");
        boolean notFirst = false;
        for (String f : softRefField) {
            if (notFirst) {
                b.append(" AND ");
            } else {
                notFirst = true;
                b.append(" ");
            }

            b.append("e.");
            b.append(f);
            b.append(" = :");
            b.append(f);
        }

        softQuery = b.toString();

        if (isVersionable(type)) {
            b.append(" AND (e.realm =:realm OR e.realm is null)");
            softQueryRealm = b.toString();
        }


    }

    public boolean isVersionable(com.hitorro.util.typesystem.Type t) {
        if (t.getIsVersionable() == 0) {

            Object o = ClassUtil.getInstanceSwallowError(t.getImplementationClass(), VersionableObject.class, false);

            if (o != null) {
                t.setIsVersionable(2);
            } else {
                t.setIsVersionable(1);
            }
        }
        return t.getIsVersionable() == 2;
    }

    public com.hitorro.util.typesystem.HTSerializable getObject(String quid, String guidSansType, DMSSession session) {
        try {
            Query qq;
            if (guidSansType.charAt(0) == 's') {
                return getObjectSoftRef(guidSansType.substring(2), session);
            } else {
                if (session.getSecurityModel().isEnabled()) {
                    qq = session.createQuery(queryRealm);
                    qq.setParameter("id", quid);
                    qq.setParameter("realm", session.getRealm());

                } else {
                    qq = session.createQuery(query);
                    qq.setParameter("id", quid);
                }
            }

            List l = qq.list();
            if (ListUtil.nullOrEmpty(l)) {
                return null;
            }
            if (l.size() > 1) {
                com.hitorro.util.typesystem.Log.typemanager.error("Got more than one result for guid: %s", quid);
            }
            return (com.hitorro.util.typesystem.HTSerializable) l.get(0);
        } catch (Exception e) {
            com.hitorro.util.typesystem.Log.typemanager.error("Unable to fetch object %s %e", e, e);
            return null;
        }
    }

    public com.hitorro.util.typesystem.HTSerializable getObjectSoftRef(String softRef, DMSSession session) {
        try {
            Query qq;
            if (session.getSecurityModel().isEnabled()) {
                qq = session.createQuery(this.softQueryRealm);
            } else {
                qq = session.createQuery(this.softQuery);
            }

            String parts[] = StringUtil.tokenizeFromSingleChar(softRef, com.hitorro.util.typesystem.Type.SoftFieldSeperator);
            if (parts.length != softRefField.length) {
                com.hitorro.util.typesystem.Log.typemanager.error("Unmatched amount of key parts for type %s key %s, expected %s",
                        type.getName(), softRef, StringUtil.mergeWithJoinToken(softRefField, ","));
                return null;
            }
            for (int i = 0; i < parts.length; i++) {
                qq.setParameter(softRefField[i], parts[i]);
            }
            if (session.getSecurityModel().isEnabled()) {
                qq.setParameter("realm", session.getRealm());
            }

            return (com.hitorro.util.typesystem.HTSerializable) qq.uniqueResult();
        } catch (Exception e) {
            com.hitorro.util.typesystem.Log.typemanager.error("Unable to fetch object %s %e", e, e);
            return null;
        }
    }
}
