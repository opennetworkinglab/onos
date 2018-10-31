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
 */

package org.onosproject.routing.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.IpAddress.Version.INET6;
import static org.onosproject.routing.impl.OsgiPropertyConstants.ENABLED;
import static org.onosproject.routing.impl.OsgiPropertyConstants.ENABLED_DEFAULT;

/**
 * Reactively handles sending packets to hosts that are directly connected to
 * router interfaces.
 */
@Component(
    immediate = true,
    enabled = false,
    property = {
        ENABLED + ":Boolean=" + ENABLED_DEFAULT
    }
)
public class DirectHostManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /** Enable reactive directly-connected host processing. */
    private volatile boolean enabled = ENABLED_DEFAULT;

    private static final String APP_NAME = "org.onosproject.directhost";

    private static final long MAX_QUEUED_PACKETS = 10000;
    private static final long MAX_QUEUE_DURATION = 2; // seconds

    private ApplicationId appId;

    private InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private InternalHostListener hostListener = new InternalHostListener();

    private Cache<IpAddress, Queue<IP>> ipPacketCache = CacheBuilder.newBuilder()
            .weigher((IpAddress key, Queue<IP> value) -> value.size())
            .maximumWeight(MAX_QUEUED_PACKETS)
            .expireAfterAccess(MAX_QUEUE_DURATION, TimeUnit.SECONDS)
            .build();

    @Activate
    public void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        appId = coreService.registerApplication(APP_NAME);
        modified(context);
    }

    @Modified
    private void modified(ComponentContext context) {
        Boolean boolEnabled = Tools.isPropertyEnabled(context.getProperties(), ENABLED);
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
        // Requests packets for IPv4 traffic.
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).build();
        packetService.requestPackets(selector, PacketPriority.REACTIVE, appId, Optional.empty());
        // Requests packets for IPv6 traffic.
        selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV6.ethType().toShort()).build();
        packetService.requestPackets(selector, PacketPriority.REACTIVE, appId, Optional.empty());
    }

    private void disable() {
        packetService.removeProcessor(packetProcessor);
        hostService.removeListener(hostListener);
        // Withdraws IPv4 request.
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort()).build();
        packetService.cancelPackets(selector, PacketPriority.REACTIVE, appId, Optional.empty());
        // Withdraws IPv6 request.
        selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.IPV6.ethType().toShort()).build();
        packetService.cancelPackets(selector, PacketPriority.REACTIVE, appId, Optional.empty());
    }

    @Deactivate
    public void deactivate() {
        if (enabled) {
            disable();
        }

        componentConfigService.unregisterProperties(getClass(), false);
    }

    private boolean handle(Ethernet eth) {
        checkNotNull(eth);
        // If the DirectHostManager is not enabled and the
        // packets are different from what we expect just
        // skip them.
        if (!enabled || (eth.getEtherType() != Ethernet.TYPE_IPV6
                && eth.getEtherType() != Ethernet.TYPE_IPV4)) {
            return false;
        }
        // According to the type we set the destIp.
        IpAddress dstIp;
        if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ip = (IPv4) eth.getPayload();
            dstIp = IpAddress.valueOf(ip.getDestinationAddress());
        } else {
            IPv6 ip = (IPv6) eth.getPayload();
            dstIp = IpAddress.valueOf(INET6, ip.getDestinationAddress());
        }
        // Looking for a candidate output port.
        Interface egressInterface = interfaceService.getMatchingInterface(dstIp);

        if (egressInterface == null) {
            log.info("No egress interface found for {}", dstIp);
            return false;
        }
        // Looking for the destination mac.
        Optional<Host> host = hostService.getHostsByIp(dstIp).stream()
                .filter(h -> h.location().equals(egressInterface.connectPoint()))
                .filter(h -> h.vlan().equals(egressInterface.vlan()))
                .findAny();
        // If we don't have a destination we start the monitoring
        // and we queue the packets waiting for a destination.
        if (host.isPresent()) {
            transformAndSend(
                    (IP) eth.getPayload(),
                    eth.getEtherType(),
                    egressInterface,
                    host.get().mac()
            );
        } else {
            hostService.startMonitoringIp(dstIp);
            ipPacketCache.asMap().compute(dstIp, (ip, queue) -> {
                if (queue == null) {
                    queue = new ConcurrentLinkedQueue<>();
                }
                queue.add((IP) eth.getPayload());
                return queue;
            });
        }

        return true;
    }

    private void transformAndSend(IP ip, short ethType,
                                  Interface egressInterface,
                                  MacAddress macAddress) {
        // Base processing for IPv4
        if (ethType == Ethernet.TYPE_IPV4) {
            IPv4 ipv4 = (IPv4) ip;
            ipv4.setTtl((byte) (ipv4.getTtl() - 1));
            ipv4.setChecksum((short) 0);
        // Base processing for IPv6.
        } else {
            IPv6 ipv6 = (IPv6) ip;
            ipv6.setHopLimit((byte) (ipv6.getHopLimit() - 1));
            ipv6.resetChecksum();
        }
        // Sends and serializes.
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(macAddress);
        eth.setSourceMACAddress(egressInterface.mac());
        eth.setEtherType(ethType);
        eth.setPayload(ip);
        if (!egressInterface.vlan().equals(VlanId.NONE)) {
            eth.setVlanID(egressInterface.vlan().toShort());
        }
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
            packets.forEach(ipPackets -> {
                Interface egressInterface = interfaceService.getMatchingInterface(ipAddress);

                if (egressInterface == null) {
                    log.info("No egress interface found for {}", ipAddress);
                    return;
                }

                // According to the type of the address we set proper
                // protocol.
                transformAndSend(
                        ipPackets,
                        ipAddress.isIp4() ? Ethernet.TYPE_IPV4 : Ethernet.TYPE_IPV6,
                        egressInterface,
                        macAddress
                );
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

            if (!handle(eth)) {
                return;
            }

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
