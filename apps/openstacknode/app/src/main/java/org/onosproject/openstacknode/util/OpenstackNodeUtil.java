/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.onosproject.net.Device;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackAuth.Perspective;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.onlab.util.Tools.get;

/**
 * An utility that used in openstack node app.
 */
public final class OpenstackNodeUtil {
    private static final Logger log = LoggerFactory.getLogger(OpenstackNodeUtil.class);

    // keystone endpoint related variables
    private static final String DOMAIN_DEFAULT = "default";
    private static final String KEYSTONE_V2 = "v2.0";
    private static final String KEYSTONE_V3 = "v3";
    private static final String SSL_TYPE = "SSL";

    private static final int HEX_LENGTH = 16;
    private static final String OF_PREFIX = "of:";
    private static final String ZERO = "0";

    private static final String DPDK_DEVARGS = "dpdk-devargs";
    private static final String NOT_AVAILABLE = "N/A";

    private static final int PORT_NAME_MAX_LENGTH = 15;

    /**
     * Prevents object installation from external.
     */
    private OpenstackNodeUtil() {
    }

    /**
     * Checks whether the controller has a connection with an OVSDB that resides
     * inside the given openstack node.
     *
     * @param osNode openstack node
     * @param ovsdbPort ovsdb port
     * @param ovsdbController ovsdb controller
     * @param deviceService device service
     * @return true if the controller is connected to the OVSDB, false otherwise
     */
    public static boolean isOvsdbConnected(OpenstackNode osNode,
                                           int ovsdbPort,
                                           OvsdbController ovsdbController,
                                           DeviceService deviceService) {
        OvsdbClientService client = getOvsdbClient(osNode, ovsdbPort, ovsdbController);
        return deviceService.isAvailable(osNode.ovsdb()) &&
                client != null &&
                client.isConnected();
    }

    /**
     * Gets the ovsdb client with supplied openstack node.
     *
     * @param osNode openstack node
     * @param ovsdbPort ovsdb port
     * @param ovsdbController ovsdb controller
     * @return ovsdb client
     */
    public static OvsdbClientService getOvsdbClient(OpenstackNode osNode,
                                                    int ovsdbPort,
                                                    OvsdbController ovsdbController) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(osNode.managementIp(), ovsdbPort);
        return ovsdbController.getOvsdbClient(ovsdb);
    }

    /**
     * Obtains a connected openstack client.
     *
     * @param osNode openstack node
     * @return a connected openstack client
     */
    public static OSClient getConnectedClient(OpenstackNode osNode) {
        OpenstackAuth auth = osNode.keystoneConfig().authentication();
        String endpoint = buildEndpoint(osNode);
        Perspective perspective = auth.perspective();

        Config config = getSslConfig();

        try {
            if (endpoint.contains(KEYSTONE_V2)) {
                IOSClientBuilder.V2 builder = OSFactory.builderV2()
                        .endpoint(endpoint)
                        .tenantName(auth.project())
                        .credentials(auth.username(), auth.password())
                        .withConfig(config);

                if (perspective != null) {
                    builder.perspective(getFacing(perspective));
                }

                return builder.authenticate();
            } else if (endpoint.contains(KEYSTONE_V3)) {

                Identifier project = Identifier.byName(auth.project());
                Identifier domain = Identifier.byName(DOMAIN_DEFAULT);

                IOSClientBuilder.V3 builder = OSFactory.builderV3()
                        .endpoint(endpoint)
                        .credentials(auth.username(), auth.password(), domain)
                        .scopeToProject(project, domain)
                        .withConfig(config);

                if (perspective != null) {
                    builder.perspective(getFacing(perspective));
                }

                return builder.authenticate();
            } else {
                log.warn("Unrecognized keystone version type");
                return null;
            }
        } catch (AuthenticationException e) {
            log.error("Authentication failed due to {}", e);
            return null;
        }
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
            log.error("Exception occurred because of {}. set valud to null..", e);
            value = null;
        }
        return value;
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
     * Adds or removes a network interface (aka port) into a given bridge of openstack node.
     *
     * @param osNode openstack node
     * @param bridgeName bridge name
     * @param intfName interface name
     * @param deviceService device service
     * @param addOrRemove add port is true, remove it otherwise
     */
    public static synchronized void addOrRemoveSystemInterface(OpenstackNode osNode,
                                                               String bridgeName,
                                                               String intfName,
                                                               DeviceService deviceService,
                                                               boolean addOrRemove) {


        Device device = deviceService.getDevice(osNode.ovsdb());
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
     * Adds or removes a dpdk interface into a given openstack node.
     *
     * @param osNode openstack node
     * @param dpdkInterface dpdk interface
     * @param ovsdbPort ovsdb port
     * @param ovsdbController ovsdb controller
     * @param addOrRemove add port is true, remove it otherwise
     */
    public static synchronized void addOrRemoveDpdkInterface(OpenstackNode osNode,
                                                             DpdkInterface dpdkInterface,
                                                             int ovsdbPort,
                                                             OvsdbController ovsdbController,
                                                             boolean addOrRemove) {

        OvsdbClientService client = getOvsdbClient(osNode, ovsdbPort, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return;
        }

        if (addOrRemove) {
            Map<String, String> options =
                    ImmutableMap.of(DPDK_DEVARGS, dpdkInterface.pciAddress());

            OvsdbInterface.Builder builder = OvsdbInterface.builder()
                    .name(dpdkInterface.intf())
                    .type(OvsdbInterface.Type.DPDK)
                    .mtu(dpdkInterface.mtu())
                    .options(options);


            client.createInterface(dpdkInterface.deviceName(), builder.build());
        } else {
            client.dropInterface(dpdkInterface.intf());
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
     * Obtains the gateway node by openstack node. Note that the gateway
     * node is determined by device's device identifier.
     *
     * @param gws                a collection of gateway nodes
     * @param openstackNode      device identifier
     * @return the hostname of selected gateway node
     */
    public static String getGwByComputeNode(Set<OpenstackNode> gws,
                                            OpenstackNode openstackNode) {
        int numOfGw = gws.size();

        if (numOfGw == 0) {
            return NOT_AVAILABLE;
        }

        if (!openstackNode.type().equals(OpenstackNode.NodeType.COMPUTE)) {
            return NOT_AVAILABLE;
        }

        int gwIndex = Math.abs(openstackNode.intgBridge().hashCode()) % numOfGw;

        return getGwByIndex(gws, gwIndex).hostname();
    }

    /**
     * Obtains gateway instance by giving index number.
     *
     * @param gws       a collection of gateway nodes
     * @param index     index number
     * @return gateway instance
     */
    private static OpenstackNode getGwByIndex(Set<OpenstackNode> gws, int index) {
        Map<String, OpenstackNode> hashMap = new HashMap<>();
        gws.forEach(gw -> hashMap.put(gw.hostname(), gw));
        TreeMap<String, OpenstackNode> treeMap = new TreeMap<>(hashMap);
        Iterator<String> iteratorKey = treeMap.keySet().iterator();

        int intIndex = 0;
        OpenstackNode gw = null;
        while (iteratorKey.hasNext()) {
            String key = iteratorKey.next();

            if (intIndex == index) {
                gw = treeMap.get(key);
            }
            intIndex++;
        }
        return gw;
    }

    /**
     * Builds up and a complete endpoint URL from gateway node.
     *
     * @param node gateway node
     * @return a complete endpoint URL
     */
    private static String buildEndpoint(OpenstackNode node) {

        OpenstackAuth auth = node.keystoneConfig().authentication();

        StringBuilder endpointSb = new StringBuilder();
        endpointSb.append(auth.protocol().name().toLowerCase());
        endpointSb.append("://");
        endpointSb.append(node.keystoneConfig().endpoint());
        return endpointSb.toString();
    }

    /**
     * Obtains the SSL config without verifying the certification.
     *
     * @return SSL config
     */
    private static Config getSslConfig() {
        // we bypass the SSL certification verification for now
        // TODO: verify server side SSL using a given certification
        Config config = Config.newConfig().withSSLVerificationDisabled();

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                        return;
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                        return;
                    }
                }
        };

        HostnameVerifier allHostsValid = (hostname, session) -> true;

        try {
            SSLContext sc = SSLContext.getInstance(SSL_TYPE);
            sc.init(null, trustAllCerts,
                    new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            config.withSSLContext(sc);
        } catch (Exception e) {
            log.error("Failed to access OpenStack service due to {}", e);
            return null;
        }

        return config;
    }

    /**
     * Obtains the facing object with given openstack perspective.
     *
     * @param perspective keystone perspective
     * @return facing object
     */
    private static Facing getFacing(Perspective perspective) {

        switch (perspective) {
            case PUBLIC:
                return Facing.PUBLIC;
            case ADMIN:
                return Facing.ADMIN;
            case INTERNAL:
                return Facing.INTERNAL;
            default:
                return null;
        }
    }
}
