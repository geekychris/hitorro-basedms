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
package com.hitorro.basedms.servlets;

import com.hitorro.base.objects.Content;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.util.core.string.Fmt;
import com.hitorro.util.typesystem.BaseSession;

import jakarta.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**

 * Read the content of a content object....only works on conten
 */
public class ContentSystemFileReaderServlet extends HttpServlet {
    public static final String DownloadKey = "/download";
    /**
     *
     */
    private static final long serialVersionUID = -6313282113050658932L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String t = request.getPathInfo();
        String guid;
        boolean downloadHeader = false;
        if (t.startsWith(DownloadKey)) {
            guid = t.substring(DownloadKey.length());
            downloadHeader = true;
        } else {
            guid = t;
        }
        if (guid == null || guid.charAt(0) != '/') {
            // can't find it
            response.sendError(404);
            return;
        }
        int index = guid.indexOf(".");
        guid = guid.substring(1, index);
        BaseSession session = null;
        try {
            session = DMSSessionFactory.getFactory().getSession();
            Object o = session.getObjectFromGuid(guid);
            Content cont = null;
            if (o == null) {
                response.sendError(404);
                return;
            }
            if (o instanceof Content) {
                cont = (Content) o;
            } else {
                response.sendError(404);
                return;
            }

            int buffsize = 1024 * 32;
            byte[] mybuff = new byte[buffsize];
            if (downloadHeader) {
                response.setHeader("Content-Disposition", Fmt.S("attachment; filename=\"%s\";", cont.getOriginalFileName()));
            }
            BufferedInputStream in = new BufferedInputStream(cont.getContent(), buffsize);
            BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream(), buffsize);
            while (true) {
                int nread = in.read(mybuff, 0, buffsize);
                if (nread > 0) {
                    out.write(mybuff, 0, nread);
                }
                if (nread < buffsize) {
                    break;
                }
            }
            out.flush();
            in.close();

        } catch (Exception exc) {
            response.sendError(404, "internal error");
        }
    }
}