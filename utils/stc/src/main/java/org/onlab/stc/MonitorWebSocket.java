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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;

import java.io.IOException;

import static org.onlab.stc.Coordinator.print;

/**
 * Web socket capable of interacting with the STC monitor GUI.
 */
public class MonitorWebSocket implements WebSocket.OnTextMessage, WebSocket.OnControl {

    private static final long MAX_AGE_MS = 30_000;

    private static final byte PING = 0x9;
    private static final byte PONG = 0xA;
    private static final byte[] PING_DATA = new byte[]{(byte) 0xde, (byte) 0xad};

    private final Monitor monitor;

    private Connection connection;
    private FrameConnection control;

    private final ObjectMapper mapper = new ObjectMapper();

    private long lastActive = System.currentTimeMillis();

    /**
     * Creates a new monitor client GUI web-socket.
     *
     * @param monitor shared process flow monitor
     */
    MonitorWebSocket(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Issues a close on the connection.
     */
    synchronized void close() {
        destroyHandlers();
        if (connection.isOpen()) {
            connection.close();
        }
    }

    /**
     * Indicates if this connection is idle.
     *
     * @return true if idle or closed
     */
    synchronized boolean isIdle() {
        long quietFor = System.currentTimeMillis() - lastActive;
        boolean idle = quietFor > MAX_AGE_MS;
        if (idle || (connection != null && !connection.isOpen())) {
            return true;
        } else if (connection != null) {
            try {
                control.sendControl(PING, PING_DATA, 0, PING_DATA.length);
            } catch (IOException e) {
                print("Unable to send ping message due to: %s", e);
            }
        }
        return false;
    }

    @Override
    public void onOpen(Connection connection) {
        this.connection = connection;
        this.control = (FrameConnection) connection;
        try {
            createHandlers();
            sendMessage(message("flow", monitor.scenarioData()));

        } catch (Exception e) {
            print("Unable to open monitor connection: %s", e);
            this.connection.close();
            this.connection = null;
            this.control = null;
        }
    }

    @Override
    public synchronized void onClose(int closeCode, String message) {
        destroyHandlers();
    }

    @Override
    public boolean onControl(byte controlCode, byte[] data, int offset, int length) {
        lastActive = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onMessage(String data) {
        lastActive = System.currentTimeMillis();
        try {
            ObjectNode message = (ObjectNode) mapper.reader().readTree(data);
            // TODO:
            print("Got message: %s", message);
        } catch (Exception e) {
            print("Unable to parse GUI message %s due to %s", data, e);
        }
    }

    public synchronized void sendMessage(ObjectNode message) {
        try {
            if (connection.isOpen()) {
                connection.sendMessage(message.toString());
            }
        } catch (IOException e) {
            print("Unable to send message %s to GUI due to %s", message, e);
        }
    }

    public ObjectNode message(String type, ObjectNode payload) {
        ObjectNode message = mapper.createObjectNode().put("event", type);
        message.set("payload", payload);
        return message;
    }

    // Creates new message handlers.
    private synchronized void createHandlers() {
    }

    // Destroys message handlers.
    private synchronized void destroyHandlers() {
    }

}

