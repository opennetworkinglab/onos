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

package org.onosproject.provider.linkdiscovery.impl;

import com.google.common.collect.Sets;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.provider.linkdiscovery.impl.OsgiPropertyConstants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Link provider capable of polling the environment using the device driver
 * {@link LinkDiscovery} behaviour.
 */
@Component(immediate = true,
        property = {
                POLL_DELAY_SECONDS + ":Integer=" + POLL_DELAY_SECONDS_DEFAULT,
                POLL_FREQUENCY_SECONDS + ":Integer=" + POLL_FREQUENCY_SECONDS_DEFAULT,
                LINK_DISCOVERY_TIMEOUT_SECONDS + ":Integer=" + POLL_DISCOVERY_TIMEOUT_DEFAULT,
        })
public class LinkDiscoveryProvider extends AbstractProvider
        implements LinkProvider {

    protected static final String APP_NAME = "org.onosproject.linkdiscovery";
    protected static final String SCHEME_NAME = "linkdiscovery";
    private static final String LINK_PROVIDER_PACKAGE = "org.onosproject.provider.linkdiscovery";
    private final Logger log = getLogger(getClass());

    /** Initial delay (in seconds) for polling link discovery. */
    protected static int linkPollDelaySeconds = POLL_DELAY_SECONDS_DEFAULT;

    /** Frequency (in seconds) for polling link discovery. */
    protected static int linkPollFrequencySeconds = POLL_FREQUENCY_SECONDS_DEFAULT;

    /** Discovery timeout (in seconds) for polling arp discovery. */
    protected static int linkDiscoveryTimeoutSeconds = POLL_DISCOVERY_TIMEOUT_DEFAULT;

    private static final int POOL_SIZE = 10;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkProviderRegistry providerRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;
    protected ExecutorService linkDiscoveryExecutor =
            Executors.newFixedThreadPool(POOL_SIZE, groupedThreads("onos/linkdiscoveryprovider",
                                                                   "link-collector-%d", log));
    protected ScheduledExecutorService executor =
            newScheduledThreadPool(2, groupedThreads("onos/netconf-link",
                                                     "discovery-%d"));
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    protected LinkProviderService providerService;
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private ApplicationId appId;
    private ScheduledFuture<?> scheduledTask;
    private ForkJoinPool scheduledTaskPool = new ForkJoinPool(POOL_SIZE);

    /**
     * Creates a provider with the supplied identifier.
     */
    public LinkDiscoveryProvider() {
        super(new ProviderId(SCHEME_NAME, LINK_PROVIDER_PACKAGE));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        appId = coreService.registerApplication(APP_NAME);
        deviceService.addListener(deviceListener);
        cfgService.registerProperties(getClass());

        if (context == null) {
            linkPollFrequencySeconds = POLL_FREQUENCY_SECONDS_DEFAULT;
            log.info("No component configuration");
        } else {
            Dictionary<?, ?> properties = context.getProperties();
            linkPollFrequencySeconds =
                    getNewPollFrequency(properties, linkPollFrequencySeconds);
        }
        scheduledTask = schedulePolling();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        deviceService.removeListener(deviceListener);
        providerRegistry.unregister(this);
        providerService = null;
        scheduledTask.cancel(true);
        executor.shutdown();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("No component configuration");
            return;
        } else {
            Dictionary<?, ?> properties = context.getProperties();

            int newPollFrequency = getNewPollFrequency(properties, linkPollFrequencySeconds);
            int newPollDelay = getNewPollDealy(properties, linkPollDelaySeconds);
            int newDiscoveryTimeout = getNewDiscoveryTimeout(properties, linkDiscoveryTimeoutSeconds);
            if (newPollFrequency != linkPollFrequencySeconds ||
                    newPollDelay != linkPollDelaySeconds ||
                    newDiscoveryTimeout != linkDiscoveryTimeoutSeconds) {
                linkPollFrequencySeconds = newPollFrequency;
                linkPollDelaySeconds = newPollDelay;
                linkDiscoveryTimeoutSeconds = newDiscoveryTimeout;
                //stops the old scheduled task
                scheduledTask.cancel(true);
                //schedules new task at the new polling rate
                scheduledTask = schedulePolling();
            }
        }
        log.info("Modified");
    }

    private int getNewPollFrequency(Dictionary<?, ?> properties, int pollFrequency) {
        int newPollFrequency;
        try {
            String s = get(properties, POLL_FREQUENCY_SECONDS);
            newPollFrequency = isNullOrEmpty(s) ? pollFrequency : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = POLL_FREQUENCY_SECONDS_DEFAULT;
        }
        return newPollFrequency;
    }

    private int getNewPollDealy(Dictionary<?, ?> properties, int pollDelay) {
        int newPollFrequency;
        try {
            String s = get(properties, POLL_DELAY_SECONDS);
            newPollFrequency = isNullOrEmpty(s) ? pollDelay : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = POLL_DELAY_SECONDS_DEFAULT;
        }
        return newPollFrequency;
    }

    private int getNewDiscoveryTimeout(Dictionary<?, ?> properties, int discoveryTimeout) {
        int newDiscoveryTimeout;
        try {
            String s = get(properties, LINK_DISCOVERY_TIMEOUT_SECONDS);
            newDiscoveryTimeout = isNullOrEmpty(s) ? discoveryTimeout : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newDiscoveryTimeout = POLL_DISCOVERY_TIMEOUT_DEFAULT;
            log.error("Cannot update Discovery Timeout", e);
        }
        return newDiscoveryTimeout;
    }

    private ScheduledFuture schedulePolling() {
        log.info("schedule: discoverLinksTasks with {} sec, {} sec",
                linkPollDelaySeconds, linkPollFrequencySeconds);
        return executor.scheduleAtFixedRate(this::discoverLinksTasks,
                                            linkPollDelaySeconds,
                                            linkPollFrequencySeconds,
                                            SECONDS);
    }

    private void discoverLinks(Device device) {
        DeviceId deviceId = device.id();
        Set<LinkDescription> response = null;
        try {
            response = CompletableFuture.supplyAsync(() -> device.as(LinkDiscovery.class).getLinks(),
                    linkDiscoveryExecutor)
                    .exceptionally(e -> {
                        log.error("Exception is occurred during update the links. Device id {} {}", deviceId, e);
                        return null;
                    })
                    .get(linkDiscoveryTimeoutSeconds, SECONDS);
        } catch (TimeoutException e) {
            log.error("Timout is occurred during update the links. Device id {}, Timeout {}",
                    deviceId, linkDiscoveryTimeoutSeconds);
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception is occurred during update the links. Device id {}, Timeout {}",
                    deviceId, linkDiscoveryTimeoutSeconds);
        }
        if (Objects.isNull(response)) {
            return;
        }
        evaluateLinks(deviceId, response);
    }

    private void discoverLinksTasks() {
        try {
            scheduledTaskPool.submit(exceptionSafe(() -> {
                Tools.stream(deviceService.getAvailableDevices()).parallel().forEach(device -> exceptionSafe(() -> {
                    if (isSupported(device)) {
                        discoverLinks(device);
                    }
                }).run());
            })).get();
        } catch (Exception e) {
            log.info("Unhandled exception {}", e.getMessage());
        }
    }

    private void evaluateLinks(DeviceId deviceId, Set<LinkDescription> discoveredLinksDesc) {
        if (discoveredLinksDesc == null) {
            return;
        }

        //The provider will get only existing links related to LinkDiscovery
        Set<Link> storedLinks = linkService.getDeviceIngressLinks(deviceId)
                .stream()
                .filter(link -> {
                    String value = link.annotations().value(AnnotationKeys.PROTOCOL);
                    return Objects.equals(value, SCHEME_NAME.toUpperCase());
                })
                .collect(Collectors.toSet());

        //Convert Link to LinkDescription for comparison
        Set<LinkDescription> storedLinkDescs = new HashSet<>();
        storedLinks.forEach(link -> storedLinkDescs
                .add(new DefaultLinkDescription(
                        link.src(), link.dst(), link.type(), link.isExpected(),
                        DefaultAnnotations.builder().putAll(link.annotations()).build())));
        log.debug("Current stored links provider related {}", storedLinks);

        //Add the correct annotation for comparison
        Set<LinkDescription> discoveredLinkDescsAnn = new HashSet<>();

        discoveredLinksDesc.forEach(linkDesc -> discoveredLinkDescsAnn
                .add(new DefaultLinkDescription(
                        linkDesc.src(), linkDesc.dst(), linkDesc.type(), false,
                        DefaultAnnotations.builder().putAll(linkDesc.annotations())
                                .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase())
                                .build())));

        Set<LinkDescription> linkDescsToBeRemoved = new HashSet<>(storedLinkDescs);
        linkDescsToBeRemoved.removeAll(discoveredLinkDescsAnn);
        log.debug("Links to be removed {}", linkDescsToBeRemoved);
        linkDescsToBeRemoved.forEach(linkDesc ->
                                             providerService.linkVanished(linkDesc));

        Set<LinkDescription> linksToBeAdded = new HashSet<>(discoveredLinkDescsAnn);
        linksToBeAdded.removeAll(storedLinkDescs);
        log.debug("Links to be added {}", linksToBeAdded);
        linksToBeAdded.forEach(linkDesc -> providerService.linkDetected(linkDesc)
        );
    }

    protected boolean isSupported(Device device) {
        boolean supported = mastershipService.isLocalMaster(device.id())
                && device.is(LinkDiscovery.class);
        if (!supported) {
            log.debug("Device {} does not support LinkDiscovery", device);
        }
        return supported;
    }

    /**
     * Listener for core device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            switch (event.type()) {
                case DEVICE_ADDED:
                    executor.execute(() -> discoverLinks(device));
                    break;
                case DEVICE_REMOVED:
                    evaluateLinks(device.id(), Sets.newHashSet());
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    if (!deviceService.isAvailable(device.id())) {
                        evaluateLinks(device.id(), Sets.newHashSet());
                    }
                    break;
                default:
                    log.debug("No implemented action for other DeviceEvents for the device {}", device.id());
                    break;
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return isSupported(event.subject());
        }
    }

    private Runnable exceptionSafe(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error("Unhandled Exception", e);
            }
        };
    }
}
