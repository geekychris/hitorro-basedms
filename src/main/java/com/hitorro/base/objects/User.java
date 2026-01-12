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
import com.hitorro.basedms.Log;
import com.hitorro.basedms.StoreUtil;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.contentconstraints.TagConstraint;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.annotation.TypeClassMetaInfo;
import com.hitorro.util.typesystem.annotation.UiProperties;
import com.hitorro.util.typesystem.annotation.UiTypeProperties;
import com.hitorro.util.typesystem.annotation.ViewClassReference;

import jakarta.persistence.*;

import java.io.IOException;
import java.util.*;

/**
 * Basic user in the system
 */
@Entity
@Table(name = "user_t")
@PrimaryKeyJoinColumn(name = "system_id")
@TypeClassMetaInfo(shortTypeName = TypeClassMetaInfo.User,
        isView = false,
        isPersisted = true,
        schemaVersion = User.SerializationVersion,
        softLinkField = "name",
        guidAccessor = GuidAccessor.class)
@UiTypeProperties(name = "User", views = {@ViewClassReference(name = ViewClassReference.ListView, viewClass = User.UserListView.class),
        @ViewClassReference(name = ViewClassReference.EditView, viewClass = User.UserEditView.class)}
)
public class User extends VersionableObject {
    public static final int SerializationVersion = 2;
    public static final String PersistedDomain = "persisted";
    public static final String UserPrefs = "prefs";
    private static final String ForumQuery = "select forum " +
            "from User as usr, RssFeedIn as feed, Forum as forum " +
            "where feed.forum = forum and usr.rssFeedIn = feed " +
            "and usr.id = :a";
    // username is the unique key, and used for login
    @Column(name = "name", length = 80, nullable = false)
    @org.hibernate.annotations.Index(name = "name_idx")
    private String name;
    
    // displayname is the user friendly displayable name.  May not be unique!
    @Column(name = "displayName", length = 80)
    private String _displayName;
    
    // password will eventually be an encrypted password.  Right now, if non-null the user can log in
    @Column(name = "password", length = 40)
    private String _password;
    
    @Column(name = "emailAddress", length = 80)
    private String _emailAddress;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> _roles;
    
    @Column(name = "passwordHash", length = 80)
    private String passwordHash;
    
    @Column(name = "activationToken", length = 80)
    private String activationToken;

    public User() {
        _roles = new HashSet<Role>();
    }

    /**
     * Fetch a User by its userName.
     *
     * @param session  the dms session
     * @param userName the userName of the user to fetch
     * @return the user, which will be null if not found
     */
    public static User getUserForName(BaseSession session, String userName) {
        return (User) session.getObject(User.class, "where name= :a", userName);
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        User other = (User) orig;
        name = other.name;
        _displayName = other._displayName;
        _password = other._password;
        _emailAddress = other._emailAddress;

        // permissions
        HashSet<Role> temp = new HashSet<Role>();
        // have to copy this way because of hibernate
        for (Role obj : other._roles) {
            temp.add(obj);
        }
        _roles = temp;
    }

    /**
     * Get the persisted user prefs or an empty user prefs if not already persisted.
     *
     * @return
     */
    public UserPreferences getUserPreferences() {
        HTSerializable pts = this.getHTSerializableContent(PersistedDomain,
                UserPrefs);
        if (pts instanceof UserPreferences) {
            return (UserPreferences) pts;
        }
        return new UserPreferences();
    }

    public Content setUserPreferences(UserPreferences up) {
        ContentType ct = ContentTypeCache.getCache().getContentTypeByMimeType(ContentType.MimeJavaSerializedObject);

        return setHTSerializableContent(PersistedDomain, UserPrefs, UserPrefs, up, ct, StoreUtil.getBlobStore(), true);
    }

    @UiProperties(displayName = "Name", displayType = UiProperties.TextFieldDisplay, order = 10)
    public String getName() {
        return name;
    }

    public void setName(String userName) {
        name = userName;
    }

    @UiProperties(displayName = "Display Name", displayType = UiProperties.TextFieldDisplay, order = 20)
    public String getDisplayName() {
        return _displayName;
    }

    public void setDisplayName(String userName) {
        _displayName = userName;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String val) {
        _password = val;
    }

    @UiProperties(displayName = "Email Address", displayType = UiProperties.TextFieldDisplay, order = 30)
    public String getEmailAddress() {
        return _emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this._emailAddress = emailAddress;
    }

    public Set<Role> getRoles() {
        return _roles;
    }

    public void setRoles(Set<Role> val) {
        _roles = val;
    }

    public boolean hasRole(String targetRole) {


        boolean hasRole = false;

        Set<Role> roles = getRoles();

        Iterator iterator = roles.iterator();

        while (iterator.hasNext()) {
            Role role = (Role) iterator.next();

            if (role.getName().equalsIgnoreCase(targetRole)) {
                hasRole = true;
                break;
            }
        }

        return hasRole;

    }

    //private static final String ConversationQuery = "select cnv from " + Conversation.class.getCanonicalName() +
    //        " as cnv where cnv.primaryPersonality= :a order by cnv.firstPostDate desc";
    /*
     * Get conversations where this personality is the primary personality.
     * We may limit the number of conversations returned.  The conversations are ordered by most recent first.
     * @param session database session
     * @param limit maximum number of conversations.  If limit < 0 we'll return them all.
     * @return a listFiles of conversations, never null
     */
    /*
    public List<Conversation> getConversations (DMSSession session, int limit)
    {
        List<Conversation> conversations = new ArrayList<Conversation>();
        if (session == null) {
            return conversations;
        }
        Iterator itr = session.getIteratorFromQuery(ConversationQuery, this);
        int count = 0;
        while (itr.hasNext() && (limit < 0 || count < limit)) {
            conversations.add((Conversation)itr.next());
            count++;
        }

        return conversations;
    }

    // todo chris - this should be factored out into app logic somewhere
    public List<Conversation> getFewConversations ()
    {
        return getConversations(getSession(), 3);
    }
    */

    //private static final String PersonalityForFeedGuidQuery = "select ps from " + Personality.class.getCanonicalName() +
    //        " as ps, " + RssFeedIn.class.getCanonicalName() + " as feed " +
    //        " where ps.rssFeedIn = feed and feed.guid= :a";
    /*
     * Fetch a Personality by checking for the guid of the associated RssFeed.
     * This method assumes that a transaction is currently open
     *
     * @param session the hibernate session
     * @param guid the guid of the feed associated with the personality
     * @return the Personality, which will be null if not found, which may be because the feed isn't associated
     * with a Personality
     */
    /*
    public static Personality getPersonalityForFeedGuid (DMSSession session, String guid)
    {
        if (guid == null || guid.length() < 1)
        {
            return null;
        }
        Iterator itr = session.getIteratorFromQuery(PersonalityForFeedGuidQuery, guid);
        Personality result = null;
        if (itr.hasNext()) {
            result = (Personality)itr.next();
        }

        return result;
    }
    */

    @UiProperties(displayName = "Picture URL", displayType = UiProperties.TextFieldDisplay, order = 40)
    public String getPictureImageUrl() {
        Content contRetrieved = getContentByConstraint(new TagConstraint("docparts", "thumbnail"), true);
        if (contRetrieved != null) {
            return contRetrieved.getExternalURL();
        } else {
            // use default
            //return "http://www.bookofjoe.com/images/opium_poppy.jpg";
            return "../public/images/whois.gif";
        }
    }

    public void setPictureImageUrl(String url) {
        // if the content already exists, delete it
        removeAllContentWithDomainValue("docparts", "thumbnail");

        if (url != null) {
            // store the new content
            ContentType cType = ContentTypeCache.getCache().getContentTypeByMimeType("image/jpeg");
            Content cnt = setContentLink(url, cType);
            try {
                cnt.addCategory("docparts", "thumbnail");
            } catch (CategoryException exc) {
                Log.objectHandling.error("Couldn't add category to personality picture image url");
            }
        }
    }

    public List<Permission> getAllPermissions() {
        List<Permission> permissions = new ArrayList<Permission>();
        Set<Role> roles = getRoles();
        if (roles != null) {
            for (Role role : roles) {
                Set<Permission> perms = role.getPermissions();
                if (perms != null) {
                    for (Permission prm : perms) {
                        permissions.add(prm);
                    }
                }
            }
        }

        return permissions;
    }

    public List<String> getAllPermissionNames() {
        List<String> permissionNames = new ArrayList<String>();

        for (Permission permission : getAllPermissions()) {
            permissionNames.add(permission.getName());
        }

        return permissionNames;
    }

    /**
     * Get the forum that this user posts to (if any)
     *
     * @param session The database session
     * @return the user's posting forum or null if there isn't any
     */
    public Forum getForum(DMSSession session) {
        Iterator itr = session.getIteratorFromQueryArgs(ForumQuery, this.getId());
        if (itr.hasNext()) {
            return (Forum) itr.next();
        }

        return null;
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(passwordHash);
        os.writeString(getActivationToken());
        os.writeString(name);
        os.writeString(_displayName);
        os.writeString(_password);
        os.writeString(_emailAddress);
        os.writeSetOfBaseType(_roles);
    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);

        switch (version) {
            case 2:
                passwordHash = os.readString();
                setActivationToken(os.readString());
            case 1:
                name = os.readString();
                _displayName = os.readString();
                _password = os.readString();
                _emailAddress = os.readString();
                os.readSetOfHTSerializable(_roles);
        }
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {
            case 1:
                //upgrade 2-3
                return true;
            default:
                return false;
        }
    }

    public int getSerializationVersion() {
        return SerializationVersion;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    /**
     * View class enumerating which fields to show when listing.
     */
    @TypeClassMetaInfo(shortTypeName = "UserListView",
            isView = true,
            isPersisted = false,
            schemaVersion = User.SerializationVersion)
    public abstract static class UserListView {
        public abstract String getName();

        public abstract String getDisplayName();
    }

    /**
     * View class enumerating which fields to show when editing.
     */
    @TypeClassMetaInfo(shortTypeName = "UserEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = User.SerializationVersion)
    public abstract static class UserEditView {
        public abstract String getName();

        public abstract String getEmailAddress();

        public abstract String getDisplayName();

        public abstract int getState();
    }
}
