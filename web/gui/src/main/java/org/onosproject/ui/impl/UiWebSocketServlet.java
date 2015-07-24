/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.ui.impl;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Web socket servlet capable of creating web sockets for the user interface.
 */
public class UiWebSocketServlet extends WebSocketServlet {

    private static final long PING_DELAY_MS = 5000;

    private static UiWebSocketServlet instance;

    private ServiceDirectory directory = new DefaultServiceDirectory();

    private final Set<UiWebSocket> sockets = new HashSet<>();
    private final Timer timer = new Timer();
    private final TimerTask pruner = new Pruner();
    private boolean isStopped = false;

    /**
     * Closes all currently open UI web-sockets.
     */
    public static void closeAll() {
        if (instance != null) {
            instance.isStopped = true;
            instance.sockets.forEach(UiWebSocket::close);
            instance.sockets.clear();
            instance.pruner.cancel();
            instance.timer.cancel();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        instance = this;
        timer.schedule(pruner, PING_DELAY_MS, PING_DELAY_MS);
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        if (isStopped) {
            return null;
        }
        UiWebSocket socket = new UiWebSocket(directory);
        synchronized (sockets) {
            sockets.add(socket);
        }
        return socket;
    }

    // Task for pruning web-sockets that are idle.
    private class Pruner extends TimerTask {
        @Override
        public void run() {
            synchronized (sockets) {
                Iterator<UiWebSocket> it = sockets.iterator();
                while (it.hasNext()) {
                    UiWebSocket socket = it.next();
                    if (socket.isIdle()) {
                        it.remove();
                        socket.close();
                    }
                }
            }
        }
    }
}
