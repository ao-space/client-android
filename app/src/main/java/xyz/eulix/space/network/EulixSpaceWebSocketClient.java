/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * date: 2021/6/16 11:11
 */
public class EulixSpaceWebSocketClient extends WebSocketClient {
    private static final String TAG = EulixSpaceWebSocketClient.class.getSimpleName();

    public EulixSpaceWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    public EulixSpaceWebSocketClient(URI serverUri, Draft protocolDraft) {
        super(serverUri, protocolDraft);
    }

    public EulixSpaceWebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    public EulixSpaceWebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders) {
        super(serverUri, protocolDraft, httpHeaders);
    }

    public EulixSpaceWebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
        super(serverUri, protocolDraft, httpHeaders, connectTimeout);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Logger.d(TAG, "on open" + (handshakedata == null ? "" : ", http status message: " + handshakedata.getHttpStatusMessage()));
    }

    @Override
    public void onMessage(String message) {
        Logger.i(TAG, "on message: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Logger.i(TAG, "on close, code: " + code + ", reason: " + reason + ", is remote: " + remote);
    }

    @Override
    public void onError(Exception ex) {
        Logger.e(TAG, "on error" + (ex == null ? "" : ": " + ex.getMessage()));
    }
}
