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
package com.hitorro.basedms;


import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.queue.QueueUtil;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.ListUtil;
import com.hitorro.util.core.hash.FPHash64;
import com.hitorro.util.core.map.IdentityMap;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.core.map.TroveMapUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.html.HTMLParser;
import com.hitorro.util.html.Link;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.json.keys.FileProperty;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.urlparser.URLUtil;
import com.hitorro.util.urlparser.UrlCursor;
import org.w3c.dom.DOMException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 */
public class PostUtil {
    public static final int MaxUrlLength = 1024;
    public static final IntegerProperty MaxDepth = new IntegerProperty("dirfileprocessor.posts.maxdepth", "Depth of crawling for post fetching", 2);


    private static final com.hitorro.util.html.constraint.UrlStartsWithLinkConstraint s_HttpOnlyConstraint = new com.hitorro.util.html.constraint.UrlStartsWithLinkConstraint("http://");
    private static FileProperty ExcludeSitesFile = new FileProperty("", "excluded sites listFiles", "${HT_BIN}/config/excludedcrawlsites.txt");

    private static TLongIntHashMap s_excludeSites = new TLongIntHashMap();
    private static boolean s_fetchedExcludeList = TroveMapUtil.loadTextFileAsFP64(ExcludeSitesFile.apply(),
            s_excludeSites, 1);
    private static com.hitorro.util.html.constraint.UrlFromSite s_urlFromSite = new com.hitorro.util.html.constraint.UrlFromSite(s_excludeSites);
    private static com.hitorro.util.html.constraint.NotLinkConstraint s_notFromSite = new com.hitorro.util.html.constraint.NotLinkConstraint(s_urlFromSite);

    private static IdentityMap<String> s_excludedFileExtensions = new IdentityMap<String>();
    private static FileProperty ExcludedFileExtensions = new FileProperty("", "excluded file extension", "${HT_BIN}/config/fileextensionsnottocrawl.txt");

    private static boolean s_loadedExcludedFileExtension = MapUtil.loadTextFileIntoIdentityMap(ExcludedFileExtensions.apply(),
            s_excludedFileExtensions);
    private static com.hitorro.util.html.constraint.UrlLengthLessThan s_maxSize = new com.hitorro.util.html.constraint.UrlLengthLessThan(1024);

    /*
    Looking for duplicates having same identity hash:
    SELECT guid, identityHash, count(*) as fred FROM conversationtracker.VersionableObject S  group by identityHash having fred > 1

    select * from conversationtracker.VersionableObject s, conversationtracker.Post p where identityHash=9196810939230640933 and s.system_id=p.system_id
     */
    public static com.hitorro.base.objects.Post createNewPost(BaseSession session,
                                                              Date dt,
                                                              String link,
                                                              String title,
                                                              HTMLPage page,
                                                              String body,
                                                              boolean bodyFromPermaLink,
                                                              boolean useLinks,
                                                              List<com.hitorro.base.objects.Forum> containers,
                                                              com.hitorro.base.objects.User author,
                                                              String siteDomain,
                                                              int degree,
                                                              int type, com.hitorro.base.objects.Post item,
                                                              List<com.hitorro.base.objects.ExternalContent> enclosures,
                                                              String authorOrig, String feedContact, List<String> cats) {
        boolean newItem = false;
        if (item == null) {
            item = new com.hitorro.base.objects.Post();
            newItem = true;
        }
        item.setDegree(degree);
        item.setType(type);
        item.setEntryOrdinal(0);
        item.setSiteLink(siteDomain);
        item.setAuthor(author);

        String dest = page.getDestinationUrl();
        if (!StringUtil.nullOrEmptyString(dest)) {
            item.setDestinationUrl(URLUtil.cleanupUrlAndCutToLength(dest, MaxUrlLength));
        }

        LinkSet linkSet = null;
        if (StringUtil.nullOrEmptyOrBlankString(authorOrig) || StringUtil.stringContainsIgnoreCase(authorOrig, "unknown")) {
            if (author != null) {
                authorOrig = author.getName();
            }
        }

        if (StringUtil.nullOrEmptyOrBlankString(authorOrig) || StringUtil.stringContainsIgnoreCase(authorOrig, "unknown")) {
            authorOrig = null;
        }
        item.setOriginalAuthor(authorOrig);

        if (StringUtil.nullOrEmptyOrBlankString(feedContact)) {
            item.setFeedContact(authorOrig);
        } else {
            if (StringUtil.nullOrEmptyString(feedContact)) {
                Log.basedms.debug("feedContact null");
            }
            item.setFeedContact(feedContact);
        }

        if (!ListUtil.nullOrEmpty(cats)) {
            for (String cat : cats) {
                try {
                    if (!StringUtil.nullOrEmptyOrBlankString(cat)) {
                        item.addCategory(com.hitorro.base.objects.Category.HTEXTERNALCATEGORY, cat);
                    }
                } catch (CategoryException e) {
                    Log.basedms.error("Exception %s %e", e, e);
                }
            }
        }
        addEnclosure(enclosures, item, session);
        try {
            HTMLParser hparser = page.getParser();
            //body = page.getSource();
            // set the html represented content before we hack it up.
            // seems to be getting set twice
            //item.setStringContent(Post.DocPartsDomain, Post.BodyHtmlLabel, Post.BodyHtmlLabel, body);

            if (useLinks) {
                // try to get the links for recursive fetching
                linkSet = getLinksNew(page, siteDomain, link);
                item.setReferenceLinksNew(linkSet);
            }

            try {
                String titleT = hparser.getTitleText();
                if (!StringUtil.nullOrEmptyString(titleT)) {
                    // html body title overides anything else.
                    title = titleT;
                } else {
                    boolean flag = true;
                }
            } catch (DOMException dome) {
                Log.basedms.error("Unable to get text from html content, errror %s", dome);
            }

        } catch (IOException exc) {
            // leave description as is
            Log.basedms.error("Unable to parse html content %s %e", exc, exc);
        }

        title = StringUtil.truncateDecodedTextToLengthWithPostText(title, com.hitorro.base.objects.Post.MaxTitleSize, "...", "unknown");

        item.setMoreLink(link);

        item.setTitle(title);
        // XXX TODO we are re-calling this each time we switch state!!
        if (bodyFromPermaLink) {
            item.setBodyFromPermaLink(body);
        } else {
            item.setBody(body);
        }
        item.setState(Constants.ActiveState);
        item.setAuthoredDate(dt);

        String owningContainerGuid = addFolders(containers, item, session);
        String guid = item.getGuid();
        if (newItem) {
            session.persist(item);
        } else {
            session.saveOrUpdate(item);
        }

        if (processLinkSet(useLinks, linkSet, degree, session, item, bodyFromPermaLink, owningContainerGuid)) {

            // commit everything we have up to now

            session.commit();
            return item;
        }
        return null;
    }

    private static boolean processLinkSet(boolean useLinks, LinkSet linkSet, int degree, BaseSession session, com.hitorro.base.objects.Post item, boolean bodyFromPermaLink, String owningContainerGuid) {
        if (useLinks && linkSet != null) {
            recursePosts(degree, linkSet, session, item);
        }
        if (!bodyFromPermaLink) {
            // this is the root post, therefor we prioritize its fetching higher than anything else.
            int priority = 10;
            // we've got bare text - queue a fetch of the permalink text, which will be used to get links
            QueueUtil.enqueueUrlFetchPostGenerator(session, item.getMoreLink(),
                    3,
                    null,
                    owningContainerGuid,
                    1,
                    com.hitorro.base.objects.Post.PostType.RSSFeedType.getOrdinal(), priority,
                    item.getGuid(), false);
        }
        return true;
    }

    private static void addEnclosure(List<com.hitorro.base.objects.ExternalContent> enclosures, com.hitorro.base.objects.Post item, BaseSession session) {
        if (enclosures != null) {
            //item.setEnclosure(enclosure);
            for (com.hitorro.base.objects.ExternalContent c : enclosures) {
                com.hitorro.base.objects.ContentType ct = ContentTypeCache.getCache().getContentTypeByMimeType(c.getType());
                com.hitorro.base.objects.Content cont = null;
                try {
                    cont = item.addContentIfNotNull(c.getUrl(), com.hitorro.base.objects.Post.HTContentAssetTag, com.hitorro.base.objects.Post.EnclosureLabel, ct, c.getPlayLength(), c.getFileLength());
                    session.persist(cont);
                } catch (CategoryException e) {
                    Log.basedms.error("Exception %s %e", e, e);
                }

            }
        }
    }

    private static String addFolders(List<com.hitorro.base.objects.Forum> containers, com.hitorro.base.objects.Post item, BaseSession session) {
        String owningContainerGuid = null;
        if (!ListUtil.nullOrEmpty(containers)) {
            boolean first = true;
            for (com.hitorro.base.objects.Forum container : containers) {
                if (!item.getContainers().contains(container)) {
                    // XXX TODO TEST CODE REMOVE
                    com.hitorro.base.objects.Reference.assertReference(item.getGuid(), container.getGuid(), com.hitorro.base.objects.Reference.ShareReference);
                    // we have to fetch it into this session as it could of been retrieved in a previous iteration of the main loop.
                    String g = container.getGuid();
                    container = (com.hitorro.base.objects.Forum) session.getHTSerializableFromGUID(g);
                    item.addContainer(container);
                    // currently dont differentiate between any forum, they are all shares.
                    com.hitorro.base.objects.Reference.createReference(item, container, com.hitorro.base.objects.Reference.ShareReference, null, session);
                    if (first) {
                        // the first provided container is what we consider the owning container
                        item.setOwningContainer(container);
                        owningContainerGuid = g;

                    }
                    first = false;
                }
            }
        }
        return owningContainerGuid;
    }

    public static boolean recursePosts(int degree, LinkSet linkSet, BaseSession session, com.hitorro.base.objects.Post item) {
        BaseSession fetchSession = DMSSessionFactory.getFactory().getSession();
        fetchSession.setName("PostFetcher");
        TLongLongHashMap m = new TLongLongHashMap();
        try {
            if (linkSet.getLinks().size() == 0) {
                return true;
            }
            if (degree <= MaxDepth.apply()) {
                UrlCursor curs = new UrlCursor();
                for (Link l : linkSet.getLinks()) {
                    String url = URLUtil.cleanupUrlAndCutToLength(l.getUrl(), MaxUrlLength);
                    // we want to ensure we use the same hash
                    long fp = FPHash64.getFP(url);
                    if (m.contains(fp)) {
                        Log.basedms.error("Duplicate hash in link set: %s", fp);
                        continue;
                    }
                    m.put(fp, fp);
                    com.hitorro.base.objects.Post priorPost = com.hitorro.base.objects.Post.getPostByMoreLinkHash(fetchSession, fp);

                    if (priorPost == null) {
                        String tld = UrlCursor.getSiteFromURL(url, curs);
                        com.hitorro.base.objects.Post ni = new com.hitorro.base.objects.Post();
                        // more link is truncated
                        ni.setMoreLink(url);
                        ni.setDegree(degree + 1);
                        ni.setType(com.hitorro.base.objects.Post.PostType.PostReferencedPost.getOrdinal());
                        ni.setSiteLink(tld);


                        session.persist(ni);
                        String ext = FileUtil.getFileExtension(url);
                        if (!StringUtil.nullOrEmptyString(ext)) {
                            ext = ext.toLowerCase();
                            if (s_excludedFileExtensions.contains(ext)) {
                                // found a url with an extension we dont crawl.
                                continue;
                            }
                        }
                        QueueUtil.enqueueUrlFetchPostGenerator(session, url,
                                3,
                                item.getGuid(),
                                null,
                                degree + 1,
                                com.hitorro.base.objects.Post.PostType.PostReferencedPost.getOrdinal(), 0,
                                ni.getGuid(), false);

                        // queue up changes.

                        // but only queue if its not something like a jpg file as we dont want to fetch them

                    }
                }
            }
        } finally {
            DMSSessionFactory.getFactory().rollbackClose(fetchSession);
        }
        return true;
    }

    private static LinkSet getLinksNew(HTMLPage page, String siteDomain, String sourceLink) throws IOException {
        com.hitorro.util.html.constraint.UrlEqualsConstraint eq = new com.hitorro.util.html.constraint.UrlEqualsConstraint(sourceLink);
        long fp = FPHash64.getFP(siteDomain);
        com.hitorro.util.html.constraint.LinkConstraint constraint = new com.hitorro.util.html.constraint.AndLinkConstraint(new com.hitorro.util.html.constraint.NotLinkConstraint(new com.hitorro.util.html.constraint.UrlStartsWithLinkConstraint(siteDomain)),
                s_HttpOnlyConstraint,
                s_notFromSite,
                new com.hitorro.util.html.constraint.NotLinkConstraint(eq),
                s_maxSize);
        List<Link> links = page.getLinkWithConstraint(constraint, Link.URLOnlyComparitor);
        LinkSet linkSet = new LinkSet();

        if (links != null) {
            for (Link l : links) {
                if (sourceLink.equals(l.getUrl())) {
                    Console.println("Link set contains source link");
                }
                if (!StringUtil.startsWithIgnoreCase(l.getUrl(), "http")) {
                    Console.println("Doesnt start with http");
                }
            }

            linkSet.setLinks(links);
        }
        return linkSet;
    }
}
