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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
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
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants.VnicType;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknetworking.impl.DefaultInstancePort;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackAuth.Perspective;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackSshAuth;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.types.Facing;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.common.BasicResource;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.networking.domain.NeutronPort;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.onlab.packet.Ip4Address.valueOf;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC_STR;
import static org.onosproject.openstacknetworking.api.Constants.DIRECT;
import static org.onosproject.openstacknetworking.api.Constants.FLOATING_IP_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.NETWORK_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_REST_PATH;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.api.Constants.PCI_VENDOR_INFO;
import static org.onosproject.openstacknetworking.api.Constants.PORT_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_PREFIX_VM;
import static org.onosproject.openstacknetworking.api.Constants.PORT_NAME_VHOST_USER_PREFIX_VM;
import static org.onosproject.openstacknetworking.api.Constants.REST_PASSWORD;
import static org.onosproject.openstacknetworking.api.Constants.REST_PORT;
import static org.onosproject.openstacknetworking.api.Constants.REST_USER;
import static org.onosproject.openstacknetworking.api.Constants.REST_UTF8;
import static org.onosproject.openstacknetworking.api.Constants.ROUTER_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.ROUTER_INTF_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.SECURITY_GROUP_FORMAT;
import static org.onosproject.openstacknetworking.api.Constants.SUBNET_FORMAT;
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

    private static final String PARENTHESES_START = "(";
    private static final String PARENTHESES_END = ")";

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

    private static final String DL_DST = "dl_dst=";
    private static final String NW_DST = "nw_dst=";
    private static final String DEFAULT_REQUEST_STRING = "sudo ovs-appctl ofproto/trace br-int ip";
    private static final String IN_PORT = "in_port=";
    private static final String NW_SRC = "nw_src=";
    private static final String COMMA = ",";
    private static final String TUN_ID = "tun_id=";

    private static final String DEVICE_OWNER_GW = "network:router_gateway";
    private static final String DEVICE_OWNER_IFACE = "network:router_interface";

    private static final String NOT_AVAILABLE = "N/A";

    private static final long TIMEOUT_MS = 5000;
    private static final long WAIT_OUTPUT_STREAM_SECOND = 2;
    private static final int SSH_PORT = 22;

    private static final int TAP_PORT_LENGTH = 11;
    private static final int PORT_NAME_MAX_LENGTH = 15;

    /**
     * Prevents object instantiation from external.
     */
    private OpenstackNetworkingUtil() {
    }

    /**
     * Interprets JSON string to corresponding openstack model entity object.
     *
     * @param inputStr JSON string
     * @param entityClazz openstack model entity class
     * @return openstack model entity object
     */
    public static ModelEntity jsonToModelEntity(String inputStr, Class entityClazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            InputStream input = toInputStream(inputStr, REST_UTF8);
            JsonNode jsonTree = mapper.enable(INDENT_OUTPUT).readTree(input);
            log.trace(new ObjectMapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(entityClazz)
                    .readerFor(entityClazz)
                    .readValue(jsonTree);
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e);
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
            return (ObjectNode) mapper.readTree(strModelEntity.getBytes(Charsets.UTF_8));
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
            if (fip.getFixedIpAddress().equals(port.ipAddress().toString()) &&
                    fip.getPortId().equals(port.portId())) {
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
    public static OpenstackNode getGwByComputeDevId(Set<OpenstackNode> gws,
                                                    DeviceId deviceId) {
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
            log.error("Authentication failed due to {}", e);
            return null;
        }
    }

    /**
     * Checks whether the given openstack port is smart NIC capable.
     *
     * @param port openstack port
     * @return true if the given port is smart NIC capable, false otherwise
     */
    public static boolean isSmartNicCapable(Port port) {
        if (port.getProfile() != null && port.getvNicType().equals(DIRECT)) {
            String vendorInfo = String.valueOf(port.getProfile().get(PCI_VENDOR_INFO));
            if (portNamePrefixMap().containsKey(vendorInfo)) {
                log.debug("Port {} is a Smart NIC capable port.", port.getId());
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Extract the interface name with the supplied port.
     *
     * @param port port
     * @return interface name
     */
    public static String getIntfNameFromPciAddress(Port port) {
        String intfName;

        if (port.getProfile() == null || port.getProfile().isEmpty()) {
            log.error("Port profile is not found");
            return null;
        }

        if (!port.getProfile().containsKey(PCISLOT) ||
                Strings.isNullOrEmpty(port.getProfile().get(PCISLOT).toString())) {
            log.error("Failed to retrieve the interface name because of no " +
                    "pci_slot information from the port");
            return null;
        }

        String vendorInfoForPort = String.valueOf(port.getProfile().get(PCI_VENDOR_INFO));

        if (!portNamePrefixMap().containsKey(vendorInfoForPort)) {
            log.debug("{} is an non-smart NIC prefix.", vendorInfoForPort);
            return UNSUPPORTED_VENDOR;
        }

        String portNamePrefix = portNamePrefixMap().get(vendorInfoForPort);

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
    public static boolean hasIntfAleadyInDevice(DeviceId deviceId,
                                                String intfName,
                                                DeviceService deviceService) {
        checkNotNull(deviceId);
        checkNotNull(intfName);

        return deviceService.getPorts(deviceId).stream().anyMatch(port ->
                Objects.equals(port.annotations().value(PORT_NAME), intfName));
    }

    /**
     * Adds router interfaces to openstack admin service.
     *
     * @param osPort        port
     * @param adminService  openstack admin service
     */
    public static void addRouterIface(Port osPort,
                                      OpenstackRouterAdminService adminService) {
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
            } catch (IOException e) {
                log.error("IOException occurred because of {}", e);
            }
        });
    }

    /**
     * Prints openstack security group.
     *
     * @param osSg  openstack security group
     */
    public static void printSecurityGroup(SecurityGroup osSg) {
        print(SECURITY_GROUP_FORMAT, osSg.getId(), deriveResourceName(osSg));
    }

    /**
     * Prints openstack network.
     *
     * @param osNet openstack network
     */
    public static void printNetwork(Network osNet) {
        final String strNet = String.format(NETWORK_FORMAT,
                osNet.getId(),
                deriveResourceName(osNet),
                osNet.getProviderSegID(),
                osNet.getSubnets());
        print(strNet);
    }

    /**
     * Prints openstack subnet.
     *
     * @param osSubnet      openstack subnet
     * @param osNetService  openstack network service
     */
    public static void printSubnet(Subnet osSubnet,
                                   OpenstackNetworkService osNetService) {
        final Network network = osNetService.network(osSubnet.getNetworkId());
        final String netName = network == null ? NOT_AVAILABLE : deriveResourceName(network);
        final String strSubnet = String.format(SUBNET_FORMAT,
                osSubnet.getId(),
                netName,
                osSubnet.getCidr());
        print(strSubnet);
    }

    /**
     * Prints openstack port.
     *
     * @param osPort        openstack port
     * @param osNetService  openstack network service
     */
    public static void printPort(Port osPort,
                                 OpenstackNetworkService osNetService) {
        List<String> fixedIps = osPort.getFixedIps().stream()
                .map(IP::getIpAddress)
                .collect(Collectors.toList());
        final Network network = osNetService.network(osPort.getNetworkId());
        final String netName = network == null ? NOT_AVAILABLE : deriveResourceName(network);
        final String strPort = String.format(PORT_FORMAT,
                osPort.getId(),
                netName,
                osPort.getMacAddress(),
                fixedIps.isEmpty() ? "" : fixedIps);
        print(strPort);
    }

    /**
     * Prints openstack router.
     *
     * @param osRouter      openstack router
     * @param osNetService  openstack network service
     */
    public static void printRouter(Router osRouter,
                                   OpenstackNetworkService osNetService) {
        List<String> externals = osNetService.ports().stream()
                .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                        Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_GW))
                .flatMap(osPort -> osPort.getFixedIps().stream())
                .map(IP::getIpAddress)
                .collect(Collectors.toList());

        List<String> internals = osNetService.ports().stream()
                .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                        Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_IFACE))
                .flatMap(osPort -> osPort.getFixedIps().stream())
                .map(IP::getIpAddress)
                .collect(Collectors.toList());

        final String strRouter = String.format(ROUTER_FORMAT,
                osRouter.getId(),
                deriveResourceName(osRouter),
                externals.isEmpty() ? "" : externals,
                internals.isEmpty() ? "" : internals);
        print(strRouter);
    }

    /**
     * Prints openstack router interface.
     *
     * @param osRouterIntf  openstack router interface
     */
    public static void printRouterIntf(RouterInterface osRouterIntf) {
        final String strRouterIntf = String.format(ROUTER_INTF_FORMAT,
                osRouterIntf.getId(),
                osRouterIntf.getTenantId(),
                osRouterIntf.getSubnetId());
        print(strRouterIntf);
    }

    /**
     * Prints openstack floating IP.
     *
     * @param floatingIp    floating IP
     */
    public static void printFloatingIp(NetFloatingIP floatingIp) {
        final String strFloating = String.format(FLOATING_IP_FORMAT,
                floatingIp.getId(),
                floatingIp.getFloatingIpAddress(),
                Strings.isNullOrEmpty(floatingIp.getFixedIpAddress()) ?
                        "" : floatingIp.getFixedIpAddress());
        print(strFloating);
    }

    /**
     * Obtains the property value with specified property key name.
     *
     * @param properties    a collection of properties
     * @param name          key name
     * @return mapping value
     */
    public static String getPropertyValue(Set<ConfigProperty> properties,
                                          String name) {
        Optional<ConfigProperty> property =
                properties.stream().filter(p -> p.name().equals(name)).findFirst();
        return property.map(ConfigProperty::value).orElse(null);
    }

    /**
     * Obtains the boolean property value with specified property key name.
     *
     * @param properties    a collection of properties
     * @param name          key name
     * @return mapping value
     */
    public static boolean getPropertyValueAsBoolean(Set<ConfigProperty> properties,
                                                    String name) {
        Optional<ConfigProperty> property =
                properties.stream().filter(p -> p.name().equals(name)).findFirst();

        return property.map(ConfigProperty::asBoolean).orElse(false);
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

    /**
     * Returns the vnic type of given port.
     *
     * @param portName port name
     * @return vnit type
     */
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
        DefaultHttpRequestParser requestParser =
                                new DefaultHttpRequestParser(sessionInputBuffer);
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

            HttpMessageWriter<HttpRequest> requestWriter =
                                new DefaultHttpRequestWriter(sessionOutputBuffer);
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
        DefaultHttpResponseParser responseParser =
                            new DefaultHttpResponseParser(sessionInputBuffer);
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

            log.debug(headerBaos.toString(Charsets.UTF_8.name()));

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
            log.debug(baos.toString(Charsets.UTF_8.name()));

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

    /**
     * Creates flow trace request string.
     *
     * @param srcIp src ip address
     * @param dstIp dst ip address
     * @param srcInstancePort src instance port
     * @param osNetService openstack networking service
     * @param uplink true if this request is for uplink
     * @return flow trace request string
     */
    public static String traceRequestString(String srcIp,
                                            String dstIp,
                                            InstancePort srcInstancePort,
                                            OpenstackNetworkService osNetService,
                                            boolean uplink) {

        StringBuilder requestStringBuilder = new StringBuilder(DEFAULT_REQUEST_STRING);

        if (uplink) {

            requestStringBuilder.append(COMMA)
                    .append(IN_PORT)
                    .append(srcInstancePort.portNumber().toString())
                    .append(COMMA)
                    .append(NW_SRC)
                    .append(srcIp)
                    .append(COMMA);

            String modifiedDstIp = dstIp;
            Type netType = osNetService.networkType(srcInstancePort.networkId());
            if (netType == Type.VXLAN || netType == Type.GRE ||
                    netType == Type.VLAN || netType == Type.GENEVE) {
                if (srcIp.equals(dstIp)) {
                    modifiedDstIp = osNetService.gatewayIp(srcInstancePort.portId());
                    requestStringBuilder.append(DL_DST)
                            .append(DEFAULT_GATEWAY_MAC_STR).append(COMMA);
                } else if (!osNetService.ipPrefix(srcInstancePort.portId()).contains(
                        IpAddress.valueOf(dstIp))) {
                    requestStringBuilder.append(DL_DST)
                            .append(DEFAULT_GATEWAY_MAC_STR)
                            .append(COMMA);
                }
            } else {
                if (srcIp.equals(dstIp)) {
                    modifiedDstIp = osNetService.gatewayIp(srcInstancePort.portId());
                }
            }

            requestStringBuilder.append(NW_DST)
                    .append(modifiedDstIp)
                    .append("\n");
        } else {
            requestStringBuilder.append(COMMA)
                    .append(NW_SRC)
                    .append(dstIp)
                    .append(COMMA);

            Type netType = osNetService.networkType(srcInstancePort.networkId());

            if (netType == Type.VXLAN || netType == Type.GRE ||
                    netType == Type.VLAN || netType == Type.GENEVE) {
                requestStringBuilder.append(TUN_ID)
                        .append(osNetService.segmentId(srcInstancePort.networkId()))
                        .append(COMMA);
            }
            requestStringBuilder.append(NW_DST)
                    .append(srcIp)
                    .append("\n");
        }

        return requestStringBuilder.toString();
    }

    /**
     * Sends flow trace string to node.
     *
     * @param requestString reqeust string
     * @param node src node
     * @return flow trace result in string format
     */
    public static String sendTraceRequestToNode(String requestString,
                                                OpenstackNode node) {
        String traceResult = null;
        OpenstackSshAuth sshAuth = node.sshAuthInfo();

        try (SshClient client = SshClient.setUpDefaultClient()) {
            client.start();

            try (ClientSession session = client
                    .connect(sshAuth.id(), node.managementIp().getIp4Address().toString(), SSH_PORT)
                    .verify(TIMEOUT_MS, TimeUnit.SECONDS).getSession()) {
                session.addPasswordIdentity(sshAuth.password());
                session.auth().verify(TIMEOUT_MS, TimeUnit.SECONDS);


                try (ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_SHELL)) {

                    log.debug("requestString: {}", requestString);
                    final InputStream inputStream =
                            new ByteArrayInputStream(requestString.getBytes(Charsets.UTF_8));

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    OutputStream errStream = new ByteArrayOutputStream();

                    channel.setIn(new NoCloseInputStream(inputStream));
                    channel.setErr(errStream);
                    channel.setOut(outputStream);

                    Collection<ClientChannelEvent> eventList = Lists.newArrayList();
                    eventList.add(ClientChannelEvent.OPENED);

                    OpenFuture channelFuture = channel.open();

                    if (channelFuture.await(TIMEOUT_MS, TimeUnit.SECONDS)) {

                        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

                        while (!channelFuture.isOpened()) {
                            if ((timeoutExpiredMs - System.currentTimeMillis()) <= 0) {
                                log.error("Failed to open channel");
                                return null;
                            }
                        }
                        TimeUnit.SECONDS.sleep(WAIT_OUTPUT_STREAM_SECOND);

                        traceResult = outputStream.toString(Charsets.UTF_8.name());

                        channel.close();
                    }
                } finally {
                    session.close();
                }
            } finally {
                client.stop();
            }

        } catch (Exception e) {
            log.error("Exception occurred because of {}", e);
        }

        return traceResult;
    }

    /**
     * Returns the floating ip with supplied instance port.
     *
     * @param instancePort instance port
     * @param osRouterAdminService openstack router admin service
     * @return floating ip
     */
    public static NetFloatingIP floatingIpByInstancePort(InstancePort instancePort,
                                                         OpenstackRouterAdminService
                                                                 osRouterAdminService) {
        return osRouterAdminService.floatingIps().stream()
                .filter(netFloatingIP -> netFloatingIP.getPortId() != null)
                .filter(netFloatingIP -> netFloatingIP.getPortId().equals(instancePort.portId()))
                .findAny().orElse(null);
    }

    /**
     * Sends GARP packet with supplied floating ip information.
     *
     * @param floatingIP floating ip
     * @param instancePort instance port
     * @param vlanId vlain id
     * @param gatewayNode gateway node
     * @param packetService packet service
     */
    public static void processGarpPacketForFloatingIp(NetFloatingIP floatingIP,
                                                      InstancePort instancePort,
                                                      VlanId vlanId,
                                                      OpenstackNode gatewayNode,
                                                      PacketService packetService) {
        Ethernet ethernet = buildGratuitousArpPacket(floatingIP, instancePort, vlanId);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(gatewayNode.uplinkPortNum()).build();

        packetService.emit(new DefaultOutboundPacket(gatewayNode.intgBridge(), treatment,
                ByteBuffer.wrap(ethernet.serialize())));
    }

    /**
     * Returns the external peer router with supplied network information.
     *
     * @param network network
     * @param osNetworkService openstack network service
     * @param osRouterAdminService openstack router admin service
     * @return external peer router
     */
    public static ExternalPeerRouter externalPeerRouterForNetwork(Network network,
                                                                  OpenstackNetworkService
                                                                          osNetworkService,
                                                                  OpenstackRouterAdminService
                                                                          osRouterAdminService) {
        if (network == null) {
            return null;
        }

        Subnet subnet = osNetworkService.subnets(network.getId())
                .stream().findAny().orElse(null);

        if (subnet == null) {
            return null;
        }

        RouterInterface osRouterIface = osRouterAdminService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), subnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            return null;
        }

        Router osRouter = osRouterAdminService.router(osRouterIface.getId());
        if (osRouter == null || osRouter.getExternalGatewayInfo() == null) {
            return null;
        }

        ExternalGateway exGatewayInfo = osRouter.getExternalGatewayInfo();
        return osNetworkService.externalPeerRouter(exGatewayInfo);

    }

    /**
     * Returns the external peer router with specified subnet information.
     *
     * @param subnet openstack subnet
     * @param osRouterService openstack router service
     * @param osNetworkService openstack network service
     * @return external peer router
     */
    public static ExternalPeerRouter externalPeerRouterFromSubnet(Subnet subnet,
                                                                  OpenstackRouterService
                                                                          osRouterService,
                                                                  OpenstackNetworkService
                                                                          osNetworkService) {
        Router osRouter = getRouterFromSubnet(subnet, osRouterService);
        if (osRouter == null) {
            return null;
        }
        if (osRouter.getExternalGatewayInfo() == null) {
            // this router does not have external connectivity
            log.trace("router({}) has no external gateway", deriveResourceName(osRouter));
            return null;
        }

        return osNetworkService.externalPeerRouter(osRouter.getExternalGatewayInfo());
    }

    /**
     * Returns the external gateway IP address with specified router information.
     *
     * @param router openstack router
     * @param osNetworkService openstack network service
     * @return external IP address
     */
    public static IpAddress externalGatewayIp(Router router,
                                              OpenstackNetworkService osNetworkService) {
        return externalGatewayIpBase(router, false, osNetworkService);
    }

    /**
     * Returns the external gateway IP address (SNAT enabled) with specified router information.
     *
     * @param router openstack router
     * @param osNetworkService openstack network service
     * @return external IP address
     */
    public static IpAddress externalGatewayIpSnatEnabled(Router router,
                                                         OpenstackNetworkService osNetworkService) {
        return externalGatewayIpBase(router, true, osNetworkService);
    }

    /**
     * Returns the tunnel port number with specified net ID and openstack node.
     *
     * @param netId network ID
     * @param netService network service
     * @param osNode openstack node
     * @return tunnel port number
     */
    public static PortNumber tunnelPortNumByNetId(String netId,
                                                  OpenstackNetworkService netService,
                                                  OpenstackNode osNode) {
        Type netType = netService.networkType(netId);

        if (netType == null) {
            return null;
        }

        return tunnelPortNumByNetType(netType, osNode);
    }

    /**
     * Returns the tunnel port number with specified net type and openstack node.
     *
     * @param netType network type
     * @param osNode openstack node
     * @return tunnel port number
     */
    public static PortNumber tunnelPortNumByNetType(Type netType, OpenstackNode osNode) {
        switch (netType) {
            case VXLAN:
                return osNode.vxlanTunnelPortNum();
            case GRE:
                return osNode.greTunnelPortNum();
            case GENEVE:
                return osNode.geneveTunnelPortNum();
            default:
                return null;
        }
    }

    /**
     * Returns the REST URL of active node.
     *
     * @param haService openstack HA service
     * @return REST URL of active node
     */
    public static String getActiveUrl(OpenstackHaService haService) {
        return "http://" + haService.getActiveIp().toString() + ":" +
                REST_PORT + "/" + OPENSTACK_NETWORKING_REST_PATH + "/";
    }

    /**
     * Returns the REST client instance with given resource path.
     *
     * @param haService         openstack HA service
     * @param resourcePath      resource path
     * @return REST client instance
     */
    public static WebTarget getActiveClient(OpenstackHaService haService,
                                            String resourcePath) {
        HttpAuthenticationFeature feature =
                HttpAuthenticationFeature.universal(REST_USER, REST_PASSWORD);
        Client client = ClientBuilder.newClient().register(feature);
        return client.target(getActiveUrl(haService)).path(resourcePath);
    }

    /**
     * Returns the post response from the active node.
     *
     * @param haService         openstack HA service
     * @param resourcePath      resource path
     * @param input             input
     * @return post response
     */
    public static Response syncPost(OpenstackHaService haService,
                                    String resourcePath,
                                    String input) {

        log.debug("Sync POST request with {} on {}",
                            haService.getActiveIp().toString(), resourcePath);

        return getActiveClient(haService, resourcePath)
                .request(APPLICATION_JSON_TYPE)
                .post(Entity.json(input));
    }

    /**
     * Returns the put response from the active node.
     *
     * @param haService         openstack HA service
     * @param resourcePath      resource path
     * @param id                resource identifier
     * @param input             input
     * @return put response
     */
    public static Response syncPut(OpenstackHaService haService,
                                   String resourcePath,
                                   String id, String input) {
        return syncPut(haService, resourcePath, null, id, input);
    }

    /**
     * Returns the put response from the active node.
     *
     * @param haService         openstack HA service
     * @param resourcePath      resource path
     * @param id                resource identifier
     * @param suffix            resource suffix
     * @param input             input
     * @return put response
     */
    public static Response syncPut(OpenstackHaService haService,
                                   String resourcePath,
                                   String suffix,
                                   String id, String input) {

        log.debug("Sync PUT request with {} on {}",
                haService.getActiveIp().toString(), resourcePath);

        String pathStr = "/" + id;

        if (suffix != null) {
            pathStr += "/" + suffix;
        }

        return getActiveClient(haService, resourcePath)
                .path(pathStr)
                .request(APPLICATION_JSON_TYPE)
                .put(Entity.json(input));
    }

    /**
     * Returns the delete response from the active node.
     *
     * @param haService         openstack HA service
     * @param resourcePath      resource path
     * @param id            resource identifier
     * @return delete response
     */
    public static Response syncDelete(OpenstackHaService haService,
                                      String resourcePath,
                                      String id) {

        log.debug("Sync DELETE request with {} on {}",
                haService.getActiveIp().toString(), resourcePath);

        return getActiveClient(haService, resourcePath)
                .path("/" + id)
                .request(APPLICATION_JSON_TYPE)
                .delete();
    }

    /**
     * Gets the ovsdb client with supplied openstack node.
     *
     * @param node          openstack node
     * @param ovsdbPort     openvswitch DB port number
     * @param controller    openvswitch DB controller instance
     * @return ovsdb client instance
     */
    public static OvsdbClientService getOvsdbClient(OpenstackNode node, int ovsdbPort,
                                                    OvsdbController controller) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(node.managementIp(), ovsdbPort);
        return controller.getOvsdbClient(ovsdb);
    }

    /**
     * Obtains the name of interface attached to the openstack VM.
     *
     * @param portId openstack port identifier
     * @return name of interface
     */
    public static String ifaceNameFromOsPortId(String portId) {
        if (portId != null) {
            return PORT_NAME_PREFIX_VM + StringUtils.substring(portId, 0, TAP_PORT_LENGTH);
        }

        return null;
    }

    /**
     * Return the router associated with the given subnet.
     *
     * @param subnet openstack subnet
     * @param osRouterService openstack router service
     * @return router
     */
    public static Router getRouterFromSubnet(Subnet subnet,
                                             OpenstackRouterService osRouterService) {
        RouterInterface osRouterIface = osRouterService.routerInterfaces().stream()
                .filter(i -> Objects.equals(i.getSubnetId(), subnet.getId()))
                .findAny().orElse(null);
        if (osRouterIface == null) {
            return null;
        }

        return osRouterService.router(osRouterIface.getId());
    }

    private static boolean isDirectPort(String portName) {
        return portNamePrefixMap().values().stream().anyMatch(portName::startsWith);
    }

    /**
     * Returns GARP packet with supplied floating ip and instance port information.
     *
     * @param floatingIP floating ip
     * @param instancePort instance port
     * @param vlanId vlan id
     * @return GARP packet
     */
    private static Ethernet buildGratuitousArpPacket(NetFloatingIP floatingIP,
                                                     InstancePort instancePort,
                                                     VlanId vlanId) {
        Ethernet ethernet = new Ethernet();
        ethernet.setDestinationMACAddress(MacAddress.BROADCAST);
        ethernet.setSourceMACAddress(instancePort.macAddress());
        ethernet.setEtherType(Ethernet.TYPE_ARP);
        ethernet.setVlanID(vlanId.id());

        ARP arp = new ARP();
        arp.setOpCode(ARP.OP_REPLY);
        arp.setProtocolType(ARP.PROTO_TYPE_IP);
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);

        arp.setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH);
        arp.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);

        arp.setSenderHardwareAddress(instancePort.macAddress().toBytes());
        arp.setTargetHardwareAddress(MacAddress.BROADCAST.toBytes());

        arp.setSenderProtocolAddress(valueOf(floatingIP.getFloatingIpAddress()).toInt());
        arp.setTargetProtocolAddress(valueOf(floatingIP.getFloatingIpAddress()).toInt());

        ethernet.setPayload(arp);

        return ethernet;
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
     * Obtains flow group key from the given id.
     *
     * @param groupId flow group identifier
     * @return flow group key
     */
    public static GroupKey getGroupKey(int groupId) {
        return new DefaultGroupKey((Integer.toString(groupId)).getBytes());
    }

    /**
     * Calculate the broadcast address from given IP address and subnet prefix length.
     *
     * @param ipAddr        IP address
     * @param prefixLength  subnet prefix length
     * @return broadcast address
     */
    public static String getBroadcastAddr(String ipAddr, int prefixLength) {
        String subnet = ipAddr + "/" + prefixLength;
        SubnetUtils utils = new SubnetUtils(subnet);
        return utils.getInfo().getBroadcastAddress();
    }

    /**
     * Obtains the DHCP server name from option.
     *
     * @param port neutron port
     * @return server name
     */
    public static String getDhcpServerName(NeutronPort port) {
        return getDhcpOptionValue(port, "server-ip-address");
    }

    /**
     * Obtains the DHCP static boot file name from option.
     *
     * @param port neutron port
     * @return DHCP static boot file name
     */
    public static String getDhcpStaticBootFileName(NeutronPort port) {
        return getDhcpOptionValue(port, "tag:!ipxe,67");
    }

    /**
     * Obtains the DHCP full boot file name from option.
     *
     * @param port neutron port
     * @return DHCP full boot file name
     */
    public static String getDhcpFullBootFileName(NeutronPort port) {
        return getDhcpOptionValue(port, "tag:ipxe,67");
    }

    /**
     * Returns a valid resource name.
     *
     * @param resource openstack basic resource object
     * @return a valid resource name
     */
    public static String deriveResourceName(BasicResource resource) {
        if (Strings.isNullOrEmpty(resource.getName())) {
            return PARENTHESES_START + resource.getId() + PARENTHESES_END;
        } else {
            return resource.getName();
        }
    }

    /**
     * Returns a shifted IP address.
     *
     * @param ipAddress     IP address to be shifted
     * @param shiftPrefix   A IP prefix used in shifted IP address
     * @return shifted Ip address
     */
    public static String shiftIpDomain(String ipAddress, String shiftPrefix) {
        String origIpPrefix = ipAddress.split("\\.")[0] + "." + ipAddress.split("\\.")[1];
        return StringUtils.replace(ipAddress, origIpPrefix, shiftPrefix);
    }

    /**
     * Returns an unshifted IP address.
     *
     * @param ipAddress     IP address to be unshifted
     * @param ipPrefix      IP prefix which to be used for unshifting
     * @param cidr          a POD network CIDR
     * @return unshifted IP address
     */
    public static String unshiftIpDomain(String ipAddress,
                                         String ipPrefix,
                                         String cidr) {

        String origIpPrefix = cidr.split("\\.")[0] + "." + cidr.split("\\.")[1];
        return StringUtils.replace(ipAddress, ipPrefix, origIpPrefix);
    }

    private static String getDhcpOptionValue(NeutronPort port, String optionNameStr) {
        ObjectNode node = modelEntityToJson(port, NeutronPort.class);

        if (node != null) {
            JsonNode portJson = node.get("port");
            ArrayNode options = (ArrayNode) portJson.get("extra_dhcp_opts");
            for (JsonNode option : options) {
                String optionName = option.get("optName").asText();
                if (StringUtils.equals(optionName, optionNameStr)) {
                    return option.get("optValue").asText();
                }
            }
        }

        return null;
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
     * Returns the external gateway IP address with specified router information.
     *
     * @param router openstack router
     * @param snatOnly true for only query SNAT enabled case, false otherwise
     * @param osNetworkService openstack network service
     * @return external IP address
     */
    private static IpAddress externalGatewayIpBase(Router router, boolean snatOnly,
                                                   OpenstackNetworkService osNetworkService) {
        if (router == null) {
            return null;
        }

        ExternalGateway externalGateway = router.getExternalGatewayInfo();
        if (externalGateway == null) {
            log.info("Failed to get external IP for router {} because no " +
                            "external gateway is associated with the router",
                    router.getId());
            return null;
        }

        if (snatOnly) {
            if (!externalGateway.isEnableSnat()) {
                log.warn("The given router {} SNAT is configured as false", router.getId());
                return null;
            }
        }

        // TODO fix openstack4j for ExternalGateway provides external fixed IP list
        Port exGatewayPort = osNetworkService.ports(externalGateway.getNetworkId())
                .stream()
                .filter(port -> Objects.equals(port.getDeviceId(), router.getId()))
                .findAny().orElse(null);

        if (exGatewayPort == null) {
            return null;
        }

        return IpAddress.valueOf(exGatewayPort.getFixedIps().stream()
                .findAny().get().getIpAddress());
    }

    private static void print(String format, Object... args) {
        System.out.println(String.format(format, args));
    }
}