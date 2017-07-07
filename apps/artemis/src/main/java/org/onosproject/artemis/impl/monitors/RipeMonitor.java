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
 * Implementation of RIPE Route Collector Monitor.
 */
public class RipeMonitor extends Monitor {
    private String host;
    private Socket socket;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public RipeMonitor(IpPrefix prefix, String host) {
        super(prefix);
        this.host = host;
    }

    /**
     * socket.io onConnect event handler.
     */
    private void onConnect() {
        try {
            socket.emit("ping");

            JSONObject parameters = new JSONObject();
            parameters.put("origin", (Object) null);
            parameters.put("type", (Object) null);
            parameters.put("moreSpecific", true);
            parameters.put("lessSpecific", false);
            parameters.put("peer", (Object) null);
            parameters.put("host", this.host);
            parameters.put("prefix", this.prefix);

            socket.emit("ris_subscribe", parameters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * socket.io onRisMessage event handler.
     * This event is custom made that triggers when it receives an BGP update/withdraw for our prefix.
     *
     * @param args RIS message
     */
    private void onRisMessage(Object[] args) {
        try {
            JSONObject message = (JSONObject) args[0];
            if (message.getString("type").equals("A")) {
                // Write BGP message to a json database
                DataHandler.Serializer.writeData(args[0]);

                if (ArtemisManager.logging) {
                    log.info(message.toString());
                }

                // Example of BGP Update message:
                // {
                //  "timestamp":1488044022.97,
                //  "prefix":"101.1.46.0/24",
                //  "host":"rrc21",
                //  "next_hop":"37.49.236.246",
                //  "peer":"37.49.236.246",
                //  "path":[2613,25091,9318,9524],
                //  "type":"A"
                // }

                // We want to keep only prefix and path in memory.
                message.remove("community");
                message.remove("timestamp");
                message.remove("next_hop");
                message.remove("peer");
                message.remove("type");
                message.remove("host");

                // Append synchronized message to message list in memory.
                DataHandler.getInstance().appendData(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        socket.emit("ping");
    }

    @Override
    public void startMonitor() {
        if (!isRunning()) {
            log.info("Starting RIPE monitor for " + prefix + " / " + host);
            IO.Options opts = new IO.Options();
            opts.path = "/stream/socket.io/";

            try {
                this.socket = IO.socket("http://stream-dev.ris.ripe.net/", opts);
                this.socket.on(Socket.EVENT_CONNECT, args -> onConnect());
                this.socket.on(Socket.EVENT_PONG, args -> socket.emit("ping"));
                this.socket.on("ris_message", this::onRisMessage);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            this.socket.connect();
        }
    }

    @Override
    public void stopMonitor() {
        if (isRunning()) {
            log.info("Stopping RIPE monitor for " + prefix + " / " + host);
            this.socket.off();
            this.socket.disconnect();
            this.socket.close();
            this.socket = null;
        }
    }

    @Override
    public Types getType() {
        return Types.RIPE;
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
        if (obj instanceof RipeMonitor) {
            final RipeMonitor that = (RipeMonitor) obj;
            return Objects.equals(this.prefix, that.prefix) &&
                    Objects.equals(this.host, that.host);
        }
        return false;
    }

}
