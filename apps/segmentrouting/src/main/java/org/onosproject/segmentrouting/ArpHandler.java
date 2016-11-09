/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.segmentrouting;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.HostId;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handler of ARP packets that responses or forwards ARP packets that
 * are sent to the controller.
 */
public class ArpHandler {

    private static Logger log = LoggerFactory.getLogger(ArpHandler.class);

    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;

    /**
     * Creates an ArpHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    public ArpHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.config = checkNotNull(srManager.deviceConfiguration);
    }

    /**
     * Processes incoming ARP packets.
     *
     * If it is an ARP request to router itself or known hosts,
     * then it sends ARP response.
     * If it is an ARP request to unknown hosts in its own subnet,
     * then it flood the ARP request to the ports.
     * If it is an ARP response, then set a flow rule for the host
     * and forward any IP packets to the host in the packet buffer to the host.
     * <p>
     * Note: We handles all ARP packet in, even for those ARP packets between
     * hosts in the same subnet.
     * For an ARP packet with broadcast destination MAC,
     * some switches pipelines will send it to the controller due to table miss,
     * other switches will flood the packets directly in the data plane without
     * packet in.
     * We can deal with both cases.
     *
     * @param pkt incoming packet
     */
    public void processPacketIn(InboundPacket pkt) {
        Ethernet ethernet = pkt.parsed();
        ARP arp = (ARP) ethernet.getPayload();
        ConnectPoint connectPoint = pkt.receivedFrom();
        DeviceId deviceId = connectPoint.deviceId();

        if (!validateArpSpa(connectPoint, arp)) {
            log.debug("Ignore ARP packet discovered on {} with unexpected src protocol address {}.",
                    connectPoint, Ip4Address.valueOf(arp.getSenderProtocolAddress()));
            return;
        }

        if (arp.getOpCode() == ARP.OP_REQUEST) {
            handleArpRequest(deviceId, connectPoint, ethernet);
        } else {
            handleArpReply(deviceId, connectPoint, ethernet);
        }
    }

    private void handleArpRequest(DeviceId deviceId, ConnectPoint inPort, Ethernet payload) {
        ARP arpRequest = (ARP) payload.getPayload();
        VlanId vlanId = VlanId.vlanId(payload.getVlanID());
        HostId targetHostId = HostId.hostId(MacAddress.valueOf(
                                            arpRequest.getTargetHardwareAddress()),
                                            vlanId);

        // ARP request for router. Send ARP reply.
        if (isArpForRouter(deviceId, arpRequest)) {
            Ip4Address targetAddress = Ip4Address.valueOf(arpRequest.getTargetProtocolAddress());
            sendArpResponse(arpRequest, config.getRouterMacForAGatewayIp(targetAddress), vlanId);
        } else {
            Host targetHost = srManager.hostService.getHost(targetHostId);
            // ARP request for known hosts. Send proxy ARP reply on behalf of the target.
            if (targetHost != null) {
                removeVlanAndForward(payload, targetHost.location());
            // ARP request for unknown host in the subnet. Flood in the subnet.
            } else {
                removeVlanAndFlood(payload, inPort);
            }
        }
    }

    private void handleArpReply(DeviceId deviceId, ConnectPoint inPort, Ethernet payload) {
        ARP arpReply = (ARP) payload.getPayload();
        VlanId vlanId = VlanId.vlanId(payload.getVlanID());
        HostId targetHostId = HostId.hostId(MacAddress.valueOf(
                                            arpReply.getTargetHardwareAddress()),
                                            vlanId);

        // ARP reply for router. Process all pending IP packets.
        if (isArpForRouter(deviceId, arpReply)) {
            Ip4Address hostIpAddress = Ip4Address.valueOf(arpReply.getSenderProtocolAddress());
            srManager.ipHandler.forwardPackets(deviceId, hostIpAddress);
        } else {
            Host targetHost = srManager.hostService.getHost(targetHostId);
            // ARP reply for known hosts. Forward to the host.
            if (targetHost != null) {
                removeVlanAndForward(payload, targetHost.location());
            // ARP reply for unknown host, Flood in the subnet.
            } else {
                // Don't flood to non-edge ports
                if (vlanId.equals(
                        VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET))) {
                    return;
                }
                removeVlanAndFlood(payload, inPort);
            }
        }
    }

    /**
     * Check if the source protocol address of an ARP packet belongs to the same
     * subnet configured on the port it is seen.
     *
     * @param connectPoint connect point where the ARP packet is seen
     * @param arpPacket ARP packet
     * @return true if the source protocol address belongs to the configured subnet
     */
    private boolean validateArpSpa(ConnectPoint connectPoint, ARP arpPacket) {
        Ip4Address spa = Ip4Address.valueOf(arpPacket.getSenderProtocolAddress());
        Ip4Prefix subnet = config.getPortSubnet(connectPoint.deviceId(), connectPoint.port());
        return subnet != null && subnet.contains(spa);
    }


    private boolean isArpForRouter(DeviceId deviceId, ARP arpMsg) {
        Ip4Address targetProtocolAddress = Ip4Address.valueOf(
                                               arpMsg.getTargetProtocolAddress());
        Set<Ip4Address> gatewayIpAddresses = null;
        try {
            if (targetProtocolAddress.equals(config.getRouterIp(deviceId))) {
                return true;
            }
            gatewayIpAddresses = config.getPortIPs(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting check for router IP in processing arp");
        }
        if (gatewayIpAddresses != null &&
                gatewayIpAddresses.contains(targetProtocolAddress)) {
            return true;
        }
        return false;
    }

    /**
     * Sends an APR request for the target IP address to all ports except in-port.
     *
     * @param deviceId Switch device ID
     * @param targetAddress target IP address for ARP
     * @param inPort in-port
     */
    public void sendArpRequest(DeviceId deviceId, IpAddress targetAddress, ConnectPoint inPort) {
        byte[] senderMacAddress;
        byte[] senderIpAddress;

        try {
            senderMacAddress = config.getDeviceMac(deviceId).toBytes();
            senderIpAddress = config.getRouterIp(deviceId).toOctets();
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting sendArpRequest.");
            return;
        }

        ARP arpRequest = new ARP();
        arpRequest.setHardwareType(ARP.HW_TYPE_ETHERNET)
                  .setProtocolType(ARP.PROTO_TYPE_IP)
                  .setHardwareAddressLength(
                        (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                  .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                  .setOpCode(ARP.OP_REQUEST)
                  .setSenderHardwareAddress(senderMacAddress)
                  .setTargetHardwareAddress(MacAddress.ZERO.toBytes())
                  .setSenderProtocolAddress(senderIpAddress)
                  .setTargetProtocolAddress(targetAddress.toOctets());

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(MacAddress.BROADCAST.toBytes())
                .setSourceMACAddress(senderMacAddress)
                .setEtherType(Ethernet.TYPE_ARP).setPayload(arpRequest);

        removeVlanAndFlood(eth, inPort);
    }

    private void sendArpResponse(ARP arpRequest, MacAddress targetMac, VlanId vlanId) {
        ARP arpReply = new ARP();
        arpReply.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setHardwareAddressLength(
                        (byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH)
                .setOpCode(ARP.OP_REPLY)
                .setSenderHardwareAddress(targetMac.toBytes())
                .setSenderProtocolAddress(arpRequest.getTargetProtocolAddress())
                .setTargetHardwareAddress(arpRequest.getSenderHardwareAddress())
                .setTargetProtocolAddress(arpRequest.getSenderProtocolAddress());

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(arpRequest.getSenderHardwareAddress())
                .setSourceMACAddress(targetMac.toBytes())
                .setEtherType(Ethernet.TYPE_ARP).setPayload(arpReply);

        MacAddress hostMac = MacAddress.valueOf(arpReply.getTargetHardwareAddress());
        HostId dstId = HostId.hostId(hostMac, vlanId);
        Host dst = srManager.hostService.getHost(dstId);
        if (dst == null) {
            log.warn("Cannot send ARP response to host {}", dstId);
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
                treatment, ByteBuffer.wrap(eth.serialize()));

        srManager.packetService.emit(packet);
    }

    /**
     * Remove VLAN tag and flood to all ports in the same subnet.
     *
     * @param packet packet to be flooded
     * @param inPort where the packet comes from
     */
    private void removeVlanAndFlood(Ethernet packet, ConnectPoint inPort) {
        Ip4Address targetProtocolAddress = Ip4Address.valueOf(
                ((ARP) packet.getPayload()).getTargetProtocolAddress()
        );

        try {
            srManager.deviceConfiguration
                 .getSubnetPortsMap(inPort.deviceId()).forEach((subnet, ports) -> {
                     if (subnet.contains(targetProtocolAddress)) {
                         ports.stream()
                         .filter(port -> port != inPort.port())
                         .forEach(port -> {
                             removeVlanAndForward(packet,
                                 new ConnectPoint(inPort.deviceId(), port));
                         });
                     }
                 });
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage()
                    + " Cannot flood in subnet as device config not available"
                    + " for device: " + inPort.deviceId());
        }
    }

    /**
     * Remove VLAN tag and packet out to given port.
     *
     * Note: In current implementation, we expect all communication with
     * end hosts within a subnet to be untagged.
     * <p>
     * For those pipelines that internally assigns a VLAN, the VLAN tag will be
     * removed before egress.
     * <p>
     * For those pipelines that do not assign internal VLAN, the packet remains
     * untagged.
     *
     * @param packet packet to be forwarded
     * @param outPort where the packet should be forwarded
     */
    private void removeVlanAndForward(Ethernet packet, ConnectPoint outPort) {
        packet.setEtherType(Ethernet.TYPE_ARP);
        packet.setVlanID(Ethernet.VLAN_UNTAGGED);
        ByteBuffer buf = ByteBuffer.wrap(packet.serialize());

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.setOutput(outPort.port());
        srManager.packetService.emit(new DefaultOutboundPacket(outPort.deviceId(),
                                                               tbuilder.build(), buf));
    }
}
