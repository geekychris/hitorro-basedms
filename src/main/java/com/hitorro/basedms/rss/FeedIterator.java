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

import com.hitorro.util.core.iterator.BaseArrayListIterator;

import java.net.URL;

/**
 * FeedIterator -> Map2bag ->filter_id_prior->collect_ids->store
 */
public class FeedIterator extends BaseArrayListIterator<ItemBean> {
    private FeedFetcherDatum output;

    public FeedIterator(URL url, String lastModified) {
        super();
        init(url, lastModified);
    }

    public String getLastModified() {
        return output.getLastModified();
    }

    private void init(URL url, String lastModified) {
        // make a "persist" bean of one of our feeds
        FeedFetcherDatum input = new FeedFetcherDatum();

        input.setURL(url);
        input.setLastModified(lastModified);
        // leave last-modified as null for now
        FeedRetrieval fr = new FeedRetrieval();

        output = fr.getFeed(input);
        e = output.getItemBeans();
    }
}
