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
package com.hitorro.basedms.transformer.squeeze;


import com.hitorro.util.core.Console;
import com.hitorro.util.core.Constants;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.Log;
import com.hitorro.util.json.keys.IntegerProperty;
import com.hitorro.util.xml.SAXUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class SqueezeListener implements Runnable {
    private static final IntegerProperty ListenerConnectTimeout = new IntegerProperty("transcoder.squeeze.ListenerConnectTimeout", "ProducerApp socket connection to Sorenson Squeeze (milliseconds)", (int) Constants.MillisInSecond * 2);
    private static final IntegerProperty ListenerConnectSleepInterval = new IntegerProperty("transcoder.squeeze.ListenerConnectSleepInterval", "ProducerApp delay between socket connection attempts to Sorenson Squeeze (milliseconds)", (int) Constants.MillisInSecond * 3);
    private static final IntegerProperty ListenerConnectStartupDelay = new IntegerProperty("transcoder.squeeze.ListenerConnectStartupDelay", "ProducerApp delay before initial socket connection attempts to Sorenson Squeeze (milliseconds)", (int) Constants.MillisInSecond * 2);
    private static final IntegerProperty ListenerConnectRetriesMax = new IntegerProperty("transcoder.squeeze.ListenerConnectRetriesMax", "ProducerApp maximum retries for connecting to Sorenson Squeeze activity", 30);
    private String m_host;
    private int m_port;
    private SqueezeService m_service;


    public SqueezeListener(String host, int port, SqueezeService service) {
        m_host = host;
        m_port = port;
        m_service = service;
    }


    public void run() {
        int connectAttempts = 0;

        Env.sleepMillis(ListenerConnectStartupDelay.apply());
        SocketAddress addr = new InetSocketAddress(m_host, m_port);
        boolean isConnected = false;

        try {
            while (!isConnected && connectAttempts < ListenerConnectRetriesMax.apply()) {
                Socket socket = new Socket();

                try {
                    Console.println("connection attempt %s", connectAttempts);
                    Log.util.info("connection attempt %s", connectAttempts);
                    connectAttempts++;

                    if (!socket.isConnected()) {
                        socket.connect(addr, ListenerConnectTimeout.apply());
                        Env.sleepMillis(ListenerConnectSleepInterval.apply());

                        if (socket.isConnected()) {
                            isConnected = true;
                            Log.util.info("Connected to Sorenson Squeeze at host: %s port: %s", m_host, m_port);

                            OutputStream rawOut = socket.getOutputStream();
                            InputStream rawIn = socket.getInputStream();
                            SqueezeXMLOutputParser handler = new SqueezeXMLOutputParser(m_service);
                            SAXUtil.readSax(rawIn, handler);

                        }
                    }
                } catch (SocketException e) {
                    if (!socket.isConnected() && connectAttempts >= ListenerConnectRetriesMax.apply()) {
                        Log.util.error("Error connecting to Sorenson Squeeze at host: %s port: %s; %s %e", m_host, m_port, e, e);
                    }

                    if (socket != null && socket.isConnected()) {
                        socket.close();
                    }

                    Env.sleepMillis(ListenerConnectSleepInterval.apply());
                }
            }
        } catch (IOException e) {
            Log.util.error("Error connecting to Sorenson Squeeze at host: %s port: %s; %s %e", m_host, m_port, e, e);
        } catch (ParserConfigurationException e) {
            Log.util.error("Error connecting to Sorenson Squeeze at host: %s port: %s; %s %e", m_host, m_port, e, e);
        } catch (SAXException e) {
            Log.util.error("Error connecting to Sorenson Squeeze at host: %s port: %s; %s %e", m_host, m_port, e, e);
        }

    }
}
