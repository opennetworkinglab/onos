/*
 * Copyright 2015-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

import javax.servlet.ServletException;
import java.security.Principal;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Web socket servlet capable of creating web sockets for the user interface.
 */
public class UiWebSocketServlet extends WebSocketServlet {

    static final long PING_DELAY_MS = 5000;

    private static UiWebSocketServlet instance;
    private static final Object INSTANCE_LOCK = new Object();

    private static ServiceDirectory directory = new DefaultServiceDirectory();

    private final Set<UiWebSocket> sockets = Sets.newConcurrentHashSet();

    private final Timer timer = new Timer();
    private final TimerTask pruner = new Pruner();
    private static boolean isStopped = false;

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.getPolicy().setIdleTimeout(Long.MAX_VALUE);
        webSocketServletFactory.setCreator(new UiWebSocketCreator());
    }

    /**
     * Closes all currently open UI web-sockets.
     */
    public static void closeAll() {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                isStopped = true;
                instance.sockets.forEach(UiWebSocket::close);
                instance.sockets.clear();
                instance.pruner.cancel();
                instance.timer.cancel();
            }
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        synchronized (INSTANCE_LOCK) {
            instance = this;
        }
        timer.schedule(pruner, PING_DELAY_MS, PING_DELAY_MS);
    }

    /**
     * Sends the specified message to all the GUI clients.
     *
     * @param type    message type
     * @param payload message payload
     */
    static void sendToAll(String type, ObjectNode payload) {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.sockets.forEach(ws -> ws.sendMessage(type, payload));
            }
        }
    }

    /**
     * Sends the specified message to all the GUI clients of the specified user.
     *
     * @param userName user name
     * @param type     message type
     * @param payload  message payload
     */
    static void sendToUser(String userName, String type, ObjectNode payload) {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.sockets.stream().filter(ws -> userName.equals(ws.userName()))
                        .forEach(ws -> ws.sendMessage(type, payload));
            }
        }
    }

    // Task for pruning web-sockets that are idle.
    private class Pruner extends TimerTask {
        @Override
        public void run() {
            ImmutableSet<UiWebSocket> set = ImmutableSet.copyOf(sockets);
            set.stream().filter(UiWebSocket::isIdle).forEach(s -> {
                sockets.remove(s);
                s.close();
            });
        }
    }

    // FIXME: This should not be necessary
    private static final String FAKE_USERNAME = "unknown";

    public class UiWebSocketCreator implements WebSocketCreator {
        @Override
        public Object createWebSocket(ServletUpgradeRequest request, ServletUpgradeResponse response) {
            if (!isStopped) {
                // FIXME: Replace this with globally shared opaque token to allow secure failover
                Principal p = request.getUserPrincipal();
                String userName = p != null ? p.getName() : FAKE_USERNAME;

                UiWebSocket socket = new UiWebSocket(directory, userName);
                sockets.add(socket);
                return socket;
            }
            return null;
        }
    }
}
