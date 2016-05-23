/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.optical;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.net.Link.Type.OPTICAL;

/**
 * Ancillary provider to activate/deactivate optical links as their respective
 * devices go online or offline.
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
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
        super(new ProviderId("optical", "org.onosproject.optical"));
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
                processDeviceLinks(device);
            } else if (type == DeviceEvent.Type.PORT_UPDATED) {
                processPortLinks(device, event.port());
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

    private void processDeviceLinks(Device device) {
        for (Link link : linkService.getDeviceLinks(device.id())) {
            if (link.isDurable() && link.type() == OPTICAL) {
                processLink(link);
            }
        }
    }

    private void processPortLinks(Device device, Port port) {
        ConnectPoint connectPoint = new ConnectPoint(device.id(), port.number());
        for (Link link : linkService.getLinks(connectPoint)) {
            if (link.isDurable() && link.type() == OPTICAL) {
                processLink(link);
            }
        }
    }

    private void processLink(Link link) {
        DeviceId srcId = link.src().deviceId();
        DeviceId dstId = link.dst().deviceId();
        Port srcPort = deviceService.getPort(srcId, link.src().port());
        Port dstPort = deviceService.getPort(dstId, link.dst().port());

        if (srcPort == null || dstPort == null) {
            return; //FIXME remove this in favor of below TODO
        }

        boolean active = deviceService.isAvailable(srcId) &&
                deviceService.isAvailable(dstId) &&
                // TODO: should update be queued if src or dstPort is null?
                //srcPort != null && dstPort != null &&
                srcPort.isEnabled() && dstPort.isEnabled();

        LinkDescription desc = new DefaultLinkDescription(link.src(), link.dst(), OPTICAL);
        if (active) {
            providerService.linkDetected(desc);
        } else {
            providerService.linkVanished(desc);
        }
    }
}
