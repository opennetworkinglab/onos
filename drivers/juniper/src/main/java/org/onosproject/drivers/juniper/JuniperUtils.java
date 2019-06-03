/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.drivers.juniper;

import com.google.common.base.MoreObjects;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Port.Type;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.slf4j.Logger;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.onosproject.drivers.juniper.StaticRoute.DEFAULT_METRIC_STATIC_ROUTE;
import static org.onosproject.drivers.juniper.StaticRoute.toFlowRulePriority;
import static org.onosproject.drivers.utilities.XmlConfigParser.loadXmlString;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.PortNumber.portNumber;
import static org.slf4j.LoggerFactory.getLogger;

// Ref: Junos YANG:
//   https://github.com/Juniper/yang

/**
 * Utility class for Netconf XML for Juniper.
 * Tested with MX240 junos 14.2
 */
public final class JuniperUtils {

    private static final Logger log = getLogger(JuniperUtils.class);

    public static final String FAILED_CFG = "Failed to retrieve configuration.";

    private static final String RPC_TAG_NETCONF_BASE = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String RPC_CLOSE_TAG = "</rpc>";

    //requests
    public static final String REQ_LLDP_NBR_INFO = "<get-lldp-neighbors-information/>";
    public static final String REQ_SYS_INFO = "<get-system-information/>";
    public static final String REQ_MAC_ADD_INFO = "<get-chassis-mac-addresses/>";
    public static final String REQ_IF_INFO = "<get-interface-information/>";

    //helper strings for parsing
    private static final String LLDP_LIST_NBR_INFO = "lldp-neighbors-information";
    private static final String LLDP_NBR_INFO = "lldp-neighbor-information";
    private static final String SYS_INFO = "system-information";
    private static final String HW_MODEL = "hardware-model";
    private static final String OS_NAME = "os-name";
    private static final String OS_VER = "os-version";
    private static final String SER_NUM = "serial-number";
    private static final String IF_INFO = "interface-information";
    private static final String IF_PHY = "physical-interface";

    private static final String IF_TYPE = "if-type";
    private static final String IF_MEDIA_TYPE = "if-media-type";
    private static final String SPEED = "speed";
    private static final String NAME = "name";
    private static final String PORT = "port";
    private static final String PROTOCOL = "protocol";

    private static final String FIBER = "fiber";
    private static final String COPPER = "copper";

    private static final String TCP = "tcp";

    // seems to be unique index within device
    private static final String SNMP_INDEX = "snmp-index";

    private static final String LLDP_LO_PORT = "lldp-local-port-id";
    private static final String LLDP_REM_CHASS = "lldp-remote-chassis-id";
    private static final String LLDP_REM_PORT_SUBTYPE = "lldp-remote-port-id-subtype";
    private static final String LLDP_REM_PORT = "lldp-remote-port-id";
    private static final String LLDP_REM_PORT_DES = "lldp-remote-port-description";
    private static final String LLDP_SUBTYPE_MAC = "Mac address";
    private static final String LLDP_SUBTYPE_INTERFACE_NAME = "Interface name";
    // For older JUNOS e.g. 15.1
    private static final Pattern ADD_PATTERN_JUNOS15_1 =
            Pattern.compile(".*Private base address\\s*([:,0-9,a-f,A-F]*).*", Pattern.DOTALL);


    private static final String PROTOCOL_NAME = "protocol-name";

    private static final String JUNIPER = "JUNIPER";
    private static final String UNKNOWN = "UNKNOWN";

    /**
     * Annotation key for interface type.
     */
    static final String AK_IF_TYPE = "ifType";

    /**
     * Annotation key for Logical link-layer encapsulation.
     */
    static final String AK_ENCAPSULATION = "encapsulation";

    /**
     * Annotation key for IP.
     */
    static final String AK_IP = "ip";


    /**
     * Annotation key for interface description.
     */
    static final String AK_DESCRIPTION = "description";

    /**
     * Annotation key for interface admin status. "up"/"down"
     */
    static final String AK_ADMIN_STATUS = "adminStatus";

    /**
     * Annotation key for interface operational status. "up"/"down"
     */
    static final String AK_OPER_STATUS = "operStatus";

    /**
     * Annotation key for logical-interfaces parent physical interface name.
     */
    static final String AK_PHYSICAL_PORT_NAME = "physicalPortName";


    private static final String NUMERIC_SPEED_REGEXP = "(\\d+)([GM])bps";

    /**
     * {@value #NUMERIC_SPEED_REGEXP} as {@link Pattern}.
     * Case insensitive
     */
    private static final Pattern SPEED_PATTERN =
            Pattern.compile(NUMERIC_SPEED_REGEXP, Pattern.CASE_INSENSITIVE);

    /**
     * Default port speed {@value} Mbps.
     */
    static final long DEFAULT_PORT_SPEED = 1000;


    private JuniperUtils() {
        //not called, preventing any allocation
    }

    /**
     * Helper method to build a XML schema given a request.
     *
     * @param request a tag element of the XML schema
     * @return string containing the XML schema
     */
    public static String requestBuilder(String request) {
        return RPC_TAG_NETCONF_BASE +
                request + RPC_CLOSE_TAG;
    }

    /**
     * Helper method to commit a config.
     *
     * @return string contains the result of the commit
     */
    public static String commitBuilder() {
        return RPC_TAG_NETCONF_BASE +
                "<commit/>" + RPC_CLOSE_TAG;
    }

    /**
     * Helper method to build the schema for returning to a previously
     * committed configuration.
     *
     * @param versionToReturn Configuration to return to. The range of values is from 0 through 49.
     *                        The most recently saved configuration is number 0,
     *                        and the oldest saved configuration is number 49.
     * @return string containing the XML schema
     */
    public static String rollbackBuilder(int versionToReturn) {
        return RPC_TAG_NETCONF_BASE +
                "<get-rollback-information>" +
                "<rollback>" + versionToReturn + "</rollback>" +
                "</get-rollback-information>" +
                RPC_CLOSE_TAG;
    }


    /**
     * Helper method to build an XML schema to configure a static route
     * given a {@link StaticRoute}.
     *
     * @param staticRoute the static route to be configured
     * @return string contains the result of the configuration
     */
    public static String routeAddBuilder(StaticRoute staticRoute) {
        StringBuilder rpc = new StringBuilder("<configuration>\n");
        rpc.append("<routing-options>\n");
        rpc.append("<static>\n");
        rpc.append("<route>\n");
        rpc.append("<destination>").append(staticRoute.ipv4Dst().toString()).append("</destination>\n");
        rpc.append("<next-hop>").append(staticRoute.nextHop()).append("</next-hop>\n");

        if (staticRoute.getMetric() != DEFAULT_METRIC_STATIC_ROUTE) {
            rpc.append("<metric>").append(staticRoute.getMetric()).append("</metric>");
        }

        rpc.append("</route>\n");
        rpc.append("</static>\n");
        rpc.append("</routing-options>\n");
        rpc.append("</configuration>\n");

        return rpc.toString();
    }

    /**
     * Helper method to build a XML schema to delete a static route
     * given a {@link StaticRoute}.
     * @param staticRoute the static route to be deleted
     * @return string contains the result of the configuratio
     */
    public static String routeDeleteBuilder(StaticRoute staticRoute) {
        return "<configuration>\n" +
        "<routing-options>\n" +
        "<static>\n" +
        "<route operation=\"delete\">\n" +
        "<name>" + staticRoute.ipv4Dst().toString() + "</name>\n" +
        "</route>\n" +
        "</static>\n" +
        "</routing-options>\n" +
        "</configuration>\n";
    }


    /**
     * Parses device configuration and returns the device description.
     *
     * @param deviceId    the id of the device
     * @param sysInfoCfg  system configuration
     * @param chassisMacAddresses chassis MAC addresses response. Its format depends on JUNOS version of device.
     * @return device description
     */
    public static DeviceDescription parseJuniperDescription(DeviceId deviceId,
                                                            HierarchicalConfiguration sysInfoCfg,
                                                            String chassisMacAddresses) {
        HierarchicalConfiguration info = sysInfoCfg.configurationAt(SYS_INFO);

        String hw = info.getString(HW_MODEL) == null ? UNKNOWN : info.getString(HW_MODEL);
        String sw = UNKNOWN;
        if (info.getString(OS_NAME) != null || info.getString(OS_VER) != null) {
            sw = info.getString(OS_NAME) + " " + info.getString(OS_VER);
        }
        String serial = info.getString(SER_NUM) == null ? UNKNOWN : info.getString(SER_NUM);

        return new DefaultDeviceDescription(deviceId.uri(), ROUTER,
                JUNIPER, hw, sw, serial,
                extractChassisId(chassisMacAddresses),
                DefaultAnnotations.EMPTY);
    }

    /**
     * Parses the chassisMacAddresses argument to find the private-base-address and maps it to a chassis id.
     * @param chassisMacAddresses XML response
     * @return the corresponding chassisId, or null if supplied chassisMacAddresses could not be parsed.
     */
    private static ChassisId extractChassisId(final String chassisMacAddresses) {

        ChassisId result = null;

        // Old JUNOS versions used CLI-style text for chassisMacAddresses, whereas recent versions provide a
        // chassis-mac-addresses XML.
        Matcher matcher = ADD_PATTERN_JUNOS15_1.matcher(chassisMacAddresses);
        if (matcher.lookingAt()) {
            result = new ChassisId(MacAddress.valueOf(matcher.group(1)).toLong());
        } else {
            String pba = loadXmlString(chassisMacAddresses)
                    .configurationAt("chassis-mac-addresses")
                    .configurationAt("mac-address-information")
                    .getString("private-base-address");
            if (StringUtils.isNotBlank(pba)) {
                result = new ChassisId(MacAddress.valueOf(pba).toLong());
            }
        }
        return result;
    }

    /**
     * Parses device ports configuration and returns a list of
     * port description.
     *
     * @param cfg interface configuration
     * @return list of interface descriptions of the device
     */
    public static List<PortDescription> parseJuniperPorts(HierarchicalConfiguration cfg) {
        //This methods ignores some internal ports

        List<PortDescription> portDescriptions = new ArrayList<>();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt(IF_INFO);
        for (HierarchicalConfiguration interfInfo : subtrees) {
            List<HierarchicalConfiguration> interfaceTree =
                    interfInfo.configurationsAt(IF_PHY);
            for (HierarchicalConfiguration phyIntf : interfaceTree) {
                if (phyIntf == null) {
                    continue;
                }
                // parse physical Interface
                parsePhysicalInterface(portDescriptions, phyIntf);
            }
        }
        return portDescriptions;
    }

    /**
     * Parses {@literal physical-interface} tree.
     *
     * @param portDescriptions list to populate Ports found parsing configuration
     * @param phyIntf physical-interface
     */
    private static void parsePhysicalInterface(List<PortDescription> portDescriptions,
                                               HierarchicalConfiguration phyIntf) {
        Builder annotations = DefaultAnnotations.builder();
        PortNumber portNumber = portNumber(phyIntf.getString(SNMP_INDEX));
        String phyPortName = phyIntf.getString(NAME);
        if (portNumber == null) {
            log.debug("Skipping physical-interface {}, no PortNumer",
                      phyPortName);
            log.trace("  {}", phyIntf);
            return;
        }

        setIfNonNull(annotations,
                     AnnotationKeys.PORT_NAME,
                     phyPortName);

        setIfNonNull(annotations,
                     AnnotationKeys.PORT_MAC,
                     phyIntf.getString("current-physical-address"));

        setIfNonNull(annotations,
                     AK_IF_TYPE,
                     phyIntf.getString(IF_TYPE));

        setIfNonNull(annotations,
                     AK_DESCRIPTION,
                     phyIntf.getString("description"));

        boolean opUp = phyIntf.getString("oper-status", "down").equals("up");
        annotations.set(AK_OPER_STATUS, toUpDown(opUp));

        boolean admUp = phyIntf.getString("admin-status", "down").equals("up");
        annotations.set(AK_ADMIN_STATUS, toUpDown(admUp));

        long portSpeed = toMbps(phyIntf.getString(SPEED));
        Type portType = phyIntf.getString(IF_MEDIA_TYPE, COPPER).equalsIgnoreCase(FIBER) ? Type.FIBER : Type.COPPER;

        portDescriptions.add(DefaultPortDescription.builder()
                .withPortNumber(portNumber)
                .isEnabled(admUp && opUp)
                .type(portType)
                .portSpeed(portSpeed)
                .annotations(annotations.build()).build());

        // parse each logical Interface
        for (HierarchicalConfiguration logIntf : phyIntf.configurationsAt("logical-interface")) {
            if (logIntf == null) {
                continue;
            }
            PortNumber lPortNumber = safePortNumber(logIntf.getString(SNMP_INDEX));
            if (lPortNumber == null) {
                log.debug("Skipping logical-interface {} under {}, no PortNumer",
                          logIntf.getString(NAME), phyPortName);
                log.trace("  {}", logIntf);
                continue;
            }

            Builder lannotations = DefaultAnnotations.builder();
            setIfNonNull(lannotations,
                         AnnotationKeys.PORT_NAME,
                         logIntf.getString(NAME));
            setIfNonNull(lannotations,
                         AK_PHYSICAL_PORT_NAME,
                         phyPortName);

            String afName = logIntf.getString("address-family.address-family-name");
            String address = logIntf.getString("address-family.interface-address.ifa-local");
            if (afName != null && address != null) {
                // e.g., inet : IPV4, inet6 : IPV6
                setIfNonNull(lannotations, afName, address);
            }

            // preserving former behavior
            setIfNonNull(lannotations,
                         AK_IP,
                         logIntf.getString("address-family.interface-address.ifa-local"));

            setIfNonNull(lannotations,
                         AK_ENCAPSULATION, logIntf.getString("encapsulation"));

            // TODO confirm if this is correct.
            // Looking at sample data,
            // it seemed all logical loop-back interfaces were down
            boolean lEnabled = logIntf.getString("if-config-flags.iff-up") != null;

            portDescriptions.add(DefaultPortDescription.builder()
                    .withPortNumber(lPortNumber)
                    .isEnabled(admUp && opUp && lEnabled)
                    .type(portType)
                    .portSpeed(portSpeed).annotations(lannotations.build())
                    .build());
        }
    }

    /**
     * Port status as "up"/"down".
     *
     * @param portStatus port status
     * @return "up" if {@code portStats} is {@literal true}, "down" otherwise
     */
    private static String toUpDown(boolean portStatus) {
        return portStatus ? "up" : "down";
    }

    /**
     * Translate interface {@literal speed} value as Mbps value.
     *
     * Note: {@literal Unlimited} and unrecognizable string will be treated as
     *  {@value #DEFAULT_PORT_SPEED} Mbps.
     *
     * @param speed in String
     * @return Mbps
     */
    private static long toMbps(String speed) {
        String s = Strings.nullToEmpty(speed).trim().toLowerCase();
        Matcher matcher = SPEED_PATTERN.matcher(s);
        if (matcher.matches()) {
            // numeric
            long n = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            if ("m".equalsIgnoreCase(unit)) {
                // Mbps
                return n;
            } else {
                // assume Gbps
                return 1000 * n;
            }
        }
        log.trace("Treating unknown speed value {} as default", speed);
        // Unlimited or unrecognizable
        return DEFAULT_PORT_SPEED;
    }

    /**
     * Sets annotation entry if {@literal value} was not {@literal null}.
     *
     * @param builder Annotation Builder
     * @param key Annotation key
     * @param value Annotation value (can be {@literal null})
     */
    private static void setIfNonNull(Builder builder, String key, String value) {
        if (value != null) {
            builder.set(key, value.trim());
        }
    }

    /**
     * Creates PortNumber instance from String.
     *
     * Instead for throwing Exception, it will return null on format error.
     *
     * @param s port number as string
     * @return PortNumber instance or null on error
     */
    private static PortNumber safePortNumber(String s) {
        try {
            return portNumber(s);
        } catch (RuntimeException e) {
            log.trace("Failed parsing PortNumber {}", s, e);
        }
        return null;
    }

    /**
     * Create two LinkDescriptions corresponding to the bidirectional links.
     *
     * @param localDevId  the identity of the local device
     * @param localPort   the port of the local device
     * @param remoteDevId the identity of the remote device
     * @param remotePort  the port of the remote device
     * @param descs       the collection to which the link descriptions
     *                    should be added
     */
    public static void createBiDirLinkDescription(DeviceId localDevId,
                                                  Port localPort,
                                                  DeviceId remoteDevId,
                                                  Port remotePort,
                                                  Set<LinkDescription> descs) {

        ConnectPoint local = new ConnectPoint(localDevId, localPort.number());
        ConnectPoint remote = new ConnectPoint(remoteDevId, remotePort.number());
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.LAYER, "ETHERNET")
                .build();
        descs.add(new DefaultLinkDescription(
                local, remote, Link.Type.DIRECT, true, annotations));
        descs.add(new DefaultLinkDescription(
                remote, local, Link.Type.DIRECT, true, annotations));
    }

    /**
     * Create one way LinkDescriptions.
     *
     * @param localDevId  the identity of the local device
     * @param localPort   the port of the local device
     * @param remoteDevId the identity of the remote device
     * @param remotePort  the port of the remote device
     * @param descs       the collection to which the link descriptions
     *                    should be added
     */
    public static void createOneWayLinkDescription(DeviceId localDevId,
                                                   Port localPort,
                                                   DeviceId remoteDevId,
                                                   Port remotePort,
                                                   Set<LinkDescription> descs) {

        ConnectPoint local = new ConnectPoint(localDevId, localPort.number());
        ConnectPoint remote = new ConnectPoint(remoteDevId, remotePort.number());
        DefaultAnnotations annotations = DefaultAnnotations.builder()
                .set(AnnotationKeys.LAYER, "ETHERNET")
                .build();
        descs.add(new DefaultLinkDescription(
                remote, local, Link.Type.DIRECT, true, annotations));
    }

    /**
     * Parses neighbours discovery information and returns a list of
     * link abstractions.
     *
     * @param info interface configuration
     * @return set of link abstractions
     */
    public static Set<LinkAbstraction> parseJuniperLldp(HierarchicalConfiguration info) {
        Set<LinkAbstraction> neighbour = new HashSet<>();
        List<HierarchicalConfiguration> subtrees =
                info.configurationsAt(LLDP_LIST_NBR_INFO);
        for (HierarchicalConfiguration neighborsInfo : subtrees) {
            List<HierarchicalConfiguration> neighbors =
                    neighborsInfo.configurationsAt(LLDP_NBR_INFO);
            for (HierarchicalConfiguration neighbor : neighbors) {
                String localPortName = neighbor.getString(LLDP_LO_PORT);
                MacAddress mac = MacAddress.valueOf(neighbor.getString(LLDP_REM_CHASS));
                String remotePortId = null;
                long remotePortIndex = -1;
                String remotePortIdSubtype = neighbor.getString(LLDP_REM_PORT_SUBTYPE, null);
                if (remotePortIdSubtype != null) {
                    if (remotePortIdSubtype.equals(LLDP_SUBTYPE_MAC)
                            || remotePortIdSubtype.equals(LLDP_SUBTYPE_INTERFACE_NAME)) {
                        remotePortId = neighbor.getString(LLDP_REM_PORT, null);
                    } else {
                        remotePortIndex = neighbor.getLong(LLDP_REM_PORT, -1);
                    }
                }
                String remotePortDescription = neighbor.getString(LLDP_REM_PORT_DES, null);
                LinkAbstraction link = new LinkAbstraction(
                        localPortName,
                        mac.toLong(),
                        remotePortIndex,
                        remotePortId,
                        remotePortDescription);
                neighbour.add(link);
            }
        }
        return neighbour;
    }

    /**
     * Device representation of the adjacency at the IP Layer. It is immutable.
     */
    static final class LinkAbstraction {
        protected final String localPortName;
        protected final ChassisId remoteChassisId;
        protected final long remotePortIndex;
        protected final String remotePortId;
        protected final String remotePortDescription;

        protected LinkAbstraction(String pName, long chassisId, long pIndex, String pPortId, String pDescription) {
            this.localPortName = pName;
            this.remoteChassisId = new ChassisId(chassisId);
            this.remotePortIndex = pIndex;
            this.remotePortId = pPortId;
            this.remotePortDescription = pDescription;
        }
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass()).omitNullValues()
                    .add("localPortName", localPortName)
                    .add("remoteChassisId", remoteChassisId)
                    .add("remotePortIndex", remotePortIndex)
                    .add("remotePortId", remotePortId)
                    .add("remotePortDescription", remotePortDescription)
                    .toString();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LinkAbstraction that = (LinkAbstraction) o;

            return new EqualsBuilder()
                    .append(remotePortIndex, that.remotePortIndex)
                    .append(localPortName, that.localPortName)
                    .append(remoteChassisId, that.remoteChassisId)
                    .append(remotePortId, that.remotePortId)
                    .append(remotePortDescription, that.remotePortDescription)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(localPortName)
                    .append(remoteChassisId)
                    .append(remotePortIndex)
                    .append(remotePortId)
                    .append(remotePortDescription)
                    .toHashCode();
        }
    }

    enum OperationType {
        ADD,
        REMOVE,
    }

    /**
     * Parses {@literal route-information} tree.
     * This implementation supports only static routes.
     *
     * @param cfg route-information
     * @return a collection of static routes
     */
    public static Collection<StaticRoute> parseRoutingTable(HierarchicalConfiguration cfg) {

        Collection<StaticRoute> staticRoutes = new HashSet<>();
        HierarchicalConfiguration routeInfo =
                cfg.configurationAt("route-information");
        List<HierarchicalConfiguration> routeTables = routeInfo.configurationsAt("route-table");
        for (HierarchicalConfiguration routeTable : routeTables) {
            List<HierarchicalConfiguration> routes = routeTable.configurationsAt("rt");
            for (HierarchicalConfiguration route : routes) {
                if (route != null) {
                    List<HierarchicalConfiguration> rtEntries = route.configurationsAt("rt-entry");
                    rtEntries.forEach(rtEntry -> {
                        if (rtEntry.getString(PROTOCOL_NAME) != null &&
                                rtEntry.getString(PROTOCOL_NAME).contains("Static")) {
                            parseStaticRoute(rtEntry,
                                    route.getString("rt-destination"),
                                    rtEntry.getString("metric"))
                                    .ifPresent(staticRoutes::add);

                        }
                    });
                }
            }
        }
        return staticRoutes;
    }

    /**
     * Parse the {@literal rt-entry} for static routes.
     *
     * @param rtEntry     rt-entry filtered by {@literal protocol-name} equals to Static
     * @param destination rt-destination
     * @return optional of static route
     */
    private static Optional<StaticRoute> parseStaticRoute(HierarchicalConfiguration rtEntry,
                                                          String destination, String metric) {

        Ip4Prefix ipDst = Ip4Prefix.valueOf(destination);

        HierarchicalConfiguration nextHop = rtEntry.configurationAt("nh");
        String to = nextHop.getString("to");
        if (StringUtils.isEmpty(to)) {
            return Optional.empty();
        }
        Ip4Address nextHopIp = Ip4Address.valueOf(to);

        if (metric == null) {
            return Optional.of(new StaticRoute(ipDst, nextHopIp, false));
        } else {
            return Optional.of(new StaticRoute(ipDst, nextHopIp, false,
                    toFlowRulePriority(Integer.parseInt(metric))));
        }
    }

    /**
     * Helper method to build a XML schema for a given "set/merge" Op inJunOS XML Format.
     *
     * @param request a CLI command
     * @return string containing the XML schema
     */
    public static String cliSetRequestBuilder(StringBuilder request) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<edit-config>");
        rpc.append("<target><candidate/></target><config>");
        rpc.append("<configuration>");
        rpc.append(request);
        rpc.append("</configuration>");
        rpc.append("</config></edit-config>");
        rpc.append(RPC_CLOSE_TAG);
        rpc.append("]]>]]>");
        return rpc.toString();
    }

    /**
     * Helper method to build a XML schema for a given "delete Op" in JunOS XML Format.
     *
     * @param request a CLI command
     * @return string containing the XML schema
     */
    public static String cliDeleteRequestBuilder(StringBuilder request) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<edit-config>");
        rpc.append("<target><candidate/></target>");
        rpc.append("<default-operation>none</default-operation>");
        rpc.append("<config>");
        rpc.append("<configuration>");
        rpc.append(request);
        rpc.append("</configuration>");
        rpc.append("</config></edit-config>");
        rpc.append(RPC_CLOSE_TAG);
        rpc.append("]]>]]>");
        return rpc.toString();
    }

    public static List<ControllerInfo> getOpenFlowControllersFromConfig(HierarchicalConfiguration cfg) {
        List<ControllerInfo> controllers = new ArrayList<>();
        String ipKey = "configuration.protocols.openflow.mode.ofagent-mode.controller.ip";

        if (!cfg.configurationsAt(ipKey).isEmpty()) {
            List<HierarchicalConfiguration> ipNodes = cfg.configurationsAt(ipKey);

            ipNodes.forEach(ipNode -> {
                int port = 0;
                String proto = UNKNOWN;
                HierarchicalConfiguration protocolNode = ipNode.configurationAt(PROTOCOL);
                HierarchicalConfiguration tcpNode = protocolNode.configurationAt(TCP);

                if (!tcpNode.isEmpty()) {
                    String portString = tcpNode.getString(PORT);

                    if (portString != null && !portString.isEmpty()) {
                        port = Integer.parseInt(portString);
                    }

                    proto = TCP;
                }

                String ipaddress = ipNode.getString(NAME);

                if (ipaddress == null) {
                    ipaddress = UNKNOWN;
                }

                if (ipaddress.equals(UNKNOWN) || proto.equals(UNKNOWN) || port == 0) {
                    log.error("Controller infomation is invalid. Skip this controller node." +
                                    " ipaddress: {}, proto: {}, port: {}", ipaddress, proto, port);
                    return;
                }

                controllers.add(new ControllerInfo(IpAddress.valueOf(ipaddress), port, proto));
            });
        } else {
            log.error("Controller not present");
        }

        return controllers;
    }
}
