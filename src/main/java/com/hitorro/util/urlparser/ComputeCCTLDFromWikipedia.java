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
package com.hitorro.util.urlparser;

import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.basefile.tools.BaseFileUtil;
import com.hitorro.util.core.Console;
import com.hitorro.util.core.GenericKeyValue;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.core.string.StringUtil;
import com.hitorro.util.html.HTMLPage;
import com.hitorro.util.html.HTMLPageFetcher;
import com.hitorro.util.html.Link;
import com.hitorro.util.io.FileUtil;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates a listFiles of TLD's and second level domains that can be used to determine how to stem a url to a meaningfull
 * domain without too much "fluff".
 */
public class ComputeCCTLDFromWikipedia {
    public static String[] RemoveAnthingWith = {"www", "regi", "examp", "goog", "yout", "yahoo", "visa", "example", "nike", "blow", "mobil", "#", "%"};

    private TreeSet<String> good = new TreeSet();
    private List<GenericKeyValue<String, String>> rejected = new ArrayList();

    private static HTMLPageFetcher getFetcher() {
        HTMLPageFetcher f;

        f = new HTMLPageFetcher();
        f.setHttpTimeout(1000);
        return f;
    }

    public void dumpToFile() {
        BaseFile outFile = FileFileSystem.Root.getFile("/hthome/cctld.txt");
        dump(outFile);
    }

    public void dump(BaseFile outputFile) {
        PrintWriter output = BaseFileUtil.bf2utf8printwriter.apply(outputFile);
        TreeSet<String> s = compute();
        Console.println("got %s elements:", s.size());
        Console.println();
        for (String t : s) {
            output.println(t);
        }
        output.flush();
        output.close();
        for (GenericKeyValue gkv : rejected) {
            Console.println("Rejected: %s, because: %s", gkv.getKey(), gkv.getValue());
        }
    }

    public TreeSet<String> compute() {
        return compute("http://en.wikipedia.org/wiki/Cctld");
    }

    public TreeSet<String> compute(String rootUrl) {
        good.clear();
        rejected.clear();
        HTMLPageFetcher fetch = getFetcher();
        HTMLPage page = fetch.fetchPage(rootUrl);
        TreeSet<String> set = new TreeSet();
        if (page != null) {
            try {
                List<Link> links = page.getLinks();
                for (Link l : links) {
                    String url = l.getUrl();
                    String name = FileUtil.getFileName(url);
                    if (name.startsWith(".")) {
                        set.add(url);
                    }
                }
            } catch (IOException e) {

            }
        }
        for (String url : set) {
            try {
                getSubParts(url, fetch, good, RemoveAnthingWith);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return good;
    }

    private void getSubParts(String url, HTMLPageFetcher fetcher, Set<String> set, String testRemove[]) throws IOException {
        TreeSet<String> temp = new TreeSet();
        String name = FileUtil.getFileName(url);
        if (name.startsWith(".")) {
            String nameP = name.substring(1);
            set.add(nameP);
        }

        HTMLPage page = fetcher.fetchPage(url);

        if (page != null) {
            String body = page.getParser().getBodyText();
            StandardAnalyzer analyzer = new StandardAnalyzer();
            StringReader reader = new StringReader(body);
            TokenStream ts =
                    analyzer.tokenStream("", reader);
            CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);

            while (ts.incrementToken()) {
                String tok = termAttribute.toString();
                if (tok.endsWith(name)) {
                    if (StringUtil.isAlphaPunkt(tok)) {
                        int i = StringUtil.containsAny(tok, testRemove, true);
                        if (i != -1) {
                            rejected.add(new GenericKeyValue(tok, Fmt.S("contains %s", testRemove[i])));
                        } else {
                            temp.add(StringUtil.reverseCanon(tok));
                        }
                    }
                }
            }
            ts.close();
        }

        set.addAll(temp);
    }
}




