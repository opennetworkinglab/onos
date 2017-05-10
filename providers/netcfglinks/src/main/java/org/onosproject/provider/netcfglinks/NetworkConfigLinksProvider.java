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
package org.onosproject.provider.netcfglinks;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONOSLLDP;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.ProbedLinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.lldpcommon.LinkDiscoveryContext;
import org.onosproject.provider.lldpcommon.LinkDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.packet.Ethernet.TYPE_BSN;
import static org.onlab.packet.Ethernet.TYPE_LLDP;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Provider to pre-discover links and devices based on a specified network
 * config.
 */

@Component(immediate = true)
public class NetworkConfigLinksProvider
        extends AbstractProvider
        implements ProbedLinkProvider {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService masterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService metadataService;

    private static final String PROP_PROBE_RATE = "probeRate";
    private static final int DEFAULT_PROBE_RATE = 3000;
    @Property(name = PROP_PROBE_RATE, intValue = DEFAULT_PROBE_RATE,
            label = "LLDP and BDDP probe rate specified in millis")
    private int probeRate = DEFAULT_PROBE_RATE;

    // Device link discovery helpers.
    protected final Map<DeviceId, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    private final LinkDiscoveryContext context = new InternalDiscoveryContext();

    private LinkProviderService providerService;

    private static final String PROVIDER_NAME =
            "org.onosproject.provider.netcfglinks";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();
    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final InternalConfigListener cfgListener = new InternalConfigListener();

    protected Set<LinkKey> configuredLinks = new HashSet<>();

    public NetworkConfigLinksProvider() {
        super(new ProviderId("lldp", PROVIDER_NAME));
    }

    private String buildSrcMac() {
        String srcMac = ProbedLinkProvider.fingerprintMac(metadataService.getClusterMetadata());
        String defMac = ProbedLinkProvider.defaultMac();
        if (srcMac.equals(defMac)) {
            log.warn("Couldn't generate fingerprint. Using default value {}", defMac);
            return defMac;
        }
        log.trace("Generated MAC address {}", srcMac);
        return srcMac;
    }

    private void createLinks() {
        netCfgService.getSubjects(LinkKey.class)
                .forEach(linkKey -> configuredLinks.add(linkKey));
    }

    @Activate
    protected void activate() {
        log.info("Activated");
        appId = coreService.registerApplication(PROVIDER_NAME);
        packetService.addProcessor(packetProcessor, PacketProcessor.advisor(0));
        providerService = providerRegistry.register(this);
        deviceService.addListener(deviceListener);
        netCfgService.addListener(cfgListener);
        requestIntercepts();
        loadDevices();
        createLinks();
    }

    @Deactivate
    protected void deactivate() {
        withdrawIntercepts();
        providerRegistry.unregister(this);
        deviceService.removeListener(deviceListener);
        netCfgService.removeListener(cfgListener);
        packetService.removeProcessor(packetProcessor);
        disable();
        log.info("Deactivated");
    }

    /**
     * Loads available devices and registers their ports to be probed.
     */
    private void loadDevices() {
        deviceService.getAvailableDevices()
                .forEach(d -> updateDevice(d)
                        .ifPresent(ld -> updatePorts(ld, d.id())));
    }

    private Optional<LinkDiscovery> updateDevice(Device device) {
        if (device == null) {
            return Optional.empty();
        }

        LinkDiscovery ld = discoverers.computeIfAbsent(device.id(),
                did -> new LinkDiscovery(device, context));
        if (ld.isStopped()) {
            ld.start();
        }
        return Optional.of(ld);
    }

    /**
     * Updates ports of the specified device to the specified discovery helper.
     */
    private void updatePorts(LinkDiscovery discoverer, DeviceId deviceId) {
        deviceService.getPorts(deviceId).forEach(p -> updatePort(discoverer, p));
    }


    private void updatePort(LinkDiscovery discoverer, Port port) {
        if (port == null) {
            return;
        }
        if (port.number().isLogical()) {
            // silently ignore logical ports
            return;
        }

        discoverer.addPort(port);
    }

    /**
     * Disables link discovery processing.
     */
    private void disable() {

        providerRegistry.unregister(this);
        discoverers.values().forEach(LinkDiscovery::stop);
        discoverers.clear();

        providerService = null;
    }

    /**
     * Provides processing context for the device link discovery helpers.
     */
    private class InternalDiscoveryContext implements LinkDiscoveryContext {
        @Override
        public MastershipService mastershipService() {
            return masterService;
        }

        @Override
        public LinkProviderService providerService() {
            return providerService;
        }

        @Override
        public PacketService packetService() {
            return packetService;
        }

        @Override
        public long probeRate() {
            return probeRate;
        }

        @Override
        public boolean useBddp() {
            return true;
        }

        @Override
        public void touchLink(LinkKey key) {
        }

        @Override
        public String fingerprint() {
            return buildSrcMac();
        }

        @Override
        public DeviceService deviceService() {
            return deviceService;
        }
    }

    LinkKey extractLinkKey(PacketContext packetContext) {
        Ethernet eth = packetContext.inPacket().parsed();
        if (eth == null) {
            return null;
        }

        ONOSLLDP onoslldp = ONOSLLDP.parseONOSLLDP(eth);
        if (onoslldp != null) {
            PortNumber srcPort = portNumber(onoslldp.getPort());
            PortNumber dstPort = packetContext.inPacket().receivedFrom().port();
            DeviceId srcDeviceId = DeviceId.deviceId(onoslldp.getDeviceString());
            DeviceId dstDeviceId = packetContext.inPacket().receivedFrom().deviceId();

            ConnectPoint src = new ConnectPoint(srcDeviceId, srcPort);
            ConnectPoint dst = new ConnectPoint(dstDeviceId, dstPort);
            return LinkKey.linkKey(src, dst);
        }
        return null;
    }

    /**
     * Removes after stopping discovery helper for specified device.
     * @param deviceId device to remove
     */
    private void removeDevice(final DeviceId deviceId) {
        discoverers.computeIfPresent(deviceId, (did, ld) -> {
            ld.stop();
            return null;
        });

    }

    /**
     * Removes a port from the specified discovery helper.
     * @param port the port
     */
    private void removePort(Port port) {
        if (port.element() instanceof Device) {
            Device d = (Device) port.element();
            LinkDiscovery ld = discoverers.get(d.id());
            if (ld != null) {
                ld.removePort(port.number());
            }
        } else {
            log.warn("Attempted to remove non-Device port", port);
        }
    }


    /**
     * Processes incoming packets.
     */
    private class InternalPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            if (context == null || context.isHandled()) {
                return;
            }

            Ethernet eth = context.inPacket().parsed();
            if (eth == null || (eth.getEtherType() != TYPE_LLDP && eth.getEtherType() != TYPE_BSN)) {
                return;
            }

            InboundPacket inPacket = context.inPacket();
            LinkKey linkKey = extractLinkKey(context);
            if (linkKey != null) {
                if (configuredLinks.contains(linkKey)) {
                    log.debug("Found configured link {}", linkKey);
                    LinkDiscovery ld = discoverers.get(inPacket.receivedFrom().deviceId());
                    if (ld == null) {
                        return;
                    }
                    if (ld.handleLldp(context)) {
                        context.block();
                    }
                } else {
                    log.debug("Found link that was not in the configuration {}", linkKey);
                    providerService.linkDetected(
                            new DefaultLinkDescription(linkKey.src(),
                                                       linkKey.dst(),
                                                       Link.Type.DIRECT,
                                                       DefaultLinkDescription.NOT_EXPECTED,
                                                       DefaultAnnotations.EMPTY));
                }
            }
        }
    }

    /**
     * Requests packet intercepts.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_LLDP);
        packetService.requestPackets(selector.build(), PacketPriority.CONTROL,
                                     appId, Optional.empty());

        selector.matchEthType(TYPE_BSN);

        packetService.requestPackets(selector.build(), PacketPriority.CONTROL,
                                     appId, Optional.empty());

    }

    /**
     * Withdraws packet intercepts.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_LLDP);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL,
                                    appId, Optional.empty());
        selector.matchEthType(TYPE_BSN);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL,
                                    appId, Optional.empty());
    }

    /**
     * Processes device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() == DeviceEvent.Type.PORT_STATS_UPDATED) {
                return;
            }
            Device device = event.subject();
            Port port = event.port();
            if (device == null) {
                log.error("Device is null.");
                return;
            }
            log.trace("{} {} {}", event.type(), event.subject(), event);
            final DeviceId deviceId = device.id();
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                    updateDevice(device).ifPresent(ld -> updatePorts(ld, deviceId));
                    break;
                case PORT_ADDED:
                case PORT_UPDATED:
                    if (port.isEnabled()) {
                        updateDevice(device).ifPresent(ld -> updatePort(ld, port));
                    } else {
                        log.debug("Port down {}", port);
                        removePort(port);
                        providerService.linksVanished(new ConnectPoint(port.element().id(),
                                                                       port.number()));
                    }
                    break;
                case PORT_REMOVED:
                    log.debug("Port removed {}", port);
                    removePort(port);
                    providerService.linksVanished(new ConnectPoint(port.element().id(),
                                                                   port.number()));
                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    log.debug("Device removed {}", deviceId);
                    removeDevice(deviceId);
                    providerService.linksVanished(deviceId);
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(deviceId)) {
                        log.debug("Device up {}", deviceId);
                        updateDevice(device).ifPresent(ld -> updatePorts(ld, deviceId));
                    } else {
                        log.debug("Device down {}", deviceId);
                        removeDevice(deviceId);
                        providerService.linksVanished(deviceId);
                    }
                    break;
                case PORT_STATS_UPDATED:
                    break;
                default:
                    log.debug("Unknown event {}", event);
            }
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        private void addLink(LinkKey linkKey) {
            configuredLinks.add(linkKey);
        }

        private void removeLink(LinkKey linkKey) {
            DefaultLinkDescription linkDescription =
                    new DefaultLinkDescription(linkKey.src(), linkKey.dst(),
                                               Link.Type.DIRECT);
            configuredLinks.remove(linkKey);
            providerService.linkVanished(linkDescription);
        }

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(BasicLinkConfig.class)) {
                log.info("net config event of type {} for basic link {}",
                         event.type(), event.subject());
                LinkKey linkKey = (LinkKey) event.subject();
                if (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED) {
                    addLink(linkKey);
                } else if (event.type() == NetworkConfigEvent.Type.CONFIG_REMOVED) {
                    removeLink(linkKey);
                }
                log.info("Link reconfigured");
            }
        }
    }

}
