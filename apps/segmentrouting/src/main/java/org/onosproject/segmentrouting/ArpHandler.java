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
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.incubator.net.neighbour.NeighbourMessageType.REQUEST;

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
     * @param pkt incoming ARP packet and context information
     * @param hostService the host service
     */
    public void processPacketIn(NeighbourMessageContext pkt, HostService hostService) {

        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId, SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(pkt.inPort())) {
            // Ignore ARP packets come from suppressed ports
            return;
        }

        if (!validateArpSpa(pkt)) {
            log.debug("Ignore ARP packet discovered on {} with unexpected src protocol address {}.",
                    pkt.inPort(), pkt.sender().getIp4Address());
            return;
        }

        if (pkt.type() == REQUEST) {
            handleArpRequest(pkt, hostService);
        } else {
            handleArpReply(pkt, hostService);
        }
    }

    private void handleArpRequest(NeighbourMessageContext pkt, HostService hostService) {
        // ARP request for router. Send ARP reply.
        if (isArpForRouter(pkt)) {
            MacAddress targetMac = config.getRouterMacForAGatewayIp(pkt.target().getIp4Address());
            sendArpResponse(pkt, targetMac, hostService);
        } else {
            Set<Host> hosts = hostService.getHostsByIp(pkt.target());
            if (hosts.size() > 1) {
                log.warn("More than one host with the same ip {}", pkt.target());
            }
            Host targetHost = hosts.stream().findFirst().orElse(null);
            // ARP request for known hosts. Send proxy ARP reply on behalf of the target.
            if (targetHost != null) {
                pkt.forward(targetHost.location());
            // ARP request for unknown host in the subnet. Flood in the subnet.
            } else {
                flood(pkt);
            }
        }
    }

    private void handleArpReply(NeighbourMessageContext pkt, HostService hostService) {
        // ARP reply for router. Process all pending IP packets.
        if (isArpForRouter(pkt)) {
            Ip4Address hostIpAddress = pkt.sender().getIp4Address();
            srManager.ipHandler.forwardPackets(pkt.inPort().deviceId(), hostIpAddress);
        } else {
            HostId targetHostId = HostId.hostId(pkt.dstMac(), pkt.vlan());
            Host targetHost = hostService.getHost(targetHostId);
            // ARP reply for known hosts. Forward to the host.
            if (targetHost != null) {
                pkt.forward(targetHost.location());
            // ARP reply for unknown host, Flood in the subnet.
            } else {
                // Don't flood to non-edge ports
                if (pkt.vlan().equals(
                        VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET))) {
                    return;
                }
                flood(pkt);
            }
        }
    }

    /**
     * Check if the source protocol address of an ARP packet belongs to the same
     * subnet configured on the port it is seen.
     *
     * @param pkt ARP packet and context information
     * @return true if the source protocol address belongs to the configured subnet
     */
    private boolean validateArpSpa(NeighbourMessageContext pkt) {
        Ip4Address spa = pkt.sender().getIp4Address();
        Set<IpPrefix> subnet = config.getPortSubnets(pkt.inPort().deviceId(), pkt.inPort().port())
                .stream()
                .filter(ipPrefix -> ipPrefix.isIp4() && ipPrefix.contains(spa))
                .collect(Collectors.toSet());
        return !subnet.isEmpty();
    }


    private boolean isArpForRouter(NeighbourMessageContext pkt) {
        Ip4Address targetProtocolAddress = pkt.target().getIp4Address();
        Set<IpAddress> gatewayIpAddresses = null;
        try {
            if (targetProtocolAddress.equals(config.getRouterIpv4(pkt.inPort().deviceId()))) {
                return true;
            }
            gatewayIpAddresses = config.getPortIPs(pkt.inPort().deviceId());
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
            senderIpAddress = config.getRouterIpAddressForASubnetHost(targetAddress.getIp4Address())
                    .toOctets();
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

        flood(eth, inPort);
    }

    private void sendArpResponse(NeighbourMessageContext pkt, MacAddress targetMac, HostService hostService) {
        HostId dstId = HostId.hostId(pkt.srcMac(), pkt.vlan());
        Host dst = hostService.getHost(dstId);
        if (dst == null) {
            log.warn("Cannot send ARP response to host {} - does not exist in the store",
                     dstId);
            return;
        }
        pkt.reply(targetMac);
    }

    /**
     * Remove VLAN tag and flood to all ports in the same subnet.
     *
     * @param packet packet to be flooded
     * @param inPort where the packet comes from
     */
    private void flood(Ethernet packet, ConnectPoint inPort) {
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
                                forward(packet, new ConnectPoint(inPort.deviceId(), port));
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
     * Remove VLAN tag and flood to all ports in the same subnet.
     *
     * @param pkt arp packet to be flooded
     */
    private void flood(NeighbourMessageContext pkt) {
        try {
            srManager.deviceConfiguration
                 .getSubnetPortsMap(pkt.inPort().deviceId()).forEach((subnet, ports) -> {
                     if (subnet.contains(pkt.target())) {
                         ports.stream()
                         .filter(port -> port != pkt.inPort().port())
                         .forEach(port -> {
                             ConnectPoint outPoint = new ConnectPoint(
                                     pkt.inPort().deviceId(),
                                     port
                             );
                             pkt.forward(outPoint);
                         });
                     }
                 });
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage()
                    + " Cannot flood in subnet as device config not available"
                    + " for device: " + pkt.inPort().deviceId());
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
    private void forward(Ethernet packet, ConnectPoint outPort) {
        packet.setEtherType(Ethernet.TYPE_ARP);

        ByteBuffer buf = ByteBuffer.wrap(packet.serialize());

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.setOutput(outPort.port());
        srManager.packetService.emit(new DefaultOutboundPacket(outPort.deviceId(),
                                                               tbuilder.build(), buf));
    }
}
