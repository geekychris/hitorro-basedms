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
package com.hitorro.basedms;

import com.hitorro.base.objects.ObjectFetcher;
import com.hitorro.base.typesystem.btadapter.DBBaseAdapterItemCacheFetcher;
import com.hitorro.network.rpc.FileHandleTopic;
import com.hitorro.network.rpc.RPCHandler;
import com.hitorro.network.rpc.RPCMessage;
import com.hitorro.util.io.StoreException;
import com.hitorro.util.typesystem.HTObjectInputStream;
import com.hitorro.util.typesystem.HTObjectOutputStream;
import com.hitorro.util.typesystem.HTSerializable;
import com.hitorro.util.typesystem.ProxyAdapter;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCache;
import com.hitorro.util.typesystem.btadapter.BaseAdapterItemCacheSet;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * <p/>
 */
public class ObjectFetcherRPC extends RPCHandler {
    public static final String ObjectFetchRPCKey = "objectfetchrpc";
    private Map<String, FileHandleTopic> m_topics = new HashMap<String, FileHandleTopic>();
    // We should be able to replace this fetcher with an RPC based mechanism IF we want tiered fetches
    private DBBaseAdapterItemCacheFetcher fetcher = new DBBaseAdapterItemCacheFetcher();

    public String getMethod() {
        return ObjectFetchRPCKey;
    }

    public void process(RPCMessage message, HTObjectInputStream is,
                        HTObjectOutputStream os, Socket connection,
                        boolean sync)
            throws IOException, StoreException {
        ObjectFetcher of = (ObjectFetcher) message.getPayload();
        if (of == null) {
            throw new IOException("ObjectFetcher payload in RPC Message not found");
        }
        String adapter = of.getAdapterMethod();
        if (of.isUseCacheIfAvailable()) {
            BaseAdapterItemCache cache = BaseAdapterItemCacheSet.get(adapter);
            for (String guid : of.getGuid()) {
                of.addToResult((HTSerializable) cache.get(guid));
            }
        } else {
            ProxyAdapter ad = BaseAdapterItemCacheSet.getProxy(adapter);
            for (String guid : of.getGuid()) {
                of.addToResult((HTSerializable) fetcher.getAndMap(guid, adapter, null, ad));
            }
        }

        RPCMessage responseMessage = message.createResponse();

        responseMessage.setPayload(of);
        os.writeVersionedObject(responseMessage);
        os.flush();
    }

    public void processAsyncResponse(RPCMessage message) {
        //not supported
    }

    public boolean getSupportsAsync() {
        return false;
    }

    public boolean getSupportsSync() {
        return true;
    }

    public void registerFileHandleTopic(FileHandleTopic fht) {
        m_topics.put(fht.getTopic(), fht);
    }

}

