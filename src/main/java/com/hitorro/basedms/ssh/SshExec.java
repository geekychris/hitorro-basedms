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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;

import java.io.IOException;
import java.io.InputStream;


public class SshExec {
    protected SshSession _session;


    public SshExec(SshSession session) {
        _session = session;
    }


    public static SshExec getSshExec(SshSession session) {
        return (new SshExec(session));
    }


    public String execute(String command) {

        Channel channel = null;
        String remoteStream = "";


        if (!StringUtil.nullOrEmptyOrBlankString(command)) {
            try {
                //   session has connection info, userinfo already set
                channel = _session.getJschSession().openChannel("exec");
                ((ChannelExec) channel).setCommand(command);


                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);
                InputStream in = channel.getInputStream();


                channel.connect();
                Log.ssh.debug("jsch channel connect: %s: with command %s", channel.isConnected(), command);


                byte[] tmp = new byte[SshConstants.BUFFER_SIZE];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, SshConstants.BUFFER_SIZE);
                        if (i < 0) {
                            break;
                        }

                        remoteStream = StringUtil.strcat(remoteStream, new String(tmp, 0, i));
                    }


                    if (channel.isClosed()) {
                        Log.ssh.debug("jsch channel exit status: %s", channel.getExitStatus());
                        break;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.ssh.error("jsch channel thread error %s %e", e, e);
                    }
                }
                Log.ssh.debug("Remote host stream: %s", remoteStream);

                /*   close the current channel, stream.   */
                in.close();
                disconnect(channel);

            } catch (Exception e) {
                if (e instanceof JSchException) {
                    Log.ssh.error("jsh channel error %s %e", e, e);

                    if (channel != null) {
                        Log.ssh.debug("jsch channel exit status: %s", channel.getExitStatus());
                    }

                } else if (e instanceof IOException) {
                    Log.ssh.error("IOException for jsch channel command %s %e", e, e);
                } else if (e instanceof Exception) {
                    Log.ssh.error("Exception during SSH exec %s %e", e, e);
                }

                /*   close the current channel.   */
                Boolean isDisconnected = disconnect(channel);

                Log.ssh.debug("jsch channel disconnect: %s", isDisconnected);
            }
        }

        return remoteStream;
    }


    public String[] executeArray(String command) {
        String remoteStreamTokens[] = null;
        String remoteStream = execute(command);

        /*   chunk up the stream results   */
        if (!StringUtil.nullOrEmptyOrBlankString(remoteStream)) {
            remoteStreamTokens = StringUtil.tokenizeRemovingNullOrEmptyStrings(remoteStream, "\n", true);
        }

        return remoteStreamTokens;
    }


    private boolean disconnect(Channel channel) {
        boolean isConnected = false;
        if (channel != null && channel.isConnected()) {
            channel.disconnect();

            if (channel.isConnected()) {
                isConnected = true;
            }
        }

        return isConnected;
    }


    public String getStatusCommand(String command) {
        return StringUtil.strcat(command, ";echo $?");
    }


    protected String getQuotedFilename(String filePathname) {

        /*   quote spaces in filepathname only.  do not attempt to quote within commands containing filenames.   */
        if (filePathname.indexOf("\\") < 0) {
            filePathname = filePathname.replaceAll("\\s", "\\\\ ");
        }
        return filePathname;
    }


    public int getExitStatus(String remoteStream) {

        int exitStatus = SshConstants.SSH_SHELLSTATUS_UNKNOWN;

        if (StringUtil.nullOrEmptyOrBlankString(remoteStream)) {
            exitStatus = SshConstants.SSH_SHELLSTATUS_UNKNOWN;
        } else {
            try {
                if (remoteStream.endsWith("\n")) {
                    int beginIndex = 0;
                    int endIndex = remoteStream.lastIndexOf("\n");
                    if (endIndex == -1) {
                        endIndex = remoteStream.length();
                    }

                    String statusStr = remoteStream.substring(beginIndex, endIndex);
                    exitStatus = Integer.parseInt(statusStr);
                }


                if (exitStatus < 0 || exitStatus > 255) {
                    exitStatus = SshConstants.SSH_SHELLSTATUS_UNKNOWN;
                }

            } catch (NumberFormatException e) {
                exitStatus = SshConstants.SSH_SHELLSTATUS_UNKNOWN;
            }

        }
        return exitStatus;
    }

}