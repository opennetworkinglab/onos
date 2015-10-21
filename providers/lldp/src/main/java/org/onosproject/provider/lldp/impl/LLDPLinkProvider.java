/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.provider.lldp.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.onlab.packet.Ethernet.TYPE_BSN;
import static org.onlab.packet.Ethernet.TYPE_LLDP;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses LLDP and BDDP packets to detect network infrastructure links.
 */
@Component(immediate = true)
public class LLDPLinkProvider extends AbstractProvider implements LinkProvider {

    private static final String PROVIDER_NAME = "org.onosproject.provider.lldp";

    private static final String FORMAT =
            "Settings: enabled={}, useBDDP={}, probeRate={}, " +
                    "staleLinkAge={}, lldpSuppression={}";

    // When a Device/Port has this annotation, do not send out LLDP/BDDP
    public static final String NO_LLDP = "no-lldp";


    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService masterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgRegistry;

    private LinkProviderService providerService;

    private ScheduledExecutorService executor;

    // TODO: Add sanity checking for the configurable params based on the delays
    private static final long DEVICE_SYNC_DELAY = 5;
    private static final long LINK_PRUNER_DELAY = 3;

    private static final String PROP_ENABLED = "enabled";
    @Property(name = PROP_ENABLED, boolValue = true,
            label = "If false, link discovery is disabled")
    private boolean enabled = false;

    private static final String PROP_USE_BDDP = "useBDDP";
    @Property(name = PROP_USE_BDDP, boolValue = true,
            label = "Use BDDP for link discovery")
    private boolean useBDDP = true;

    private static final String PROP_PROBE_RATE = "probeRate";
    private static final int DEFAULT_PROBE_RATE = 3_000;
    @Property(name = PROP_PROBE_RATE, intValue = DEFAULT_PROBE_RATE,
            label = "LLDP and BDDP probe rate specified in millis")
    private int probeRate = DEFAULT_PROBE_RATE;

    private static final String PROP_STALE_LINK_AGE = "staleLinkAge";
    private static final int DEFAULT_STALE_LINK_AGE = 10_000;
    @Property(name = PROP_STALE_LINK_AGE, intValue = DEFAULT_STALE_LINK_AGE,
            label = "Number of millis beyond which links will be considered stale")
    private int staleLinkAge = DEFAULT_STALE_LINK_AGE;

    // FIXME: convert to use network config subsystem instead
    private static final String PROP_LLDP_SUPPRESSION = "lldpSuppression";
    private static final String DEFAULT_LLDP_SUPPRESSION_CONFIG = "../config/lldp_suppression.json";
    @Property(name = PROP_LLDP_SUPPRESSION, value = DEFAULT_LLDP_SUPPRESSION_CONFIG,
            label = "Path to LLDP suppression configuration file")
    private String lldpSuppression = DEFAULT_LLDP_SUPPRESSION_CONFIG;

    private final DiscoveryContext context = new InternalDiscoveryContext();
    private final InternalRoleListener roleListener = new InternalRoleListener();
    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    // Device link discovery helpers.
    protected final Map<DeviceId, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    // Most recent time a tracked link was seen; links are tracked if their
    // destination connection point is mastered by this controller instance.
    private final Map<LinkKey, Long> linkTimes = Maps.newConcurrentMap();

    private SuppressionRules rules;
    private ApplicationId appId;

    /**
     * Creates an OpenFlow link provider.
     */
    public LLDPLinkProvider() {
        super(new ProviderId("lldp", PROVIDER_NAME));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication(PROVIDER_NAME);
        modified(context);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        disable();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        boolean newEnabled, newUseBddp;
        int newProbeRate, newStaleLinkAge;
        String newLldpSuppression;
        try {
            String s = get(properties, PROP_ENABLED);
            newEnabled = isNullOrEmpty(s) || Boolean.parseBoolean(s.trim());

            s = get(properties, PROP_USE_BDDP);
            newUseBddp = isNullOrEmpty(s) || Boolean.parseBoolean(s.trim());

            s = get(properties, PROP_PROBE_RATE);
            newProbeRate = isNullOrEmpty(s) ? probeRate : Integer.parseInt(s.trim());

            s = get(properties, PROP_STALE_LINK_AGE);
            newStaleLinkAge = isNullOrEmpty(s) ? staleLinkAge : Integer.parseInt(s.trim());

            s = get(properties, PROP_LLDP_SUPPRESSION);
            newLldpSuppression = isNullOrEmpty(s) ? DEFAULT_LLDP_SUPPRESSION_CONFIG : s;

        } catch (NumberFormatException e) {
            log.warn("Component configuration had invalid values", e);
            newEnabled = enabled;
            newUseBddp = useBDDP;
            newProbeRate = probeRate;
            newStaleLinkAge = staleLinkAge;
            newLldpSuppression = lldpSuppression;
        }

        boolean wasEnabled = enabled;

        enabled = newEnabled;
        useBDDP = newUseBddp;
        probeRate = newProbeRate;
        staleLinkAge = newStaleLinkAge;
        lldpSuppression = newLldpSuppression;

        if (!wasEnabled && enabled) {
            enable();
        } else if (wasEnabled && !enabled) {
            disable();
        } else {
            // reflect changes in suppression rules to discovery helpers
            // FIXME: After migrating to Network Configuration Subsystem,
            //        it should be possible to update only changed subset
            if (enabled) {
                // update all discovery helper state
                loadDevices();
            }
        }

        log.info(FORMAT, enabled, useBDDP, probeRate, staleLinkAge, lldpSuppression);
    }

    /**
     * Enables link discovery processing.
     */
    private void enable() {
        providerService = providerRegistry.register(this);
        masterService.addListener(roleListener);
        deviceService.addListener(deviceListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.advisor(0));

        loadSuppressionRules();
        loadDevices();

        executor = newSingleThreadScheduledExecutor(groupedThreads("onos/link", "discovery-%d"));
        executor.scheduleAtFixedRate(new SyncDeviceInfoTask(),
                                     DEVICE_SYNC_DELAY, DEVICE_SYNC_DELAY, SECONDS);
        executor.scheduleAtFixedRate(new LinkPrunerTask(),
                                     LINK_PRUNER_DELAY, LINK_PRUNER_DELAY, SECONDS);

        requestIntercepts();
    }

    /**
     * Disables link discovery processing.
     */
    private void disable() {
        withdrawIntercepts();

        providerRegistry.unregister(this);
        masterService.removeListener(roleListener);
        deviceService.removeListener(deviceListener);
        packetService.removeProcessor(packetProcessor);

        if (executor != null) {
            executor.shutdownNow();
        }
        discoverers.values().forEach(LinkDiscovery::stop);
        discoverers.clear();

        providerService = null;
    }

    /**
     * Loads available devices and registers their ports to be probed.
     */
    private void loadDevices() {
        deviceService.getAvailableDevices()
                .forEach(d -> updateDevice(d)
                               .ifPresent(ld -> updatePorts(ld, d.id())));
    }

    /**
     * Updates discovery helper for specified device.
     *
     * Adds and starts a discovery helper for specified device if enabled,
     * calls {@link #removeDevice(DeviceId)} otherwise.
     *
     * @param device device to add
     * @return discovery helper if discovery is enabled for the device
     */
    private Optional<LinkDiscovery> updateDevice(Device device) {
        if (rules.isSuppressed(device)) {
            log.trace("LinkDiscovery from {} disabled by configuration", device.id());
            removeDevice(device.id());
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
     * Removes after stopping discovery helper for specified device.
     * @param deviceId device to remove
     */
    private void removeDevice(final DeviceId deviceId) {
        discoverers.computeIfPresent(deviceId, (did, ld) -> {
            ld.stop();
            providerService.linksVanished(deviceId);
            return null;
        });

    }

    /**
     * Updates ports of the specified device to the specified discovery helper.
     */
    private void updatePorts(LinkDiscovery discoverer, DeviceId deviceId) {
        deviceService.getPorts(deviceId).forEach(p -> updatePort(discoverer, p));
    }

    /**
     * Updates discovery helper state of the specified port.
     *
     * Adds a port to the discovery helper if up and discovery is enabled,
     * or calls {@link #removePort(Port)} otherwise.
     */
    private void updatePort(LinkDiscovery discoverer, Port port) {
        if (rules.isSuppressed(port)) {
            log.trace("LinkDiscovery from {} disabled by configuration", port);
            removePort(port);
            return;
        }

        // check if enabled and turn off discovery?
        if (!port.isEnabled()) {
            removePort(port);
            return;
        }

        if (!port.number().isLogical()) {
            discoverer.addPort(port);
        }
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

            ConnectPoint point = new ConnectPoint(d.id(), port.number());
            providerService.linksVanished(point);
        } else {
            log.warn("Attempted to remove non-Device port", port);
        }
    }

    /**
     * Loads LLDP suppression rules.
     */
    private void loadSuppressionRules() {
        // FIXME: convert to use network configuration
        SuppressionRulesStore store = new SuppressionRulesStore(lldpSuppression);
        try {
            log.info("Reading suppression rules from {}", lldpSuppression);
            rules = store.read();
        } catch (IOException e) {
            log.info("Failed to load {}, using built-in rules", lldpSuppression);
            // default rule to suppress ROADM to maintain compatibility
            rules = new SuppressionRules(ImmutableSet.of(),
                                         EnumSet.of(Device.Type.ROADM),
                                         ImmutableMap.of(NO_LLDP, SuppressionRules.ANY_VALUE));
        }

        // should refresh discoverers when we need dynamic reconfiguration
    }

    /**
     * Requests packet intercepts.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_LLDP);
        packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);

        selector.matchEthType(TYPE_BSN);
        if (useBDDP) {
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
        } else {
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
        }
    }

    /**
     * Withdraws packet intercepts.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_LLDP);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
        selector.matchEthType(TYPE_BSN);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
    }

    /**
     * Processes device mastership role changes.
     */
    private class InternalRoleListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            if (MastershipEvent.Type.BACKUPS_CHANGED.equals(event.type())) {
                // only need new master events
                return;
            }

            DeviceId deviceId = event.subject();
            Device device = deviceService.getDevice(deviceId);
            if (device == null) {
                log.debug("Device {} doesn't exist, or isn't there yet", deviceId);
                return;
            }
            if (clusterService.getLocalNode().id().equals(event.roleInfo().master())) {
                updateDevice(device).ifPresent(ld -> updatePorts(ld, device.id()));
            }
        }

    }

    /**
     * Processes device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
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
                    }
                    break;
                case PORT_REMOVED:
                    log.debug("Port removed {}", port);
                    removePort(port);
                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    log.debug("Device removed {}", deviceId);
                    removeDevice(deviceId);
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(deviceId)) {
                        log.debug("Device up {}", deviceId);
                        updateDevice(device);
                    } else {
                        log.debug("Device down {}", deviceId);
                        removeDevice(deviceId);
                    }
                    break;
                case PORT_STATS_UPDATED:
                    break;
                default:
                    log.debug("Unknown event {}", event);
            }
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

            LinkDiscovery ld = discoverers.get(context.inPacket().receivedFrom().deviceId());
            if (ld == null) {
                return;
            }

            if (ld.handleLLDP(context)) {
                context.block();
            }
        }
    }

    /**
     * Auxiliary task to keep device ports up to date.
     */
    private final class SyncDeviceInfoTask implements Runnable {
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }
            // check what deviceService sees, to see if we are missing anything
            try {
                loadDevices();
            } catch (Exception e) {
                // Catch all exceptions to avoid task being suppressed
                log.error("Exception thrown during synchronization process", e);
            }
        }
    }

    /**
     * Auxiliary task for pruning stale links.
     */
    private class LinkPrunerTask implements Runnable {
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }

            try {
                // TODO: There is still a slight possibility of mastership
                // change occurring right with link going stale. This will
                // result in the stale link not being pruned.
                Maps.filterEntries(linkTimes, e -> {
                    if (!masterService.isLocalMaster(e.getKey().dst().deviceId())) {
                        return true;
                    }
                    if (isStale(e.getValue())) {
                        providerService.linkVanished(new DefaultLinkDescription(e.getKey().src(),
                                                                                e.getKey().dst(),
                                                                                DIRECT));
                        return true;
                    }
                    return false;
                }).clear();

            } catch (Exception e) {
                // Catch all exceptions to avoid task being suppressed
                log.error("Exception thrown during link pruning process", e);
            }
        }

        private boolean isStale(long lastSeen) {
            return lastSeen < System.currentTimeMillis() - staleLinkAge;
        }
    }

    /**
     * Provides processing context for the device link discovery helpers.
     */
    private class InternalDiscoveryContext implements DiscoveryContext {
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
        public boolean useBDDP() {
            return useBDDP;
        }

        @Override
        public void touchLink(LinkKey key) {
            linkTimes.put(key, System.currentTimeMillis());
        }
    }

}
