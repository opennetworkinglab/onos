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

package org.onosproject.provider.linkdiscovery.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Link provider capable of polling the environment using the device driver
 * {@link LinkDiscovery} behaviour.
 */
@Component(immediate = true)
public class LinkDiscoveryProvider extends AbstractProvider
        implements LinkProvider {

    protected static final String APP_NAME = "org.onosproject.linkdiscovery";
    protected static final String SCHEME_NAME = "linkdiscovery";
    private static final String LINK_PROVIDER_PACKAGE = "org.onosproject.provider.linkdiscovery";
    private final Logger log = getLogger(getClass());
    private static final int DEFAULT_POLL_DELAY_SECONDS = 20;
    @Property(name = "linkPollDelaySeconds", intValue = DEFAULT_POLL_DELAY_SECONDS,
            label = "Initial delay (in seconds) for polling link discovery")
    protected static int linkPollDelaySeconds = DEFAULT_POLL_DELAY_SECONDS;
    private static final int DEFAULT_POLL_FREQUENCY_SECONDS = 10;
    @Property(name = "linkPollFrequencySeconds", intValue = DEFAULT_POLL_FREQUENCY_SECONDS,
            label = "Frequency (in seconds) for polling link discovery")
    protected static int linkPollFrequencySeconds = DEFAULT_POLL_FREQUENCY_SECONDS;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    protected ScheduledExecutorService executor =
            newScheduledThreadPool(2, groupedThreads("onos/netconf-link",
                                                     "discovery-%d"));
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    protected LinkProviderService providerService;
    private InternalDeviceListener deviceListener = new InternalDeviceListener();
    private ApplicationId appId;
    private ScheduledFuture<?> scheduledTask;

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
            linkPollFrequencySeconds = DEFAULT_POLL_FREQUENCY_SECONDS;
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
            if (newPollFrequency != linkPollFrequencySeconds ||
                    newPollDelay != linkPollDelaySeconds) {
                linkPollFrequencySeconds = newPollFrequency;
                linkPollDelaySeconds = newPollDelay;
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
            String s = get(properties, "linkPollFrequencySeconds");
            newPollFrequency = isNullOrEmpty(s) ? pollFrequency : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = DEFAULT_POLL_FREQUENCY_SECONDS;
        }
        return newPollFrequency;
    }

    private int getNewPollDealy(Dictionary<?, ?> properties, int pollDelay) {
        int newPollFrequency;
        try {
            String s = get(properties, "linkPollDelaySeconds");
            newPollFrequency = isNullOrEmpty(s) ? pollDelay : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPollFrequency = DEFAULT_POLL_DELAY_SECONDS;
        }
        return newPollFrequency;
    }

    private ScheduledFuture schedulePolling() {
        return executor.scheduleAtFixedRate(this::discoverLinksTasks,
                                            linkPollDelaySeconds,
                                            linkPollFrequencySeconds,
                                            SECONDS);
    }

    private void discoverLinksTasks() {
        deviceService.getAvailableDevices().forEach(device -> {
            if (isSupported(device)) {
                evaluateLinks(device.id(), device.as(LinkDiscovery.class).getLinks());
            }
        });
    }

    private void evaluateLinks(DeviceId deviceId, Set<LinkDescription> discoveredLinksDesc) {
        //The provider will get only existing links related to LinkDiscovery
        Set<Link> storedLinks = linkService.getDeviceEgressLinks(deviceId)
                .stream()
                .filter(link -> {
                    String value = link.annotations().value(AnnotationKeys.PROTOCOL);
                    if (value != null && value.equals(SCHEME_NAME.toUpperCase())) {
                        return true;
                    }
                    return false;
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
            if ((event.type() == DeviceEvent.Type.DEVICE_ADDED)) {
                executor.execute(() -> event.subject().as(LinkDiscovery.class).getLinks()
                        .forEach(linkDesc -> {
                            providerService.linkDetected(new DefaultLinkDescription(
                                    linkDesc.src(), linkDesc.dst(), linkDesc.type(), false,
                                    DefaultAnnotations.builder()
                                            .putAll(linkDesc.annotations())
                                            .set(AnnotationKeys.PROTOCOL, SCHEME_NAME.toUpperCase())
                                            .build()));
                        }));
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return isSupported(event.subject());
        }
    }
}
