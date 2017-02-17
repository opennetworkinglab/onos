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
package org.onosproject.openstacknetworking.impl;

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
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC_STR;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;

/**
 * Handles ARP packet from VMs.
 */
@Component(immediate = true)
public final class OpenstackSwitchingArpHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String GATEWAY_MAC = "gatewayMac";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Property(name = GATEWAY_MAC, value = DEFAULT_GATEWAY_MAC_STR,
            label = "Fake MAC address for virtual network subnet gateway")
    private String gatewayMac = DEFAULT_GATEWAY_MAC_STR;

    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InternalOpenstackNetworkListener osNetworkListener =
            new InternalOpenstackNetworkListener();
    private final Set<IpAddress> gateways = Sets.newConcurrentHashSet();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        osNetworkService.addListener(osNetworkListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        osNetworkService.subnets().forEach(this::addSubnetGateway);
        requestPacket();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        osNetworkService.removeListener(osNetworkListener);
        configService.unregisterProperties(getClass(), false);

        log.info("Stopped");
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

    private void requestPacket() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .build();

        packetService.requestPackets(
                selector,
                PacketPriority.CONTROL,
                appId);
    }

    private void addSubnetGateway(Subnet osSubnet) {
        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            return;
        }
        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        gateways.add(gatewayIp);
        log.debug("Added ARP proxy entry IP:{}", gatewayIp);
    }

    private void removeSubnetGateway(Subnet osSubnet) {
        if (Strings.isNullOrEmpty(osSubnet.getGateway())) {
            return;
        }
        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        gateways.remove(gatewayIp);
        log.debug("Removed ARP proxy entry IP:{}", gatewayIp);
    }

    /**
     * Processes ARP request packets.
     * It checks if the target IP is owned by a known host first and then ask to
     * OpenStack if it's not. This ARP proxy does not support overlapping IP.
     *
     * @param context   packet context
     * @param ethPacket ethernet packet
     */
    private void processPacketIn(PacketContext context, Ethernet ethPacket) {
        ARP arpPacket = (ARP) ethPacket.getPayload();
        if (arpPacket.getOpCode() != ARP.OP_REQUEST) {
            return;
        }

        InstancePort srcInstPort = instancePortService.instancePort(ethPacket.getSourceMAC());
        if (srcInstPort == null) {
            log.trace("Failed to find source instance port(MAC:{})",
                    ethPacket.getSourceMAC());
            return;
        }

        IpAddress targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());
        MacAddress replyMac = gateways.contains(targetIp) ? MacAddress.valueOf(gatewayMac) :
                getMacFromHostOpenstack(targetIp, srcInstPort.networkId());
        if (replyMac == MacAddress.NONE) {
            log.trace("Failed to find MAC address for {}", targetIp);
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
     * instance port service.
     *
     * @param targetIp target ip
     * @param osNetId  openstack network id of the source instance port
     * @return mac address, or none mac address if it fails to find the mac
     */
    private MacAddress getMacFromHostOpenstack(IpAddress targetIp, String osNetId) {
        checkNotNull(targetIp);

        InstancePort instPort = instancePortService.instancePort(targetIp, osNetId);
        if (instPort != null) {
            log.trace("Found MAC from host service for {}", targetIp);
            return instPort.macAddress();
        } else {
            return MacAddress.NONE;
        }
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

    private class InternalOpenstackNetworkListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            Subnet osSubnet = event.subnet();
            if (osSubnet == null) {
                return false;
            }
            return !Strings.isNullOrEmpty(osSubnet.getGateway());
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            switch (event.type()) {
                case OPENSTACK_SUBNET_CREATED:
                case OPENSTACK_SUBNET_UPDATED:
                    addSubnetGateway(event.subnet());
                    break;
                case OPENSTACK_SUBNET_REMOVED:
                    removeSubnetGateway(event.subnet());
                    break;
                case OPENSTACK_NETWORK_CREATED:
                case OPENSTACK_NETWORK_UPDATED:
                case OPENSTACK_NETWORK_REMOVED:
                case OPENSTACK_PORT_CREATED:
                case OPENSTACK_PORT_UPDATED:
                case OPENSTACK_PORT_REMOVED:
                default:
                    // do nothing for the other events
                    break;
            }
        }
    }
}
