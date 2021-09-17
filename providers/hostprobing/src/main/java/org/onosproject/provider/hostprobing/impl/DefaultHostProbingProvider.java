/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.provider.hostprobing.impl;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostProbe;
import org.onosproject.net.host.HostProbingEvent;
import org.onosproject.net.host.HostProbingProvider;
import org.onosproject.net.host.HostProbingProviderRegistry;
import org.onosproject.net.host.HostProbingProviderService;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which sends host location probes to discover or verify a host at specific location.
 */
@Component(immediate = true, service = { HostProvider.class, HostProbingProvider.class })
public class DefaultHostProbingProvider extends AbstractProvider implements HostProvider, HostProbingProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private HostProbingProviderRegistry hostProbingProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    private HostProviderService providerService;
    private HostProbingProviderService hostProbingProviderService;
    private ExecutorService packetHandler;
    private ExecutorService probeEventHandler;
    private ScheduledExecutorService hostProber;

    private final PacketProcessor packetProcessor = context ->
        packetHandler.execute(() -> {
            Ethernet eth = context.inPacket().parsed();
            if (eth == null) {
                return;
            }
            MacAddress srcMac = eth.getSourceMAC();
            MacAddress destMac = eth.getDestinationMAC();
            VlanId vlan = VlanId.vlanId(eth.getVlanID());
            ConnectPoint heardOn = context.inPacket().receivedFrom();

            // Receives a location probe. Invalid entry from the cache
            if (destMac.isOnos() && !MacAddress.NONE.equals(destMac)) {
                log.debug("Receives probe for {}/{} on {}", srcMac, vlan, heardOn);
                hostProbingProviderService.removeProbingHost(destMac);
            }
        });

    // TODO Make this configurable
    private static final int PROBE_INIT_DELAY_MS = 1000;
    private static final int DEFAULT_RETRY = 5;

    /**
     * Creates an OpenFlow host provider.
     */
    public DefaultHostProbingProvider() {
        super(new ProviderId("hostprobing", "org.onosproject.provider.hostprobing"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        hostProbingProviderService = hostProbingProviderRegistry.register(this);

        packetHandler = newSingleThreadScheduledExecutor(groupedThreads("onos/host-loc-provider",
                "packet-handler", log));
        probeEventHandler = newSingleThreadScheduledExecutor(groupedThreads("onos/host-loc-provider",
                "probe-handler", log));
        hostProber = newScheduledThreadPool(32, groupedThreads("onos/host-loc-probe", "%d", log));

        packetService.addProcessor(packetProcessor, PacketProcessor.advisor(1));
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        hostProbingProviderRegistry.unregister(this);
        providerService = null;

        packetService.removeProcessor(packetProcessor);

        packetHandler.shutdown();
        probeEventHandler.shutdown();
        hostProber.shutdown();
    }

    @Override
    public void triggerProbe(Host host) {
        // Not doing anything at this moment...
    }

    @Override
    public void processEvent(HostProbingEvent event) {
        probeEventHandler.execute(() -> {
            log.debug("Receiving HostProbingEvent {}", event);
            HostProbe hostProbe = event.subject();

            switch (event.type()) {
                case PROBE_REQUESTED:
                    // Do nothing
                    break;
                case PROBE_TIMEOUT:
                    // Retry probe until PROBE_FAIL
                    // TODO Only retry DISCOVER probes
                    probeHostInternal(hostProbe, hostProbe.connectPoint(),
                            hostProbe.mode(), hostProbe.probeMac(), hostProbe.retry());
                    break;
                case PROBE_FAIL:
                    // Remove this location if this is a verify probe.
                    if (hostProbe.mode() == ProbeMode.VERIFY) {
                        ConnectPoint oldConnectPoint = hostProbe.connectPoint();
                        if (!oldConnectPoint.port().hasName()) {
                            oldConnectPoint = translateSwitchPort(oldConnectPoint);
                        }
                        providerService.removeLocationFromHost(hostProbe.id(),
                                new HostLocation(oldConnectPoint, 0L));
                    }
                    break;
                case PROBE_COMPLETED:
                    // Add this location if this is a discover probe.
                    if (hostProbe.mode() == ProbeMode.DISCOVER) {
                        ConnectPoint newConnectPoint = hostProbe.connectPoint();
                        if (!newConnectPoint.port().hasName()) {
                            newConnectPoint = translateSwitchPort(newConnectPoint);
                        }
                        providerService.addLocationToHost(hostProbe.id(),
                                new HostLocation(newConnectPoint, System.currentTimeMillis()));
                    }
                    break;
                default:
                    log.warn("Unknown HostProbingEvent type: {}", event.type());
            }
        });
    }

    @Override
    public void probeHost(Host host, ConnectPoint connectPoint, ProbeMode probeMode) {
        probeHostInternal(host, connectPoint, probeMode, null, DEFAULT_RETRY);
    }

    // probeMac can be null if this is the very first probe and the mac is to-be-generated.
    private void probeHostInternal(Host host, ConnectPoint connectPoint, ProbeMode probeMode,
                                   MacAddress probeMac, int retry) {
        if (!mastershipService.isLocalMaster(connectPoint.deviceId())) {
            log.debug("Current node is not master of {}, abort probing {}", connectPoint.deviceId(), host);
            return;
        }

        log.debug("probeHostInternal host={}, cp={}, mode={}, probeMac={}, retry={}", host, connectPoint,
                probeMode, probeMac, retry);
        Optional<IpAddress> ipOptional = host.ipAddresses().stream().findFirst();

        if (ipOptional.isPresent()) {
            probeMac = hostProbingProviderService.addProbingHost(host, connectPoint, probeMode, probeMac, retry);

            IpAddress ip = ipOptional.get();
            log.debug("Constructing {} probe for host {} with {}", probeMode, host.id(), ip);
            Ethernet probe;
            if (ip.isIp4()) {
                probe = ARP.buildArpRequest(probeMac.toBytes(), Ip4Address.ZERO.toOctets(),
                        host.id().mac().toBytes(), ip.toOctets(),
                        host.id().mac().toBytes(), host.id().vlanId().toShort());
            } else {
                probe = NeighborSolicitation.buildNdpSolicit(
                        ip.getIp6Address(),
                        Ip6Address.valueOf(IPv6.getLinkLocalAddress(probeMac.toBytes())),
                        ip.getIp6Address(),
                        probeMac,
                        host.id().mac(),
                        host.id().vlanId());
            }

            // NOTE: delay the probe a little bit to wait for the store synchronization is done
            hostProber.schedule(() ->
                    sendLocationProbe(probe, connectPoint), PROBE_INIT_DELAY_MS, TimeUnit.MILLISECONDS);
        } else {
            log.debug("Host {} has no IP address yet. Skip probing.", host);
        }
    }

    /**
     * Send the probe packet on given port.
     *
     * @param probe the probe packet
     * @param connectPoint the port we want to probe
     */
    private void sendLocationProbe(Ethernet probe, ConnectPoint connectPoint) {
        log.debug("Sending probe for host {} on location {} with probeMac {}",
                probe.getDestinationMAC(), connectPoint, probe.getSourceMAC());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(connectPoint.port()).build();
        OutboundPacket outboundPacket = new DefaultOutboundPacket(connectPoint.deviceId(),
                treatment, ByteBuffer.wrap(probe.serialize()));
        packetService.emit(outboundPacket);
    }

    /* Connect point generated from netcfg may not have port name
       we use the device service as translation service */
    private ConnectPoint translateSwitchPort(ConnectPoint connectPoint) {
        Port devicePort = deviceService.getPort(connectPoint);
        if (devicePort != null) {
            return new ConnectPoint(connectPoint.deviceId(), devicePort.number());
        }
        return connectPoint;
    }
}
