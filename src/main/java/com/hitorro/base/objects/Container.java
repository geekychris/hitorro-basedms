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

import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import org.hibernate.query.Query;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Oct 31, 2006 Time: 2:15:41 PM
 */

@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Container,
        isView = false,
        isPersisted = true,
        schemaVersion = Container.SerializationVersion)
public class Container extends VersionableObject {
    public static final int SerializationVersion = 1;

    private String queryString;
    private String description;
    // no member for contained - get that data from the contained object side
    // we use the contained field only for hql queries.  Note that since we have containers that hold hundreds of
    // thousands of items (potentially) accessing the contained field from the container end would be bad

    public Container() {
        // needed for serialization and hybernate
    }

    public Container(Class minimimalClassConstraint, String orderBy) {
        queryString = Fmt.S("from %s s join s.containers c where c.id = :id",
                minimimalClassConstraint.getCanonicalName(),
                orderBy);
    }

    public Query getQuery(BaseSession sess) {
        return getQuery(sess, queryString);
    }

    public Query getQueryWithAppend(DMSSession sess, String appendMe) {
        return getQuery(sess, StringUtil.strcat(queryString, " ", appendMe));
    }

    public Query getQuery(BaseSession sess, String queryString) {
        return ((DMSSession) sess).createQuery(queryString).
                setParameter("id", this.getId());
    }

    public int removeCategoryFromAllSubordinates(BaseSession session) {
        Iterator<Object[]> iter = getIterator(session);
        int hitCounter = 0;
        while (iter.hasNext()) {
            Object[] a = iter.next();
            VersionableObject so = (VersionableObject) a[0];
            so.removeContainer(this);
            session.saveOrUpdate(so);
            hitCounter++;
        }
        return hitCounter;
    }

    public Iterator<Object[]> getIterator() {
        return getIterator(getSession());
    }

    public Iterator<Object[]> getIterator(BaseSession sess) {
        return (Iterator<Object[]>) getQuery(sess).stream().iterator();
    }

    /**
     * THIS METHOD ASSUMES YOU HAVE SET A SESSION ON THIS OBJECT
     *
     * @return
     */
    public List<VersionableObject> getList() {
        return getList(getSession());
    }

    public List<VersionableObject> getList(BaseSession sess) {
        return (List<VersionableObject>) getQuery(sess).list();
    }

    String getQueryString() {
        return queryString;
    }

    public void setQueryString(String q) {
        queryString = q;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    @UiProperties(displayName = "System Id", displayType = UiProperties.TextFieldDisplay)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<VersionableObject> getContained() {
        // no implementation - get the relationship from the containee's side
        return null;
    }

    public void setContained(Set<VersionableObject> vals) {
        // no implementation because we don't actually want to set the contained from here.
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Container origD = (Container) orig;
        description = origD.description;
        queryString = origD.queryString;
        // no contained field - see comment above
    }

    public void serialize(HTObjectOutputStream os) throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(description);
        os.writeString(queryString);
        // no contained field - see comment above
    }

    /**
     * @param os
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                description = os.readString();
                queryString = os.readString();
        }
    }
}
