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
import org.onosproject.net.host.HostService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.incubator.net.neighbour.NeighbourMessageType.REQUEST;

/**
 * Handler of ARP packets that responses or forwards ARP packets that
 * are sent to the controller.
 */
public class ArpHandler extends SegmentRoutingNeighbourHandler {

    private static Logger log = LoggerFactory.getLogger(ArpHandler.class);

    /**
     * Creates an ArpHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    public ArpHandler(SegmentRoutingManager srManager) {
        super(srManager);
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
            pkt.drop();
            return;
        }

        if (!validateArpSpa(pkt)) {
            log.debug("Ignore ARP packet discovered on {} with unexpected src protocol address {}.",
                    pkt.inPort(), pkt.sender().getIp4Address());
            pkt.drop();
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
            sendResponse(pkt, targetMac, hostService);
        } else {
            // NOTE: Ignore ARP packets except those target for the router
            //       We will reconsider enabling this when we have host learning support
            /*
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
            */
        }
    }

    private void handleArpReply(NeighbourMessageContext pkt, HostService hostService) {
        // ARP reply for router. Process all pending IP packets.
        if (isArpForRouter(pkt)) {
            Ip4Address hostIpAddress = pkt.sender().getIp4Address();
            srManager.ipHandler.forwardPackets(pkt.inPort().deviceId(), hostIpAddress);
        } else {
            // NOTE: Ignore ARP packets except those target for the router
            //       We will reconsider enabling this when we have host learning support
            /*
            HostId targetHostId = HostId.hostId(pkt.dstMac(), pkt.vlan());
            Host targetHost = hostService.getHost(targetHostId);
            // ARP reply for known hosts. Forward to the host.
            if (targetHost != null) {
                pkt.forward(targetHost.location());
            // ARP reply for unknown host, Flood in the subnet.
            } else {
                // Don't flood to non-edge ports
                if (pkt.vlan().equals(SegmentRoutingManager.INTERNAL_VLAN)) {
                    return;
                }
                flood(pkt);
            }
            */
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
        byte[] senderMacAddress = new byte[MacAddress.MAC_ADDRESS_LENGTH];
        byte[] senderIpAddress = new byte[Ip4Address.BYTE_LENGTH];
        /*
         * Retrieves device info.
         */
        if (!getSenderInfo(senderMacAddress, senderIpAddress, deviceId, targetAddress)) {
            log.warn("Aborting sendArpRequest, we cannot get all the information needed");
            return;
        }
        /*
         * Creates the request.
         */
        Ethernet arpRequest = ARP.buildArpRequest(
                senderMacAddress,
                senderIpAddress,
                targetAddress.toOctets(),
                VlanId.NO_VID
        );
        flood(arpRequest, inPort, targetAddress);
    }

}
