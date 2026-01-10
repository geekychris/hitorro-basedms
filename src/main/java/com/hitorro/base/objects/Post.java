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

import gnu.trove.map.hash.TIntObjectHashMap;
import com.hitorro.base.typesystem.btadapter.rssadapters.PostRssItemAdapter;
import com.hitorro.base.typesystem.btadapter.rssadapters.VersionableObjectRssItem;
import com.hitorro.basedms.Log;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.contentconstraints.TagConstraint;
import com.hitorro.basedms.db.JDBCUtils;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLParser;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.urlparser.URLUtil;
import org.hibernate.query.Query;

import jakarta.persistence.*;

import java.io.IOException;
import java.util.*;

/**
 * Represents the data resulting from a single Rss feed item.
 */
@Entity
@Table(name = "Post")
@PrimaryKeyJoinColumn(name = "system_id")
@com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = com.hitorro.util.typesystem.annotation.TypeClassMetaInfo.Post,
        adapters = {@com.hitorro.util.typesystem.annotation.AdapterClassMeta(className = PostRssItemAdapter.class, adapterGroup = VersionableObjectRssItem.AdapterGroup)},
        isView = false,
        isPersisted = true,
        schemaVersion = Post.SerializationVersion,
        softLinkField = "title")
@com.hitorro.util.typesystem.annotation.UiTypeProperties(name = "Post",
        views = {@com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.ListView, viewClass = Post.PostListView.class),
                @com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.EditView, viewClass = Post.PostEditView.class),
                @com.hitorro.util.typesystem.annotation.ViewClassReference(name = com.hitorro.util.typesystem.annotation.ViewClassReference.InspectView, viewClass = Post.PostInspectView.class)})
public class Post extends Document {
    public static final String PostDegreeKey = "postDegree";
    public static final String PostTypeKey = "degreeType";
    public static final int MaxTitleSize = 100;
    public static final String HTContentAssetTag = "htcontentassettype";

    public static final String VideoTag = "video";
    public static final String AudioTag = "audio";
    public static final String TranscriptionTag = "transcription";
    public static final String LargeImageTag = "largeimage";
    public static final String ThumbnailImageTag = "thumbnailimage";


    public static final String OriginalAuthorKey = "originalAuthor";
    /**
     * Return a listFiles of documents similar to this one.
     *
     * @param maxResults maximum number of results to return?
     * @param minRank    minimum rank?
     * @param maxTerms   maximum terms to test on?
     * @return a listFiles of key-values, the key is the document guid, the value is a "score"
     */


    public static final int SerializationVersion = 2;
    public static final String DocPartsDomain = "docparts";
    public static final String BodyLabel = "body";
    public static final String BodyFromPermaLinkLabel = "bodyfrompermalink";
    public static final String BodyHtmlLabel = "bodyhtml";
    public static final String EnclosureLabel = "enclosure";
    public static final String OriginatorLabel = "originator";
    public static final String ThumbnailLabel = "thumbnail";
    public static final String ReferenceLinks = "reflinks";
    public static final String ReferenceLinksNew = "reflinksnew";
    private static final String PostByTitle = "select bentry from " + Post.class.getName() + " as bentry " +
            "where bentry.title = :title and containers.id = :rssid ";
    private static final String PostByLink = "select bentry from " + Post.class.getName() + " as bentry " +
            "where bentry.identityHash = :hash ";
    private static final String PostByIdentityHashForum = "select bentry from " + Post.class.getName() + " as bentry " +
            "where bentry.identityHash = :hash and containers.id = :forumid ";
    private static final String PostByIdentityHash = "select pst from Post as pst where identityHash = :a";
    
    @Transient
    private long titleHash;
    
    @Column(name = "entryOrdinal", nullable = false)
    private int entryOrdinal;
    
    @Column(name = "moreLink", length = 1024)
    private String moreLink;
    
    @Transient
    private int state;
    
    @Column(name = "degree")
    private int degree = 0;
    
    @Column(name = "type")
    private int type = PostType.RSSFeedType.getOrdinal();
    
    @Column(name = "siteLink", length = 1024)
    private String siteLink;
    
    // the parent Post of this post (non-null for comments.  In this case _entryOrdinal gives ordering of listChildren.)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private Post parent;
    
    // which feeds that showed this post (if it came from a feed)
    //private Set<RssFeedIn> _rssFeedIns;
    
    @Column(name = "originalAuthor")
    private String originalAuthor;
    
    @Column(name = "refCount")
    private int refCount;
    
    @Column(name = "feedContact")
    private String feedContact;
    
    @Column(name = "destinationUrl", length = 1024)
    private String destinationUrl;

    public Post() {
        entryOrdinal = 0;
        setTitle("");
        setContent("");
        //_rssFeedIns = new HashSet<RssFeedIn>();
        setState(Constants.ActiveState);
    }

    /**
     * Fetch a Post by its title. This method assumes that a transaction is currently open
     *
     * @param session the hibernate session
     * @param rssid   the id of the rss input feed
     * @param title   title of the post to look for
     * @return the Post, which will be null if not found
     */
    public static Post getPostByTitle(DMSSession session, long rssid, String title) {
        Query qq = session.createQuery(PostByTitle);
        Post bl = null;
        qq.setParameter("rssid", rssid);
        qq.setParameter("title", title);
        List result = qq.list();
        if (result != null && result.size() > 0) {
            bl = (Post) result.get(0);
        }

        return bl;
    }

    /**
     * Fetch the post using only the link
     *
     * @param session - database session
     * @param link    - link we're looking for
     * @return - the post or null if not found
     */
    public static Post getPostByMoreLink(BaseSession session, String link) {
        Post post = null;
        Query qq = ((DMSSession) session).createQuery(PostByLink);
        String moreLink = URLUtil.cleanupUrl(link);
        long fp = calculateIdentityHash(moreLink);
        qq.setParameter("hash", fp);
        List result = qq.list();

        if (result != null && result.size() > 0) {
            post = (Post) result.get(0);
        }

        return post;
    }

    /**
     * Fetch the post by its hash of the more link.
     *
     * @param session  database session
     * @param forumid  Id of the containing forum
     * @param moreLink link we're looking for
     * @return the post, or null if not found
     */
    public static Post getPostByMoreLink(DMSSession session, long forumid, String moreLink) {
        Query qq = session.createQuery(PostByIdentityHashForum);
        Post bl = null;
        qq.setParameter("forumid", forumid);
        moreLink = URLUtil.cleanupUrl(moreLink);
        long fp = calculateIdentityHash(moreLink);
        qq.setParameter("hash", fp);
        List result = qq.list();
        if (result != null && result.size() > 0) {
            bl = (Post) result.get(0);
        }

        return bl;
    }

    public static Post getPostByMoreLinkHash(BaseSession session, long linkHash) {
        return (Post) session.getObject(PostByIdentityHash, true, true, linkHash);
    }

    /**
     * Calculate an identity hash for a Post. This method can be used to calculate what the identity has would be for a
     * Post, if it exists
     *
     * @param link the permalink of the Post.
     * @return the identity hash a Post would have with the given permalink
     */
    public static long calculateIdentityHash(String link) {
        return FPHash64.getFP(link);
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = OriginalAuthorKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = OriginalAuthorKey, stringLiteral = false, allField = false, stored = true)
    public String getOriginalAuthor() {
        return originalAuthor;
    }

    public void setOriginalAuthor(String originalAuthor) {
        this.originalAuthor = originalAuthor;
    }

    public int getRefCount() {
        return refCount;
    }

    public void setRefCount(int refCount) {
        this.refCount = refCount;
    }

    public String getFeedContact() {
        return feedContact;
    }

    public void setFeedContact(String feedContact) {
        this.feedContact = feedContact;
    }

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public void setDestinationUrl(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    public void copy(VersionableObject orig) {
        super.copy(orig);
        Post other = (Post) orig;
        entryOrdinal = other.entryOrdinal;
        moreLink = other.moreLink;
        state = other.state;
        degree = other.degree;
        type = other.type;
        siteLink = other.siteLink;
        parent = other.parent;
        refCount = other.refCount;
        feedContact = other.feedContact;
        destinationUrl = other.destinationUrl;

        // feeds
        /*
        HashSet<RssFeedIn> temp = new HashSet<RssFeedIn>();
        // have to copy this way because of hibernate
        for (RssFeedIn obj : other._rssFeedIns) {
            temp.add(obj);
        }
        _rssFeedIns = temp;
        */
    }

    public Forum getForum() {
        Container owc = getOwningContainer();
        if (owc instanceof Forum) {
            return (Forum) owc;
        }
        return null;
    }

    public String getSiteLink() {
        return siteLink;
    }

    public void setSiteLink(String siteLink) {
        this.siteLink = siteLink;
    }

    /*
    public Set<RssFeedIn> getRssFeedIns ()
    {
        return _rssFeedIns;
    }

    public void setRssFeedIns (Set<RssFeedIn> val)
    {
        _rssFeedIns = val;
    }
    */

    public Post getParent() {
        return parent;
    }

    public void setParent(Post val) {
        parent = val;
    }

    public PostType getPostType() {
        return PostType.getByOrdinal(type);
    }

    public void setPostType(PostType type) {
        setType(type.getOrdinal());
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = PostTypeKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = PostTypeKey, stringLiteral = true, allField = false, stored = true)
    public int getType() {
        return type;
    }

    public void setType(int type) {
        type = type;
    }

    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = PostDegreeKey,
            isFullTextIndexable = true, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = PostDegreeKey, stringLiteral = true, allField = false, stored = true)
    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        degree = degree;
    }

    public Forum getRssFeed() {
        Container owc = getOwningContainer();
        if (owc instanceof Forum) {
            // we only expect to have one RssFeedIn container - so this must be it
            return (Forum) owc;
        }
        return null;
    }

    public int getEntryOrdinal() {
        return entryOrdinal;
    }

    public void setEntryOrdinal(int entryOrdinal) {
        entryOrdinal = entryOrdinal;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Link", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 40)
    @com.hitorro.util.typesystem.annotation.FullTextAttributeMetaInfo(displayName = "moreLink",
            isFullTextIndexable = false, luceneIndexingFilters = "STANDARD,CASE,PORTERSTEM",
            luceneFieldName = "moreLink", stringLiteral = true, allField = false, stored = true)
    public String getMoreLink() {
        return moreLink;
    }

    public void setMoreLink(String link) {
        moreLink = link;
        setIdentityHash(calculateIdentityHash(link));
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Video Link", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 80)
    public String getVideoLink() {
        Content contRetrieved = getContentByConstraint(new TagConstraint("docparts", "video"), true);
        if (contRetrieved != null) {
            return contRetrieved.getExternalURL();
        } else {
            return null;
        }
    }

    public void setVideoLink(String url) {
        // if the content already exists, delete it
        removeAllContentWithDomainValue("docparts", "video");

        ContentType cType = ContentTypeCache.getCache().getContentTypeByMimeType("video/mp4");
        Content cnt = setContentLink(url, cType);
        try {
            cnt.addCategory("docparts", "video");
        } catch (CategoryException exc) {
            Log.basedms.error("Couldn't add video link to post");
        }
    }

    /**
     * Get the video url that is usable by the HiTorro player.
     *
     * @return the url to the video for this post, or null if there isn't any
     */
    public String getVideoUrlForPlayer() {
        String url = getVideoLink();
        if (url != null) {
            int ipindx = url.indexOf("_ipod.mp4");
            if (ipindx >= 0) {
                url = StringUtil.strcat(url.substring(0, ipindx), ".flv");
            } else if (!url.endsWith(".flv")) {
                // not a player-playable video
                url = null;
            }
        }

        return url;

    }

    /**
     * External links referenced by this post.
     *
     * @return a listFiles of external link strings, or null if there were none.
     */
    public List<String> getReferenceLinks() {
        // the reference links were stored as a long string, with links separated by newlines (see setReferenceLinks())
        String allLinks = getStringContent(Post.DocPartsDomain, Post.ReferenceLinks);
        if (allLinks == null) {
            return null;
        }
        String[] links = StringUtil.tokenizeFromSingleChar(allLinks, "\n");
        List<String> result = new ArrayList<String>();
        for (String link : links) {
            result.add(link);
        }

        return result;
    }

    public void setReferenceLinks(List<String> refLinks) {
        //RssFeedIn feed = getRssFeed();
        //String feedName = feed.getSiteUrl();

        StringBuilder rLinksText = null;
        if (refLinks != null) {
            //Console.println("For feed " + feedName);
            rLinksText = new StringBuilder();
            for (String lnk : refLinks) {
                //Console.println("    see a link of: " + lnk);
                if (rLinksText.length() > 0) {
                    // put in separator token
                    rLinksText.append('\n');
                }
                rLinksText.append(lnk);
            }
        }

        if (rLinksText != null) {
            setStringContent(Post.ReferenceLinks, rLinksText.toString());
        }
    }

    public LinkSet getReferenceLinksNew() {
        HTSerializable pts = this.getHTSerializableContent(Post.DocPartsDomain,
                Post.ReferenceLinksNew);
        if (pts instanceof LinkSet) {
            return (LinkSet) pts;
        }
        return null;
    }

    public void setReferenceLinksNew(LinkSet set) {
        if (set != null) {
            ContentType ct = ContentTypeCache.getCache().getContentTypeByMimeType(ContentType.MimeJavaSerializedObject);
            this.setHTSerializableContent(Post.DocPartsDomain,
                    Post.ReferenceLinksNew,
                    Post.ReferenceLinksNew,
                    set, ct);
        }
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Title", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextFieldDisplay, order = 10)
    public String getTitle() {
        return super.getTitle();
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "Body", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextAreaDisplay, order = 20)
    public String getBody() {
        String s = getStringContent(Post.DocPartsDomain, Post.BodyFromPermaLinkLabel);
        if (StringUtil.nullOrEmptyString(s)) {
            s = getStringContent(Post.DocPartsDomain, Post.BodyLabel);

            if (StringUtil.nullOrEmptyString(s)) {
                s = Constants.EmptyString;
            }
        }
        return s;
    }

    @com.hitorro.util.typesystem.annotation.UiProperties(displayName = "BodyText", displayType = com.hitorro.util.typesystem.annotation.UiProperties.TextAreaDisplay, order = 20)
    public String getBodyText() {
        String s = getStringContent(Post.DocPartsDomain, Post.BodyFromPermaLinkLabel);
        boolean mustEncap = false;
        if (StringUtil.nullOrEmptyString(s)) {
            s = getStringContent(Post.DocPartsDomain, Post.BodyLabel);
            mustEncap = true;
            if (StringUtil.nullOrEmptyString(s)) {
                s = Constants.EmptyString;
            }
        }
        // its in html format
        HTMLParser parser = new HTMLParser();
        try {
            if (mustEncap) {
                parser.setSourceFromPlainText(s);
            } else {
                parser.setHtmlPage(s);
            }

        } catch (IOException e) {
            return s;
        }
        return parser.getHtmlText();
    }

    /**
     * Get the body text that we originally recieved from the RssFeed. This will return non-null only if this post was
     * originally created from an RssFeed.
     *
     * @return the body text that the RssFeed gave us.
     */
    public String getRssFeedBody() {
        return getStringContent(Post.DocPartsDomain, Post.BodyLabel);
    }

    public String getContentText() {
        return getBodyText();
    }

    public Content setBody(String body) {
        return setStringContent(Post.BodyLabel, body);
    }

    public Content setBodyFromPermaLink(String body) {
        return setStringContent(Post.BodyFromPermaLinkLabel, body);
    }

    public Content setEnclosure(String enc) {
        return setStringContent(Post.EnclosureLabel, enc);
    }

    public String getEnclosure() {
        return getStringContent(Post.DocPartsDomain, Post.EnclosureLabel);
    }

    /**
     * The state of the item.
     *
     * @return a state constant (one of the constants on app.Constants)
     */
    public int getState() {
        return state;
    }

    public void setState(int st) {
        state = st;
    }

    /**
     * Add an additional comment (follow-on Post) to this Post.
     *
     * @param session Database session
     * @param comment the new comment, appended to the listFiles of other comments.
     */
    public void addComment(DMSSession session, Post comment) {
        Iterator itr = session.getIteratorFromQueryArgs("select max(entryOrdinal) from Post where parent= :a", this);
        int max = 0;
        if (itr.hasNext()) {
            Integer mv = (Integer) itr.next();
            if (mv != null) {
                max = mv;
            }
        }

        // so we want to add the comment with a higher entryOrdinal
        comment.setParent(this);
        comment.setEntryOrdinal(max + 1);
    }

    private Content setStringContent(String label, String value) {
        // using the label as the filename - will only work as long as labels are unique
        this.removeAllContentWithDomainValue(Post.DocPartsDomain, label);
        if (value != null) {
            return setStringContent(Post.DocPartsDomain, label, label, value);
        } else {
            return null;
        }
    }

    public void serialize(HTObjectOutputStream os)
            throws IOException, StoreException {
        os.writeInt(getSerializationVersion());
        super.serialize(os);
        os.writeString(destinationUrl);
        os.writeString(originalAuthor);
        os.writeInt(entryOrdinal);
        os.writeString(moreLink);
        os.writeLong(titleHash);
        os.writeVersionedObject(parent);
        //os.writeSetOfBaseType(_rssFeedIns);

    }

    public void deserialize(HTObjectInputStream os)
            throws IOException, ClassNotFoundException, StoreException {
        int version = os.readInt();
        super.deserialize(os);
        switch (version) {
            case 2:
                destinationUrl = os.readString();
                originalAuthor = os.readString();
                degree = os.readInt();
                type = os.readInt();
                siteLink = os.readString();
            case 1:
                entryOrdinal = os.readInt();
                moreLink = os.readString();
                titleHash = os.readLong();
                parent = (Post) os.readVersionedObject();

                //os.readSetOfHTSerializable(_rssFeedIns);
        }
    }

    public boolean upgradeAllInstances(long currentSchemaVersion) {
        switch ((int) currentSchemaVersion) {      //update conversationtracker.Post set refCount=0;
            case 1:
                //upgrade 1-2
                JDBCUtils.performSimpleJDBCUpdate("update Post set refCount=0");
                return true;
            default:
                return false;
        }
    }

    public enum PostType {
        RSSFeedType("RssFeedPost", 1),
        PostReferencedPost("PostReferencedPost", 2),
        CategoryExamplePost("CategoryExamplePost", 10);

        private static HashMap<String, PostType> s_byShortName;
        private static TIntObjectHashMap m_ords;
        private String m_name;
        private int m_ordinal;

        PostType(String name, int ord) {
            m_name = name.toLowerCase();
            m_ordinal = ord;
            setMapEntry(this);

        }

        public static PostType getTypeName(String name) {
            return s_byShortName.get(name.toLowerCase());
        }

        public static int size() {
            return s_byShortName.size();
        }

        public static PostType getByOrdinal(int ord) {
            return (PostType) m_ords.get(ord);
        }

        private static void setMapEntry(PostType filter) {
            if (s_byShortName == null) {
                s_byShortName = new HashMap<String, PostType>();
            }
            s_byShortName.put(filter.getName(), filter);

            if (m_ords == null) {
                m_ords = new TIntObjectHashMap();
            }
            m_ords.put(filter.m_ordinal, filter);
        }

        public String getName() {
            return m_name;
        }

        public int getOrdinal() {
            return m_ordinal;
        }
    }

    /**
     * View class enumerating which fields to show when listing
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "PostListView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class PostListView {
        public abstract String getTitle();
    }

    /**
     * View class enumerating which fields to show when editing
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "PostEditView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class PostEditView {
        public abstract String getTitle();

        public abstract String getBody();

        public abstract String getMoreLink();

        public abstract int getState();
    }

    /**
     * View class enumerating which fields to show when editing
     */
    @com.hitorro.util.typesystem.annotation.TypeClassMetaInfo(shortTypeName = "PostInspectView",
            isView = true,
            isPersisted = false,
            schemaVersion = SerializationVersion)
    public abstract static class PostInspectView {
        public abstract String getTitle();

        public abstract String getBody();

        public abstract String getMoreLink();

        public abstract int getState();

        public abstract Set<Content> getContents();
    }
}
