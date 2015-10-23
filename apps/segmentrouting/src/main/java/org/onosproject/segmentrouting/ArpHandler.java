/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.HostId;
import org.onosproject.net.packet.OutboundPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import static com.google.common.base.Preconditions.checkNotNull;

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
     * If it is an ARP request to router itself or known hosts,
     * then it sends ARP response.
     * If it is an ARP request to unknown hosts in its own subnet,
     * then it flood the ARP request to the ports.
     * If it is an ARP response, then set a flow rule for the host
     * and forward any IP packets to the host in the packet buffer to the host.
     *
     * @param pkt incoming packet
     */
    public void processPacketIn(InboundPacket pkt) {

        Ethernet ethernet = pkt.parsed();
        ARP arp = (ARP) ethernet.getPayload();

        ConnectPoint connectPoint = pkt.receivedFrom();
        PortNumber inPort = connectPoint.port();
        DeviceId deviceId = connectPoint.deviceId();
        byte[] senderMacAddressByte = arp.getSenderHardwareAddress();
        Ip4Address hostIpAddress = Ip4Address.valueOf(arp.getSenderProtocolAddress());

        srManager.routingRulePopulator.populateIpRuleForHost(deviceId, hostIpAddress, MacAddress.
                valueOf(senderMacAddressByte), inPort);

        if (arp.getOpCode() == ARP.OP_REQUEST) {
            handleArpRequest(deviceId, connectPoint, ethernet);
        } else {
            srManager.ipHandler.forwardPackets(deviceId, hostIpAddress);
        }
    }

    private void handleArpRequest(DeviceId deviceId, ConnectPoint inPort, Ethernet payload) {
        ARP arpRequest = (ARP) payload.getPayload();
        HostId targetHostId = HostId.hostId(MacAddress.valueOf(
                arpRequest.getTargetHardwareAddress()));

        // ARP request for router
        if (isArpReqForRouter(deviceId, arpRequest)) {
            Ip4Address targetAddress = Ip4Address.valueOf(arpRequest.getTargetProtocolAddress());

            sendArpResponse(arpRequest, config.getRouterMacForAGatewayIp(targetAddress));
        } else {
            Host targetHost = srManager.hostService.getHost(targetHostId);
            // ARP request for known hosts
            if (targetHost != null) {
                sendArpResponse(arpRequest, targetHost.mac());

            // ARP request for unknown host in the subnet
            } else if (isArpReqForSubnet(deviceId, arpRequest)) {
                flood(payload, inPort);
            }
        }
    }


    private boolean isArpReqForRouter(DeviceId deviceId, ARP arpRequest) {
        List<Ip4Address> gatewayIpAddresses = config.getPortIPs(deviceId);
        if (gatewayIpAddresses != null) {
            Ip4Address targetProtocolAddress = Ip4Address.valueOf(arpRequest
                    .getTargetProtocolAddress());
            if (gatewayIpAddresses.contains(targetProtocolAddress)) {
                return true;
            }
        }
        return false;
    }

    private boolean isArpReqForSubnet(DeviceId deviceId, ARP arpRequest) {
        return config.getSubnets(deviceId).stream()
                     .anyMatch((prefix)->
                     prefix.contains(Ip4Address.
                                     valueOf(arpRequest.
                                             getTargetProtocolAddress())));
    }

    /**
     * Sends an APR request for the target IP address to all ports except in-port.
     *
     * @param deviceId Switch device ID
     * @param targetAddress target IP address for ARP
     * @param inPort in-port
     */
    public void sendArpRequest(DeviceId deviceId, IpAddress targetAddress, ConnectPoint inPort) {

        byte[] senderMacAddress = config.getDeviceMac(deviceId).toBytes();
        byte[] senderIpAddress = config.getRouterIp(deviceId).toOctets();

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

        flood(eth, inPort);
    }

    private void sendArpResponse(ARP arpRequest, MacAddress targetMac) {

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


        HostId dstId = HostId.hostId(MacAddress.valueOf(
                arpReply.getTargetHardwareAddress()));
        Host dst = srManager.hostService.getHost(dstId);
        if (dst == null) {
            log.warn("Cannot send ARP response to unknown device");
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
                treatment, ByteBuffer.wrap(eth.serialize()));

        srManager.packetService.emit(packet);
    }

    private void flood(Ethernet request, ConnectPoint inPort) {
        TrafficTreatment.Builder builder;
        ByteBuffer buf = ByteBuffer.wrap(request.serialize());

        for (Port port: srManager.deviceService.getPorts(inPort.deviceId())) {
            if (!port.number().equals(inPort.port()) &&
                    port.number().toLong() > 0) {
                builder = DefaultTrafficTreatment.builder();
                builder.setOutput(port.number());
                srManager.packetService.emit(new DefaultOutboundPacket(inPort.deviceId(),
                        builder.build(), buf));
            }
        }
    }

}
