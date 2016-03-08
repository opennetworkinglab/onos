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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
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
     * @return set of CordVtnNodeConfig or empty set
     */
    public Set<CordVtnNode> cordVtnNodes() {

        Set<CordVtnNode> nodes = Sets.newHashSet();
        JsonNode jsonNodes = object.get(CORDVTN_NODES);
        if (jsonNodes == null) {
            log.debug("No CORD VTN nodes found");
            return nodes;
        }

        for (JsonNode jsonNode : jsonNodes) {
            try {
                NetworkAddress hostMgmt = NetworkAddress.valueOf(getConfig(jsonNode, HOST_MANAGEMENT_IP));
                NetworkAddress localMgmt = NetworkAddress.valueOf(getConfig(object, LOCAL_MANAGEMENT_IP));
                if (hostMgmt.prefix().contains(localMgmt.prefix()) ||
                        localMgmt.prefix().contains(hostMgmt.prefix())) {
                    log.error("hostMamt and localMgmt cannot be overlapped, skip this node");
                    continue;
                }

                Ip4Address hostMgmtIp = hostMgmt.ip().getIp4Address();
                SshAccessInfo sshInfo = new SshAccessInfo(
                        hostMgmtIp,
                        TpPort.tpPort(Integer.parseInt(getConfig(object, SSH_PORT))),
                        getConfig(object, SSH_USER), getConfig(object, SSH_KEY_FILE));

                String hostname = getConfig(jsonNode, HOSTNAME);
                CordVtnNode newNode = new CordVtnNode(
                        hostname, hostMgmt, localMgmt,
                        NetworkAddress.valueOf(getConfig(jsonNode, DATA_PLANE_IP)),
                        TpPort.tpPort(Integer.parseInt(getConfig(object, OVSDB_PORT))),
                        sshInfo,
                        DeviceId.deviceId(getConfig(jsonNode, BRIDGE_ID)),
                        getConfig(jsonNode, DATA_PLANE_INTF),
                        CordVtnNodeState.noState());

                log.info("Successfully read {} from the config", hostname);
                nodes.add(newNode);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.error("{}", e.toString());
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
            log.debug("{} : {}", path, jsonNode.asText());
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
}

