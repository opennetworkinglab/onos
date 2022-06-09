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
 * Implementation of ExaBGP Route Collector Monitors.
 */
public class ExaBgpMonitors implements Monitors {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String host;
    private Socket socket;
    private IpPrefix prefix;
    private ArtemisPacketProcessor packetProcessor;

    public ExaBgpMonitors(IpPrefix prefix, String host, ArtemisPacketProcessor packetProcessor) {
        this.host = host;
        this.prefix = prefix;
        this.packetProcessor = packetProcessor;
    }

    /**
     * socket.io onConnect event handler.
     */
    private void onConnect() {
        JsonObject parameters = new JsonObject();
        parameters.set("prefix", this.prefix.toString());

        socket.emit("exa_subscribe", parameters);
    }

    /**
     * ExaBGP message received on the socket.io.
     *
     * @param args exabgp message
     */
    private void onExaMessage(Object[] args) {
        JsonObject message = (JsonObject) args[0];
            if (message.get("type").asString().equals("A")) {
                // Example of BGP Update message:
                // {
                //  "path":[65001],
                //  "peer":"1.1.1.s1",
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
                packetProcessor.processMonitorPacket(message);
            }
    }

    @Override
    public IpPrefix getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(IpPrefix prefix) {
        this.prefix = prefix;
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
                log.warn("startMonitor()", e);
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
        if (obj instanceof ExaBgpMonitors) {
            final ExaBgpMonitors that = (ExaBgpMonitors) obj;
            return Objects.equals(this.prefix, that.prefix) &&
                    Objects.equals(this.host, that.host);
        }
        return false;
    }

}
