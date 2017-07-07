/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.artemis.impl.monitors;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.impl.ArtemisManager;
import org.onosproject.artemis.impl.DataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Implementation of ExaBGP Route Collector Monitor.
 */
public class ExaBgpMonitor extends Monitor {
    private String host;
    private Socket socket;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ExaBgpMonitor(IpPrefix prefix, String host) {
        super(prefix);
        this.host = host;
    }

    /**
     * socket.io onConnect event handler.
     */
    private void onConnect() {
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("prefix", this.prefix);

            socket.emit("exa_subscribe", parameters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onExaMessage(Object[] args) {
        JSONObject message = (JSONObject) args[0];

        try {
            if (message.getString("type").equals("A")) {
                // Write BGP message to a json database
                DataHandler.Serializer.writeData(args[0]);

                if (ArtemisManager.logging) {
                    log.info(message.toString());
                }

                // Example of BGP Update message:
                // {
                //  "path":[65001],
                //  "peer":"1.1.1.1",
                //  "prefix":"12.0.0.0/8",
                //  "host":"exabgp", <-- Can put IP here
                //  "type":"A",
                //  "timestamp":1488120484
                // }

                // We want to keep only prefix and path in memory.
                message.remove("peer");
                message.remove("host");
                message.remove("type");
                message.remove("timestamp");

                // Append synchronized message to message list in memory.
                DataHandler.getInstance().appendData(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startMonitor() {
        if (!isRunning()) {
            log.info("Starting EXA monitor for " + prefix + " / " + host);
            try {
                this.socket = IO.socket("http://" + this.host + "/onos");
                this.socket.on(Socket.EVENT_CONNECT, args -> onConnect());
                this.socket.on(Socket.EVENT_PING, args -> socket.emit("pong"));
                this.socket.on("exa_message", this::onExaMessage);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            this.socket.connect();
        }
    }

    @Override
    public void stopMonitor() {
        if (isRunning()) {
            log.info("Stopping EXA monitor for " + prefix + " / " + host);
            this.socket.off();
            this.socket.disconnect();
            this.socket.close();
            this.socket = null;
        }
    }

    @Override
    public Types getType() {
        return Types.EXABGP;
    }

    @Override
    public boolean isRunning() {
        return this.socket != null;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, host);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExaBgpMonitor) {
            final ExaBgpMonitor that = (ExaBgpMonitor) obj;
            return Objects.equals(this.prefix, that.prefix) &&
                    Objects.equals(this.host, that.host);
        }
        return false;
    }

}
