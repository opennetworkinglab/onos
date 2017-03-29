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

package org.onosproject.segmentrouting;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This handler provides provides useful functions to the
 * neighbour handlers (ARP, NDP).
 */
public class SegmentRoutingNeighbourHandler {

    private static Logger log = LoggerFactory.getLogger(SegmentRoutingNeighbourHandler.class);

    protected SegmentRoutingManager srManager;
    protected DeviceConfiguration config;

    /**
     * Creates an SegmentRoutingNeighbourHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    public SegmentRoutingNeighbourHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.config = checkNotNull(srManager.deviceConfiguration);
    }

    /**
     * Creates an SegmentRoutingNeighbourHandler object.
     */
    public SegmentRoutingNeighbourHandler() {
        this.srManager = null;
        this.config = null;
    }

    /**
     * Retrieve router (device) info.
     *
     * @param mac where to copy the mac
     * @param ip where to copy the ip
     * @param deviceId the device id
     * @param targetAddress the target address
     * @return true if it was possible to get the necessary info.
     * False for errors
     */
    protected boolean getSenderInfo(byte[] mac,
                                 byte[] ip,
                                 DeviceId deviceId,
                                 IpAddress targetAddress) {
        byte[] senderMacAddress;
        byte[] senderIpAddress;
        IpAddress sender;
        try {
            senderMacAddress = config.getDeviceMac(deviceId).toBytes();
            if (targetAddress.isIp4()) {
                sender = config.getRouterIpAddressForASubnetHost(targetAddress.getIp4Address());
            } else {
                sender = config.getRouterIpAddressForASubnetHost(targetAddress.getIp6Address());
            }
            // If sender is null we abort.
            if (sender == null) {
                log.warn("Sender ip is null. Aborting getSenderInfo");
                return false;
            }
            senderIpAddress = sender.toOctets();
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting getSenderInfo");
            return false;
        }
        System.arraycopy(senderMacAddress, 0, mac, 0, senderMacAddress.length);
        System.arraycopy(senderIpAddress, 0, ip, 0, senderIpAddress.length);
        return true;
    }

    /**
     * Utility to send a ND reply using the supplied information.
     *
     * @param pkt the request
     * @param targetMac the target mac
     * @param hostService the host service
     */
    protected void sendResponse(NeighbourMessageContext pkt, MacAddress targetMac, HostService hostService) {
        HostId dstId = HostId.hostId(pkt.srcMac(), pkt.vlan());
        Host dst = hostService.getHost(dstId);
        if (dst == null) {
            log.warn("Cannot send {} response to host {} - does not exist in the store",
                     pkt.protocol(), dstId);
            return;
        }
        pkt.reply(targetMac);
    }

    /**
     * Flood to all ports in the same subnet.
     *
     * @param packet packet to be flooded
     * @param inPort where the packet comes from
     * @param targetAddress the target address
     */
    protected void flood(Ethernet packet, ConnectPoint inPort, IpAddress targetAddress) {
        try {
            srManager.deviceConfiguration
                    .getSubnetPortsMap(inPort.deviceId()).forEach((subnet, ports) -> {
                if (subnet.contains(targetAddress)) {
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

    /*
     * Floods only on the port which have been configured with the subnet
     * of the target address. The in port is excluded.
     *
     * @param pkt the ndp/arp packet and context information
     */
    protected void flood(NeighbourMessageContext pkt) {
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
     * Packet out to given port.
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
        ByteBuffer buf = ByteBuffer.wrap(packet.serialize());

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.setOutput(outPort.port());
        srManager.packetService.emit(new DefaultOutboundPacket(outPort.deviceId(),
                                                               tbuilder.build(), buf));
    }

}
