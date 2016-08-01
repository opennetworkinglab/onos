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
package org.onosproject.openstacknetworking.switching;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstacknetworking.AbstractVmHandler;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacknetworking.Constants.*;

/**
 * Handles ARP packet from VMs.
 */
@Component(immediate = true)
public final class OpenstackSwitchingArpHandler extends AbstractVmHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String GATEWAY_MAC = "gatewayMac";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Property(name = GATEWAY_MAC, value = DEFAULT_GATEWAY_MAC_STR,
            label = "Fake MAC address for virtual network subnet gateway")
    private String gatewayMac = DEFAULT_GATEWAY_MAC_STR;

    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final Set<Ip4Address> gateways = Sets.newConcurrentHashSet();

    @Activate
    protected void activate() {
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        super.activate();
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        super.deactivate();
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        String updatedMac;

        updatedMac = Tools.get(properties, GATEWAY_MAC);
        if (!Strings.isNullOrEmpty(updatedMac) && !updatedMac.equals(gatewayMac)) {
            gatewayMac = updatedMac;
        }

        log.info("Modified");
    }

    /**
     * Processes ARP request packets.
     * It checks if the target IP is owned by a known host first and then ask to
     * OpenStack if it's not. This ARP proxy does not support overlapping IP.
     *
     * @param context packet context
     * @param ethPacket ethernet packet
     */
    private void processPacketIn(PacketContext context, Ethernet ethPacket) {
        ARP arpPacket = (ARP) ethPacket.getPayload();
        if (arpPacket.getOpCode() != ARP.OP_REQUEST) {
            return;
        }

        Ip4Address targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());
        MacAddress replyMac = gateways.contains(targetIp) ? MacAddress.valueOf(gatewayMac) :
                getMacFromHostService(targetIp);
        if (replyMac.equals(MacAddress.NONE)) {
            replyMac = getMacFromOpenstack(targetIp);
        }

        if (replyMac == MacAddress.NONE) {
            log.debug("Failed to find MAC address for {}", targetIp.toString());
            return;
        }

        Ethernet ethReply = ARP.buildArpReply(
                targetIp.getIp4Address(),
                replyMac,
                ethPacket);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                context.inPacket().receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));
    }

    /**
     * Returns MAC address of a host with a given target IP address by asking to
     * OpenStack. It does not support overlapping IP.
     *
     * @param targetIp target ip address
     * @return mac address, or null if it fails to fetch the mac
     */
    private MacAddress getMacFromOpenstack(IpAddress targetIp) {
        checkNotNull(targetIp);

        OpenstackPort openstackPort = openstackService.ports()
                .stream()
                .filter(port -> port.fixedIps().containsValue(targetIp.getIp4Address()))
                .findFirst()
                .orElse(null);

        if (openstackPort != null) {
            log.debug("Found MAC from OpenStack for {}", targetIp.toString());
            return openstackPort.macAddress();
        } else {
            return MacAddress.NONE;
        }
    }

    /**
     * Returns MAC address of a host with a given target IP address by asking to
     * host service. It does not support overlapping IP.
     *
     * @param targetIp target ip
     * @return mac address, or null if it fails to find the mac
     */
    private MacAddress getMacFromHostService(IpAddress targetIp) {
        checkNotNull(targetIp);

        Host host = hostService.getHostsByIp(targetIp)
                .stream()
                .findFirst()
                .orElse(null);

        if (host != null) {
            log.debug("Found MAC from host service for {}", targetIp.toString());
            return host.mac();
        } else {
            return MacAddress.NONE;
        }
    }

    @Override
    protected void hostDetected(Host host) {
        OpenstackNetwork osNet = openstackService.network(host.annotations().value(NETWORK_ID));
        if (osNet == null) {
            log.warn("Failed to get OpenStack network for {}", host);
            return;
        }
        osNet.subnets().forEach(subnet -> gateways.add(Ip4Address.valueOf(subnet.gatewayIp())));
    }

    @Override
    protected void hostRemoved(Host host) {
        // TODO remove subnet gateway from gateways if no hosts exists on that subnet
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null || ethPacket.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }
            processPacketIn(context, ethPacket);
        }
    }
}
