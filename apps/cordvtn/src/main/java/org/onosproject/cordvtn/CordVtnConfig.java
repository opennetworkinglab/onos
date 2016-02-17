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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration object for CordVtn service.
 */
public class CordVtnConfig extends Config<ApplicationId> {

    protected final Logger log = getLogger(getClass());

    public static final String PRIVATE_GATEWAY_MAC = "privateGatewayMac";
    public static final String PUBLIC_GATEWAYS = "publicGateways";
    public static final String GATEWAY_IP = "gatewayIp";
    public static final String GATEWAY_MAC = "gatewayMac";
    public static final String LOCAL_MANAGEMENT_IP = "localManagementIp";
    public static final String OVSDB_PORT = "ovsdbPort";
    public static final String SSH_PORT = "sshPort";
    public static final String SSH_USER = "sshUser";
    public static final String SSH_KEY_FILE = "sshKeyFile";
    public static final String CORDVTN_NODES = "nodes";

    public static final String HOSTNAME = "hostname";
    public static final String HOST_MANAGEMENT_IP = "hostManagementIp";
    public static final String DATA_PLANE_IP = "dataPlaneIp";
    public static final String DATA_PLANE_INTF = "dataPlaneIntf";
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

        jsonNodes.forEach(jsonNode -> {
            try {
                nodes.add(new CordVtnNodeConfig(
                        jsonNode.path(HOSTNAME).asText(),
                        NetworkAddress.valueOf(jsonNode.path(HOST_MANAGEMENT_IP).asText()),
                        NetworkAddress.valueOf(jsonNode.path(DATA_PLANE_IP).asText()),
                        jsonNode.path(DATA_PLANE_INTF).asText(),
                        DeviceId.deviceId(jsonNode.path(BRIDGE_ID).asText())));
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Failed to read {}", e.toString());
            }
        });

        return nodes;
    }

    /**
     * Returns private network gateway MAC address.
     *
     * @return mac address, or null
     */
    public MacAddress privateGatewayMac() {
        JsonNode jsonNode = object.get(PRIVATE_GATEWAY_MAC);
        if (jsonNode == null) {
            return null;
        }

        try {
            return MacAddress.valueOf(jsonNode.asText());
        } catch (IllegalArgumentException e) {
            log.error("Wrong MAC address format {}", jsonNode.asText());
            return null;
        }
    }

    /**
     * Returns public network gateway IP and MAC address pairs.
     *
     * @return map of ip and mac address
     */
    public Map<IpAddress, MacAddress> publicGateways() {
        JsonNode jsonNodes = object.get(PUBLIC_GATEWAYS);
        if (jsonNodes == null) {
            return null;
        }

        Map<IpAddress, MacAddress> publicGateways = Maps.newHashMap();
        jsonNodes.forEach(jsonNode -> {
            try {
                publicGateways.put(
                        IpAddress.valueOf(jsonNode.path(GATEWAY_IP).asText()),
                        MacAddress.valueOf(jsonNode.path(GATEWAY_MAC).asText()));
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("Wrong address format {}", e.toString());
            }
        });

        return publicGateways;
    }

    /**
     * Returns local management network address.
     *
     * @return network address
     */
    public NetworkAddress localMgmtIp() {
        JsonNode jsonNode = object.get(LOCAL_MANAGEMENT_IP);
        if (jsonNode == null) {
            return null;
        }

        try {
            return NetworkAddress.valueOf(jsonNode.asText());
        } catch (IllegalArgumentException e) {
            log.error("Wrong address format {}", jsonNode.asText());
            return null;
        }
    }

    /**
     * Returns the port number used for OVSDB connection.
     *
     * @return port number, or null
     */
    public TpPort ovsdbPort() {
        JsonNode jsonNode = object.get(OVSDB_PORT);
        if (jsonNode == null) {
            return null;
        }

        try {
            return TpPort.tpPort(jsonNode.asInt());
        } catch (IllegalArgumentException e) {
            log.error("Wrong TCP port format {}", jsonNode.asText());
            return null;
        }
    }

    /**
     * Returns the port number used for SSH connection.
     *
     * @return port number, or null
     */
    public TpPort sshPort() {
        JsonNode jsonNode = object.get(SSH_PORT);
        if (jsonNode == null) {
            return null;
        }

        try {
            return TpPort.tpPort(jsonNode.asInt());
        } catch (IllegalArgumentException e) {
            log.error("Wrong TCP port format {}", jsonNode.asText());
            return null;
        }
    }

    /**
     * Returns the user name for SSH connection.
     *
     * @return user name, or null
     */
    public String sshUser() {
        JsonNode jsonNode = object.get(SSH_USER);
        if (jsonNode == null) {
            return null;
        }

        return jsonNode.asText();
    }

    /**
     * Returns the private key file for SSH connection.
     *
     * @return file path, or null
     */
    public String sshKeyFile() {
        JsonNode jsonNode = object.get(SSH_KEY_FILE);
        if (jsonNode == null) {
            return null;
        }

        return jsonNode.asText();
    }

    /**
     * Configuration for CordVtn node.
     */
    public static class CordVtnNodeConfig {

        private final String hostname;
        private final NetworkAddress hostMgmtIp;
        private final NetworkAddress dpIp;
        private final String dpIntf;
        private final DeviceId bridgeId;

        public CordVtnNodeConfig(String hostname, NetworkAddress hostMgmtIp, NetworkAddress dpIp,
                                 String dpIntf, DeviceId bridgeId) {
            this.hostname = checkNotNull(hostname);
            this.hostMgmtIp = checkNotNull(hostMgmtIp);
            this.dpIp = checkNotNull(dpIp);
            this.dpIntf = checkNotNull(dpIntf);
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
         * Returns the host management network address of the node.
         *
         * @return management network address
         */
        public NetworkAddress hostMgmtIp() {
            return this.hostMgmtIp;
        }

        /**
         * Returns the data plane network address.
         *
         * @return network address
         */
        public NetworkAddress dpIp() {
            return this.dpIp;
        }

        /**
         * Returns the data plane interface name.
         *
         * @return interface name
         */
        public String dpIntf() {
            return this.dpIntf;
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
