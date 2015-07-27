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
package org.onlab.stc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Web socket servlet capable of creating web sockets for the STC monitor.
 */
public class MonitorWebSocketServlet extends WebSocketServlet
        implements MonitorDelegate {

    private static final long PING_DELAY_MS = 5000;
    private static final String DOT = ".";

    private static Monitor monitor;
    private static MonitorWebSocketServlet instance;

    private final Set<MonitorWebSocket> sockets = new HashSet<>();
    private final Timer timer = new Timer();
    private final TimerTask pruner = new Pruner();

    /**
     * Binds the shared process flow monitor.
     *
     * @param m process monitor reference
     */
    public static void setMonitor(Monitor m) {
        monitor = m;
    }

    /**
     * Closes all currently open monitor web-sockets.
     */
    public static void closeAll() {
        if (instance != null) {
            instance.sockets.forEach(MonitorWebSocket::close);
            instance.sockets.clear();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        instance = this;
        monitor.setDelegate(this);
        timer.schedule(pruner, PING_DELAY_MS, PING_DELAY_MS);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        uri = uri.length() <= 1 ? "/index.html" : uri;
        InputStream resource = getClass().getResourceAsStream(uri);
        if (resource == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            byte[] entity = ByteStreams.toByteArray(resource);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(contentType(uri).toString());
            resp.setContentLength(entity.length);
            resp.getOutputStream().write(entity);
        }
    }

    private MediaType contentType(String uri) {
        int sep = uri.lastIndexOf(DOT);
        String ext = sep > 0 ? uri.substring(sep + 1) : null;
        return ext == null ? MediaType.APPLICATION_BINARY :
                ext.equals("html") ? MediaType.HTML_UTF_8 :
                        ext.equals("js") ? MediaType.JAVASCRIPT_UTF_8 :
                                ext.equals("css") ? MediaType.CSS_UTF_8 :
                                        MediaType.APPLICATION_BINARY;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        MonitorWebSocket socket = new MonitorWebSocket(monitor);
        synchronized (sockets) {
            sockets.add(socket);
        }
        return socket;
    }

    @Override
    public void notify(ObjectNode event) {
        if (instance != null) {
            instance.sockets.forEach(ws -> ws.sendMessage(event));
        }
    }

    // Task for pruning web-sockets that are idle.
    private class Pruner extends TimerTask {
        @Override
        public void run() {
            synchronized (sockets) {
                Iterator<MonitorWebSocket> it = sockets.iterator();
                while (it.hasNext()) {
                    MonitorWebSocket socket = it.next();
                    if (socket.isIdle()) {
                        it.remove();
                        socket.close();
                    }
                }
            }
        }
    }
}
