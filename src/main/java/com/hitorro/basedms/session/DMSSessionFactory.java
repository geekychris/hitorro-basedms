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
package com.hitorro.basedms.session;

import com.hitorro.util.commandandcontrol.MultiRowResponse;
import com.hitorro.util.commandandcontrol.Response;
import com.hitorro.util.commandandcontrol.ResponseShape;
import com.hitorro.util.core.Log;
import com.hitorro.util.typesystem.BaseSession;
import com.hitorro.util.typesystem.BaseSessionFactory;
import com.hitorro.util.typesystem.SessionException;

import java.util.LinkedList;
import java.util.List;

/**
 */
public class DMSSessionFactory extends BaseSessionFactory<BaseSession> {
    private static ResponseShape header = initHeader();
    private List<BaseSession> m_sessions = new LinkedList<BaseSession>();

    static ResponseShape initHeader() {
        ResponseShape header = new ResponseShape("dmssessions", "session");
        header.addHeader("CreateTime", "ThreadName", "ThreadGroup", "Stack");
        header.addHeaderShortNames("CreateTime", "ThreadName", "ThreadGroup", "Stack");
        header.addRowTypes(String.class, String.class);
        return header;
    }


    public static void commitAndFlush(BaseSession closeMe) {
        if (closeMe != null) {
            closeMe.commit();
            closeMe.flush();
        }
    }

    void reRegisterAttatchSession(DMSSession sess) {
        Log.dmssession.debug("Session %s is being re-atatched", sess);
        registerSession(sess);
    }

    synchronized void dumpDebugInfo(Response response, int stackDepth) {
        response.setResponseShape(header);
        MultiRowResponse mrm = response.getMultiRowResponse();
        for (BaseSession session : m_sessions) {
            mrm.add(0, session.getReadableCreateTime());
            mrm.add(1, session.getThreadName());
            mrm.add(2, session.getGroupName());
            mrm.addThrowable(3, session.getThrowable(), stackDepth, 2);
            response.addMultiRowResponse(mrm);
        }
    }

    synchronized DMSSession registerSession(DMSSession sess) {
        m_sessions.add(sess);
        sess.setThreadInfo(Log.dmssession.isDebugEnabled());

        return sess;
    }

    synchronized BaseSession releaseSession(BaseSession sess) {
        if (m_sessions.remove(sess)) {
            return sess;
        }
        return null;
    }

    public synchronized int getSessionCount() {
        return m_sessions.size();
    }

    public DMSSession getSession() {
        return registerSession(new DMSSession());
    }

    public DMSSession getCachedDMSSession() {
        return registerSession(new DMSSession(true));
    }

    public DMSSession getCachedDMSSession(String key) throws SessionException {
        return registerSession(new DMSSession(key, true));
    }

    public DMSSession getDMSSession(String key) throws SessionException {
        return registerSession(new DMSSession(key, false));
    }

    public void close(BaseSession closeMe) {
        if (closeMe != null) {
            closeMe.close();
            releaseSession(closeMe);
        }
    }

    public void rollbackCloseSession(BaseSession closeMe) {
        if (closeMe != null) {
            getFactory().rollbackClose(closeMe);
        }
    }

    public void disconnectSession(BaseSession closeMe) {
        if (closeMe != null) {
            closeMe.disconnectSession();
            releaseSession(closeMe);
        }
    }

    public void rollbackClose(BaseSession closeMe) {
        if (closeMe != null) {
            closeMe.rollbackAndClose();
            releaseSession(closeMe);
        }
    }

    public void commitAndClose(BaseSession closeMe) {
        if (closeMe != null) {
            closeMe.commit();
            closeMe.close();
            releaseSession(closeMe);
        }
    }
}
