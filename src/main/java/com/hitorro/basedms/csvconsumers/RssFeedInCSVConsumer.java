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
import com.hitorro.base.objects.RssFeedIn;
import com.hitorro.base.objects.SubjectArea;
import com.hitorro.basedms.db.CSVHibernateLoaderConsumer;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.map.MapUtil;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.typesystem.BaseSession;
import org.hibernate.ObjectNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Reader of CSV files to create RssFeedIn objects. Also creates Forums as a side effect - one per RssFeed. Also
 * connects SubjectArea objects up to the Forums, possibly creating them as well.
 */
public class RssFeedInCSVConsumer extends CSVHibernateLoaderConsumer<RssFeedIn> {
    public static final String NameColumn = "name";
    public static final String UriColumn = "rssurl";
    public static final String SiteUrlColumn = "website";
    public static final String ContactNameColumn = "contactname";
    public static final String ContactEmailColumn = "contactemail";
    public static final String SubjectAreaColumn = "startgroup";

    public static final String ForumNameColumn = "forumname";
    public static final String FeedInPriorityColumn = "priority";
    public static final String FeedTypeColumn = "feedtype";
    private static final String[][] Keys = {{NameColumn, "name"}};
    private Map<String, SubjectArea> _areaMap = new HashMap<String, SubjectArea>();
    //public static final String FrequencyColumn = "frequency";
    private Map<String, Forum> _forumMap = new HashMap<String, Forum>();

    public void start() {
    }

    public Class getPersistingClass() {
        return RssFeedIn.class;
    }

    public String[][] getKeyNameMap() {
        return Keys;
    }

    public boolean add(String[] row, RssFeedIn feed, boolean existsAlready) {
        String contactName = MapUtil.getColumnFromColumMap(ContactNameColumn, m_headerMap, row);
        feed.setContactName(StringUtil.cutToLength(contactName, 40));

        String name = MapUtil.getColumnFromColumMap(NameColumn, this.m_headerMap, row);
        if (name == null || name.length() < 1) {
            name = contactName;
        }
        feed.setName(StringUtil.cutToLength(name, 80));

        feed.setUri(MapUtil.getColumnFromColumMap(UriColumn, m_headerMap, row));

        String priorityString = MapUtil.getColumnFromColumMap(FeedInPriorityColumn, m_headerMap, row);
        int priority = 0;
        if (!StringUtil.nullOrEmptyString(priorityString)) {
            priority = Integer.parseInt(priorityString);
        }

        String feedTypeString = MapUtil.getColumnFromColumMap(FeedTypeColumn, m_headerMap, row);
        int feedType = 0;
        if (!StringUtil.nullOrEmptyString(feedTypeString)) {
            feedType = Integer.parseInt(feedTypeString);
        }

        try {
            //String sfreq = MapUtil.getColumnFromColumMap(FrequencyColumn, columnMap, row);
            //int frequency = Integer.parseInt(sfreq);
            int frequency = 1000;
            int highFrequency = 30;
            if (priority > 1) {
                feed.setReadInMinutes(highFrequency);
            } else {
                feed.setReadInMinutes(frequency);
            }


            feed.setContactEmail(StringUtil.cutToLength(
                    MapUtil.getColumnFromColumMap(ContactEmailColumn, m_headerMap, row), 80));
            feed.setSiteUrl(StringUtil.cutToLength(MapUtil.getColumnFromColumMap(SiteUrlColumn, m_headerMap, row), 255));

            feed.setPriority(priority);
            feed.setForumType(feedType);
            //XXX TODO priority to be set and forumType.
            BaseSession session = getSession();

            // if this is a new feed, make sure there is a forum with the same name
            // otherwise get the associated forum
            Forum forum;
            Forum sharedF = null;


            String sharedForum = MapUtil.getColumnFromColumMap(ForumNameColumn, m_headerMap, row);
            if (!StringUtil.nullOrEmptyString(sharedForum)) {
                // shared forum for those that end with two forums.
                sharedF = getForum(session, sharedForum);
                feed.setSharedForum(sharedF);
            } else {
                feed.setSharedForum(null);
            }

            forum = getForum(session, feed.getName());
            feed.setForum(forum);

            // take care of the subject area
            // we add the feed to the named area.  It is allowed that multiple areas point to a feed
            // so if the feed is already referenced by other areas we leave that alone.
            String areaName = MapUtil.getColumnFromColumMap(SubjectAreaColumn, m_headerMap, row);
            SubjectArea subjectArea = _areaMap.get(areaName);
            if (subjectArea == null) {
                subjectArea = SubjectArea.getSubjectAreaForName(session, areaName);
                if (subjectArea == null) {
                    // need to create it
                    subjectArea = new SubjectArea();
                    subjectArea.setName(areaName);
                    subjectArea.setDisplayName(areaName);
                    subjectArea.setAdapterSource(adapterSource);
                    session.persist(subjectArea);
                }
                _areaMap.put(areaName, subjectArea);
            }
            forum.setArea(subjectArea);
            if (sharedF != null) {
                if (sharedF.getArea() == null) {
                    // only set a subject area if we currently dont have a subject area.
                    sharedF.setArea(subjectArea);
                }
            }
            this.saveOrUpdate(existsAlready, feed);
            return true;
        } catch (ObjectNotFoundException on) {
            Log.hibernate.error("Unale to load rss feed item with name: %s, %s %e", name, on, on);
            throw on;
        }
    }

    private Forum getForum(BaseSession session, String forumNameString) {
        Forum forum;
        forum = Forum.getForumForName(session, forumNameString);
        if (forum == null) {
            forum = _forumMap.get(forumNameString);
            if (forum == null) {
                forum = new Forum();
                forum.setName(forumNameString);
                forum.setAdapterSource(adapterSource);

                session.persist(forum);
                _forumMap.put(forumNameString, forum);
            }
        }
        return forum;
    }

    public void done() {
    }
}
