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

import com.hitorro.basedms.db.JDBCUtils;
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
 * Represent an incoming rss feed.
 */
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.RssFeedIn,
        isView = false,
        isPersisted = true,
        softLinkField = "name",
        schemaVersion = RssFeedIn.SerializationVersion)
@UiTypeProperties(name = "Rss Input Feed",
        views = {
                @ViewClassReference(name = ViewClassReference.ListView, viewClass = RssFeedIn.RssFeedInListView.class),
                @ViewClassReference(name = ViewClassReference.EditView, viewClass = RssFeedIn.RssFeedInEditView.class),
                @ViewClassReference(name = ViewClassReference.PublicEditView, viewClass = RssFeedIn.RssFeedInPublicEditView.class)})
public class RssFeedIn extends VersionableObject {
    public static final int NormalForum = 0;
    public static final int AggregateForum = 1;

    public static final int SerializationVersion = 2;

    private String _name;
    private String _description;
    private String _uri;
    private Date _nextRead;
    private int _readInMinutes;
    private String _contactEmail;
    private String _contactName;
    private String _lastModified;
    private String _siteUrl;
    private Forum _forum;
    private Forum sharedForum;
    private int _forumType = NormalForum;
    private int _priority = 0;

    public RssFeedIn() {
        // by default we read every 30 minutes, and haven't read yet
        _readInMinutes = 30;
        _nextRead = new Date();
    }

    /**
     * Fetch an RssFeedIn by its name. This method assumes that a transaction is currently open
     *
     * @param session the hibernate session
     * @param name    the name of the rss feed to fetch
     * @return the RssFeedIn, which will be null if not found
     */
    public static RssFeedIn getRssFeedInForName(BaseSession session, String name) {
        if (name == null || name.length() < 1) {
            return null;
        }
        return (RssFeedIn) session.getObject(RssFeedIn.class, "where name= :a", name);
    }

    /**
     * Fetch an RssFeedIn by its uri. This method assumes that a transaction is currently open
     *
     * @param session the hibernate session
     * @param uri     the uri of the rss feed to fetch
     * @return the RssFeedIn, which will be null if not found
     */
    public static RssFeedIn getRssFeedInForUri(BaseSession session, String uri) {
        return (RssFeedIn) session.getObject(RssFeedIn.class, "where uri= :a", uri);
    }

    /**
     * Get all the active feeds.
     *
     * @param session Session to use for query
     * @return a listFiles (never null) of the active feed objects
     */
    public static List<RssFeedIn> getActiveFeeds(BaseSession session) {
        return getFeeds(session, "where state= :a", Constants.ActiveState);
    }

    /**
     * Get feeds based on a general query.
     *
     * @param session     Session to use for query
     * @param query       the query to use, possibly with parameters specified with '?' characters
     * @param paramValues values of parameters, in the order that the ? tokens appear in the query string
     * @return a listFiles (never null) of the active feed objects
     */
    public static List<RssFeedIn> getFeeds(BaseSession session, String query, Object... paramValues) {
        List<RssFeedIn> feeds = new ArrayList<RssFeedIn>();
        session.getObjects(RssFeedIn.class, query, feeds, paramValues);

        return feeds;
    }

    /**
     * Get a listFiles of guids of all the forums.
     *
     * @param session database connection
     * @return a listFiles of all forums, never null
     */
    public static List<String> getAllFeedGuids(DMSSession session) {
        Iterator itr = session.getIteratorFromQuery("select guid from RssFeedIn");
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
        RssFeedIn other = (RssFeedIn) orig;
        _name = other._name;
        _uri = other._uri;
        _nextRead = (Date) other._nextRead.clone();
        _readInMinutes = other._readInMinutes;
        _contactEmail = other._contactEmail;
        _contactName = other._contactName;
        _lastModified = other._lastModified;
        _siteUrl = other._siteUrl;
        _description = other._description;
        _forum = other._forum;
    }

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getName() {
        return _name;
    }

    public void setName(String val) {
        _name = val;
    }

    @UiProperties(displayName = "Forum", displayType = UiProperties.VersionableObjectDisplay, order = 12)
    public Forum getForum() {
        return _forum;
    }

    public void setForum(Forum val) {
        _forum = val;
    }

    @UiProperties(displayName = "Description", displayType = UiProperties.TextAreaDisplay, order = 15)
    public String getDescription() {
        return _description;
    }

    public void setDescription(String val) {
        _description = val;
    }

    @UiProperties(displayName = "Contact Name", displayType = UiProperties.TextFieldDisplay, order = 40)
    public String getContactName() {
        return _contactName;
    }

    public void setContactName(String name) {
        _contactName = name;
    }

    @UiProperties(displayName = "Contact Email", displayType = UiProperties.TextFieldDisplay, order = 50)
    public String getContactEmail() {
        return _contactEmail;
    }

    public void setContactEmail(String name) {
        _contactEmail = name;
    }

    @UiProperties(displayName = "Read Frequency (minutes)", displayType = UiProperties.IntFieldDisplay, order = 30)
    public int getReadInMinutes() {
        return _readInMinutes;
    }

    public void setReadInMinutes(int readInMinutes) {
        _readInMinutes = readInMinutes;
    }

    @UiProperties(displayName = "URI", displayType = UiProperties.TextFieldDisplay, order = 20)
    public String getUri() {
        return _uri;
    }

    /*
    @UiProperties(displayName = "Items", displayType = UiProperties.DetailListDisplay)
    public List<VersionableObject> getItems ()
    {
        return super.apply();
    }
    */

    public void setUri(String uri) {
        _uri = uri;
    }

    @UiProperties(displayName = "Site URL", displayType = UiProperties.TextFieldDisplay, order = 80)
    public String getSiteUrl() {
        return _siteUrl;
    }

    public void setSiteUrl(String su) {
        _siteUrl = su;
    }

    @UiProperties(displayName = " read date", displayType = UiProperties.TextFieldDisplay)
    public Date getNextRead() {
        return _nextRead;
    }

    public void setNextRead(Date lastRead) {
        _nextRead = lastRead;
    }

    /**
     * The last modified tag, as reported by the feed.
     *
     * @return the last modified tag
     */
    @UiProperties(displayName = "Last Modified", displayType = UiProperties.TextFieldDisplay, order = 60)
    public String getLastModified() {
        return _lastModified;
    }

    public void setLastModified(String lastModified) {
        _lastModified = lastModified;
    }

    /**
     * Calculate the next read time, assuming that we have just finished reading.
     *
     * @param minutes how far in the future (in minutes) we should read next.  If zero, feed's own idea.
     */
    public void updateNextRead(int minutes) {
        GregorianCalendar calen = new GregorianCalendar();
        if (minutes < 1) {
            // use the feed's interval
            minutes = _readInMinutes;
        }
        calen.add(GregorianCalendar.MINUTE, minutes);
        setNextRead(calen.getTime());
    }

    /**
     * Get the number of items that this feed contains.
     *
     * @return the number of items.
     */
    public int getItemCount() {
        BaseSession session = getSession();
        if (session == null) {
            return 0;
        }

        Iterator itr = session.getIteratorFromQueryArgs("select count(*) from Post where containers.id= :a", getId());
        if (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof Long) {
                return ((Long) obj).intValue();
            }
        }

        return 0;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);

        os.writeInt(_forumType);
        os.writeInt(_priority);
        os.writeString(_name);
        os.writeString(_uri);
        os.writeInt(_readInMinutes);
        os.writeDate(_nextRead);
        os.writeString(_contactEmail);
        os.writeString(_contactName);
        os.writeString(_lastModified);
        os.writeString(_siteUrl);
        os.writeString(_description);
        os.writeVersionedObject(_forum);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 2:
                _forumType = os.readInt();
                _priority = os.readInt();
            case 1:
                _name = os.readString();
                _uri = os.readString();
                _readInMinutes = os.readInt();
                _nextRead = os.readDate();
                _contactEmail = os.readString();
                _contactName = os.readString();
                _lastModified = os.readString();
                _siteUrl = os.readString();
                _description = os.readString();
                _forum = (Forum) os.readVersionedObject();
        }
    }

    public int getForumType() {
        return _forumType;
    }

    public void setForumType(int _forumType) {
        this._forumType = _forumType;
    }

    public int getPriority() {
        return _priority;
    }

    public void setPriority(int _priority) {
        this._priority = _priority;
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 1:
                //upgrade 1-2
                JDBCUtils.performSimpleJDBCUpdate("update RssFeedIn set priority=0, forumType=0");
                return true;
            default:
                return false;
        }
    }

    public Forum getSharedForum() {
        return sharedForum;
    }

    public void setSharedForum(Forum sharedForum) {
        this.sharedForum = sharedForum;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "RssFeedInListView",
            isView = true,
            isPersisted = false,
            schemaVersion = RssFeedIn.SerializationVersion)
    public abstract static class RssFeedInListView {
        public abstract String getName();

        public abstract int getState();
    }

    @TypeClassMetaInfo(shortTypeName = "RssFeedInEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = RssFeedIn.SerializationVersion)
    public abstract static class RssFeedInEditView {
        public abstract String getName();

        public abstract String getUri();

        public abstract String getReadInMinutes();

        public abstract String getContactName();

        public abstract String getContactEmail();

        public abstract int getState();

        public abstract String getLastModified();

        public abstract String getSiteUrl();

        public abstract String getDescription();

        public abstract Forum getForum();
    }

    @TypeClassMetaInfo(shortTypeName = "RssFeedInPublicEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = RssFeedIn.SerializationVersion)
    public abstract static class RssFeedInPublicEditView {
        public abstract String getName();

        public abstract String getUri();

        public abstract String getContactName();

        public abstract String getContactEmail();

        public abstract String getDescription();
    }
}
