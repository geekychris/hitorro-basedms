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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represent a forum topic.
 */
@Entity
@Table(name = "Forum")
@PrimaryKeyJoinColumn(name = "system_id")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.Forum,
        isView = false,
        isPersisted = true,
        softLinkField = "name",
        schemaVersion = Forum.SerializationVersion)
@UiTypeProperties(name = "Forum",
        views = {
                @ViewClassReference(name = ViewClassReference.ListView, viewClass = Forum.ForumListView.class),
                @ViewClassReference(name = ViewClassReference.EditView, viewClass = Forum.ForumEditView.class)})
public class Forum extends Container {
    public static final int SerializationVersion = 1;
    private static final String UserQuery = "select usr " +
            "from RssFeedIn as feed, User as usr " +
            "where usr.rssFeedIn = feed and feed.forum.id= :a";
    
    @Column(name = "name", length = 80, nullable = false)
    @org.hibernate.annotations.Index(name = "name_idx")
    private String name;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id")
    private SubjectArea subjectArea;

    public Forum() {
        // forums contain posts
        super(Post.class, "id");
        name = " ";
    }

    Forum(Class c, String idField) {
        // forums contain posts
        super(c, idField);
        name = " ";
    }

    /**
     * Get a listFiles of guids of all the forums.
     *
     * @param session the database session to use for the query
     * @return a listFiles of all forums, never null
     */
    public static List<String> getAllForumGuids(DMSSession session) {
        Iterator itr = session.getIteratorFromQuery("select guid from Forum");
        List<String> result = new ArrayList<String>();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof String) {
                result.add((String) obj);
            }
        }

        return result;
    }

    /**
     * Get a listFiles of guids of all the forums in a group.
     *
     * @param session   the database session to use for the query
     * @param groupName the groupname to look for
     * @return a listFiles of all forums, never null
     */
    public static List<String> getAllForumGuidsForGroup(DMSSession session, String groupName) {
        Iterator itr = session.getIteratorFromQueryArgs("select guid from Forum where groupName= :a", groupName);
        List<String> result = new ArrayList<String>();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof String) {
                result.add((String) obj);
            }
        }

        return result;
    }

    /**
     * Get a listFiles of all forum groups.
     *
     * @param session the database session to use for the query
     * @return a listFiles of all group names, never null
     */
    public static List<String> getAllForumGroups(DMSSession session) {
        Iterator itr = session.getIteratorFromQuery("select distinct groupName from Forum");
        List<String> result = new ArrayList<String>();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof String) {
                result.add((String) obj);
            }
        }

        return result;
    }

    /**
     * Fetch an Forum by its name. This method assumes that a transaction is currently open
     *
     * @param session the hibernate session
     * @param name    the name of the rss feed to fetch
     * @return the Forum, which will be null if not found
     */
    public static Forum getForumForName(BaseSession session, String name) {
        if (name == null) {
            return null;
        }
        return (Forum) session.getObject(Forum.class, "where name= :a", name);
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Forum other = (Forum) orig;
        name = other.name;
    }

    public void delete(BaseSession session) {
        removeCategoryFromAllSubordinates(session);
        super.delete(session);
    }

    /**
     * Get topics as cached on the Forum object.
     * NOTE! these will almost certainly be stale with respect to the session.
     * @return the topics on this forum, never null, but may be stale
     */
    /*
    public List<ForumTopic> getCachedTopics ()
    {
        if (_cachedTopics == null) {
            List<VersionableObject> topics = getItems();
            _cachedTopics = new ArrayList<ForumTopic>();
            for (VersionableObject obj : topics) {
                if (obj instanceof ForumTopic) {
                    _cachedTopics.add((ForumTopic)obj);
                }
            }
        }
        return _cachedTopics;
    }
    */

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @UiProperties(displayName = "Items", displayType = UiProperties.DetailListDisplay)
    public List<VersionableObject> getItems() {
        return super.getList();
    }

    @UiProperties(displayName = "Area", displayType = UiProperties.VersionableObjectDisplay)
    public SubjectArea getArea() {
        return subjectArea;
    }

    public void setArea(SubjectArea val) {
        subjectArea = val;
    }

    /**
     * Get the number of topics that this forum contains.
     *
     * @return the number of items.
     */
    public int getTopicCount() {
        BaseSession session = getSession();
        if (session == null) {
            return 0;
        }

        Iterator itr = session.getIteratorFromQueryArgs(
                "select count(*) from Post where containers.id= :a and entryOrdinal = 0", getId());
        if (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof Long) {
                return ((Long) obj).intValue();
            }
        }

        return 0;
    }

    /**
     * Get the user associated with this forum, if any.
     *
     * @param session the database session
     * @return the user associated with this forum, or null if there is none.
     */
    public User getUser(BaseSession session) {
        Iterator itr = session.getIteratorFromQueryArgs(UserQuery, getId());
        if (itr.hasNext()) {
            return (User) itr.next();
        }

        return null;
    }

    public List<RssFeedIn> getRssFeedIns(DMSSession session) {
        List<RssFeedIn> result = new ArrayList<RssFeedIn>();
        Iterator itr = session.getIteratorFromQueryArgs("select feed from RssFeedIn as feed where forum= :a", this);
        while (itr.hasNext()) {
            result.add((RssFeedIn) itr.next());
        }

        return result;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);

        os.writeString(name);

    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 1:
                name = os.readString();
        }
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "ForumListView",
            isView = true,
            isPersisted = false,
            schemaVersion = Forum.SerializationVersion)
    public abstract static class ForumListView {
        public abstract String getName();
    }

    @TypeClassMetaInfo(shortTypeName = "ForumEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = Forum.SerializationVersion)
    public abstract static class ForumEditView {
        public abstract String getName();

        public abstract SubjectArea getArea();

        public abstract int getState();
    }
}
