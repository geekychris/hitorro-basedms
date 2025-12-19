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
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.Constants;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import java.io.IOException;
import java.util.*;

/**
 * Object representing a subject area (tech, poetry, golf, cooking).
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.SubjectArea,
        isView = false,
        isPersisted = true,
        schemaVersion = SubjectArea.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class)
@UiTypeProperties(name = "Area", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = SubjectArea.SubjectAreaListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = SubjectArea.SubjectAreaEditView.class)}
)
public class SubjectArea extends VersionableObject {
    public static final int SerializationVersion = 1;
    // the name where we put forums by default
    public static final String DefaultArea = "Other";
    // name is the unique key
    private String _name;
    // displayname is the user friendly displayable name.  May not be unique!
    private String _displayName;
    // feeds associated with this area
    //private Set<Forum> _forums;
    // personalities associated with this area
    private Set<User> _personalities;


    public SubjectArea() {
        _personalities = new HashSet<User>();
        //_forums = new HashSet<Forum>();
    }

    /**
     * Fetch a SubjectArea by its name.
     *
     * @param session the dms session
     * @param name    the userName of the user to fetch
     * @return the SubjectArea, which will be null if not found
     */
    public static SubjectArea getSubjectAreaForName(BaseSession session, String name) {
        if (name == null) {
            return null;
        }
        return (SubjectArea) session.getObject(SubjectArea.class, "where name= :a", name);
    }

    /**
     * Get a listFiles of all active area names.
     *
     * @param session the database session to use for the query
     * @return a listFiles of all area names, never null
     */
    public static List<String> getAllAreaNames(DMSSession session) {
        Iterator itr = session.getIteratorFromQueryArgs("select distinct name from SubjectArea as area where area.state= :a",
                Constants.ActiveState);
        List<String> result = new ArrayList<String>();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof String) {
                result.add((String) obj);
            }
        }

        return result;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        SubjectArea other = (SubjectArea) orig;
        _name = other._name;
        _displayName = other._displayName;

        // personalities
        HashSet<User> temp = new HashSet<User>();
        // have to copy this way because of hibernate
        for (User obj : other._personalities) {
            temp.add(obj);
        }
        _personalities = temp;

        // forums
        /*
        HashSet<Forum> temp2 = new HashSet<Forum>();
        // have to copy this way because of hibernate
        for (Forum obj : other._forums) {
            temp2.add(obj);
        }
        _forums = temp2;
        */
    }

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getName() {
        return _name;
    }

    public void setName(String val) {
        _name = val;
    }

    @UiProperties(displayName = "Display Name", displayType = UiProperties.TextFieldDisplay, order = 20)
    public String getDisplayName() {
        return _displayName;
    }

    public void setDisplayName(String userName) {
        _displayName = userName;
    }

    /*
    public Set<Forum> getForums ()
    {
        return _forums;
    }

    public void setForums (Set<Forum> val)
    {
        _forums = val;
    }
    */

    /**
     * Add a forum to the set of forums of this area.
     * This method will add the forum if it is not already in the area's set.  So it is safe to call this
     * without checking whether the feed is in the area's set.
     * @param newFeed new feed to add to the area
     */
    /*
    public void addForum (Forum newFeed)
    {
        if (newFeed != null && !_forums.contains(newFeed)) {
            _forums.add(newFeed);
        }
    }
    */

    public Set<User> getPersonalities() {
        return _personalities;
    }

    public void setPersonalities(Set<User> val) {
        _personalities = val;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(_name);
        os.writeString(_displayName);
        os.writeSetOfBaseType(_personalities);
        //os.writeSetOfBaseType(_forums);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 1:
                _name = os.readString();
                _displayName = os.readString();
                os.readSetOfHTSerializable(_personalities);
                //os.readSetOfHTSerializable(_forums);
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "SubjectAreaListView",
            isView = true,
            isPersisted = false,
            schemaVersion = SubjectArea.SerializationVersion)
    public abstract static class SubjectAreaListView {
        public abstract String getName();

        public abstract String getDisplayName();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "SubjectAreaEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = SubjectArea.SerializationVersion)
    public abstract static class SubjectAreaEditView {
        public abstract String getName();

        public abstract String getDisplayName();

        public abstract int getState();
    }
}
