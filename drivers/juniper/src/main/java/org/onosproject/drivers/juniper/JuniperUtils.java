/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.packet.ChassisId;
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
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.slf4j.Logger;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String SPEED = "speed";
    private static final String NAME = "name";

    // seems to be unique index within device
    private static final String SNMP_INDEX = "snmp-index";

    private static final String LLDP_LO_PORT = "lldp-local-port-id";
    private static final String LLDP_REM_CHASS = "lldp-remote-chassis-id";
    private static final String LLDP_REM_PORT = "lldp-remote-port-id";
    private static final String REGEX_ADD =
            ".*Private base address\\s*([:,0-9,a-f,A-F]*).*";
    private static final Pattern ADD_PATTERN =
            Pattern.compile(REGEX_ADD, Pattern.DOTALL);

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
    private static final long DEFAULT_PORT_SPEED = 1000;


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
     * Parses device configuration and returns the device description.
     *
     * @param deviceId    the id of the device
     * @param sysInfoCfg  system configuration
     * @param chassisText chassis string
     * @return device description
     */
    public static DeviceDescription parseJuniperDescription(DeviceId deviceId,
                                                            HierarchicalConfiguration sysInfoCfg,
                                                            String chassisText) {
        HierarchicalConfiguration info = sysInfoCfg.configurationAt(SYS_INFO);

        String hw = info.getString(HW_MODEL) == null ? UNKNOWN : info.getString(HW_MODEL);
        String sw = UNKNOWN;
        if (info.getString(OS_NAME) != null || info.getString(OS_VER) != null) {
            sw = info.getString(OS_NAME) + " " + info.getString(OS_VER);
        }
        String serial = info.getString(SER_NUM) == null ? UNKNOWN : info.getString(SER_NUM);

        Matcher matcher = ADD_PATTERN.matcher(chassisText);
        if (matcher.lookingAt()) {
            String chassis = matcher.group(1);
            MacAddress chassisMac = MacAddress.valueOf(chassis);
            return new DefaultDeviceDescription(deviceId.uri(), ROUTER,
                                                JUNIPER, hw, sw, serial,
                                                new ChassisId(chassisMac.toLong()),
                                                DefaultAnnotations.EMPTY);
        }
        return new DefaultDeviceDescription(deviceId.uri(), ROUTER,
                                            JUNIPER, hw, sw, serial,
                                            null, DefaultAnnotations.EMPTY);
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

        portDescriptions.add(new DefaultPortDescription(portNumber,
                                                        admUp & opUp,
                                                        Type.COPPER,
                                                        portSpeed,
                                                        annotations.build()));

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
                         "ip",
                         logIntf.getString("address-family.interface-address.ifa-local"));

            setIfNonNull(lannotations,
                         AK_ENCAPSULATION, logIntf.getString("encapsulation"));

            // TODO confirm if this is correct.
            // Looking at sample data,
            // it seemed all logical loop-back interfaces were down
            boolean lEnabled = logIntf.getString("if-config-flags.iff-up") != null;

            portDescriptions.add(new DefaultPortDescription(lPortNumber,
                                                            admUp & opUp & lEnabled,
                                                            Type.COPPER,
                                                            portSpeed,
                                                            lannotations.build()));
        }
    }

    /**
     * Port status as "up"/"down".
     *
     * @param portStatus port status
     * @return "up" if {@code portStats} is {@literal true}, "down" otherwise
     */
    static String toUpDown(boolean portStatus) {
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
    static long toMbps(String speed) {
        String s = Strings.nullToEmpty(speed).trim().toLowerCase();
        Matcher matcher = SPEED_PATTERN.matcher(s);
        if (matcher.matches()) {
            // numeric
            int n = Integer.parseInt(matcher.group(1));
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
    static void setIfNonNull(Builder builder, String key, String value) {
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
    static PortNumber safePortNumber(String s) {
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
                .set("layer", "IP")
                .build();
        descs.add(new DefaultLinkDescription(
                local, remote, Link.Type.INDIRECT, false, annotations));
        descs.add(new DefaultLinkDescription(
                remote, local, Link.Type.INDIRECT, false, annotations));
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
                MacAddress mac = MacAddress.valueOf(
                        neighbor.getString(LLDP_REM_CHASS));
                long remotePortIndex =
                        neighbor.getInt(LLDP_REM_PORT);
                LinkAbstraction link = new LinkAbstraction(
                        localPortName,
                        mac.toLong(),
                        remotePortIndex);
                neighbour.add(link);
            }
        }
        return neighbour;
    }

    /**
     * Device representation of the adjacency at the IP Layer.
     */
    protected static final class LinkAbstraction {
        protected String localPortName;
        protected ChassisId remoteChassisId;
        protected long remotePortIndex;

        protected LinkAbstraction(String pName, long chassisId, long pIndex) {
            this.localPortName = pName;
            this.remoteChassisId = new ChassisId(chassisId);
            this.remotePortIndex = pIndex;
        }
    }
}
