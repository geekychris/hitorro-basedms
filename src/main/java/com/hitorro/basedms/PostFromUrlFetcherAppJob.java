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

import com.hitorro.base.objects.Forum;
import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.User;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.html.HTMLPageFetcher;
import com.hitorro.util.html.Link;
import com.hitorro.util.html.LinkSet;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.urlparser.UrlCursor;
import org.apache.log4j.Level;
import org.hibernate.exception.GenericJDBCException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2003 - present HiTorro All rights reserved. User: chris Date: Feb 28, 2005 Time: 11:49:03 AM
 */
public class PostFromUrlFetcherAppJob extends Job {

    public static final String PostFromUrlFetcherAppJob = "postfromurlfetcherjob";

    public String getName() {
        return PostFromUrlFetcherAppJob;
    }

    public boolean needsSession() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) {
        PostFromUrlParameters p = (PostFromUrlParameters) parameters;

        boolean dontRetry = false;
        String url = p.getReadUrl();
        if (StringUtil.nullOrEmptyOrBlankString(url)) {
            JobExecutionResult jer = new JobExecutionResult(Level.WARN, "No url provided for the source guid %s", p.getParentGuid());
            jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
            return jer;
        }
        try {
            BaseSession session = getSession();
            Post post = (Post) session.getHTSerializableFromGUID(p.getGuid());
            if (post == null) {
                return doneNotExist(p);
            }
            if (post.getDegree() != 1) {
                LinkSet links = post.getReferenceLinksNew();
                if (links != null) {
                    JobExecutionResult jer = new JobExecutionResult(Level.WARN, "Duplicate request %s, url: %s", p.getParentGuid(), url);
                    jer.setNewEventName("DuplicateRequest", PersistedSerializedObject.CollectionID_QueueItemDone);
                    return jer;
                }
            } else {
                com.hitorro.basedms.Log.httpfetcher.debug("fetching degree 1 item: %s", url);
            }
            HTMLPage page = null;

            HTMLPageFetcher fetcher = getFetcher();


            UrlCursor cur = getCursor();
            if (cur.setUrl(url)) {
                try {
                    // only fetch if it looks like a valid url.
                    page = fetcher.fetchPage(url);
                } catch (IllegalArgumentException iae) {
                    com.hitorro.basedms.Log.httpfetcher.debug("Failed to fetch: %s", url);
                    return reportException(p, url, iae, false);
                } catch (ArrayIndexOutOfBoundsException e) {
                    com.hitorro.basedms.Log.httpfetcher.debug("Failed to fetch: %s", url);
                    return reportException(p, url, e, false);
                }
                com.hitorro.basedms.Log.httpfetcher.debug("Fetched: %s", url);
            }
            int statusCode = fetcher.getCode();

            if (statusCode >= 400 && statusCode <= 407) {
                dontRetry = true;
            }
            if (statusCode >= 500) {
                // server error
                dontRetry = true;
            }
            if (statusCode == 204) {
                // no content (its probably an ad pixel)
                dontRetry = true;
            }


            if (page != null && page.getSource() != null) {
                String uNew = page.getUrl();
                Forum container = null;
                User author = null;
                if (!StringUtil.nullOrEmptyString(p.getContainerGuid())) {
                    container = (Forum) session.getHTSerializableFromGUID(p.getContainerGuid());
                    if (container != null) {
                        author = container.getUser(session);
                    }
                }
                if (container != null && !post.getContainers().contains(container)) {
                    // put this container as we are part of a feed that we didnt know of before.
                    post.addContainer(container);
                }
                List<Link> links = page.getLinks();
                LinkSet set = new LinkSet();
                set.setLinks(links);
                String siteDomain = UrlCursor.getSiteFromURL(url);

                try {

                    List<Forum> forums = new ArrayList<Forum>();
                    if (container != null) {
                        forums.add(container);
                    }
                    com.hitorro.basedms.PostUtil.createNewPost(session, new Date(p.getAuthoredDate()),
                            url, null, page, page.getSource(), true, true,
                            forums, author, siteDomain,
                            p.getDegree(), p.getType(), post, null, null, null, null);
                } catch (org.hibernate.StaleStateException sse) {
                    return reportException(p, url, sse, false);
                } catch (GenericJDBCException ex) {
                    this.getSession().rollback();
                    return reportException(p, url, ex, false);
                }

                return doneSuccess();
            }
        } catch (IOException e) {
            return reportException(p, url, e, false);
        }
        if (p.getRetries() == 0 || dontRetry == true) {
            JobExecutionResult jer = new JobExecutionResult(Level.INFO, "Done");
            jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
            return jer;
        } else {
            p.setRetries(p.getRetries() - 1);
            return new JobExecutionResult(1, p);
        }
    }

    private JobExecutionResult reportException(PostFromUrlParameters p, String url, Exception e, boolean perminent) {
        if (p.getRetries() <= 1) {
            perminent = true;
        }

        JobExecutionResult jer = null;
        if (perminent) {
            jer = new JobExecutionResult(Level.ERROR, "Perminent Error");
            jer.setMessage(Level.ERROR, "Exception attempting to fetch content and save to object %s %s %e", url, e, e);
            p.setRetries(0);
            jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
        } else {
            jer = new JobExecutionResult(1, p);
            jer.setMessage(Level.ERROR, "Exception attempting to fetch content and save to object %s %s %e", url, e, e);
            p.setRetries(p.getRetries() - 1);
        }

        return jer;
    }

    private JobExecutionResult doneSuccess() {
        JobExecutionResult jer = new JobExecutionResult(Level.INFO, "Done");

        // lets not re-assing completed items to a different pso, simply delete the items.
        //jer.setNewEventName("UrlToPostFetchedHTML", PersistedSerializedObject.CollectionID_QueueItemDone);
        return jer;
    }

    private JobExecutionResult doneNotExist(PostFromUrlParameters p) {
        JobExecutionResult jer = null;
        jer = new JobExecutionResult(Level.ERROR, "Perminent Error");
        jer.setMessage(Level.ERROR, "Post does not exist %s", p.getGuid());
        p.setRetries(0);
        jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
        return jer;
    }

    private HTMLPageFetcher getFetcher() {
        HTMLPageFetcher f;

        f = new HTMLPageFetcher();
        f.setHttpTimeout(5000);
        return f;
    }

    private UrlCursor getCursor() {
        UrlCursor c = null;
        c = new UrlCursor();

        return c;
    }
}
