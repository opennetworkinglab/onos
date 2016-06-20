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

package org.onosproject.routing.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reactively handles sending packets to hosts that are directly connected to
 * router interfaces.
 */
@Component(immediate = true, enabled = false)
public class DirectHostManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    private static final boolean DEFAULT_ENABLED = false;

    @Property(name = "enabled", boolValue = DEFAULT_ENABLED,
            label = "Enable reactive directly-connected host processing")
    private volatile boolean enabled = DEFAULT_ENABLED;

    private static final String APP_NAME = "org.onosproject.directhost";

    private static final long MAX_QUEUED_PACKETS = 10000;
    private static final long MAX_QUEUE_DURATION = 2; // seconds

    private ApplicationId appId;

    private InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private InternalHostListener hostListener = new InternalHostListener();

    private Cache<IpAddress, Queue<IPv4>> ipPacketCache = CacheBuilder.newBuilder()
            .weigher((IpAddress key, Queue<IPv4> value) -> value.size())
            .maximumWeight(MAX_QUEUED_PACKETS)
            .expireAfterAccess(MAX_QUEUE_DURATION, TimeUnit.SECONDS)
            .build();

    @Activate
    public void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        modified(context);

        appId = coreService.registerApplication(APP_NAME);

        if (enabled) {
            enable();
        }
    }

    @Modified
    private void modified(ComponentContext context) {
        Boolean boolEnabled = Tools.isPropertyEnabled(context.getProperties(), "enabled");
        if (boolEnabled != null) {
            if (enabled && !boolEnabled) {
                enabled = false;
                disable();
            } else if (!enabled && boolEnabled) {
                enabled = true;
                enable();
            }
        }
    }

    private void enable() {
        hostService.addListener(hostListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(3));

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).build();
        packetService.requestPackets(selector, PacketPriority.REACTIVE, appId, Optional.empty());
    }

    private void disable() {
        packetService.removeProcessor(packetProcessor);
        hostService.removeListener(hostListener);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).build();
        packetService.cancelPackets(selector, PacketPriority.REACTIVE, appId, Optional.empty());
    }

    @Deactivate
    public void deactivate() {
        disable();

        componentConfigService.unregisterProperties(getClass(), false);
    }

    private void handle(Ethernet eth) {
        checkNotNull(eth);

        if (!(eth.getEtherType() == EthType.EtherType.IPV4.ethType().toShort())) {
            return;
        }

        IPv4 ipv4 = (IPv4) eth.getPayload().clone();

        Ip4Address dstIp = Ip4Address.valueOf(ipv4.getDestinationAddress());

        Interface egressInterface = interfaceService.getMatchingInterface(dstIp);

        if (egressInterface == null) {
            log.info("No egress interface found for {}", dstIp);
            return;
        }

        Optional<Host> host = hostService.getHostsByIp(dstIp).stream()
                .filter(h -> h.location().equals(egressInterface.connectPoint()))
                .filter(h -> h.vlan().equals(egressInterface.vlan()))
                .findAny();

        if (host.isPresent()) {
            transformAndSend(ipv4, egressInterface, host.get().mac());
        } else {
            hostService.startMonitoringIp(dstIp);
            ipPacketCache.asMap().compute(dstIp, (ip, queue) -> {
                if (queue == null) {
                    queue = new ConcurrentLinkedQueue();
                }
                queue.add(ipv4);
                return queue;
            });
        }
    }

    private void transformAndSend(IPv4 ipv4, Interface egressInterface, MacAddress macAddress) {

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(macAddress);
        eth.setSourceMACAddress(egressInterface.mac());
        eth.setEtherType(EthType.EtherType.IPV4.ethType().toShort());
        eth.setPayload(ipv4);
        if (!egressInterface.vlan().equals(VlanId.NONE)) {
            eth.setVlanID(egressInterface.vlan().toShort());
        }

        ipv4.setTtl((byte) (ipv4.getTtl() - 1));
        ipv4.setChecksum((short) 0);

        send(eth, egressInterface.connectPoint());
    }

    private void send(Ethernet eth, ConnectPoint cp) {
        OutboundPacket packet = new DefaultOutboundPacket(cp.deviceId(),
                DefaultTrafficTreatment.builder().setOutput(cp.port()).build(), ByteBuffer.wrap(eth.serialize()));
        packetService.emit(packet);
    }

    private void sendQueued(IpAddress ipAddress, MacAddress macAddress) {
        log.debug("Sending queued packets for {} ({})", ipAddress, macAddress);
        ipPacketCache.asMap().computeIfPresent(ipAddress, (ip, packets) -> {
            packets.forEach(ipv4 -> {
                Interface egressInterface = interfaceService.getMatchingInterface(ipAddress);

                if (egressInterface == null) {
                    log.info("No egress interface found for {}", ipAddress);
                    return;
                }

                transformAndSend(ipv4, egressInterface, macAddress);
            });
            return null;
        });
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            if (interfaceService.getInterfacesByPort(context.inPacket().receivedFrom()).isEmpty()) {
                // Don't handle packets that don't come from one of our configured interfaces
                return;
            }

            Ethernet eth = context.inPacket().parsed();
            if (eth == null) {
                return;
            }

            handle(eth);

            context.block();
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
            case HOST_ADDED:
                event.subject().ipAddresses().forEach(ip ->
                        DirectHostManager.this.sendQueued(ip, event.subject().mac()));
                break;
            case HOST_REMOVED:
            case HOST_UPDATED:
            case HOST_MOVED:
            default:
                break;
            }
        }
    }
}
