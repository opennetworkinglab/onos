/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.optical;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.onos.net.Link.Type.OPTICAL;

/**
 * Ancillary provider to activate/deactivate optical links as their respective
 * devices go online or offline.
 */
@Component(immediate = true)
public class OpticalLinkProvider extends AbstractProvider implements LinkProvider {

    private static final Logger log = LoggerFactory.getLogger(OpticalLinkProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    private LinkProviderService providerService;
    private DeviceListener deviceListener = new InternalDeviceListener();
    private LinkListener linkListener = new InternalLinkListener();

    public OpticalLinkProvider() {
        super(new ProviderId("optical", "org.onlab.onos.optical"));
    }

    @Activate
    protected void activate() {
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        providerService = registry.register(this);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        registry.unregister(this);
        log.info("Stopped");
    }

    //Listens to device events and processes their links.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            Device device = event.subject();
            if (type == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                    type == DeviceEvent.Type.DEVICE_ADDED ||
                    type == DeviceEvent.Type.DEVICE_UPDATED) {
                processLinks(device);
            }
        }
    }

    //Listens to link events and processes the link additions.
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if (event.type() == LinkEvent.Type.LINK_ADDED) {
                Link link = event.subject();
                if (link.providerId().scheme().equals("cfg")) {
                    processLink(event.subject());
                }
            }
        }
    }

    private void processLinks(Device device) {
        for (Link link : linkService.getDeviceLinks(device.id())) {
            if (link.isDurable() && link.type() == OPTICAL) {
                processLink(link);
            }
        }
    }

    private void processLink(Link link) {
        boolean active = deviceService.isAvailable(link.src().deviceId()) &&
                deviceService.isAvailable(link.dst().deviceId());
        LinkDescription desc = new DefaultLinkDescription(link.src(), link.dst(), OPTICAL);
        if (active) {
            providerService.linkDetected(desc);
        } else {
            providerService.linkVanished(desc);
        }
    }
}
