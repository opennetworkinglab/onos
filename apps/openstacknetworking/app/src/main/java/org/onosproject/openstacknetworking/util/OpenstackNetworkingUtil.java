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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.DefaultHttpRequestWriter;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.io.HttpMessageWriter;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknetworking.api.Constants.VnicType;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.impl.DefaultInstancePort;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.api.Constants.PCI_VENDOR_INFO;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_PREFIX_VM;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_VHOST_USER_PREFIX_VM;
import static org.onosproject.openstacknetworking.api.Constants.UNSUPPORTED_VENDOR;
import static org.onosproject.openstacknetworking.api.Constants.portNamePrefixMap;
import static org.openstack4j.core.transport.ObjectMapperSingleton.getContext;

/**
 * An utility that used in openstack networking app.
 */
public final class OpenstackNetworkingUtil {

    private static final Logger log = LoggerFactory.getLogger(OpenstackNetworkingUtil.class);

    private static final int HEX_RADIX = 16;
    private static final String ZERO_FUNCTION_NUMBER = "0";
    private static final String PREFIX_DEVICE_NUMBER = "s";
    private static final String PREFIX_FUNCTION_NUMBER = "f";

    // keystone endpoint related variables
    private static final String DOMAIN_DEFAULT = "default";
    private static final String KEYSTONE_V2 = "v2.0";
    private static final String KEYSTONE_V3 = "v3";
    private static final String SSL_TYPE = "SSL";

    private static final String PROXY_MODE = "proxy";
    private static final String BROADCAST_MODE = "broadcast";

    private static final String ENABLE = "enable";
    private static final String DISABLE = "disable";

    private static final int HTTP_PAYLOAD_BUFFER = 8 * 1024;

    private static final String HMAC_SHA256 = "HmacSHA256";

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
        } catch (IOException e) {
            log.error("IOException occurred because of {}", e.toString());
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
        if (port.getProfile() == null || port.getProfile().isEmpty()) {
            log.error("Port profile is not found");
            return null;
        }

        if (!port.getProfile().containsKey(PCISLOT) ||
                Strings.isNullOrEmpty(port.getProfile().get(PCISLOT).toString())) {
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

        if (!portNamePrefixMap().containsKey(vendorInfoForPort)) {
            log.warn("Failed to retrieve the interface name because of unsupported prefix for vendor ID {}",
                    vendorInfoForPort);
            return UNSUPPORTED_VENDOR;
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
     * Check if the given interface is added to the given device or not.
     *
     * @param deviceId device ID
     * @param intfName interface name
     * @param deviceService device service
     * @return true if the given interface is added to the given device or false otherwise
     */
    public static boolean hasIntfAleadyInDevice(DeviceId deviceId, String intfName, DeviceService deviceService) {
        checkNotNull(deviceId);
        checkNotNull(intfName);

        return deviceService.getPorts(deviceId).stream()
                .anyMatch(port -> Objects.equals(port.annotations().value(PORT_NAME), intfName));
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
                log.error("Exception occurred because of {}", ignore.toString());
            }
        });
    }

    /**
     * Obtains the property value with specified property key name.
     *
     * @param properties    a collection of properties
     * @param name          key name
     * @return mapping value
     */
    public static String getPropertyValue(Set<ConfigProperty> properties, String name) {
        Optional<ConfigProperty> property =
                properties.stream().filter(p -> p.name().equals(name)).findFirst();
        return property.map(ConfigProperty::value).orElse(null);
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
        } catch (JsonParseException e) {
            log.debug("JsonParseException caused by {}", e);
        } catch (JsonMappingException e) {
            log.debug("JsonMappingException caused by {}", e);
        } catch (JsonProcessingException e) {
            log.debug("JsonProcessingException caused by {}", e);
        } catch (IOException e) {
            log.debug("IOException caused by {}", e);
        }
        return null;
    }

    /**
     * Checks the validity of ARP mode.
     *
     * @param arpMode ARP mode
     * @return returns true if the ARP mode is valid, false otherwise
     */
    public static boolean checkArpMode(String arpMode) {

        if (isNullOrEmpty(arpMode)) {
            return false;
        } else {
            return arpMode.equals(PROXY_MODE) || arpMode.equals(BROADCAST_MODE);
        }
    }

    /**
     * Checks the validity of activation flag.
     *
     * @param activationFlag activation flag
     * @return returns true if the activation flag is valid, false otherwise
     */
    public static boolean checkActivationFlag(String activationFlag) {

        switch (activationFlag) {
            case ENABLE:
                return true;
            case DISABLE:
                return false;
            default:
                throw new IllegalArgumentException("The given activation flag is not valid!");
        }
    }

    /**
     * Swaps current location with old location info.
     * The revised instance port will be used to mod the flow rules after migration.
     *
     * @param instPort instance port
     * @return location swapped instance port
     */
    public static InstancePort swapStaleLocation(InstancePort instPort) {
        return DefaultInstancePort.builder()
                .deviceId(instPort.oldDeviceId())
                .portNumber(instPort.oldPortNumber())
                .state(instPort.state())
                .ipAddress(instPort.ipAddress())
                .macAddress(instPort.macAddress())
                .networkId(instPort.networkId())
                .portId(instPort.portId())
                .build();
    }

    /**
     * Compares two router interfaces are equal.
     * Will be remove this after Openstack4j implements equals.
     *
     * @param routerInterface1 router interface
     * @param routerInterface2 router interface
     * @return returns true if two router interfaces are equal, false otherwise
     */
    public static boolean routerInterfacesEquals(RouterInterface routerInterface1,
                                                 RouterInterface routerInterface2) {
        return Objects.equals(routerInterface1.getId(), routerInterface2.getId()) &&
                Objects.equals(routerInterface1.getPortId(), routerInterface2.getPortId()) &&
                Objects.equals(routerInterface1.getSubnetId(), routerInterface2.getSubnetId()) &&
                Objects.equals(routerInterface1.getTenantId(), routerInterface2.getTenantId());
    }

    public static VnicType vnicType(String portName) {
        if (portName.startsWith(PORT_NAME_PREFIX_VM) ||
                portName.startsWith(PORT_NAME_VHOST_USER_PREFIX_VM)) {
            return VnicType.NORMAL;
        } else if (isDirectPort(portName)) {
            return VnicType.DIRECT;
        } else {
            return VnicType.UNSUPPORTED;
        }
    }

    /**
     * Deserializes raw payload into HttpRequest object.
     *
     * @param rawData raw http payload
     * @return HttpRequest object
     */
    public static HttpRequest parseHttpRequest(byte[] rawData) {
        SessionInputBufferImpl sessionInputBuffer =
                new SessionInputBufferImpl(
                        new HttpTransportMetricsImpl(), HTTP_PAYLOAD_BUFFER);
        sessionInputBuffer.bind(new ByteArrayInputStream(rawData));
        DefaultHttpRequestParser requestParser = new DefaultHttpRequestParser(sessionInputBuffer);
        try {
            return requestParser.parse();
        } catch (IOException | HttpException e) {
            log.warn("Failed to parse HttpRequest, due to {}", e);
        }

        return null;
    }

    /**
     * Serializes HttpRequest object to byte array.
     *
     * @param request http request object
     * @return byte array
     */
    public static byte[] unparseHttpRequest(HttpRequest request) {
        try {
            SessionOutputBufferImpl sessionOutputBuffer =
                    new SessionOutputBufferImpl(
                            new HttpTransportMetricsImpl(), HTTP_PAYLOAD_BUFFER);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            sessionOutputBuffer.bind(baos);

            HttpMessageWriter<HttpRequest> requestWriter = new DefaultHttpRequestWriter(
                    sessionOutputBuffer);
            requestWriter.write(request);
            sessionOutputBuffer.flush();

            return baos.toByteArray();
        } catch (HttpException | IOException e) {
            log.warn("Failed to unparse HttpRequest, due to {}", e);
        }

        return null;
    }

    /**
     * Deserializes raw payload into HttpResponse object.
     *
     * @param rawData raw http payload
     * @return HttpResponse object
     */
    public static HttpResponse parseHttpResponse(byte[] rawData) {
        SessionInputBufferImpl sessionInputBuffer =
                new SessionInputBufferImpl(
                        new HttpTransportMetricsImpl(), HTTP_PAYLOAD_BUFFER);
        sessionInputBuffer.bind(new ByteArrayInputStream(rawData));
        DefaultHttpResponseParser responseParser = new DefaultHttpResponseParser(sessionInputBuffer);
        try {
            return responseParser.parse();
        } catch (IOException | HttpException e) {
            log.warn("Failed to parse HttpResponse, due to {}", e);
        }

        return null;
    }

    /**
     * Serializes HttpResponse header to byte array.
     *
     * @param response http response object
     * @return byte array
     */
    public static byte[] unparseHttpResponseHeader(HttpResponse response) {
        try {
            SessionOutputBufferImpl sessionOutputBuffer =
                    new SessionOutputBufferImpl(
                            new HttpTransportMetricsImpl(), HTTP_PAYLOAD_BUFFER);

            ByteArrayOutputStream headerBaos = new ByteArrayOutputStream();
            sessionOutputBuffer.bind(headerBaos);

            HttpMessageWriter<HttpResponse> responseWriter =
                    new DefaultHttpResponseWriter(sessionOutputBuffer);
            responseWriter.write(response);
            sessionOutputBuffer.flush();

            log.debug(headerBaos.toString());

            return headerBaos.toByteArray();
        } catch (IOException | HttpException e) {
            log.warn("Failed to unparse HttpResponse headers, due to {}", e);
        }

        return null;
    }

    /**
     * Serializes HttpResponse object to byte array.
     *
     * @param response http response object
     * @return byte array
     */
    public static byte[] unparseHttpResponseBody(HttpResponse response) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo(baos);

            log.debug(response.toString());
            log.debug(baos.toString());

            return baos.toByteArray();
        } catch (IOException e) {
            log.warn("Failed to unparse HttpResponse, due to {}", e);
        }

        return null;
    }

    /**
     * Encodes the given data using HmacSHA256 encryption method with given secret key.
     *
     * @param key       secret key
     * @param data      data to be encrypted
     * @return Hmac256 encrypted data
     */
    public static String hmacEncrypt(String key, String data) {
        try {
            Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), HMAC_SHA256);
            sha256Hmac.init(secretKey);
            return Hex.encodeHexString(sha256Hmac.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            log.warn("Failed to encrypt data {} using key {}, due to {}", data, key, e);
        }
        return null;
    }

    private static boolean isDirectPort(String portName) {
        return portNamePrefixMap().values().stream().anyMatch(p -> portName.startsWith(p));
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