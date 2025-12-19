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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Basic Rss reader.
 */
public class RssReadImpl implements RssRead {

    public FeedFetcherDatum read(String url, String lastModified, RssItemStore store) {
        FeedFetcherDatum output = null;
        try {
            FeedFetcherDatum input = new FeedFetcherDatum();
            input.setURL(new URL(url));
            input.setLastModified(lastModified);
            FeedRetrieval fr = new FeedRetrieval();

            // read the feed
            output = fr.getFeed(input);
            List<ItemBean> items = output.getItemBeans();
            if (items == null) {
                return output;
            }

            // hand the items back into the store
            /*
            int size = items.size();
            for (int i = size - 1; i >= 0; i--)
            {
                store.store(items.get(i), url);
            }
             */
            for (ItemBean ib : items) {
                store.store(ib, url);
            }
        } catch (MalformedURLException exc) {
            Log.util.error("Exception ", exc);
        }

        return output;
    }

}
