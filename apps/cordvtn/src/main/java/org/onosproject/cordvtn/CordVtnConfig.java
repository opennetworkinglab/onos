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

    public static final String CORDVTN_NODES = "nodes";
    public static final String HOSTNAME = "hostname";
    public static final String OVSDB_IP = "ovsdbIp";
    public static final String OVSDB_PORT = "ovsdbPort";
    public static final String BRIDGE_ID = "bridgeId";

    /**
     * Returns the set of nodes read from network config.
     *
     * @return set of CordVtnNodeConfig or null
     */
    public Set<CordVtnNodeConfig> cordVtnNodes() {
        Set<CordVtnNodeConfig> nodes = Sets.newHashSet();

        JsonNode jsonNodes = object.get(CORDVTN_NODES);
        if (jsonNodes == null) {
            return null;
        }
        jsonNodes.forEach(jsonNode -> nodes.add(new CordVtnNodeConfig(
            jsonNode.path(HOSTNAME).asText(),
            IpAddress.valueOf(jsonNode.path(OVSDB_IP).asText()),
            TpPort.tpPort(jsonNode.path(OVSDB_PORT).asInt()),
            DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText()))));

        return nodes;
    }

    /**
     * Configuration for CordVtn node.
     */
    public static class CordVtnNodeConfig {

        private final String hostname;
        private final IpAddress ovsdbIp;
        private final TpPort ovsdbPort;
        private final DeviceId bridgeId;

        public CordVtnNodeConfig(String hostname, IpAddress ovsdbIp, TpPort ovsdbPort, DeviceId bridgeId) {
            this.hostname = checkNotNull(hostname);
            this.ovsdbIp = checkNotNull(ovsdbIp);
            this.ovsdbPort = checkNotNull(ovsdbPort);
            this.bridgeId = checkNotNull(bridgeId);
        }

        /**
         * Returns hostname of the node.
         *
         * @return hostname
         */
        public String hostname() {
            return this.hostname;
        }

        /**
         * Returns OVSDB ip address of the node.
         *
         * @return OVSDB server IP address
         */
        public IpAddress ovsdbIp() {
            return this.ovsdbIp;
        }

        /**
         * Returns OVSDB port number of the node.
         *
         * @return port number
         */
        public TpPort ovsdbPort() {
            return this.ovsdbPort;
        }

        /**
         * Returns integration bridge id of the node.
         *
         * @return device id
         */
        public DeviceId bridgeId() {
            return this.bridgeId;
        }
    }
}
