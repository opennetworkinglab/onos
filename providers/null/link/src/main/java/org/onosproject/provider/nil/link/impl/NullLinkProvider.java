/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.nil.link.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provider which advertises fake/nonexistent links to the core. To be used for
 * benchmarking only.
 */
@Component(immediate = true)
public class NullLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    private LinkProviderService providerService;

    private static final boolean FLICKER = false;
    private static final int DEFAULT_RATE = 3000;
    // For now, static switch port values
    private static final PortNumber SRCPORT = PortNumber.portNumber(5);
    private static final PortNumber DSTPORT = PortNumber.portNumber(6);

    private final InternalLinkProvider linkProvider = new InternalLinkProvider();

    // Link descriptions
    private final ConcurrentMap<ConnectPoint, LinkDescription> descriptions = Maps
            .newConcurrentMap();

    // Device ID's that have been seen so far
    private final List<DeviceId> devices = Lists.newArrayList();

    private ExecutorService linkDriver = Executors.newFixedThreadPool(1,
            namedThreads("null-link-driver"));

    // If true, 'flickers' links by alternating link up/down events at eventRate
    @Property(name = "flicker", boolValue = FLICKER,
            label = "Setting to flap links")
    private boolean flicker = FLICKER;

    // For flicker = true, duration between events in msec.
    @Property(name = "eventRate", intValue = DEFAULT_RATE,
            label = "Duration between Link Event")
    private int eventRate = 3000;

    public NullLinkProvider() {
        super(new ProviderId("null", "org.onosproject.provider.nil"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        deviceService.addListener(linkProvider);
        modified(context);
        if (flicker) {
            linkDriver.submit(new LinkDriver());
        }
        log.info("started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        if (flicker) {
            try {
                linkDriver.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("LinkBuilder did not terminate");
            }
            linkDriver.shutdownNow();
        }
        deviceService.removeListener(linkProvider);
        providerRegistry.unregister(this);
        deviceService = null;

        log.info("stopped");
    }

    public void modified(ComponentContext context) {
        if (context == null) {
            log.info("No configs, using defaults: flicker={}, eventRate={}",
                    FLICKER, DEFAULT_RATE);
            return;
        }
        Dictionary<?, ?> properties = context.getProperties();

        boolean flickSetting;
        int newRate;
        try {
            String s = (String) properties.get("flicker");
            flickSetting = isNullOrEmpty(s) ? flicker : Boolean.valueOf(s);
            s = (String) properties.get("eventRate");
            newRate = isNullOrEmpty(s) ? eventRate : Integer.valueOf(s);
        } catch (Exception e) {
            log.warn(e.getMessage());
            flickSetting = flicker;
            newRate = eventRate;
        }

        if (flicker != flickSetting) {
            flicker = flickSetting;
        }

        if (flicker) {
            if (eventRate != newRate) {
                eventRate = newRate;
            }
            linkDriver.submit(new LinkDriver());
        }
        log.info("Using new settings: flicker={}, eventRate={}", flicker,
                eventRate);
    }

    /**
     * Adds links as devices are found, and generates LinkEvents.
     */
    private class InternalLinkProvider implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
                addLink(event.subject());
                break;
            case DEVICE_REMOVED:
                removeLink(event.subject());
                break;
            default:
                break;
            }
        }

        private void addLink(Device current) {
            devices.add(current.id());
            // No link if only one device
            if (devices.size() == 1) {
                return;
            }

            // Attach new device to the last-seen device
            DeviceId prev = devices.get(devices.size() - 2);
            ConnectPoint src = new ConnectPoint(prev, SRCPORT);
            ConnectPoint dst = new ConnectPoint(current.id(), DSTPORT);

            LinkDescription fdesc = new DefaultLinkDescription(src, dst,
                    Link.Type.DIRECT);
            LinkDescription rdesc = new DefaultLinkDescription(dst, src,
                    Link.Type.DIRECT);
            descriptions.put(src, fdesc);
            descriptions.put(dst, rdesc);

            providerService.linkDetected(fdesc);
            providerService.linkDetected(rdesc);
        }

        private void removeLink(Device device) {
            providerService.linksVanished(device.id());
            devices.remove(device.id());
        }
    }

    /**
     * Generates link events using fake links.
     */
    private class LinkDriver implements Runnable {

        @Override
        public void run() {
            while (!linkDriver.isShutdown()) {
                for (LinkDescription desc : descriptions.values()) {
                    providerService.linkVanished(desc);
                    delay(eventRate);
                    providerService.linkDetected(desc);
                    delay(eventRate);
                }
            }
        }
    }
}
