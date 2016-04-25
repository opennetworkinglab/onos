/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cordvtn.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onosproject.xosclient.api.XosAccess;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

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

    public static final String CORDVTN_NODES = "nodes";
    public static final String HOSTNAME = "hostname";
    public static final String HOST_MANAGEMENT_IP = "hostManagementIp";
    public static final String DATA_PLANE_IP = "dataPlaneIp";
    public static final String DATA_PLANE_INTF = "dataPlaneIntf";
    public static final String BRIDGE_ID = "bridgeId";

    public static final String SSH = "ssh";
    public static final String SSH_PORT = "sshPort";
    public static final String SSH_USER = "sshUser";
    public static final String SSH_KEY_FILE = "sshKeyFile";

    public static final String OPENSTACK = "openstack";
    public static final String XOS = "xos";

    public static final String ENDPOINT = "endpoint";
    public static final String TENANT = "tenant";
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    /**
     * Returns the set of nodes read from network config.
     *
     * @return set of CordVtnNodeConfig or empty set
     */
    public Set<CordVtnNode> cordVtnNodes() {

        Set<CordVtnNode> nodes = Sets.newHashSet();

        JsonNode cordvtnNodes = object.get(CORDVTN_NODES);
        if (cordvtnNodes == null) {
            log.debug("No CORD VTN nodes found");
            return nodes;
        }

        JsonNode sshNode = object.get(SSH);
        if (sshNode == null) {
            log.warn("SSH information not found");
            return nodes;
        }

        for (JsonNode cordvtnNode : cordvtnNodes) {
            try {
                NetworkAddress hostMgmt = NetworkAddress.valueOf(getConfig(cordvtnNode, HOST_MANAGEMENT_IP));
                NetworkAddress localMgmt = NetworkAddress.valueOf(getConfig(object, LOCAL_MANAGEMENT_IP));
                if (hostMgmt.prefix().contains(localMgmt.prefix()) ||
                        localMgmt.prefix().contains(hostMgmt.prefix())) {
                    log.error("hostMamt and localMgmt cannot be overlapped, skip this node");
                    continue;
                }

                Ip4Address hostMgmtIp = hostMgmt.ip().getIp4Address();
                SshAccessInfo sshInfo = new SshAccessInfo(
                        hostMgmtIp,
                        TpPort.tpPort(Integer.parseInt(getConfig(sshNode, SSH_PORT))),
                        getConfig(sshNode, SSH_USER), getConfig(sshNode, SSH_KEY_FILE));

                String hostname = getConfig(cordvtnNode, HOSTNAME);
                CordVtnNode newNode = new CordVtnNode(
                        hostname, hostMgmt, localMgmt,
                        NetworkAddress.valueOf(getConfig(cordvtnNode, DATA_PLANE_IP)),
                        TpPort.tpPort(Integer.parseInt(getConfig(object, OVSDB_PORT))),
                        sshInfo,
                        DeviceId.deviceId(getConfig(cordvtnNode, BRIDGE_ID)),
                        getConfig(cordvtnNode, DATA_PLANE_INTF),
                        CordVtnNodeState.noState());

                nodes.add(newNode);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("{}", e);
            }
        }

        return nodes;
    }

    /**
     * Returns value of a given path. If the path is missing, show log and return
     * null.
     *
     * @param path path
     * @return value or null
     */
    private String getConfig(JsonNode jsonNode, String path) {
        jsonNode = jsonNode.path(path);

        if (jsonNode.isMissingNode()) {
            log.error("{} is not configured", path);
            return null;
        } else {
            return jsonNode.asText();
        }
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
            return Maps.newHashMap();
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
     * Returns XOS access information.
     *
     * @return XOS access, or null
     */
    public XosAccess xosAccess() {
        JsonNode jsonNode = object.get(XOS);
        if (jsonNode == null) {
            log.error("Failed to get XOS configurations");
            return null;
        }

        try {
            return new XosAccess(getConfig(jsonNode, ENDPOINT),
                                 getConfig(jsonNode, USER),
                                 getConfig(jsonNode, PASSWORD));
        } catch (NullPointerException e) {
            log.error("Failed to get XOS access");
            return null;
        }
    }

    /**
     * Returns OpenStack API access information.
     *
     * @return openstack config
     */
    public OpenStackConfig openstackConfig() {
        JsonNode jsonNode = object.get(OPENSTACK);
        if (jsonNode == null) {
            log.error("Failed to get OpenStack configurations");
            return null;
        }

        try {
            return new OpenStackConfig(
                    jsonNode.path(ENDPOINT).asText(),
                    jsonNode.path(TENANT).asText(),
                    jsonNode.path(USER).asText(),
                    jsonNode.path(PASSWORD).asText());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Failed to get OpenStack configurations");
            return null;
        }
    }

    /**
     * Configuration for OpenStack API access.
     */
    public static class OpenStackConfig {

        private final String endpoint;
        private final String tenant;
        private final String user;
        private final String password;

        /**
         * Default constructor.
         *
         * @param endpoint Keystone endpoint
         * @param tenant tenant name
         * @param user user name
         * @param password passwowrd
         */
        public OpenStackConfig(String endpoint, String tenant, String user, String password) {
            this.endpoint = endpoint;
            this.tenant = tenant;
            this.user = user;
            this.password = password;
        }

        /**
         * Returns OpenStack API endpoint.
         *
         * @return endpoint
         */
        public String endpoint() {
            return this.endpoint;
        }

        /**
         * Returns OpenStack tenant name.
         *
         * @return tenant name
         */
        public String tenant() {
            return this.tenant;
        }

        /**
         * Returns OpenStack user.
         *
         * @return user name
         */
        public String user() {
            return this.user;
        }

        /**
         * Returns OpenStack password for the user.
         *
         * @return password
         */
        public String password() {
            return this.password;
        }
    }
}
