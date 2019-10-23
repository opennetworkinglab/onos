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
package org.onosproject.openstackvtap.util;

import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.net.Host;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_PORT_ID;

/**
 * An utilities that used in openstack vtap app.
 */
public final class OpenstackVtapUtil {

    private static final Logger log = LoggerFactory.getLogger(OpenstackVtapUtil.class);

    private static final String VTAP_TUNNEL_GRE = "vtap_gre";
    private static final String VTAP_TUNNEL_VXLAN = "vtap_vxlan";
    private static final String VTAP_GROUP_KEY = "VTAP_GROUP_KEY";

    /**
     * Prevents object instantiation from external.
     */
    private OpenstackVtapUtil() {
    }

    /**
     * Obtains IP protocol type from the given string.
     *
     * @param str protocol string
     * @return IP protocol number
     */
    public static byte getProtocolTypeFromString(String str) {
        switch (str) {
            case "tcp":
                return IPv4.PROTOCOL_TCP;
            case "udp":
                return IPv4.PROTOCOL_UDP;
            case "icmp":
                return IPv4.PROTOCOL_ICMP;
            case "any":
                return 0;
            default:
                throw new IllegalArgumentException("Invalid vtap protocol string");
        }
    }

    /**
     * Obtains IP protocol string from the given type.
     *
     * @param type protocol type
     * @return IP protocol string
     */
    public static String getProtocolStringFromType(byte type) {
        switch (type) {
            case IPv4.PROTOCOL_TCP:
                return "tcp";
            case IPv4.PROTOCOL_UDP:
                return "udp";
            case IPv4.PROTOCOL_ICMP:
                return "icmp";
            case 0:
                return "any";
            default:
                throw new IllegalArgumentException("Invalid vtap protocol type");
        }
    }

    /**
     * Obtains openstack vtap type from the given string.
     *
     * @param str vtap type string
     * @return vtap type
     */
    public static OpenstackVtap.Type getVtapTypeFromString(String str) {
        switch (str) {
            case "all":
                return OpenstackVtap.Type.VTAP_ALL;
            case "rx":
                return OpenstackVtap.Type.VTAP_RX;
            case "tx":
                return OpenstackVtap.Type.VTAP_TX;
            case "any":
                return OpenstackVtap.Type.VTAP_ANY;
            default:
                throw new IllegalArgumentException("Invalid vtap type string");
        }
    }

    /**
     * Checks whether the given IP address is included in vtap criterion with
     * TX and RX directions by given vtap type.
     *
     * @param type      vtap type
     * @param criterion vtap criterion
     * @param ip        IP address to check
     * @return true on match address, false otherwise
     */
    public static boolean containsIp(OpenstackVtap.Type type, OpenstackVtapCriterion criterion, IpAddress ip) {
        boolean isTxEdge = type.isValid(OpenstackVtap.Type.VTAP_TX) &&
                criterion.srcIpPrefix().contains(ip);
        boolean isRxEdge = type.isValid(OpenstackVtap.Type.VTAP_RX) &&
                criterion.dstIpPrefix().contains(ip);
        return isTxEdge || isRxEdge;
    }

    /**
     * Checks the host validation from annotation information.
     *
     * @param host host to check
     * @return true on validate, false otherwise
     */
    public static boolean isValidHost(Host host) {
        return !host.ipAddresses().isEmpty() &&
                host.annotations().value(ANNOTATION_NETWORK_ID) != null &&
                host.annotations().value(ANNOTATION_PORT_ID) != null;
    }

    /**
     * Checks whether the given IP prefix is contained in the first host rather
     * than in the second host.
     *
     * @param host1     first host instance
     * @param host2     second host instance
     * @param ipPrefix  IP prefix to be looked up
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     */
    public static int hostCompareIp(Host host1, Host host2, IpPrefix ipPrefix) {
        if ((host1 == null || host1.ipAddresses().stream().noneMatch(ipPrefix::contains)) &&
                (host2 == null || host2.ipAddresses().stream().anyMatch(ipPrefix::contains))) {
            return -1;
        } else if ((host1 != null && host1.ipAddresses().stream().anyMatch(ipPrefix::contains)) &&
                (host2 == null || host2.ipAddresses().stream().noneMatch(ipPrefix::contains))) {
            return 1;
        }
        return 0;
    }

    /**
     * Obtains flow group key from the given id.
     *
     * @param groupId flow group identifier
     * @return flow group key
     */
    public static GroupKey getGroupKey(int groupId) {
        return new DefaultGroupKey((VTAP_GROUP_KEY + Integer.toString(groupId)).getBytes());
    }

    /**
     * Obtains tunnel interface name from the given openstack vtap network mode.
     *
     * @param mode vtap network mode
     * @return tunnel interface name
     */
    public static String getTunnelName(OpenstackVtapNetwork.Mode mode) {
        switch (mode) {
            case GRE:
                return VTAP_TUNNEL_GRE;
            case VXLAN:
                return VTAP_TUNNEL_VXLAN;
            default:
                return null;
        }
    }

    /**
     * Obtains tunnel description type from the given openstack vtap network mode.
     *
     * @param mode vtap network mode
     * @return tunnel description type
     */
    public static TunnelDescription.Type getTunnelType(OpenstackVtapNetwork.Mode mode) {
        return TunnelDescription.Type.valueOf(mode.toString());
    }

    /**
     * Makes Openstack vTap criterion from the given src, dst IP and port.
     *
     * @param srcIp     source IP address
     * @param dstIp     destination IP address
     * @param ipProto   IP protocol
     * @param srcPort   source port
     * @param dstPort   destination port
     * @return openstack vTap criterion
     */
    public static OpenstackVtapCriterion makeVtapCriterion(String srcIp,
                                                           String dstIp,
                                                           String ipProto,
                                                           int srcPort,
                                                           int dstPort) {
        OpenstackVtapCriterion.Builder cBuilder = DefaultOpenstackVtapCriterion.builder();

        try {
            cBuilder.srcIpPrefix(IpPrefix.valueOf(srcIp));
            cBuilder.dstIpPrefix(IpPrefix.valueOf(dstIp));
        } catch (Exception e) {
            log.error("The given IP addresses are invalid");
         }

        cBuilder.ipProtocol(getProtocolTypeFromString(ipProto.toLowerCase()));

        cBuilder.srcTpPort(TpPort.tpPort(srcPort));
        cBuilder.dstTpPort(TpPort.tpPort(dstPort));

        return cBuilder.build();
    }

    /**
     * Print stack trace of given exception.
     *
     * @param log logger for using print
     * @param e   exception to print
     */
    public static void dumpStackTrace(Logger log, Exception e) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        log.error("\n{}", new String(outputStream.toByteArray(), StandardCharsets.UTF_8));
    }

}
