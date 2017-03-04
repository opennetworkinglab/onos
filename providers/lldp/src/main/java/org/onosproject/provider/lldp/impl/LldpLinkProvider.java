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
package org.onosproject.provider.lldp.impl;

import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterMetadataService;
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
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceEvent.Type;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.link.ProbedLinkProvider;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.lldpcommon.LinkDiscovery;
import org.onosproject.provider.lldpcommon.LinkDiscoveryContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.onlab.packet.Ethernet.TYPE_BSN;
import static org.onlab.packet.Ethernet.TYPE_LLDP;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.onosproject.net.config.basics.SubjectFactories.DEVICE_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses LLDP and BDDP packets to detect network infrastructure links.
 */
@Component(immediate = true)
public class LldpLinkProvider extends AbstractProvider implements ProbedLinkProvider {

    private static final String PROVIDER_NAME = "org.onosproject.provider.lldp";

    private static final String FORMAT =
            "Settings: enabled={}, useBDDP={}, probeRate={}, " +
                    "staleLinkAge={}";

    // When a Device/Port has this annotation, do not send out LLDP/BDDP
    public static final String NO_LLDP = "no-lldp";

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 1_000; // millis

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService clusterMetadataService;

    private LinkProviderService providerService;

    private ScheduledExecutorService executor;
    protected ExecutorService eventExecutor;

    private boolean shuttingDown = false;

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
    private boolean useBddp = true;

    private static final String PROP_PROBE_RATE = "probeRate";
    private static final int DEFAULT_PROBE_RATE = 3000;
    @Property(name = PROP_PROBE_RATE, intValue = DEFAULT_PROBE_RATE,
            label = "LLDP and BDDP probe rate specified in millis")
    private int probeRate = DEFAULT_PROBE_RATE;

    private static final String PROP_STALE_LINK_AGE = "staleLinkAge";
    private static final int DEFAULT_STALE_LINK_AGE = 10000;
    @Property(name = PROP_STALE_LINK_AGE, intValue = DEFAULT_STALE_LINK_AGE,
            label = "Number of millis beyond which links will be considered stale")
    private int staleLinkAge = DEFAULT_STALE_LINK_AGE;

    private final LinkDiscoveryContext context = new InternalDiscoveryContext();
    private final InternalRoleListener roleListener = new InternalRoleListener();
    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    // Device link discovery helpers.
    protected final Map<DeviceId, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    // Most recent time a tracked link was seen; links are tracked if their
    // destination connection point is mastered by this controller instance.
    private final Map<LinkKey, Long> linkTimes = Maps.newConcurrentMap();

    private ApplicationId appId;

    static final SuppressionRules DEFAULT_RULES
        = new SuppressionRules(EnumSet.of(Device.Type.ROADM,
                                          Device.Type.FIBER_SWITCH,
                                          Device.Type.OPTICAL_AMPLIFIER,
                                          Device.Type.OTN),
                               ImmutableMap.of(NO_LLDP, SuppressionRules.ANY_VALUE));

    private SuppressionRules rules = LldpLinkProvider.DEFAULT_RULES;

    public static final String CONFIG_KEY = "suppression";
    public static final String FEATURE_NAME = "linkDiscovery";

    private final Set<ConfigFactory<?, ?>> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, SuppressionConfig>(APP_SUBJECT_FACTORY,
                    SuppressionConfig.class,
                    CONFIG_KEY) {
                @Override
                public SuppressionConfig createConfig() {
                    return new SuppressionConfig();
                }
            },
            new ConfigFactory<DeviceId, LinkDiscoveryFromDevice>(DEVICE_SUBJECT_FACTORY,
                    LinkDiscoveryFromDevice.class, FEATURE_NAME) {
                @Override
                public LinkDiscoveryFromDevice createConfig() {
                    return new LinkDiscoveryFromDevice();
                }
            },
            new ConfigFactory<ConnectPoint, LinkDiscoveryFromPort>(CONNECT_POINT_SUBJECT_FACTORY,
                    LinkDiscoveryFromPort.class, FEATURE_NAME) {
                @Override
                public LinkDiscoveryFromPort createConfig() {
                    return new LinkDiscoveryFromPort();
                }
            }
    );

    private final InternalConfigListener cfgListener = new InternalConfigListener();

    /**
     * Creates an OpenFlow link provider.
     */
    public LldpLinkProvider() {
        super(new ProviderId("lldp", PROVIDER_NAME));
    }

    private String buildSrcMac() {
        String srcMac = ProbedLinkProvider.fingerprintMac(clusterMetadataService.getClusterMetadata());
        String defMac = ProbedLinkProvider.defaultMac();
        if (srcMac.equals(defMac)) {
            log.warn("Couldn't generate fingerprint. Using default value {}", defMac);
            return defMac;
        }
        log.trace("Generated MAC address {}", srcMac);
        return srcMac;
    }

    @Activate
    public void activate(ComponentContext context) {
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/linkevents", "events-%d", log));
        shuttingDown = false;
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication(PROVIDER_NAME);

        cfgRegistry.addListener(cfgListener);
        factories.forEach(cfgRegistry::registerConfigFactory);

        SuppressionConfig cfg = cfgRegistry.getConfig(appId, SuppressionConfig.class);
        if (cfg == null) {
            // If no configuration is found, register default.
            cfg = this.setDefaultSuppressionConfig();
        }
        cfgListener.reconfigureSuppressionRules(cfg);

        modified(context);
        log.info("Started");
    }

    private SuppressionConfig setDefaultSuppressionConfig() {
        SuppressionConfig cfg = cfgRegistry.addConfig(appId, SuppressionConfig.class);
        cfg.deviceTypes(DEFAULT_RULES.getSuppressedDeviceType())
           .annotation(DEFAULT_RULES.getSuppressedAnnotation())
           .apply();
        return cfg;
    }

    @Deactivate
    public void deactivate() {
        shuttingDown = true;
        cfgRegistry.removeListener(cfgListener);
        factories.forEach(cfgRegistry::unregisterConfigFactory);

        cfgService.unregisterProperties(getClass(), false);
        disable();
        eventExecutor.shutdownNow();
        eventExecutor = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        boolean newEnabled, newUseBddp;
        int newProbeRate, newStaleLinkAge;
        try {
            String s = get(properties, PROP_ENABLED);
            newEnabled = isNullOrEmpty(s) || Boolean.parseBoolean(s.trim());

            s = get(properties, PROP_USE_BDDP);
            newUseBddp = isNullOrEmpty(s) || Boolean.parseBoolean(s.trim());

            s = get(properties, PROP_PROBE_RATE);
            newProbeRate = isNullOrEmpty(s) ? probeRate : Integer.parseInt(s.trim());

            s = get(properties, PROP_STALE_LINK_AGE);
            newStaleLinkAge = isNullOrEmpty(s) ? staleLinkAge : Integer.parseInt(s.trim());

        } catch (NumberFormatException e) {
            log.warn("Component configuration had invalid values", e);
            newEnabled = enabled;
            newUseBddp = useBddp;
            newProbeRate = probeRate;
            newStaleLinkAge = staleLinkAge;
        }

        boolean wasEnabled = enabled;

        enabled = newEnabled;
        useBddp = newUseBddp;
        probeRate = newProbeRate;
        staleLinkAge = newStaleLinkAge;

        if (!wasEnabled && enabled) {
            enable();
        } else if (wasEnabled && !enabled) {
            disable();
        } else {
            if (enabled) {
                // update all discovery helper state
                loadDevices();
            }
        }

        log.info(FORMAT, enabled, useBddp, probeRate, staleLinkAge);
    }

    /**
     * Enables link discovery processing.
     */
    private void enable() {
        providerService = providerRegistry.register(this);
        masterService.addListener(roleListener);
        deviceService.addListener(deviceListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.advisor(0));

        loadDevices();

        executor = newSingleThreadScheduledExecutor(groupedThreads("onos/link", "discovery-%d", log));
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
        if (!enabled || deviceService == null) {
            return;
        }
        deviceService.getAvailableDevices()
                .forEach(d -> updateDevice(d)
                               .ifPresent(ld -> updatePorts(ld, d.id())));
    }

    private boolean isBlacklisted(DeviceId did) {
        LinkDiscoveryFromDevice cfg = cfgRegistry.getConfig(did, LinkDiscoveryFromDevice.class);
        if (cfg == null) {
            return false;
        }
        return !cfg.enabled();
    }

    private boolean isBlacklisted(ConnectPoint cp) {
        // if parent device is blacklisted, so is the port
        if (isBlacklisted(cp.deviceId())) {
            return true;
        }
        LinkDiscoveryFromPort cfg = cfgRegistry.getConfig(cp, LinkDiscoveryFromPort.class);
        if (cfg == null) {
            return false;
        }
        return !cfg.enabled();
    }

    private boolean isBlacklisted(Port port) {
        return isBlacklisted(new ConnectPoint(port.element().id(), port.number()));
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
        if (device == null) {
            return Optional.empty();
        }
        if (rules.isSuppressed(device) || isBlacklisted(device.id())) {
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
        if (port == null) {
            return;
        }
        if (port.number().isLogical()) {
            // silently ignore logical ports
            return;
        }

        if (rules.isSuppressed(port) || isBlacklisted(port)) {
            log.trace("LinkDiscovery from {} disabled by configuration", port);
            removePort(port);
            return;
        }

        // check if enabled and turn off discovery?
        if (!port.isEnabled()) {
            removePort(port);
            return;
        }

        discoverer.addPort(port);
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
     * Requests packet intercepts.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_LLDP);
        packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);

        selector.matchEthType(TYPE_BSN);
        if (useBddp) {
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

    protected SuppressionRules rules() {
        return rules;
    }

    protected void updateRules(SuppressionRules newRules) {
        if (!rules.equals(newRules)) {
            rules = newRules;
            loadDevices();
        }
    }

    /**
     * Processes device mastership role changes.
     */
    private class InternalRoleListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            if (MastershipEvent.Type.MASTER_CHANGED.equals(event.type())) {
                // only need new master events
                eventExecutor.execute(() -> {
                    DeviceId deviceId = event.subject();
                    Device device = deviceService.getDevice(deviceId);
                    if (device == null) {
                        log.debug("Device {} doesn't exist, or isn't there yet", deviceId);
                        return;
                    }
                    if (clusterService.getLocalNode().id().equals(event.roleInfo().master())) {
                        updateDevice(device).ifPresent(ld -> updatePorts(ld, device.id()));
                    }
                });
            }
        }
    }

    private class DeviceEventProcessor implements Runnable {

        DeviceEvent event;

        DeviceEventProcessor(DeviceEvent event) {
            this.event = event;
        }

        @Override
        public void run() {
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

    /**
     * Processes device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (event.type() == Type.PORT_STATS_UPDATED) {
                return;
            }

            Runnable deviceEventProcessor = new DeviceEventProcessor(event);

            eventExecutor.execute(deviceEventProcessor);
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

            if (ld.handleLldp(context)) {
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
                if (!shuttingDown) {
                    // Error condition
                    log.error("Exception thrown during link pruning process", e);
                } else {
                    // Provider is shutting down, the error can be ignored
                    log.trace("Shutting down, ignoring error", e);
                }
            }
        }

        private boolean isStale(long lastSeen) {
            return lastSeen < System.currentTimeMillis() - staleLinkAge;
        }
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
            return useBddp;
        }

        @Override
        public void touchLink(LinkKey key) {
            linkTimes.put(key, System.currentTimeMillis());
        }

        @Override
        public DeviceService deviceService() {
            return deviceService;
        }

        @Override
        public String fingerprint() {
            return buildSrcMac();
        }
    }

    static final EnumSet<NetworkConfigEvent.Type> CONFIG_CHANGED
                    = EnumSet.of(NetworkConfigEvent.Type.CONFIG_ADDED,
                                 NetworkConfigEvent.Type.CONFIG_UPDATED,
                                 NetworkConfigEvent.Type.CONFIG_REMOVED);

    private class InternalConfigListener implements NetworkConfigListener {

        private synchronized void reconfigureSuppressionRules(SuppressionConfig cfg) {
            if (cfg == null) {
                log.debug("Suppression Config is null.");
                return;
            }

            SuppressionRules newRules = new SuppressionRules(cfg.deviceTypes(),
                                                             cfg.annotation());

            updateRules(newRules);
        }

        @Override
        public void event(NetworkConfigEvent event) {
            eventExecutor.execute(() -> {
                if (event.configClass() == LinkDiscoveryFromDevice.class &&
                        CONFIG_CHANGED.contains(event.type())) {

                    if (event.subject() instanceof DeviceId) {
                        final DeviceId did = (DeviceId) event.subject();
                        Device device = deviceService.getDevice(did);
                        updateDevice(device).ifPresent(ld -> updatePorts(ld, did));
                    }

                } else if (event.configClass() == LinkDiscoveryFromPort.class &&
                        CONFIG_CHANGED.contains(event.type())) {

                    if (event.subject() instanceof ConnectPoint) {
                        ConnectPoint cp = (ConnectPoint) event.subject();
                        if (cp.elementId() instanceof DeviceId) {
                            final DeviceId did = (DeviceId) cp.elementId();
                            Device device = deviceService.getDevice(did);
                            Port port = deviceService.getPort(did, cp.port());
                            updateDevice(device).ifPresent(ld -> updatePort(ld, port));
                        }
                    }

                } else if (event.configClass().equals(SuppressionConfig.class) &&
                        (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                                event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)) {
                    SuppressionConfig cfg = cfgRegistry.getConfig(appId, SuppressionConfig.class);
                    reconfigureSuppressionRules(cfg);
                    log.trace("Network config reconfigured");
                }
            });
        }
    }
}
