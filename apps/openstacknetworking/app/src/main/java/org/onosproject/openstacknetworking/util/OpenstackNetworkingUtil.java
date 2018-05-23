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
package org.onosproject.openstacknetworking.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackAuth.Perspective;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.api.Constants.PCI_VENDOR_INFO;
import static org.onosproject.openstacknetworking.api.Constants.portNamePrefixMap;
import static org.openstack4j.core.transport.ObjectMapperSingleton.getContext;

/**
 * An utility that used in openstack networking app.
 */
public final class OpenstackNetworkingUtil {

    protected static final Logger log = LoggerFactory.getLogger(OpenstackNetworkingUtil.class);

    private static final int HEX_RADIX = 16;
    private static final String ZERO_FUNCTION_NUMBER = "0";
    private static final String PREFIX_DEVICE_NUMBER = "s";
    private static final String PREFIX_FUNCTION_NUMBER = "f";

    // keystone endpoint related variables
    private static final String DOMAIN_DEFAULT = "default";
    private static final String KEYSTONE_V2 = "v2.0";
    private static final String KEYSTONE_V3 = "v3";
    private static final String IDENTITY_PATH = "identity/";
    private static final String SSL_TYPE = "SSL";

    private static final String ERR_FLOW = "Failed set flows for floating IP %s: ";

    /**
     * Prevents object instantiation from external.
     */
    private OpenstackNetworkingUtil() {
    }

    /**
     * Interprets JSON string to corresponding openstack model entity object.
     *
     * @param input JSON string
     * @param entityClazz openstack model entity class
     * @return openstack model entity object
     */
    public static ModelEntity jsonToModelEntity(InputStream input, Class entityClazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonTree = mapper.enable(INDENT_OUTPUT).readTree(input);
            log.trace(new ObjectMapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(entityClazz)
                    .readerFor(entityClazz)
                    .readValue(jsonTree);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Converts openstack model entity object into JSON object.
     *
     * @param entity openstack model entity object
     * @param entityClazz openstack model entity class
     * @return JSON object
     */
    public static ObjectNode modelEntityToJson(ModelEntity entity, Class entityClazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String strModelEntity = ObjectMapperSingleton.getContext(entityClazz)
                    .writerFor(entityClazz)
                    .writeValueAsString(entity);
            log.trace(strModelEntity);
            return (ObjectNode) mapper.readTree(strModelEntity.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    /**
     * Obtains a floating IP associated with the given instance port.
     *
     * @param port instance port
     * @param fips a collection of floating IPs
     * @return associated floating IP
     */
    public static NetFloatingIP associatedFloatingIp(InstancePort port,
                                                     Set<NetFloatingIP> fips) {
        for (NetFloatingIP fip : fips) {
            if (Strings.isNullOrEmpty(fip.getFixedIpAddress())) {
                continue;
            }
            if (Strings.isNullOrEmpty(fip.getFloatingIpAddress())) {
                continue;
            }
            if (fip.getFixedIpAddress().equals(port.ipAddress().toString())) {
                return fip;
            }
        }
        return null;
    }

    /**
     * Checks whether the given floating IP is associated with a VM.
     *
     * @param service openstack network service
     * @param fip floating IP
     * @return true if the given floating IP associated with a VM, false otherwise
     */
    public static boolean isAssociatedWithVM(OpenstackNetworkService service,
                                             NetFloatingIP fip) {
        Port osPort = service.port(fip.getPortId());
        if (osPort == null) {
            return false;
        }

        if (!Strings.isNullOrEmpty(osPort.getDeviceId())) {
            Network osNet = service.network(osPort.getNetworkId());
            if (osNet == null) {
                final String errorFormat = ERR_FLOW + "no network(%s) exists";
                final String error = String.format(errorFormat,
                        fip.getFloatingIpAddress(), osPort.getNetworkId());
                throw new IllegalStateException(error);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Obtains the gateway node by instance port.
     *
     * @param gateways      a collection of gateway nodes
     * @param instPort      instance port
     * @return a gateway node
     */
    public static OpenstackNode getGwByInstancePort(Set<OpenstackNode> gateways,
                                                    InstancePort instPort) {
        OpenstackNode gw = null;
        if (instPort != null && instPort.deviceId() != null) {
            gw = getGwByComputeDevId(gateways, instPort.deviceId());
        }
        return gw;
    }

    /**
     * Obtains the gateway node by device in compute node. Note that the gateway
     * node is determined by device's device identifier.
     *
     * @param gws           a collection of gateway nodes
     * @param deviceId      device identifier
     * @return a gateway node
     */
    public static OpenstackNode getGwByComputeDevId(Set<OpenstackNode> gws, DeviceId deviceId) {
        int numOfGw = gws.size();

        if (numOfGw == 0) {
            return null;
        }

        int gwIndex = Math.abs(deviceId.hashCode()) % numOfGw;

        return getGwByIndex(gws, gwIndex);
    }

    /**
     * Obtains a connected openstack client.
     *
     * @param osNode openstack node
     * @return a connected openstack client
     */
    public static OSClient getConnectedClient(OpenstackNode osNode) {
        OpenstackAuth auth = osNode.authentication();
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
            log.error("Authentication failed due to {}", e.toString());
            return null;
        }
    }

    /**
     * Extract the interface name with the supplied port.
     *
     * @param port port
     * @return interface name
     */
    public static String getIntfNameFromPciAddress(Port port) {

        if (port.getProfile() == null) {
            log.error("Port profile is not found");
            return null;
        }

        if (port.getProfile() != null && port.getProfile().get(PCISLOT) == null) {
            log.error("Failed to retrieve the interface name because of no pci_slot information from the port");
            return null;
        }

        String busNumHex = port.getProfile().get(PCISLOT).toString().split(":")[1];
        String busNumDecimal = String.valueOf(Integer.parseInt(busNumHex, HEX_RADIX));

        String deviceNumHex = port.getProfile().get(PCISLOT).toString()
                .split(":")[2]
                .split("\\.")[0];
        String deviceNumDecimal = String.valueOf(Integer.parseInt(deviceNumHex, HEX_RADIX));

        String functionNumHex = port.getProfile().get(PCISLOT).toString()
                .split(":")[2]
                .split("\\.")[1];
        String functionNumDecimal = String.valueOf(Integer.parseInt(functionNumHex, HEX_RADIX));

        String intfName;

        String vendorInfoForPort = String.valueOf(port.getProfile().get(PCI_VENDOR_INFO));

        if (vendorInfoForPort == null) {
            log.error("Failed to retrieve the interface name because of no pci vendor information from the port");
            return null;
        }
        String portNamePrefix = portNamePrefixMap().get(vendorInfoForPort);

        if (functionNumDecimal.equals(ZERO_FUNCTION_NUMBER)) {
            intfName = portNamePrefix + busNumDecimal + PREFIX_DEVICE_NUMBER + deviceNumDecimal;
        } else {
            intfName = portNamePrefix + busNumDecimal + PREFIX_DEVICE_NUMBER + deviceNumDecimal
                    + PREFIX_FUNCTION_NUMBER + functionNumDecimal;
        }

        return intfName;
    }

    /**
     * Adds router interfaces to openstack admin service.
     * TODO fix the logic to add router interface to router
     *
     * @param osPort        port
     * @param adminService  openstack admin service
     */
    public static void addRouterIface(Port osPort, OpenstackRouterAdminService adminService) {
        osPort.getFixedIps().forEach(p -> {
            JsonNode jsonTree = new ObjectMapper().createObjectNode()
                    .put("id", osPort.getDeviceId())
                    .put("tenant_id", osPort.getTenantId())
                    .put("subnet_id", p.getSubnetId())
                    .put("port_id", osPort.getId());
            try {
                RouterInterface rIface = getContext(NeutronRouterInterface.class)
                        .readerFor(NeutronRouterInterface.class)
                        .readValue(jsonTree);
                if (adminService.routerInterface(rIface.getPortId()) != null) {
                    adminService.updateRouterInterface(rIface);
                } else {
                    adminService.addRouterInterface(rIface);
                }
            } catch (IOException ignore) {
            }
        });
    }

    /**
     * Builds up and a complete endpoint URL from gateway node.
     *
     * @param node gateway node
     * @return a complete endpoint URL
     */
    private static String buildEndpoint(OpenstackNode node) {

        OpenstackAuth auth = node.authentication();

        StringBuilder endpointSb = new StringBuilder();
        endpointSb.append(auth.protocol().name().toLowerCase());
        endpointSb.append("://");
        endpointSb.append(node.endPoint());
        endpointSb.append(":");
        endpointSb.append(auth.port());
        endpointSb.append("/");

        // in case the version is v3, we need to append identity path into endpoint
        if (auth.version().equals(KEYSTONE_V3)) {
            endpointSb.append(IDENTITY_PATH);
        }

        endpointSb.append(auth.version());
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
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
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
            log.error("Failed to access OpenStack service due to {}", e.toString());
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
}
