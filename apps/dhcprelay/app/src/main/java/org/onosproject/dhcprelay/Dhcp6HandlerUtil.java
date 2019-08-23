/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */
package org.onosproject.dhcprelay;

import org.onlab.packet.BasePacket;
import org.onlab.packet.DHCP6;
import org.onlab.packet.DHCP6.MsgType;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.dhcp.Dhcp6ClientIdOption;
import org.onlab.packet.dhcp.Dhcp6RelayOption;
import org.onlab.packet.dhcp.Dhcp6Option;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;

import org.onlab.util.HexString;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.dhcprelay.store.DhcpRelayCounters;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.DeviceId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.onosproject.net.intf.InterfaceService;

import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.net.HostLocation;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Dhcp6HandlerUtil {

    private static final Logger log = LoggerFactory.getLogger(Dhcp6HandlerUtil.class);

    private Dhcp6HandlerUtil() {
    }

    // Returns the first v6 interface ip out of a set of interfaces or null.
    // Checks all interfaces, and ignores v6 interface ips
    public static Ip6Address getRelayAgentIPv6Address(Set<Interface> intfs) {
        for (Interface intf : intfs) {
            for (InterfaceIpAddress ip : intf.ipAddressesList()) {
                Ip6Address relayAgentIp = ip.ipAddress().getIp6Address();
                if (relayAgentIp != null) {
                    return relayAgentIp;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first interface ip from interface.
     *
     * @param iface interface of one connect point
     * @return the first interface IP; null if not exists an IP address in
     *         these interfaces
     */
    private static Ip6Address getFirstIpFromInterface(Interface iface) {
        checkNotNull(iface, "Interface can't be null");
        return iface.ipAddressesList().stream()
                .map(InterfaceIpAddress::ipAddress)
                .filter(IpAddress::isIp6)
                .map(IpAddress::getIp6Address)
                .findFirst()
                .orElse(null);
    }
    /**
     *
     * process the LQ reply packet from dhcp server.
     *
     * @param defaultServerInfoList default server list
     * @param indirectServerInfoList default indirect server list
     * @param serverInterface server interface
     * @param interfaceService interface service
     * @param hostService host service
     * @param context packet context
     * @param receivedPacket server ethernet packet
     * @param recevingInterfaces set of server side interfaces
     * @return a packet ready to be sent to relevant output interface
     */
    public static InternalPacket processLQ6PacketFromServer(
            List<DhcpServerInfo> defaultServerInfoList,
            List<DhcpServerInfo> indirectServerInfoList,
            Interface serverInterface,
            InterfaceService interfaceService,
            HostService hostService,
            PacketContext context,
            Ethernet receivedPacket, Set<Interface> recevingInterfaces) {
        // get dhcp6 header.
        Ethernet etherReply = (Ethernet) receivedPacket.clone();
        IPv6 ipv6Packet = (IPv6) etherReply.getPayload();
        UDP udpPacket = (UDP) ipv6Packet.getPayload();
        DHCP6 lq6Reply = (DHCP6) udpPacket.getPayload();

        // TODO: refactor
        ConnectPoint receivedFrom = context.inPacket().receivedFrom();
        DeviceId receivedFromDevice = receivedFrom.deviceId();
        DhcpServerInfo serverInfo;
        Ip6Address dhcpServerIp = null;
        ConnectPoint dhcpServerConnectPoint = null;
        MacAddress dhcpConnectMac = null;
        VlanId dhcpConnectVlan = null;
        Ip6Address dhcpGatewayIp = null;

        // todo: refactor
        Ip6Address indirectDhcpServerIp = null;
        ConnectPoint indirectDhcpServerConnectPoint = null;
        MacAddress indirectDhcpConnectMac = null;
        VlanId indirectDhcpConnectVlan = null;
        Ip6Address indirectDhcpGatewayIp = null;
        Ip6Address indirectRelayAgentIpFromCfg = null;

        if (!defaultServerInfoList.isEmpty()) {
            serverInfo = defaultServerInfoList.get(0);
            dhcpConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
            dhcpGatewayIp = serverInfo.getDhcpGatewayIp6().orElse(null);
            dhcpServerIp = serverInfo.getDhcpServerIp6().orElse(null);
            dhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            dhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
        }

        if (!indirectServerInfoList.isEmpty()) {
            serverInfo = indirectServerInfoList.get(0);
            indirectDhcpConnectMac = serverInfo.getDhcpConnectMac().orElse(null);
            indirectDhcpGatewayIp = serverInfo.getDhcpGatewayIp6().orElse(null);
            indirectDhcpServerIp = serverInfo.getDhcpServerIp6().orElse(null);
            indirectDhcpServerConnectPoint = serverInfo.getDhcpServerConnectPoint().orElse(null);
            indirectDhcpConnectVlan = serverInfo.getDhcpConnectVlan().orElse(null);
            indirectRelayAgentIpFromCfg = serverInfo.getRelayAgentIp6(receivedFromDevice).orElse(null);
        }

        Boolean directConnFlag = directlyConnected(lq6Reply);
        ConnectPoint inPort = context.inPacket().receivedFrom();
        if ((directConnFlag || indirectDhcpServerIp == null)
                && !inPort.equals(dhcpServerConnectPoint)) {
            log.warn("Receiving port {} is not the same as server connect point {} for direct or indirect-null",
                    inPort, dhcpServerConnectPoint);
            return null;
        }

        if (!directConnFlag && indirectDhcpServerIp != null &&
                !inPort.equals(indirectDhcpServerConnectPoint)) {
            log.warn("Receiving port {} is not the same as server connect point {} for indirect",
                    inPort, indirectDhcpServerConnectPoint);
            return null;
        }


        Ip6Address nextHopIP =  Ip6Address.valueOf(ipv6Packet.getDestinationAddress());
        // use hosts store to find out the next hop mac and connection point
        Set<Host> hosts = hostService.getHostsByIp(nextHopIP);
        Host host;
        if (!hosts.isEmpty()) {
            host = hosts.iterator().next();
        } else {
            log.warn("Host {} is not in store", nextHopIP);
            return null;
        }

        HostLocation hl = host.location();
        String clientConnectionPointStr = hl.toString(); // iterator().next());
        ConnectPoint clientConnectionPoint = ConnectPoint.deviceConnectPoint(clientConnectionPointStr);


        VlanId originalPacketVlanId = VlanId.vlanId(etherReply.getVlanID());
        Interface iface;
        iface = interfaceService.getInterfacesByPort(clientConnectionPoint)
                .stream()
                .filter(iface1 -> interfaceContainsVlan(iface1, originalPacketVlanId))
                .findFirst()
                .orElse(null);

        etherReply.setSourceMACAddress(iface.mac());
        etherReply.setDestinationMACAddress(host.mac());

        // workaround for a bug where core sends src port as 547 (server)
        udpPacket.setDestinationPort(UDP.DHCP_V6_SERVER_PORT);
        udpPacket.setPayload(lq6Reply);
        udpPacket.resetChecksum();
        ipv6Packet.setPayload(udpPacket);
        etherReply.setPayload(ipv6Packet);

        return InternalPacket.internalPacket(etherReply, clientConnectionPoint);
    }

    /**
     * extract DHCP6 payload from dhcp6 relay message within relay-forwrd/reply.
     *
     * @param dhcp6 dhcp6 relay-reply or relay-forward
     * @return dhcp6Packet dhcp6 packet extracted from relay-message
     */
    public static DHCP6 dhcp6PacketFromRelayPacket(DHCP6 dhcp6) {

        // extract the relay message if exist
        DHCP6 dhcp6Payload = dhcp6.getOptions().stream()
                .filter(opt -> opt instanceof Dhcp6RelayOption)
                .map(BasePacket::getPayload)
                .map(pld -> (DHCP6) pld)
                .findFirst()
                .orElse(null);
        if (dhcp6Payload == null) {
            // Can't find dhcp payload
            log.debug("Can't find dhcp6 payload from relay message");
        } else {
            log.debug("dhcp6 payload found from relay message {}", dhcp6Payload);
        }
        return dhcp6Payload;
    }

    /**
     * find the leaf DHCP6 packet from multi-level relay packet.
     *
     * @param relayPacket dhcp6 relay packet
     * @return leafPacket non-relay dhcp6 packet
     */
    public static DHCP6 getDhcp6Leaf(DHCP6 relayPacket) {
        DHCP6 dhcp6Parent = relayPacket;
        DHCP6 dhcp6Child = null;

        log.debug("getDhcp6Leaf entered.");
        while (dhcp6Parent != null) {
            dhcp6Child = dhcp6PacketFromRelayPacket(dhcp6Parent);
            if (dhcp6Child != null) {
                if (dhcp6Child.getMsgType() != DHCP6.MsgType.RELAY_FORW.value() &&
                        dhcp6Child.getMsgType() != DHCP6.MsgType.RELAY_REPL.value()) {
                    log.debug("leaf dhcp6 packet found.");
                    break;
                } else {
                    // found another relay, go for another loop
                    dhcp6Parent = dhcp6Child;
                }
            } else {
                log.debug("Expected dhcp6 within relay pkt, but no dhcp6 leaf found.");
                break;
            }
        }
        return dhcp6Child;
    }

    /**
     * Determine DHCP message type (direct DHCPv6 or wrapped into relay messages).
     *
     * @param relayPacket {@link DHCP6} packet to be parsed
     * @return {@link DHCP6.MsgType} contained message type of dhcpv6 packet/relay-message
     */
    public static DHCP6.MsgType getDhcp6LeafMessageType(DHCP6 relayPacket) {
        checkNotNull(relayPacket);
        DHCP6 dhcp6Child = getDhcp6Leaf(relayPacket);
        return DHCP6.MsgType.getType(dhcp6Child != null ? dhcp6Child.getMsgType() : relayPacket.getMsgType());
    }

    /**
     * check if DHCP6 relay-reply is reply.
     *
     * @param relayPacket dhcp6 relay-reply
     * @return boolean relay-reply contains ack
     */
    public static boolean isDhcp6Reply(DHCP6 relayPacket) {
        DHCP6 leafDhcp6 = getDhcp6Leaf(relayPacket);
        if (leafDhcp6 != null) {
            if (leafDhcp6.getMsgType() == DHCP6.MsgType.REPLY.value()) {
                log.debug("isDhcp6Reply  true.");
                return true;  // must be directly connected
            } else {
                log.debug("isDhcp6Reply false. leaf dhcp6 is not replay. MsgType {}", leafDhcp6.getMsgType());
            }
        } else {
            log.debug("isDhcp6Reply false. Expected dhcp6 within relay pkt but not found.");
        }
        log.debug("isDhcp6Reply  false.");
        return false;
    }

    /**
     * check if DHCP6 is release or relay-forward contains release.
     *
     * @param dhcp6Payload dhcp6 packet
     * @return boolean dhcp6 contains release
     */
    public static  boolean isDhcp6Release(DHCP6 dhcp6Payload) {
        if (dhcp6Payload.getMsgType() ==  DHCP6.MsgType.RELEASE.value()) {
            log.debug("isDhcp6Release  true.");
            return true;  // must be directly connected
        } else {
            DHCP6 dhcp6Leaf = getDhcp6Leaf(dhcp6Payload);
            if (dhcp6Leaf != null) {
                if (dhcp6Leaf.getMsgType() ==  DHCP6.MsgType.RELEASE.value()) {
                    log.debug("isDhcp6Release  true. indirectlry connected");
                    return true;
                } else {
                    log.debug("leaf dhcp6 is not release. MsgType {}",  dhcp6Leaf.getMsgType());
                    return false;
                }
            } else {
                log.debug("isDhcp6Release  false. dhcp6 is niether relay nor release.");
                return false;
            }
        }
    }


    /**
     * convert dhcp6 msgType to String.
     *
     * @param msgTypeVal msgType byte of dhcp6 packet
     * @return String string value of dhcp6 msg type
     */
    public static String getMsgTypeStr(byte msgTypeVal) {
        MsgType msgType = DHCP6.MsgType.getType(msgTypeVal);
        return DHCP6.MsgType.getMsgTypeStr(msgType);
    }

    /**
     * find the string of dhcp6 leaf packets's msg type.
     *
     * @param directConnFlag boolean value indicating direct/indirect connection
     * @param dhcp6Packet dhcp6 packet
     * @return String string value of dhcp6 leaf packet msg type
     */
    public static String findLeafMsgType(boolean directConnFlag, DHCP6  dhcp6Packet) {
        if (directConnFlag) {
            return getMsgTypeStr(dhcp6Packet.getMsgType());
        } else {
            DHCP6 leafDhcp = getDhcp6Leaf(dhcp6Packet);
            if (leafDhcp != null) {
                return getMsgTypeStr(leafDhcp.getMsgType());
            } else {
                return DhcpRelayCounters.INVALID_PACKET;
            }
        }
    }

    /**
     * Determind if an Interface contains a vlan id.
     *
     * @param iface the Interface
     * @param vlanId the vlan id
     * @return true if the Interface contains the vlan id
     */
    public static boolean interfaceContainsVlan(Interface iface, VlanId vlanId) {
        if (vlanId.equals(VlanId.NONE)) {
            // untagged packet, check if vlan untagged or vlan native is not NONE
            return !iface.vlanUntagged().equals(VlanId.NONE) ||
                    !iface.vlanNative().equals(VlanId.NONE);
        }
        // tagged packet, check if the interface contains the vlan
        return iface.vlanTagged().contains(vlanId);
    }

    /**
     * Check if the host is directly connected to the network or not.
     *
     * @param dhcp6Payload the dhcp6 payload
     * @return true if the host is directly connected to the network; false otherwise
     */
    public static boolean directlyConnected(DHCP6 dhcp6Payload) {

        log.debug("directlyConnected enters");
        if (dhcp6Payload.getMsgType() == DHCP6.MsgType.LEASEQUERY.value() ||
                dhcp6Payload.getMsgType() == DHCP6.MsgType.LEASEQUERY_REPLY.value()) {
            log.debug("directlyConnected false. MsgType {}", dhcp6Payload.getMsgType());

            return false;
        }

        if (dhcp6Payload.getMsgType() != DHCP6.MsgType.RELAY_FORW.value() &&
                dhcp6Payload.getMsgType() != DHCP6.MsgType.RELAY_REPL.value()) {
            log.debug("directlyConnected true. MsgType {}", dhcp6Payload.getMsgType());

            return true;
        }
        // Regardless of relay-forward or relay-replay, check if we see another relay message
        DHCP6 dhcp6Payload2 = dhcp6PacketFromRelayPacket(dhcp6Payload);
        if (dhcp6Payload2 != null) {
            if (dhcp6Payload.getMsgType() == DHCP6.MsgType.RELAY_FORW.value()) {
                log.debug("directlyConnected  false. 1st relay-forward, 2nd MsgType {}", dhcp6Payload2.getMsgType());
                return false;
            } else {
                // relay-reply
                if (dhcp6Payload2.getMsgType() != DHCP6.MsgType.RELAY_REPL.value()
                        && dhcp6Payload2.getMsgType() != MsgType.LEASEQUERY_REPLY.value()) {
                    log.debug("directlyConnected  true. 2nd MsgType {}", dhcp6Payload2.getMsgType());
                    return true;  // must be directly connected
                } else {
                    log.debug("directlyConnected  false. 1st relay-reply, 2nd relay-reply MsgType {}",
                            dhcp6Payload2.getMsgType());
                    return false;  // must be indirectly connected
                }
            }
        } else {
            log.debug("directlyConnected  true.");
            return true;
        }
    }
    /**
     * Check if a given server info has v6 ipaddress.
     *
     * @param serverInfo server info to check
     * @return true if server info has v6 ip address; false otherwise
     */
    public static boolean isServerIpEmpty(DhcpServerInfo serverInfo) {
        if (!serverInfo.getDhcpServerIp6().isPresent()) {
            log.warn("DhcpServerIp not available, use default DhcpServerIp {}",
                    HexString.toHexString(serverInfo.getDhcpServerIp6().get().toOctets()));
            return true;
        }
        return false;
    }

    private static boolean isConnectMacEmpty(DhcpServerInfo serverInfo, Set<Interface> clientInterfaces) {
        if (!serverInfo.getDhcpConnectMac().isPresent()) {
            log.warn("DHCP6 {} not yet resolved .. Aborting DHCP "
                            + "packet processing from client on port: {}",
                    !serverInfo.getDhcpGatewayIp6().isPresent() ? "server IP " + serverInfo.getDhcpServerIp6()
                            : "gateway IP " + serverInfo.getDhcpGatewayIp6(),
                    clientInterfaces.iterator().next().connectPoint());
            return true;
        }
        return false;
    }

    private static Dhcp6Option getInterfaceIdIdOption(PacketContext context, Ethernet clientPacket) {
        String inPortString = "-" + context.inPacket().receivedFrom().toString() + ":";
        Dhcp6Option interfaceId = new Dhcp6Option();
        interfaceId.setCode(DHCP6.OptionCode.INTERFACE_ID.value());
        byte[] clientSoureMacBytes = clientPacket.getSourceMACAddress();
        byte[] inPortStringBytes = inPortString.getBytes();
        byte[] vlanIdBytes = new byte[2];
        vlanIdBytes[0] = (byte) ((clientPacket.getVlanID() >> 8) & 0xff);
        vlanIdBytes[1] = (byte) (clientPacket.getVlanID() & 0xff);
        byte[] interfaceIdBytes = new byte[clientSoureMacBytes.length +
                inPortStringBytes.length + vlanIdBytes.length];
        log.debug("Length: interfaceIdBytes  {} clientSoureMacBytes {} inPortStringBytes {} vlan {}",
                interfaceIdBytes.length, clientSoureMacBytes.length, inPortStringBytes.length,
                vlanIdBytes.length);

        System.arraycopy(clientSoureMacBytes, 0, interfaceIdBytes, 0, clientSoureMacBytes.length);
        System.arraycopy(inPortStringBytes, 0, interfaceIdBytes, clientSoureMacBytes.length,
                inPortStringBytes.length);
        System.arraycopy(vlanIdBytes, 0, interfaceIdBytes,
                clientSoureMacBytes.length + inPortStringBytes.length,
                vlanIdBytes.length);
        interfaceId.setData(interfaceIdBytes);
        interfaceId.setLength((short) interfaceIdBytes.length);
        log.debug("interfaceId write srcMac {} portString {}, vlanId {}",
                HexString.toHexString(clientSoureMacBytes, ":"), inPortString, vlanIdBytes);
        return interfaceId;
    }

    private static void addDhcp6OptionsFromClient(List<Dhcp6Option> options, byte[] dhcp6PacketByte,
                                           PacketContext context, Ethernet clientPacket) {
        Dhcp6Option relayMessage = new Dhcp6Option();
        relayMessage.setCode(DHCP6.OptionCode.RELAY_MSG.value());
        relayMessage.setLength((short) dhcp6PacketByte.length);
        relayMessage.setData(dhcp6PacketByte);
        options.add(relayMessage);
        // create interfaceId option
        Dhcp6Option interfaceId = getInterfaceIdIdOption(context, clientPacket);
        options.add(interfaceId);
    }

    /**
     * build the DHCP6 solicit/request packet with gatewayip.
     *
     * @param context packet context
     * @param clientPacket client ethernet packet
     * @param clientInterfaces set of client side interfaces
     * @param serverInfo target server which a packet is generated for
     * @param serverInterface target server interface
     * @return ethernet packet with dhcp6 packet info
     */
    public static Ethernet buildDhcp6PacketFromClient(PacketContext context, Ethernet clientPacket,
                                               Set<Interface> clientInterfaces, DhcpServerInfo serverInfo,
                                               Interface serverInterface) {
        ConnectPoint receivedFrom = context.inPacket().receivedFrom();
        DeviceId receivedFromDevice = receivedFrom.deviceId();

        Ip6Address relayAgentIp = getRelayAgentIPv6Address(clientInterfaces);
        MacAddress relayAgentMac = clientInterfaces.iterator().next().mac();
        if (relayAgentIp == null || relayAgentMac == null) {
            log.warn("Missing DHCP relay agent interface Ipv6 addr config for "
                            + "packet from client on port: {}. Aborting packet processing",
                    clientInterfaces.iterator().next().connectPoint());
            return null;
        }
        IPv6 clientIpv6 = (IPv6) clientPacket.getPayload();
        UDP clientUdp = (UDP) clientIpv6.getPayload();
        DHCP6 clientDhcp6 = (DHCP6) clientUdp.getPayload();
        boolean directConnFlag = directlyConnected(clientDhcp6);

        Ip6Address serverIpFacing = getFirstIpFromInterface(serverInterface);
        if (serverIpFacing == null || serverInterface.mac() == null) {
            log.warn("No IP v6 address for server Interface {}", serverInterface);
            return null;
        }

        Ethernet etherReply = clientPacket.duplicate();
        etherReply.setSourceMACAddress(serverInterface.mac());

        // set default info and replace with indirect if available later on.
        if (serverInfo.getDhcpConnectMac().isPresent()) {
            etherReply.setDestinationMACAddress(serverInfo.getDhcpConnectMac().get());
        }
        if (serverInfo.getDhcpConnectVlan().isPresent()) {
            etherReply.setVlanID(serverInfo.getDhcpConnectVlan().get().toShort());
        }
        IPv6 ipv6Packet = (IPv6) etherReply.getPayload();
        byte[] peerAddress = clientIpv6.getSourceAddress();
        ipv6Packet.setSourceAddress(serverIpFacing.toOctets());
        ipv6Packet.setDestinationAddress(serverInfo.getDhcpServerIp6().get().toOctets());
        UDP udpPacket = (UDP) ipv6Packet.getPayload();
        udpPacket.setSourcePort(UDP.DHCP_V6_SERVER_PORT);
        DHCP6 dhcp6Packet = (DHCP6) udpPacket.getPayload();
        byte[] dhcp6PacketByte = dhcp6Packet.serialize();

        DHCP6 dhcp6Relay = new DHCP6();

        dhcp6Relay.setMsgType(DHCP6.MsgType.RELAY_FORW.value());

        if (directConnFlag) {
            dhcp6Relay.setLinkAddress(relayAgentIp.toOctets());
        } else {
            if (isServerIpEmpty(serverInfo)) {
                log.warn("indirect DhcpServerIp empty... use default server ");
            } else {
                // Indirect case, replace destination to indirect dhcp server if exist
                // Check if mac is obtained for valid server ip
                if (isConnectMacEmpty(serverInfo, clientInterfaces)) {
                    log.warn("indirect Dhcp ConnectMac empty ...");
                    return null;
                }
                etherReply.setDestinationMACAddress(serverInfo.getDhcpConnectMac().get());
                etherReply.setVlanID(serverInfo.getDhcpConnectVlan().get().toShort());
                ipv6Packet.setDestinationAddress(serverInfo.getDhcpServerIp6().get().toOctets());
            }
            if (!serverInfo.getRelayAgentIp6(receivedFromDevice).isPresent()) {
                log.debug("indirect connection: relayAgentIp NOT availale from config file! Use dynamic. {}",
                        HexString.toHexString(relayAgentIp.toOctets(), ":"));
                serverIpFacing = relayAgentIp;
            } else {
                serverIpFacing = serverInfo.getRelayAgentIp6(receivedFromDevice).get();
            }
            log.debug("Source IP address set as relay agent IP with value: {}", serverIpFacing);
            dhcp6Relay.setLinkAddress(serverIpFacing.toOctets());
            ipv6Packet.setSourceAddress(serverIpFacing.toOctets());
        }
        // peer address: address of the client or relay agent from which the message to be relayed was received.
        dhcp6Relay.setPeerAddress(peerAddress);
        // directly connected case, hop count is zero; otherwise, hop count + 1
        if (directConnFlag) {
            dhcp6Relay.setHopCount((byte) 0);
        } else {
            dhcp6Relay.setHopCount((byte) (dhcp6Packet.getHopCount() + 1));
        }

        List<Dhcp6Option> options = new ArrayList<>();
        addDhcp6OptionsFromClient(options, dhcp6PacketByte, context, clientPacket);
        dhcp6Relay.setOptions(options);
        udpPacket.setPayload(dhcp6Relay);
        udpPacket.resetChecksum();
        ipv6Packet.setPayload(udpPacket);
        ipv6Packet.setHopLimit((byte) 64);
        etherReply.setPayload(ipv6Packet);

        return etherReply;
    }

    /**
     * build the DHCP6 solicit/request packet with gatewayip.
     *
     * @param directConnFlag flag indicating if packet is from direct client or not
     * @param serverInfo server to check its connect point
     * @return boolean true if serverInfo is found; false otherwise
     */
    public static boolean checkDhcpServerConnPt(boolean directConnFlag,
                                          DhcpServerInfo serverInfo) {
        if (serverInfo.getDhcpServerConnectPoint() == null) {
            log.warn("DHCP6 server connect point for {} connPt {}",
                    directConnFlag ? "direct" : "indirect", serverInfo.getDhcpServerConnectPoint());
            return false;
        }
        return true;
    }

    /**
     * extract from dhcp6 packet ClientIdOption.
     *
     * @param directConnFlag directly connected host
     * @param dhcp6Payload the dhcp6 payload
     * @return Dhcp6ClientIdOption clientIdOption, or null if not exists.
     */
    static Dhcp6ClientIdOption extractClientId(Boolean directConnFlag, DHCP6 dhcp6Payload) {
        Dhcp6ClientIdOption clientIdOption;

        if (directConnFlag) {
            clientIdOption = dhcp6Payload.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6ClientIdOption)
                    .map(opt -> (Dhcp6ClientIdOption) opt)
                    .findFirst()
                    .orElse(null);
        } else {
            DHCP6 leafDhcp = Dhcp6HandlerUtil.getDhcp6Leaf(dhcp6Payload);
            clientIdOption = leafDhcp.getOptions()
                    .stream()
                    .filter(opt -> opt instanceof Dhcp6ClientIdOption)
                    .map(opt -> (Dhcp6ClientIdOption) opt)
                    .findFirst()
                    .orElse(null);
        }

        return clientIdOption;
    }
}
