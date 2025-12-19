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
package com.hitorro.basedms.rss;


import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.hitorro.base.objects.ExternalContent;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FeedRetrieval {
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String GZIP = "gzip";
    private static final String USER_AGENT = "user-agent";
    private static final String USER_AGENT_NAME = "HiTorro Crawler";
    private static final int TIMEOUT = 30000; //30 secs
    private static final String IF_MODIFIED = "If-Modified-Since";
    private static final String LAST_MODIFIED = "Last-Modified";


    /**
     * Read an RSS feed. Safely read an RSS feed, handling timeouts and honoring a lastModified (the last time we read)
     * date.
     *
     * @param persistBean - input, we use the URL and lastModified date
     * @return feedBean - output, with all values set, including a listFiles of ItemBeans for the items read
     */
    public FeedFetcherDatum getFeed(FeedFetcherDatum persistBean) {
        URLConnection connection = null;
        HttpURLConnection httpURLConnection = null;
        FeedFetcherDatum feedBean = new FeedFetcherDatum();
        SyndFeed syndFeed = null;

        if (persistBean.getURL() == null) {
            Log.rss.debug("Null param passed to getFeed");
            return null;
        }

        try {
            connection = persistBean.getURL().openConnection();
            //set timeout
            connection.setConnectTimeout(TIMEOUT);
            //set request headers
            setRequestHeaders(connection, persistBean.getLastModified());
            //connection
            connection.connect();
            httpURLConnection = (HttpURLConnection) connection;
            //check response
            feedBean.setResponseCode(httpURLConnection.getResponseCode());
            //200
            if (feedBean.getResponseCode() == HttpURLConnection.HTTP_OK) {
                //retrieve
                syndFeed = getStream(connection);
                feedBean = populateFeedBean(syndFeed, persistBean.getURL().toString());
                //grab lastModified if present
                String lm = httpURLConnection.getHeaderField(LAST_MODIFIED);
                feedBean.setLastModified(lm);

                //304
            } else if (feedBean.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.rss.debug("304 returned for URL %s", persistBean.getURL().toString());
            } else {
                Log.rss.error("Unable to connect - URL %s, HTTP Response code: %s",
                        persistBean.getURL().toString(), feedBean.getResponseCode());
            }

        } catch (SocketTimeoutException e) {
            Log.rss.error("Connection timed out for %s. Exception: %s", persistBean.getURL().toString(), e);
        } catch (IOException e) {
            Log.rss.error("Connection fails for %s. Exception: %s", persistBean.getURL().toString(), e);
        }
        return feedBean;
    }

    /**
     * Retrieve contents of feed via stream
     *
     * @param connection
     */
    protected SyndFeed getStream(URLConnection connection) {
        InputStream inputStream = null;
        SyndFeed syndFeed = null;
        SyndFeedInput input = new SyndFeedInput();
        BufferedInputStream buffInputStream = null;
        try {
            inputStream = connection.getInputStream();
            //gzip encoding?
            if ((GZIP).equalsIgnoreCase(connection.getContentEncoding())) {
                // handle gzip encoded content
                buffInputStream = new BufferedInputStream(new GZIPInputStream(inputStream));
            } else {
                buffInputStream = new BufferedInputStream(inputStream);
            }
            //pull feed, using character encoding defined by the publisher (if it exists)
            syndFeed = input.build(new BufferedReader(new InputStreamReader(buffInputStream,
                    ResponseHandler.getCharacterEncoding(connection))));

            buffInputStream.close();
        } catch (IOException e) {
            Log.rss.error("IOException encountered for feed %s, error: %s",
                    connection.getURL().toString(), e);
        } catch (FeedException fe) {
            Log.rss.error("FeedException encountered for feed %s, error: %s",
                    connection.getURL().toString(), fe);
        }

        return syndFeed;
    }

    /**
     * Method to set request header parameters.
     *
     * @param connection
     */
    protected void setRequestHeaders(URLConnection connection, String lastModified) {
        //set gzip
        connection.setRequestProperty(ACCEPT_ENCODING, GZIP);

        // set the user agent
        connection.addRequestProperty(USER_AGENT, USER_AGENT_NAME);
        //lastModified, if exists
        if (lastModified != null) {
            connection.addRequestProperty(IF_MODIFIED, lastModified);
        }

        //etag
    }

    /**
     * Method to populate feedBean object from syndFeed
     *
     * @param syndFeed
     * @return
     */
    protected FeedFetcherDatum populateFeedBean(SyndFeed syndFeed, String srcUrl) {
        FeedFetcherDatum feedBean = setFeed(syndFeed);
        if (syndFeed != null) {
            feedBean.setItemBeans(setItems(syndFeed.getEntries(), srcUrl));
        }
        return feedBean;
    }


    /**
     * @param syndFeed
     * @return
     */
    protected FeedFetcherDatum setFeed(SyndFeed syndFeed) {
        FeedFetcherDatum feedBean = new FeedFetcherDatum();
        if (syndFeed == null) {
            return feedBean;
        }

        //apply
        feedBean.setAuthors(syndFeed.getAuthors());
        List l = syndFeed.getCategories();
        List<String> cats = new ArrayList<String>();
        for (Object o : l) {
            SyndCategory sc = (SyndCategory) o;
            cats.add(sc.getName());

        }
        feedBean.setCategories(cats);
        feedBean.setCopyright(syndFeed.getCopyright());
        feedBean.setDescription(syndFeed.getDescription());
        feedBean.setEncoding(syndFeed.getEncoding());
        //image?
        if (syndFeed.getImage() != null) {
            feedBean.setImageURL(syndFeed.getImage().getUrl());
        }
        feedBean.setLink(syndFeed.getLink());
        feedBean.setPubDate(syndFeed.getPublishedDate());
        feedBean.setTitle(syndFeed.getTitle());

        return feedBean;
    }

    /**
     * @param categories
     * @return
     */
    protected List<String> buildCategories(List categories) {
        List<String> categoryList = new ArrayList<String>();
        SyndCategory syndCategory = null;

        for (Iterator it = categories.iterator(); it.hasNext(); ) {
            syndCategory = (SyndCategory) it.next();
            categoryList.add(syndCategory.getName());
        }

        return categoryList;
    }

    /**
     * @param syndEntries
     * @return
     */
    protected List<ItemBean> setItems(List syndEntries, String srcUrl) {
        List<ItemBean> items = new ArrayList<ItemBean>();
        ItemBean itemBean = null;
        SyndEntry syndEntry = null;
        SyndContent syndContent = null;

        for (Iterator it = syndEntries.iterator(); it.hasNext(); ) {
            syndEntry = (SyndEntry) it.next();
            itemBean = new ItemBean();
            List auths = syndEntry.getAuthors();
            List<String> authors = new ArrayList();
            if (auths != null) {
                for (Object a : auths) {
                    SyndPerson pers = (SyndPerson) a;
                    String name = pers.getName();
                    authors.add(name);
                }
            }

            itemBean.setAuthors(authors);
            itemBean.setCategories(buildCategories(syndEntry.getCategories()));
            //description?
            if (syndEntry.getDescription() != null) {
                itemBean.setDescription(syndEntry.getDescription().getValue());
            }
            //content:encoded? (requires Rome 0.9)
            if (syndEntry.getContents().size() > 0) {
                syndContent = syndEntry.getContents().get(0);
                itemBean.setContent(syndContent.getValue());
                itemBean.setContentType(syndContent.getType());
            }
            //enclosure?
            if (syndEntry.getEnclosures().size() > 0) {
                for (Object o : syndEntry.getEnclosures()) {
                    SyndEnclosure se = (SyndEnclosure) o;
                    ExternalContent enc = new ExternalContent();
                    enc.setUrl(se.getUrl());
                    enc.setType(se.getType());
                    enc.setPlayLength(se.getLength());
                    //only grab the first one
                    itemBean.getEnclosures().add(enc);
                }
            }
            itemBean.setLink(syndEntry.getLink());
            itemBean.setSrcUrl(srcUrl);
            itemBean.setPubDate(syndEntry.getPublishedDate());
            itemBean.setTitle(syndEntry.getTitle());
            items.add(itemBean);
        }

        return items;
    }

}


