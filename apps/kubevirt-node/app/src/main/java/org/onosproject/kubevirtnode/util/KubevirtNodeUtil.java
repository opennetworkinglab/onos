/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.NodeSpec;
import io.fabric8.kubernetes.api.model.Taint;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.onlab.packet.IpAddress;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.DefaultKubevirtPhyInterface;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.get;
import static org.onosproject.kubevirtnode.api.Constants.SONA_PROJECT_DOMAIN;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.GATEWAY;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.MASTER;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.OTHER;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;

/**
 * An utility that used in KubeVirt node app.
 */
public final class KubevirtNodeUtil {

    private static final Logger log = LoggerFactory.getLogger(KubevirtNodeUtil.class);

    private static final String COLON_SLASH = "://";
    private static final String COLON = ":";

    private static final int HEX_LENGTH = 16;
    private static final String OF_PREFIX = "of:";
    private static final String ZERO = "0";
    private static final String INTERNAL_IP = "InternalIP";
    private static final String K8S_ROLE = "node-role.kubernetes.io";
    private static final String PHYSNET_CONFIG_KEY = SONA_PROJECT_DOMAIN + "/physnet-config";
    private static final String DATA_IP_KEY = SONA_PROJECT_DOMAIN + "/data-ip";
    private static final String GATEWAY_CONFIG_KEY = SONA_PROJECT_DOMAIN + "/gateway-config";
    private static final String GATEWAY_BRIDGE_NAME = "gatewayBridgeName";
    private static final String NETWORK_KEY = "network";
    private static final String INTERFACE_KEY = "interface";
    private static final String PHYS_BRIDGE_ID = "physBridgeId";

    private static final int PORT_NAME_MAX_LENGTH = 15;

    private static final String NO_SCHEDULE_EFFECT = "NoSchedule";
    private static final String KUBEVIRT_IO_KEY = "kubevirt.io/drain";
    private static final String DRAINING_VALUE = "draining";

    /**
     * Prevents object installation from external.
     */
    private KubevirtNodeUtil() {
    }

    /**
     * Generates endpoint URL by referring to scheme, ipAddress and port.
     *
     * @param apiConfig     kubernetes API config
     * @return generated endpoint URL
     */
    public static String endpoint(KubevirtApiConfig apiConfig) {
        return endpoint(apiConfig.scheme(), apiConfig.ipAddress(), apiConfig.port());
    }

    /**
     * Generates endpoint URL by referring to scheme, ipAddress and port.
     *
     * @param scheme        scheme
     * @param ipAddress     IP address
     * @param port          port number
     * @return generated endpoint URL
     */
    public static String endpoint(KubevirtApiConfig.Scheme scheme, IpAddress ipAddress, int port) {
        StringBuilder endpoint = new StringBuilder();
        String protocol = StringUtils.lowerCase(scheme.name());

        endpoint.append(protocol);
        endpoint.append(COLON_SLASH);
        endpoint.append(ipAddress.toString());
        endpoint.append(COLON);
        endpoint.append(port);

        return endpoint.toString();
    }

    /**
     * Generates a DPID (of:0000000000000001) from an index value.
     *
     * @param index index value
     * @return generated DPID
     */
    public static String genDpid(long index) {
        if (index < 0) {
            return null;
        }

        String hexStr = Long.toHexString(index);

        StringBuilder zeroPadding = new StringBuilder();
        for (int i = 0; i < HEX_LENGTH - hexStr.length(); i++) {
            zeroPadding.append(ZERO);
        }

        return OF_PREFIX + zeroPadding.toString() + hexStr;
    }

    /**
     * Generates string format based on the given string length list.
     *
     * @param stringLengths a list of string lengths
     * @return string format (e.g., %-28s%-15s%-24s%-20s%-15s)
     */
    public static String genFormatString(List<Integer> stringLengths) {
        StringBuilder fsb = new StringBuilder();
        stringLengths.forEach(length -> {
            fsb.append("%-");
            fsb.append(length);
            fsb.append("s");
        });
        return fsb.toString();
    }

    /**
     * Prints out the JSON string in pretty format.
     *
     * @param mapper        Object mapper
     * @param jsonString    JSON string
     * @return pretty formatted JSON string
     */
    public static String prettyJson(ObjectMapper mapper, String jsonString) {
        try {
            Object jsonObject = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (IOException e) {
            log.debug("Json string parsing exception caused by {}", e);
        }
        return null;
    }

    /**
     * Obtains workable kubernetes client.
     *
     * @param config kubernetes API config
     * @return kubernetes client
     */
    public static KubernetesClient k8sClient(KubevirtApiConfig config) {
        if (config == null) {
            log.warn("Kubernetes API server config is empty.");
            return null;
        }

        String endpoint = endpoint(config);

        ConfigBuilder configBuilder = new ConfigBuilder().withMasterUrl(endpoint);

        if (config.scheme() == KubevirtApiConfig.Scheme.HTTPS) {
            configBuilder.withTrustCerts(true)
                    .withCaCertData(config.caCertData())
                    .withClientCertData(config.clientCertData())
                    .withClientKeyData(config.clientKeyData());

            if (StringUtils.isNotEmpty(config.token())) {
                configBuilder.withOauthToken(config.token());
            }
        }

        return new DefaultKubernetesClient(configBuilder.build());
    }

    /**
     * Gets the ovsdb client with supplied openstack node.
     *
     * @param node              kubernetes node
     * @param ovsdbPort         ovsdb port
     * @param ovsdbController   ovsdb controller
     * @return ovsdb client
     */
    public static OvsdbClientService getOvsdbClient(KubevirtNode node,
                                                    int ovsdbPort,
                                                    OvsdbController ovsdbController) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(node.managementIp(), ovsdbPort);
        return ovsdbController.getOvsdbClient(ovsdb);
    }

    /**
     * Checks whether the controller has a connection with an OVSDB that resides
     * inside the given kubernetes node.
     *
     * @param node              kubernetes node
     * @param ovsdbPort         OVSDB port
     * @param ovsdbController   OVSDB controller
     * @param deviceService     device service
     * @return true if the controller is connected to the OVSDB, false otherwise
     */
    public static boolean isOvsdbConnected(KubevirtNode node,
                                           int ovsdbPort,
                                           OvsdbController ovsdbController,
                                           DeviceService deviceService) {
        OvsdbClientService client = getOvsdbClient(node, ovsdbPort, ovsdbController);
        return deviceService.isAvailable(node.ovsdb()) &&
                client != null &&
                client.isConnected();
    }

    /**
     * Adds or removes a network interface (aka port) into a given bridge of kubernetes node.
     *
     * @param k8sNode       kubernetes node
     * @param bridgeName    bridge name
     * @param intfName      interface name
     * @param deviceService device service
     * @param addOrRemove   add port is true, remove it otherwise
     */
    public static synchronized void addOrRemoveSystemInterface(KubevirtNode k8sNode,
                                                               String bridgeName,
                                                               String intfName,
                                                               DeviceService deviceService,
                                                               boolean addOrRemove) {


        Device device = deviceService.getDevice(k8sNode.ovsdb());
        if (device == null || !device.is(BridgeConfig.class)) {
            log.info("device is null or this device if not ovsdb device");
            return;
        }
        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);

        if (addOrRemove) {
            bridgeConfig.addPort(BridgeName.bridgeName(bridgeName), intfName);
        } else {
            bridgeConfig.deletePort(BridgeName.bridgeName(bridgeName), intfName);
        }
    }

    /**
     * Re-structures the OVS port name.
     * The length of OVS port name should be not large than 15.
     *
     * @param portName  original port name
     * @return re-structured OVS port name
     */
    public static String structurePortName(String portName) {

        // The size of OVS port name should not be larger than 15
        if (portName.length() > PORT_NAME_MAX_LENGTH) {
            return StringUtils.substring(portName, 0, PORT_NAME_MAX_LENGTH);
        }

        return portName;
    }

    /**
     * Gets Boolean property from the propertyName
     * Return null if propertyName is not found.
     *
     * @param properties   properties to be looked up
     * @param propertyName the name of the property to look up
     * @return value when the propertyName is defined or return null
     */
    public static Boolean getBooleanProperty(Dictionary<?, ?> properties,
                                             String propertyName) {
        Boolean value;
        try {
            String s = get(properties, propertyName);
            value = Strings.isNullOrEmpty(s) ? null : Boolean.valueOf(s);
        } catch (ClassCastException e) {
            value = null;
        }
        return value;
    }

    /**
     * Returns the type of the given kubernetes node.
     *
     * @param node kubernetes node
     * @return node type
     */
    public static KubevirtNode.Type getNodeType(Node node) {
        Set<String> rolesFull = node.getMetadata().getLabels().keySet().stream()
                .filter(l -> l.contains(K8S_ROLE))
                .collect(Collectors.toSet());

        KubevirtNode.Type nodeType = WORKER;

        for (String roleStr : rolesFull) {
            String role = roleStr.split("/")[1];
            if (MASTER.name().equalsIgnoreCase(role)) {
                nodeType = MASTER;
                break;
            }
        }

        Map<String, String> annots = node.getMetadata().getAnnotations();
        String gatewayConfig = annots.get(GATEWAY_CONFIG_KEY);
        if (gatewayConfig != null) {
            nodeType = GATEWAY;
        }

        return nodeType;
    }

    /**
     * Returns the kubevirt node from the node.
     *
     * @param node a raw node object returned from a k8s client
     * @return kubevirt node
     */
    public static KubevirtNode buildKubevirtNode(Node node) {
        String hostname = node.getMetadata().getName();
        IpAddress managementIp = null;
        IpAddress dataIp = null;

        for (NodeAddress nodeAddress:node.getStatus().getAddresses()) {
            if (nodeAddress.getType().equals(INTERNAL_IP)) {
                managementIp = IpAddress.valueOf(nodeAddress.getAddress());
                dataIp = IpAddress.valueOf(nodeAddress.getAddress());
            }
        }

        Set<String> rolesFull = node.getMetadata().getLabels().keySet().stream()
                .filter(l -> l.contains(K8S_ROLE))
                .collect(Collectors.toSet());

        KubevirtNode.Type nodeType = WORKER;

        for (String roleStr : rolesFull) {
            String role = roleStr.split("/")[1];
            if (MASTER.name().equalsIgnoreCase(role)) {
                nodeType = MASTER;
                break;
            }
        }

        // start to parse kubernetes annotation
        Map<String, String> annots = node.getMetadata().getAnnotations();
        String physnetConfig = annots.get(PHYSNET_CONFIG_KEY);
        String gatewayConfig = annots.get(GATEWAY_CONFIG_KEY);
        String dataIpStr = annots.get(DATA_IP_KEY);
        Set<KubevirtPhyInterface> phys = new HashSet<>();
        String gatewayBridgeName = null;
        try {
            if (physnetConfig != null) {
                JsonArray configJson = JsonArray.readFrom(physnetConfig);

                for (int i = 0; i < configJson.size(); i++) {
                    JsonObject object = configJson.get(i).asObject();
                    String network = object.get(NETWORK_KEY).asString();
                    String intf = object.get(INTERFACE_KEY).asString();

                    if (network != null && intf != null) {
                        String physBridgeId;
                        if (object.get(PHYS_BRIDGE_ID) != null) {
                            physBridgeId = object.get(PHYS_BRIDGE_ID).asString();
                        } else {
                            physBridgeId = genDpidFromName(network + intf + hostname);
                            log.trace("host {} physnet dpid for network {} intf {} is null so generate dpid {}",
                                    hostname, network, intf, physBridgeId);
                        }

                        phys.add(DefaultKubevirtPhyInterface.builder()
                                .network(network)
                                .intf(intf)
                                .physBridge(DeviceId.deviceId(physBridgeId))
                                .build());
                    }
                }
            }

            if (dataIpStr != null) {
                dataIp = IpAddress.valueOf(dataIpStr);
            }

            if (gatewayConfig != null) {
                JsonNode jsonNode = new ObjectMapper().readTree(gatewayConfig);

                nodeType = GATEWAY;
                gatewayBridgeName = jsonNode.get(GATEWAY_BRIDGE_NAME).asText();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse physnet config or gateway config object", e);
        }

        // if the node is taint with kubevirt.io key configured,
        // we mark this node as OTHER type, and do not add it into the cluster
        NodeSpec spec = node.getSpec();
        if (spec.getTaints() != null) {
            for (Taint taint : spec.getTaints()) {
                String effect = taint.getEffect();
                String key = taint.getKey();
                String value = taint.getValue();

                if (StringUtils.equals(effect, NO_SCHEDULE_EFFECT) &&
                    StringUtils.equals(key, KUBEVIRT_IO_KEY) &&
                    StringUtils.equals(value, DRAINING_VALUE)) {
                    nodeType = OTHER;
                }
            }
        }

        return DefaultKubevirtNode.builder()
                .hostname(hostname)
                .managementIp(managementIp)
                .dataIp(dataIp)
                .type(nodeType)
                .state(KubevirtNodeState.ON_BOARDED)
                .phyIntfs(phys)
                .gatewayBridgeName(gatewayBridgeName)
                .build();
    }

    /**
     * Generates a unique dpid from given name.
     *
     * @param name name
     * @return device id in string
     */
    public static String genDpidFromName(String name) {
        if (name != null) {
            String hexString = Integer.toHexString(name.hashCode());
            return OF_PREFIX + Strings.padStart(hexString, 16, '0');
        }
        return null;
    }

    /**
     * Resolve a DNS with the given DNS server and hostname.
     *
     * @param hostname      hostname to be resolved
     * @return resolved IP address
     */
    public static IpAddress resolveHostname(String hostname) {
        try {
            InetAddress addr = Address.getByName(hostname);
            return IpAddress.valueOf(IpAddress.Version.INET, addr.getAddress());
        } catch (UnknownHostException e) {
            log.warn("Failed to resolve IP address of host {}", hostname);
        }
        return null;
    }

    /**
     * Waits for the given length of time.
     *
     * @param timeSecond the amount of time for wait in second unit
     */
    public static void waitFor(int timeSecond) {
        try {
            Thread.sleep(timeSecond * 1000L);
        } catch (Exception e) {
            log.error(e.toString());
        }
    }
}
