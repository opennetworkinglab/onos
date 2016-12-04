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

import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.packet.ChassisId;
import org.onlab.packet.MacAddress;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static org.onosproject.net.DefaultAnnotations.Builder;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Utility class for Netconf XML for Juniper.
 * Tested with MX240 junos 14.2
 */
public final class JuniperUtils {

    public static final String FAILED_CFG = "Failed to retrieve configuration.";

    private static final String RPC_TAG_NETCONF_BASE = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String RPC_CLOSE_TAG = "</rpc>";

    //requests
    public static final String REQ_LLDP_NBR_INFO = "<get-lldp-neighbors-information/>";
    public static final String REQ_SYS_INFO = "<get-system-information/>";
    public static final String REQ_MAC_ADD_INFO = "<get-chassis-mac-addresses/>";
    public static final String REQ_IF_INFO = "<get-interface-information/>";

    //helper strings for parsing
    private static final String LLDP_NBR_INFO = "lldp-neighbors-information";
    private static final String SYS_INFO = "system-information";
    private static final String HW_MODEL = "hardware-model";
    private static final String OS_NAME = "os-name";
    private static final String OS_VER = "os-version";
    private static final String SER_NUM = "serial-number";
    private static final String IF_INFO = "interface-information";
    private static final String IF_PHY = "physical-interface";
    private static final String IF_TYPE = "if-type";
    private static final String SPEED = "speed";
    private static final String ETH = "Ethernet";
    private static final String MBPS = "mbps";
    private static final String NAME = "name";
    private static final String IF_LO_ENCAP = "logical-interface.encapsulation";
    private static final String IF_LO_NAME = "logical-interface.name";
    private static final String IF_LO_ADD =
            "logical-interface.address-family.interface-address.ifa-local";
    private static final String LO_INDEX = "local-index";
    private static final String STATUS = "admin-status";
    private static final String SNMP_INDEX = "snmp-index";
    private static final String IF_LO_INDEX = "logical-interface.local-index";
    private static final String IF_LO_STATUS =
            "logical-interface.if-config-flags.iff-up";
    private static final String LLDP_LO_PORT = "lldp-local-port-id";
    private static final String LLDP_REM_CHASS = "lldp-remote-chassis-id";
    private static final String LLDP_REM_PORT = "lldp-remote-port-id";
    private static final String REGEX_ADD =
            ".*Private base address\\s*([:,0-9,a-f,A-F]*).*";
    private static final Pattern ADD_PATTERN =
            Pattern.compile(REGEX_ADD, Pattern.DOTALL);

    private static final String JUNIPER = "JUNIPER";
    private static final String UNKNOWN = "UNKNOWN";
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

        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt(IF_INFO);
        for (HierarchicalConfiguration interfInfo : subtrees) {
            List<HierarchicalConfiguration> interfaceTree =
                    interfInfo.configurationsAt(IF_PHY);
            for (HierarchicalConfiguration interf : interfaceTree) {
                if (interf != null) {
                    if (interf.getString(IF_TYPE) != null &&
                            interf.getString(SPEED) != null) {
                        if (interf.getString(IF_TYPE).contains(ETH) &&
                                interf.getString(SPEED).contains(MBPS)) {
                            portDescriptions.add(parseDefaultPort(interf));
                        }
                    } else if (interf.getString(IF_LO_ENCAP) != null &&
                            !interf.getString(NAME).contains("pfe") &&
                            interf.getString(IF_LO_ENCAP).contains("ENET2")) {
                        portDescriptions.add(parseLogicalPort(interf));
                    } else if (interf.getString(NAME).contains("lo")) {
                        portDescriptions.add(parseLoopback(interf));
                    }
                }
            }
        }
        return portDescriptions;
    }

    private static PortDescription parseLoopback(HierarchicalConfiguration cfg) {
        String name = cfg.getString(IF_LO_NAME).trim();
        PortNumber portNumber = portNumber(name.replace("lo0.", ""));

        Builder annotationsBuilder = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, name);
        String ip = cfg.getString(IF_LO_ADD);
        if (ip != null) {
            annotationsBuilder.set("ip", ip);
        }

        return new DefaultPortDescription(portNumber,
                                          true,
                                          COPPER,
                                          DEFAULT_PORT_SPEED,
                                          annotationsBuilder.build());
    }

    private static DefaultPortDescription parseDefaultPort(HierarchicalConfiguration cfg) {
        PortNumber portNumber = portNumber(cfg.getString(LO_INDEX));
        boolean enabled = cfg.getString(STATUS).equals("up");
        int speed = parseInt(cfg.getString(SPEED).replaceAll(MBPS, ""));


        Builder annotationsBuilder = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, cfg.getString(NAME).trim());
        setIpIfPresent(cfg, annotationsBuilder);

        return new DefaultPortDescription(portNumber,
                                          enabled,
                                          COPPER,
                                          speed,
                                          annotationsBuilder.build());
    }

    private static DefaultPortDescription parseLogicalPort(HierarchicalConfiguration cfg) {

        String name = cfg.getString(NAME).trim();
        String index = cfg.getString(SNMP_INDEX).trim();
        Builder annotationsBuilder = DefaultAnnotations.builder()
                .set(AnnotationKeys.PORT_NAME, name)
                .set("index", index);
        setIpIfPresent(cfg, annotationsBuilder);

        PortNumber portNumber = portNumberFromName(cfg.getString(IF_LO_INDEX), name);

        boolean enabled = false;
        if (cfg.getString(IF_LO_STATUS) != null) {
            enabled = true;
        }
        //FIXME: port speed should be exposed
        return new DefaultPortDescription(
                portNumber,
                enabled,
                COPPER,
                DEFAULT_PORT_SPEED,
                annotationsBuilder.build());
    }

    private static PortNumber portNumberFromName(String ifIndex, String name) {
        PortNumber portNumber = portNumber(ifIndex);
        if (name.contains("-")) {
            String[] splitted = name.split("-");
            String typeInt = "[" + splitted[0] + "]";
            String number = splitted[1].replace("/", "");
            number = "(" + number + ")";
            portNumber = PortNumber.fromString(typeInt + number);
        }
        return portNumber;
    }

    private static void setIpIfPresent(HierarchicalConfiguration cfg,
                                       Builder annotationsBuilder) {
        String ip = cfg.getString(IF_LO_ADD);
        if (ip != null) {
            annotationsBuilder.set("ip", ip);
        }
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
                info.configurationsAt(LLDP_NBR_INFO);
        for (HierarchicalConfiguration neighborsInfo : subtrees) {
            List<HierarchicalConfiguration> neighbors =
                    neighborsInfo.configurationsAt(LLDP_NBR_INFO);
            for (HierarchicalConfiguration neighbor : neighbors) {
                String localPortName = neighbor.getString(LLDP_LO_PORT);
                MacAddress mac = MacAddress.valueOf(
                        neighbor.getString(LLDP_REM_CHASS));
                int remotePortIndex =
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
        protected int remotePortIndex;

        protected LinkAbstraction(String pName, long chassisId, int pIndex) {
            this.localPortName = pName;
            this.remoteChassisId = new ChassisId(chassisId);
            this.remotePortIndex = pIndex;
        }
    }
}
