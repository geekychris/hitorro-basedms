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
package com.hitorro.basedms.ssh.scp;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.hitorro.util.core.Log;
import com.hitorro.util.core.string.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.HashMap;


public abstract class Scp {

    protected Session session;


    /**
     * Constructor for Scp
     *
     * @param session the ssh session to use
     */
    public Scp(Session session) {
        this.session = session;
    }


    /**
     * Open an ssh channel.
     *
     * @param command the command to use
     * @return the channel
     * @throws JSchException on error
     */
    protected Channel openExecChannel(String command) throws JSchException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        return channel;
    }


    /**
     * Send an ack.
     *
     * @param out the output stream to use
     * @throws IOException on error
     */
    protected void sendAck(OutputStream out) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = 0;
        out.write(buf);
        out.flush();
    }


    /**
     * Reads the response, throws a JSchException if the response indicates an error.
     *
     * @param in the input stream to use
     * @throws IOException   on I/O error
     * @throws JSchException on other errors
     */
    protected void waitForAck(InputStream in)
            throws IOException, JSchException {
        int b = in.read();

        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,

        if (b == -1) {
            // didn't receive any response
            throw new JSchException("No response from server");
        } else if (b != 0) {
            StringBuffer sb = new StringBuffer();

            int c = in.read();
            while (c > 0 && c != '\n') {
                sb.append((char) c);
                c = in.read();
            }

            if (b == 1) {
                throw new JSchException(StringUtil.strcat("server indicated an error: %s.", sb.toString()));
            } else if (b == 2) {
                throw new JSchException(StringUtil.strcat("server indicated a fatal error: %s", sb.toString()));
            } else {
                throw new JSchException(StringUtil.strcat("unknown response, code: %s; message: %s ", b, sb.toString()));
            }
        }
    }

    /**
     * Carry out the transfer.
     *
     * @return HashMap<String, Integer>   key is fully qualified filepathname; value is file transfer status, defined in
     * SshConstants.
     * @throws IOException   on I/O errors
     * @throws JSchException on ssh errors
     */
    public abstract HashMap<String, ScpFileStatus> execute() throws IOException, JSchException;


    /**
     * Log transfer stats to the log listener.
     *
     * @param timeStarted the time started
     * @param timeEnded   the finishing time
     * @param totalLength the total length
     */
    protected void logStats(long timeStarted,
                            long timeEnded,
                            int totalLength) {
        double duration = (timeEnded - timeStarted) / 1000.0;
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(1);
        Log.ssh.debug("File transfer time: %s Average Rate: %s B/s",
                format.format(duration),
                format.format(totalLength / duration));
    }


    /**
     * Track progress every 5%.
     *
     * @param filesize           the size of the file been transmitted
     * @param totalLength        the total transmission size
     * @param percentTransmitted the current percent transmitted
     * @return the percent that the file is of the total
     */
    protected final int trackProgress(long filesize, int totalLength,
                                      int percentTransmitted) {

        int percent = (int) Math.round(Math.floor((totalLength / (double) filesize) * 100));

        if (percent > percentTransmitted) {
            if (percent % 5 == 0) {
                if (percent == 100) {
                    Log.ssh.debug("100%");
                } else {
                    Log.ssh.debug(Integer.toString(percent));
                }

            }
        }

        return percent;
    }

}