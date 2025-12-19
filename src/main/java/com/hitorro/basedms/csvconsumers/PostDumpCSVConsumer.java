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
package com.hitorro.basedms.csvconsumers;

import com.hitorro.base.objects.Forum;
import com.hitorro.base.objects.Post;
import com.hitorro.base.objects.RssFeedIn;
import com.hitorro.base.objects.User;
import com.hitorro.basedms.PostUtil;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.io.FileUtil;
import com.hitorro.util.io.csv.CSVReader;
import com.hitorro.util.job.Job;
import com.hitorro.util.job.JobExecutionResult;
import com.hitorro.util.job.JobParameters;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Read posts from the post dump file (written by WritePostsToCSVJob).
 */
public class PostDumpCSVConsumer extends Job {
    public static final String TitleColumn = "title";
    public static final String LinkColumn = "link";
    public static final String DateColumn = "authordate";
    public static final String FeedNameColumn = "feedname";
    // note that the body text is the text from RssFeed (or as edited), not pulled from the permalink
    public static final String BodyColumn = "body";

    public static final String DateFormat = "yyyy-MM-dd HH:mm";


    /**
     * This job must run on the db leader.
     *
     * @return
     */
    public boolean getMustRunOnDBLeader() {
        return true;
    }

    public String getName() {
        return "Read Posts from dump";
    }

    public boolean needsSession() {
        return false;
    }

    public JobExecutionResult doAction(JobParameters parameters)
            throws JobExecutionException {
        /*
            Read each line of the dump csv file.  For each line queue a job to re-read the post
            content.  This will get the content, links, and so on.
         */

        try {
            File infile = new File(Env.getHome(), StringUtil.strcat("data", File.separator, "posts.csv"));
            CSVReader reader = new CSVReader(FileUtil.getBufferedReaderFromFile(infile, StringUtil.UTF8Encoding));
            String[] header = reader.getColumnNames();
            Map<String, Integer> map = MapUtil.getMapColumnNameToIndexPosition(header, true);

            SimpleDateFormat dfmt = new SimpleDateFormat(DateFormat);
            ParsePosition pos = new ParsePosition(0);
            Forum lastForum = null;
            RssFeedIn lastFeed = null;
            User author = null;
            BaseSessionFactory factory = DMSSessionFactory.getFactory();
            BaseSession session = factory.getSession();

            int count = 0;
            String row[];
            while ((row = reader.getNextRow()) != null) {
                String title = MapUtil.getColumnFromColumMap(TitleColumn, map, row);
                String link = MapUtil.getColumnFromColumMap(LinkColumn, map, row);
                String dateText = MapUtil.getColumnFromColumMap(DateColumn, map, row);
                String feedName = MapUtil.getColumnFromColumMap(FeedNameColumn, map, row);
                String body = MapUtil.getColumnFromColumMap(BodyColumn, map, row);

                pos.setIndex(0);

                Date authoredDate = dfmt.parse(dateText, pos);
                if (authoredDate == null) {
                    Log.scheduledJobs.warn("Could not parse date |%s| on post %s", dateText, link);
                    continue;
                }
                if (body == null || body.length() < 1) {
                    // allow the empty body
                    body = null;
                    /*
                    Log.scheduledJobs.warn("Empty body for %s", link);
                    continue;
                    */
                }

                // check feed

                if (feedName != null) {
                    if (lastForum == null || !lastForum.getName().equals(feedName)) {
                        lastForum = Forum.getForumForName(session, feedName);
                        lastFeed = RssFeedIn.getRssFeedInForName(session, feedName);
                        author = (lastForum != null) ? lastForum.getUser(session) : null;
                    }
                    if (lastForum == null || lastFeed == null) {
                        Log.scheduledJobs.warn("Could not get feed or forum %s on %s", feedName, link);
                        continue;
                    }
                } else {
                    Log.scheduledJobs.warn("Null feed name on %s", link);
                    continue;
                }

                // see if we already have Post
                long hash = Post.calculateIdentityHash(link);
                Iterator itr = session.getIteratorFromQueryArgs("select guid from VersionableObject where identityHash= :a", hash);
                if (itr.hasNext()) {
                    // we've already got this post - skip it
                    continue;
                }


                HTMLPage page = new HTMLPage();
                page.setUrl(link);
                // wrap body in html tags to make the html parser happy
                page.setSourceFromPlainText(body);
                page.setPublishedDate(authoredDate.getTime());
                List<Forum> forums = new ArrayList<Forum>();
                if (lastForum != null) {
                    forums.add(lastForum);
                }
                PostUtil.createNewPost(session, authoredDate,
                        link, title,
                        page, page.getSource(), false, false,
                        forums, author,
                        lastFeed.getSiteUrl(), 1,
                        Post.PostType.RSSFeedType.getOrdinal(), null, null, null, null, null);


                if (++count % 25 == 0) {
                    session.commit();
                }

                if (count % 100 == 0) {
                    System.out.println("Run through " + count + " posts");
                    factory.close(session);
                    session = factory.getSession();
                    lastForum = Forum.getForumForName(session, feedName);
                    lastFeed = RssFeedIn.getRssFeedInForName(session, feedName);
                    author = (lastForum != null) ? lastForum.getUser(session) : null;
                }
            }

            session.commit();
            factory.close(session);
            System.out.println("Finished reading in posts...");

            reader.close();
        } catch (IOException exc) {
            throw new JobExecutionException(exc);
        }


        return null;
    }
}
