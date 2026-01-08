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
package com.hitorro.basedms.html;

import com.hitorro.base.objects.PersistedSerializedObject;
import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.html.HTMLPageFetcher;
import com.hitorro.util.html.Link;
import com.hitorro.util.html.constraint.AndLinkConstraint;
import com.hitorro.util.html.constraint.LinkConstraint;
import com.hitorro.util.html.constraint.NotLinkConstraint;
import com.hitorro.util.html.constraint.UrlStartsWithLinkConstraint;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.urlparser.UrlCursor;
import org.apache.log4j.Level;

import java.util.List;

/**
 * <p/>
 * Job to fetch html content and put it as a piece of content to the
 */
public class HTMLFetcherAppJob extends Job {
    public static final String HTMLFetcherAppJob = "htmlfetcherappjob";

    public String getName() {
        return HTMLFetcherAppJob;
    }

    public boolean needsSession() {
        return true;
    }

    public JobExecutionResult doAction(JobParameters parameters) {
        HTMLFetcherAppJobParameters p = (HTMLFetcherAppJobParameters) parameters;
        String url = p.getReadUrl();
        if (StringUtil.nullOrEmptyOrBlankString(url)) {
            JobExecutionResult jer = new JobExecutionResult(Level.WARN, "No url provided for the html fetch target guid %s", p.getTargetGuid());
            jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
            return jer;
        }
        HTMLPage page = getPage(url);
        if (page != null && page.getSource() != null) {
            try {
                BaseSession session = getSession();
                VersionableObject so = (VersionableObject) session.getHTSerializableFromGUID(p.getTargetGuid());
                if (so == null) {
                    JobExecutionResult jer = new JobExecutionResult(Level.WARN, "No object found for guid %s", p.getTargetGuid());
                    jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
                    return jer;
                }
                String label = p.getLabel();
                so.setStringContent(Post.DocPartsDomain, label, label, page.getSource(), "text/html");
                session.saveOrUpdate(so);
                UrlCursor curs = getCursor();
                curs.setUrl(url);
                curs.nextToken();
                String tok = curs.getAllToCurrentPos();

                // Only do websites that are not from the same TLD and are http
                LinkConstraint constraint = new AndLinkConstraint(new NotLinkConstraint(new UrlStartsWithLinkConstraint(tok)),
                        new UrlStartsWithLinkConstraint("http://"));
                List<Link> links = page.getLinkWithConstraint(constraint, Link.URLOnlyComparitor);
                /*for (Link l : links)
                {
                    Console.println("Anchor %s, Link: %s", l.getLinkType().name(), l.getUrl());
                }
                Console.println();
                Console.println();*/
                session.commit();

                JobExecutionResult jer = new JobExecutionResult(Level.INFO, "Done");
                jer.setNewEventName("FetchedHTML", PersistedSerializedObject.CollectionID_QueueItemDone);
                return jer;
            } catch (Exception e) {

                p.setRetries(p.getRetries() - 1);
                JobExecutionResult jer = new JobExecutionResult(1, p);
                jer.setMessage(Level.ERROR, "Exception attempting to fetch content and save to object %s %e", e, e);
                return jer;
            }
        }
        if (p.getRetries() == 0) {
            JobExecutionResult jer = new JobExecutionResult(Level.INFO, "Done");
            jer.setNewEventName("FailedHTMLFetches", PersistedSerializedObject.CollectionID_QueueItemDone);
            return jer;
        } else {
            p.setRetries(p.getRetries() - 1);
            return new JobExecutionResult(1, p);
        }
    }

    private HTMLPage getPage(String url) {
        HTMLPageFetcher fetcher = getFetcher();


        UrlCursor cur = getCursor();
        if (cur.setUrl(url)) {
            // only fetch if it looks like a valid url.
            HTMLPage page = fetcher.fetchPage(url);
            return page;
        }
        return null;

    }

    private UrlCursor getCursor() {
        UrlCursor c = null;
        c = new UrlCursor();

        return c;
    }

    private HTMLPageFetcher getFetcher() {
        HTMLPageFetcher f;

        f = new HTMLPageFetcher();
        f.setHttpTimeout(1000);
        return f;
    }
}
