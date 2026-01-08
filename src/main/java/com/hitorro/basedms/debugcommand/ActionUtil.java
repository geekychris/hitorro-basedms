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
package com.hitorro.basedms.debugcommand;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.network.debugcommand.ActionRPCHandler;
import com.hitorro.network.rpc.RPCMessage;
import com.hitorro.network.rpc.RPCService;
import com.hitorro.util.commandandcontrol.ActionRequest;
import com.hitorro.util.commandandcontrol.serialized.InfoRow;
import com.hitorro.util.core.Log;
import com.hitorro.util.io.StoreException;

import java.io.IOException;
import java.util.List;

/**
 * <p/>
 */
public class ActionUtil {
    /**
     * @param address
     * @param port
     * @param command
     * @param args
     * @return
     * @throws java.io.IOException
     */
    public static List<InfoRow> executeActionRPC(String address, int port, String command, JVS args) throws IOException {
        RPCMessage message = RPCService.s_service.getMessageToSend(address,
                port,
                ActionRPCHandler.ActionRPCKey,
                true);
        ActionRequest ar = new ActionRequest();
        ar.setMethod(command);

        ar.setArgs(args);
        message.setPayload(ar);
        RPCMessage resp = null;
        try {
            resp = message.sendSyncMessage();
        } catch (StoreException e) {
            Log.util.error("Exception %s %e", e, e);
        } catch (ClassNotFoundException e) {
            Log.util.error("Exception %s %e", e, e);
        }
        ar = (ActionRequest) resp.getPayload();
        List<InfoRow> rows = ar.getInfoRows();
        return rows;
    }
}
