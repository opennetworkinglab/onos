/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl.provider;

import com.google.common.collect.Sets;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkEvent;
import org.onosproject.incubator.net.virtual.VirtualNetworkListener;
import org.onosproject.incubator.net.virtual.VirtualPacketContext;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Component(service = VirtualPacketProvider.class)
public class DefaultVirtualPacketProvider extends AbstractVirtualProvider
        implements VirtualPacketProvider {

    private static final int PACKET_PROCESSOR_PRIORITY = 1;
    private static final PacketPriority VIRTUAL_PACKET_PRIORITY = PacketPriority.REACTIVE;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualNetworkAdminService vnaService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VirtualProviderRegistryService providerRegistryService;

    private final VirtualNetworkListener virtualNetListener = new InternalVirtualNetworkListener();

    private InternalPacketProcessor processor = null;

    private Set<NetworkId> networkIdSet = Sets.newConcurrentHashSet();

    private ApplicationId appId;

    /**
     * Creates a provider with the supplied identifier.
     */
    public DefaultVirtualPacketProvider() {
        super(new ProviderId("virtual-packet", "org.onosproject.virtual.virtual-packet"));
    }

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.virtual.virtual-packet");
        providerRegistryService.registerProvider(this);
        vnaService.addListener(virtualNetListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        providerRegistryService.unregisterProvider(this);
        vnaService.removeListener(virtualNetListener);

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
    }


    @Override
    public void emit(NetworkId networkId, OutboundPacket packet) {
       devirtualize(networkId, packet)
               .forEach(outboundPacket -> packetService.emit(outboundPacket));
    }

    /**
     * Just for test.
     */
    protected void startPacketHandling() {
        processor = new InternalPacketProcessor();
        packetService.addProcessor(processor, PACKET_PROCESSOR_PRIORITY);
    }

    /**
     * Send the outbound packet of a virtual context.
     * This method is designed to support Context's send() method that invoked
     * by applications.
     * See {@link org.onosproject.net.packet.PacketContext}
     *
     * @param virtualPacketContext virtual packet context
     */
    protected void send(VirtualPacketContext virtualPacketContext) {
        devirtualizeContext(virtualPacketContext)
                .forEach(outboundPacket -> packetService.emit(outboundPacket));
    }

    /**
     * Translate the requested physical PacketContext into a virtual PacketContext.
     * See {@link org.onosproject.net.packet.PacketContext}
     *
     * @param context A physical PacketContext be translated
     * @return A translated virtual PacketContext
     */
    private VirtualPacketContext virtualize(PacketContext context) {

        VirtualPort vPort = getMappedVirtualPort(context.inPacket().receivedFrom());

        if (vPort != null) {
            ConnectPoint cp = new ConnectPoint(vPort.element().id(),
                                               vPort.number());

            Ethernet eth = context.inPacket().parsed();
            eth.setVlanID(Ethernet.VLAN_UNTAGGED);

            InboundPacket inPacket =
                    new DefaultInboundPacket(cp, eth,
                                             ByteBuffer.wrap(eth.serialize()));

            DefaultOutboundPacket outPkt =
                    new DefaultOutboundPacket(cp.deviceId(),
                                              DefaultTrafficTreatment.builder().build(),
                                              ByteBuffer.wrap(eth.serialize()));

            VirtualPacketContext vContext =
                    new DefaultVirtualPacketContext(context.time(), inPacket, outPkt,
                                             false, vPort.networkId(),
                                             this);

            return vContext;
        } else {
            return null;
        }

    }

    /**
     * Find the corresponding virtual port with the physical port.
     *
     * @param cp the connect point for the physical network
     * @return a virtual port
     */
    private VirtualPort getMappedVirtualPort(ConnectPoint cp) {
        Set<TenantId> tIds = vnaService.getTenantIds();

        Set<VirtualNetwork> vNetworks = new HashSet<>();
        tIds.forEach(tid -> vNetworks.addAll(vnaService.getVirtualNetworks(tid)));

        for (VirtualNetwork vNet : vNetworks) {
            Set<VirtualDevice> vDevices = vnaService.getVirtualDevices(vNet.id());

            Set<VirtualPort> vPorts = new HashSet<>();
            vDevices.forEach(dev -> vPorts
                    .addAll(vnaService.getVirtualPorts(dev.networkId(), dev.id())));

            VirtualPort vPort = vPorts.stream()
                    .filter(vp -> vp.realizedBy().equals(cp))
                    .findFirst().orElse(null);

            if (vPort != null) {
                return vPort;
            }
        }

        return null;
    }

    /**
     * Translate the requested virtual outbound packet into
     * a set of physical OutboundPacket.
     * See {@link org.onosproject.net.packet.OutboundPacket}
     *
     * @param packet an OutboundPacket to be translated
     * @return a set of de-virtualized (physical) OutboundPacket
     */
    private Set<OutboundPacket> devirtualize(NetworkId networkId, OutboundPacket packet) {
        Set<OutboundPacket> outboundPackets = new HashSet<>();
        Set<VirtualPort> vPorts = vnaService
                .getVirtualPorts(networkId, packet.sendThrough());

        TrafficTreatment.Builder commonTreatmentBuilder
                = DefaultTrafficTreatment.builder();
        packet.treatment().allInstructions().stream()
                .filter(i -> i.type() != Instruction.Type.OUTPUT)
                .forEach(i -> commonTreatmentBuilder.add(i));
        TrafficTreatment commonTreatment = commonTreatmentBuilder.build();

        PortNumber vOutPortNum = packet.treatment().allInstructions().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .map(i -> ((Instructions.OutputInstruction) i).port())
                .findFirst().get();

        if (!vOutPortNum.isLogical()) {
            Optional<ConnectPoint> optionalCpOut = vPorts.stream()
                    .filter(v -> v.number().equals(vOutPortNum))
                    .map(v -> v.realizedBy())
                    .findFirst();
            if (!optionalCpOut.isPresent()) {
                log.warn("Port {} is not realized yet, in Network {}, Device {}",
                        vOutPortNum, networkId, packet.sendThrough());
                return outboundPackets;
            }
            ConnectPoint egressPoint = optionalCpOut.get();

            TrafficTreatment treatment = DefaultTrafficTreatment
                    .builder(commonTreatment)
                    .setOutput(egressPoint.port()).build();

            OutboundPacket outboundPacket = new DefaultOutboundPacket(
                    egressPoint.deviceId(), treatment, packet.data());
            outboundPackets.add(outboundPacket);
        } else {
            if (vOutPortNum == PortNumber.FLOOD) {
                for (VirtualPort outPort : vPorts) {
                    ConnectPoint cpOut = outPort.realizedBy();
                    if (cpOut != null) {
                        TrafficTreatment treatment = DefaultTrafficTreatment
                                .builder(commonTreatment)
                                .setOutput(cpOut.port()).build();
                        OutboundPacket outboundPacket = new DefaultOutboundPacket(
                                cpOut.deviceId(), treatment, packet.data());
                        outboundPackets.add(outboundPacket);
                    } else {
                        log.warn("Port {} is not realized yet, in Network {}, Device {}",
                                outPort.number(), networkId, packet.sendThrough());
                    }
                }
            }
        }

        return outboundPackets;
    }

    /**
     * Translate the requested virtual packet context into
     * a set of physical outbound packets.
     *
     * @param context A handled virtual packet context
     */
    private Set<OutboundPacket> devirtualizeContext(VirtualPacketContext context) {

        Set<OutboundPacket> outboundPackets = new HashSet<>();

        NetworkId networkId = context.networkId();
        TrafficTreatment vTreatment = context.treatmentBuilder().build();
        DeviceId sendThrough = context.outPacket().sendThrough();

        Set<VirtualPort> vPorts = vnaService
                .getVirtualPorts(networkId, sendThrough);

        PortNumber vOutPortNum = vTreatment.allInstructions().stream()
                .filter(i -> i.type() == Instruction.Type.OUTPUT)
                .map(i -> ((Instructions.OutputInstruction) i).port())
                .findFirst().get();

        TrafficTreatment.Builder commonTreatmentBuilder
                = DefaultTrafficTreatment.builder();
        vTreatment.allInstructions().stream()
                .filter(i -> i.type() != Instruction.Type.OUTPUT)
                .forEach(i -> commonTreatmentBuilder.add(i));
        TrafficTreatment commonTreatment = commonTreatmentBuilder.build();

        if (!vOutPortNum.isLogical()) {
            Optional<ConnectPoint> optionalCpOut = vPorts.stream()
                    .filter(v -> v.number().equals(vOutPortNum))
                    .map(v -> v.realizedBy())
                    .findFirst();
            if (!optionalCpOut.isPresent()) {
                log.warn("Port {} is not realized yet, in Network {}, Device {}",
                        vOutPortNum, networkId, sendThrough);
                return outboundPackets;
            }
            ConnectPoint egressPoint = optionalCpOut.get();

            TrafficTreatment treatment = DefaultTrafficTreatment
                    .builder(commonTreatment)
                    .setOutput(egressPoint.port()).build();

            OutboundPacket outboundPacket = new DefaultOutboundPacket(
                    egressPoint.deviceId(), treatment, context.outPacket().data());
            outboundPackets.add(outboundPacket);
        } else {
            if (vOutPortNum == PortNumber.FLOOD) {
                Set<VirtualPort> outPorts = vPorts.stream()
                        .filter(vp -> !vp.number().isLogical())
                        .filter(vp -> vp.number() !=
                                context.inPacket().receivedFrom().port())
                        .collect(Collectors.toSet());

                for (VirtualPort outPort : outPorts) {
                    ConnectPoint cpOut = outPort.realizedBy();
                    if (cpOut != null) {
                        TrafficTreatment treatment = DefaultTrafficTreatment
                                .builder(commonTreatment)
                                .setOutput(cpOut.port()).build();
                        OutboundPacket outboundPacket = new DefaultOutboundPacket(
                                cpOut.deviceId(), treatment, context.outPacket().data());
                        outboundPackets.add(outboundPacket);
                    } else {
                        log.warn("Port {} is not realized yet, in Network {}, Device {}",
                                outPort.number(), networkId, sendThrough);
                    }
                }
            }
        }
        return outboundPackets;
    }

    private final class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }
            VirtualPacketContext vContexts = virtualize(context);

            if (vContexts == null) {
                return;
            }

            VirtualPacketProviderService service =
                    (VirtualPacketProviderService) providerRegistryService
                            .getProviderService(vContexts.networkId(),
                                                VirtualPacketProvider.class);
            if (service != null) {
                service.processPacket(vContexts);
            }
        }
    }

    private class InternalVirtualNetworkListener implements VirtualNetworkListener {

        @Override
        public void event(VirtualNetworkEvent event) {
            switch (event.type()) {
                case NETWORK_ADDED:
                    if (networkIdSet.isEmpty()) {
                        processor = new InternalPacketProcessor();
                        packetService.addProcessor(processor, PACKET_PROCESSOR_PRIORITY);
                        log.info("Packet processor {} for virtual network is added.", processor.getClass().getName());
                    }
                    networkIdSet.add(event.subject());
                    break;

                case NETWORK_REMOVED:
                    networkIdSet.remove(event.subject());
                    if (networkIdSet.isEmpty()) {
                        packetService.removeProcessor(processor);
                        log.info("Packet processor {} for virtual network is removed.", processor.getClass().getName());
                        processor = null;
                    }
                    break;

                default:
                    // do nothing
                    break;
            }
        }
    }

}
