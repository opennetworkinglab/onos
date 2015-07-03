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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
public class LLDPLinkProvider extends AbstractProvider implements LinkProvider {

    private static final String PROVIDER_NAME = "org.onosproject.provider.lldp";

    private static final String PROP_USE_BDDP = "useBDDP";
    private static final String PROP_DISABLE_LD = "disableLinkDiscovery";
    private static final String PROP_LLDP_SUPPRESSION = "lldpSuppression";

    private static final String DEFAULT_LLDP_SUPPRESSION_CONFIG = "../config/lldp_suppression.json";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService masterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private LinkProviderService providerService;

    private ScheduledExecutorService executor;

    @Property(name = PROP_USE_BDDP, boolValue = true,
            label = "Use BDDP for link discovery")
    private boolean useBDDP = true;

    @Property(name = PROP_DISABLE_LD, boolValue = false,
            label = "Permanently disable link discovery")
    private boolean disableLinkDiscovery = false;

    private static final long INIT_DELAY = 5;
    private static final long DELAY = 5;

    @Property(name = PROP_LLDP_SUPPRESSION, value = DEFAULT_LLDP_SUPPRESSION_CONFIG,
            label = "Path to LLDP suppression configuration file")
    private String lldpSuppression = DEFAULT_LLDP_SUPPRESSION_CONFIG;


    private final InternalLinkProvider listener = new InternalLinkProvider();

    private final InternalRoleListener roleListener = new InternalRoleListener();

    protected final Map<DeviceId, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

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

        // to load configuration at startup
        modified(context);
        if (disableLinkDiscovery) {
            log.info("LinkDiscovery has been permanently disabled by configuration");
            return;
        }

        providerService = providerRegistry.register(this);
        deviceService.addListener(listener);
        packetService.addProcessor(listener, 0);
        masterService.addListener(roleListener);

        LinkDiscovery ld;
        for (Device device : deviceService.getAvailableDevices()) {
            if (rules.isSuppressed(device)) {
                log.debug("LinkDiscovery from {} disabled by configuration", device.id());
                continue;
            }
            ld = new LinkDiscovery(device, packetService, masterService,
                                   providerService, useBDDP);
            discoverers.put(device.id(), ld);
            for (Port p : deviceService.getPorts(device.id())) {
                if (rules.isSuppressed(p)) {
                    log.debug("LinkDiscovery from {}@{} disabled by configuration",
                              p.number(), device.id());
                    continue;
                }
                if (!p.number().isLogical()) {
                    ld.addPort(p);
                }
            }
        }

        executor = newSingleThreadScheduledExecutor(groupedThreads("onos/device", "sync-%d"));
        executor.scheduleAtFixedRate(new SyncDeviceInfoTask(), INIT_DELAY, DELAY, SECONDS);

        requestIntercepts();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        if (disableLinkDiscovery) {
            return;
        }

        withdrawIntercepts();

        providerRegistry.unregister(this);
        deviceService.removeListener(listener);
        packetService.removeProcessor(listener);
        masterService.removeListener(roleListener);

        executor.shutdownNow();
        discoverers.values().forEach(LinkDiscovery::stop);
        discoverers.clear();
        providerService = null;

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            loadSuppressionRules();
            return;
        }
        @SuppressWarnings("rawtypes")
        Dictionary properties = context.getProperties();

        String s = get(properties, PROP_DISABLE_LD);
        if (!Strings.isNullOrEmpty(s)) {
            disableLinkDiscovery = Boolean.valueOf(s);
        }
        s = get(properties, PROP_USE_BDDP);
        if (!Strings.isNullOrEmpty(s)) {
            useBDDP = Boolean.valueOf(s);
        }
        s = get(properties, PROP_LLDP_SUPPRESSION);
        if (!Strings.isNullOrEmpty(s)) {
            lldpSuppression = s;
        }
        requestIntercepts();
        loadSuppressionRules();
    }

    private void loadSuppressionRules() {
        SuppressionRulesStore store = new SuppressionRulesStore(lldpSuppression);
        try {
            log.info("Reading suppression rules from {}", lldpSuppression);
            rules = store.read();
        } catch (IOException e) {
            log.info("Failed to load {}, using built-in rules", lldpSuppression);
            // default rule to suppress ROADM to maintain compatibility
            rules = new SuppressionRules(ImmutableSet.of(),
                                         EnumSet.of(Device.Type.ROADM),
                                         ImmutableMap.of());
        }

        // should refresh discoverers when we need dynamic reconfiguration
    }

    /**
     * Request packet intercepts.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_LLDP);
        packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);

        selector.matchEthType(Ethernet.TYPE_BSN);
        if (useBDDP) {
            packetService.requestPackets(selector.build(), PacketPriority.CONTROL, appId);
        } else {
            packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
        }
    }

    /**
     * Withdraw packet intercepts.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_LLDP);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
        selector.matchEthType(Ethernet.TYPE_BSN);
        packetService.cancelPackets(selector.build(), PacketPriority.CONTROL, appId);
    }


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
            if (rules.isSuppressed(device)) {
                return;
            }
            synchronized (discoverers) {
                if (!discoverers.containsKey(deviceId)) {
                    // ideally, should never reach here
                    log.debug("Device mastership changed ({}) {}",
                              event.type(), deviceId);
                    discoverers.put(deviceId, new LinkDiscovery(device,
                                                                packetService, masterService, providerService,
                                                                useBDDP));
                }
            }
        }

    }

    private class InternalLinkProvider implements PacketProcessor, DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            LinkDiscovery ld = null;
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
                    synchronized (discoverers) {
                        ld = discoverers.get(deviceId);
                        if (ld == null) {
                            if (rules.isSuppressed(device)) {
                                log.debug("LinkDiscovery from {} disabled by configuration", device.id());
                                return;
                            }
                            log.debug("Device added ({}) {}", event.type(),
                                      deviceId);
                            discoverers.put(deviceId, new LinkDiscovery(device,
                                                                        packetService, masterService,
                                                                        providerService, useBDDP));
                        } else {
                            if (ld.isStopped()) {
                                log.debug("Device restarted ({}) {}", event.type(),
                                          deviceId);
                                ld.start();
                            }
                        }
                    }
                    break;
                case PORT_ADDED:
                case PORT_UPDATED:
                    if (port.isEnabled()) {
                        ld = discoverers.get(deviceId);
                        if (ld == null) {
                            return;
                        }
                        if (rules.isSuppressed(port)) {
                            log.debug("LinkDiscovery from {}@{} disabled by configuration",
                                      port.number(), device.id());
                            return;
                        }
                        if (!port.number().isLogical()) {
                            log.debug("Port added {}", port);
                            ld.addPort(port);
                        }
                    } else {
                        log.debug("Port down {}", port);
                        ConnectPoint point = new ConnectPoint(deviceId,
                                                              port.number());
                        providerService.linksVanished(point);
                    }
                    break;
                case PORT_REMOVED:
                    log.debug("Port removed {}", port);
                    ConnectPoint point = new ConnectPoint(deviceId,
                                                          port.number());
                    providerService.linksVanished(point);

                    break;
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                    log.debug("Device removed {}", deviceId);
                    ld = discoverers.get(deviceId);
                    if (ld == null) {
                        return;
                    }
                    ld.stop();
                    providerService.linksVanished(deviceId);
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    ld = discoverers.get(deviceId);
                    if (ld == null) {
                        return;
                    }
                    if (deviceService.isAvailable(deviceId)) {
                        log.debug("Device up {}", deviceId);
                        ld.start();
                    } else {
                        providerService.linksVanished(deviceId);
                        log.debug("Device down {}", deviceId);
                        ld.stop();
                    }
                    break;
                case PORT_STATS_UPDATED:
                    break;
                default:
                    log.debug("Unknown event {}", event);
            }
        }

        @Override
        public void process(PacketContext context) {
            if (context == null) {
                return;
            }
            LinkDiscovery ld = discoverers.get(
                    context.inPacket().receivedFrom().deviceId());
            if (ld == null) {
                return;
            }

            if (ld.handleLLDP(context)) {
                context.block();
            }
        }
    }

    private final class SyncDeviceInfoTask implements Runnable {

        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                log.info("Interrupted, quitting");
                return;
            }
            // check what deviceService sees, to see if we are missing anything
            try {
                LinkDiscovery ld = null;
                for (Device dev : deviceService.getDevices()) {
                    if (rules.isSuppressed(dev)) {
                        continue;
                    }
                    DeviceId did = dev.id();
                    synchronized (discoverers) {
                        if (!discoverers.containsKey(did)) {
                            ld = new LinkDiscovery(dev, packetService,
                                                   masterService, providerService, useBDDP);
                            discoverers.put(did, ld);
                            for (Port p : deviceService.getPorts(did)) {
                                if (rules.isSuppressed(p)) {
                                    continue;
                                }
                                if (!p.number().isLogical()) {
                                    ld.addPort(p);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // catch all Exception to avoid Scheduled task being suppressed.
                log.error("Exception thrown during synchronization process", e);
            }
        }
    }

}
