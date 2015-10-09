/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration object for CordVtn service.
 */
public class CordVtnConfig extends Config<ApplicationId> {

    public static final String OVSDB_NODES = "ovsdbNodes";
    public static final String HOST = "host";
    public static final String IP = "ip";
    public static final String PORT = "port";
    public static final String BRIDGE_ID = "bridgeId";

    /**
     * Returns the set of ovsdb nodes read from network config.
     *
     * @return set of OvsdbNodeConfig or null
     */
    public Set<OvsdbNodeConfig> ovsdbNodes() {
        Set<OvsdbNodeConfig> ovsdbNodes = Sets.newHashSet();

        JsonNode nodes = object.get(OVSDB_NODES);
        if (nodes == null) {
            return null;
        }
        nodes.forEach(jsonNode -> ovsdbNodes.add(new OvsdbNodeConfig(
            jsonNode.path(HOST).asText(),
            IpAddress.valueOf(jsonNode.path(IP).asText()),
            TpPort.tpPort(jsonNode.path(PORT).asInt()),
            DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText()))));

        return ovsdbNodes;
    }

    /**
     * Configuration for an ovsdb node.
     */
    public static class OvsdbNodeConfig {

        private final String host;
        private final IpAddress ip;
        private final TpPort port;
        private final DeviceId bridgeId;

        public OvsdbNodeConfig(String host, IpAddress ip, TpPort port, DeviceId bridgeId) {
            this.host = checkNotNull(host);
            this.ip = checkNotNull(ip);
            this.port = checkNotNull(port);
            this.bridgeId = checkNotNull(bridgeId);
        }

        /**
         * Returns host information of the node.
         *
         * @return host
         */
        public String host() {
            return this.host;
        }

        /**
         * Returns ip address to access ovsdb-server of the node.
         *
         * @return ip address
         */
        public IpAddress ip() {
            return this.ip;
        }

        /**
         * Returns port number to access ovsdb-server of the node.
         *
         * @return port number
         */
        public TpPort port() {
            return this.port;
        }

        public DeviceId bridgeId() {
            return this.bridgeId;
        }
    }
}
