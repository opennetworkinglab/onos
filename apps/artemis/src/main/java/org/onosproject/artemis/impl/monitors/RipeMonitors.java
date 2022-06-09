/*
 * Copyright 2017-present Open Networking Foundation
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
import com.eclipsesource.json.JsonObject;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.onosproject.artemis.Monitors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Implementation of RIPE Route Collector Monitors.
 */
public class RipeMonitors implements Monitors {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String host;
    private Socket socket;
    private IpPrefix prefix;
    private ArtemisPacketProcessor packetProcessor;

    public RipeMonitors(IpPrefix prefix, String host, ArtemisPacketProcessor packetProcessor) {
        this.prefix = prefix;
        this.host = host;
        this.packetProcessor = packetProcessor;
    }

    /**
     * socket.io onConnect event handler.
     */
    private void onConnect() {
        socket.emit("ping");

        JsonObject parameters = new JsonObject();
        parameters.set("origin", "");
        parameters.set("type", "");
        parameters.set("moreSpecific", true);
        parameters.set("lessSpecific", false);
        parameters.set("peer", "");
        parameters.set("host", this.host);
        parameters.set("prefix", this.prefix.toString());

        socket.emit("ris_subscribe", parameters);
    }

    @Override
    public IpPrefix getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(IpPrefix prefix) {
        this.prefix = prefix;
    }

    /**
     * socket.io onRisMessage event handler.
     * This event is custom made that triggers when it receives an BGP update/withdraw for our prefix.
     *
     * @param args RIS message
     */
    private void onRisMessage(Object[] args) {
        JsonObject message = (JsonObject) args[0];
        if (message.get("type").asString().equals("A")) {
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
            packetProcessor.processMonitorPacket(message);
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
                log.error("startMonitor()", e);
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
        if (obj instanceof RipeMonitors) {
            final RipeMonitors that = (RipeMonitors) obj;
            return Objects.equals(this.prefix, that.prefix) &&
                    Objects.equals(this.host, that.host);
        }
        return false;
    }

}
