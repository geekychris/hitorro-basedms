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
package com.hitorro.basedms.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.hitorro.util.core.Log;

import java.util.Properties;


public class SshSession {
    public static final int _sessionMaxTimeout = 1000 * 60 * 100;   //  millis * secs * mins
    private JSch _jsch;
    private Session _jschSession;
    private String _user;
    private String _password;
    private String _host;
    private int _port = 22;                                         //   default tcp port
    private Boolean _trust = true;                                  //   todo: wide-open security within lan.  tighten up via .rhosts? with keys?


    public SshSession(String user, String password, String host, int port, boolean trust) {
        _user = user;
        _password = password;
        _host = host;
        _port = port;
        _trust = trust;
    }


    public static SshSession getSshSession(String user, String password, String host, int port, boolean trust) {
        SshSession sshSession = new SshSession(user, password, host, port, trust);
        Session jschSession = null;
        JSch jsch = new JSch();


        try {
            jschSession = jsch.getSession(user, host, port);

            /*   set user-specific info on session.   */
            jschSession.setUserInfo(new SshUserInfo(password, trust));

            /*   set session properties: compression; host key.   */
            Properties config = new java.util.Properties();
            config.put("compression.s2c", "none");
            config.put("compression.c2s", "none");
            config.put("StrictHostKeyChecking", "no");
            jschSession.setConfig(config);

            /*   set session timeout on connection.   */
            jschSession.connect(SshSession._sessionMaxTimeout);

            sshSession.setJsch(jsch);
            sshSession.setJschSession(jschSession);

            Log.ssh.debug("connect ssh/jsch session: user: %s; host %s; port %s; trust %s.", user, host, port, trust);
        } catch (JSchException e) {
            if (jschSession != null) {
                jschSession.disconnect();

                Log.ssh.debug("Disconnect ssh/jsch session: user: %s; host %s.", user, host);

            }
            Log.ssh.error("error initializing SshSession's underlying JschSession %s %e", e, e);
        }

        return sshSession;
    }


    public boolean disconnect() {
        if (_jschSession != null) {
            _jschSession.disconnect();
            Log.ssh.debug("disconnect ssh/jsch session: user: %s; host %s.", _user, _host);
            return !_jschSession.isConnected();
        }

        return true;
    }


    public JSch getJsch() {
        return _jsch;
    }

    private void setJsch(JSch jsch) {
        _jsch = jsch;
    }

    public Session getJschSession() {
        return _jschSession;
    }

    private void setJschSession(Session jschSession) {
        _jschSession = jschSession;
    }

}