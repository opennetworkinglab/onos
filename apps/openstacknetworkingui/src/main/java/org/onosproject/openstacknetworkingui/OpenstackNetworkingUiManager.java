/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworkingui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiTopoOverlayFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.UiViewHidden;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.Device.Type.SWITCH;

/**
 * Implementation of OpenStack Networking UI service.
 */
@Component(immediate = true, service = OpenstackNetworkingUiService.class)
public class OpenstackNetworkingUiManager implements OpenstackNetworkingUiService {

    private static final ClassLoader CL = OpenstackNetworkingUiManager.class.getClassLoader();
    private static final String VIEW_ID = "sonaTopov";
    private static final String PORT_NAME = "portName";
    private static final String VXLAN = "vxlan";
    private static final String APP_ID = "org.onosproject.openstacknetworkingui";
    private static final String SONA_GUI = "sonagui";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkStore linkStore;

    Set<Device> vDevices;

    private OpenstackNetworkingUiMessageHandler messageHandler = new OpenstackNetworkingUiMessageHandler();

    private final List<UiView> uiViews = ImmutableList.of(
            new UiViewHidden(VIEW_ID)
    );


    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(messageHandler);

    private final UiTopoOverlayFactory topoOverlayFactory =
            () -> ImmutableList.of(new OpenstackNetworkingUiOverlay());

    protected UiExtension extension =
            new UiExtension.Builder(CL, uiViews)
                    .resourcePath(VIEW_ID)
                    .messageHandlerFactory(messageHandlerFactory)
                    .topoOverlayFactory(topoOverlayFactory)
                    .build();

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);

        vDevices = Streams.stream(deviceService.getAvailableDevices())
                .filter(device -> device.type() == SWITCH)
                .collect(Collectors.toSet());

        vDevices.forEach(this::createLinksConnectedToTargetvDevice);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }

    private Optional<Port> vxlanPort(DeviceId deviceId) {
        return deviceService.getPorts(deviceId)
                .stream()
                .filter(port -> port.annotations().value(PORT_NAME).equals(VXLAN))
                .findAny();
    }

    private void createLinksConnectedToTargetvDevice(Device targetvDevice) {
        vDevices.stream().filter(d -> !d.equals(targetvDevice))
                .forEach(device -> {
                    if (vxlanPort(targetvDevice.id()).isPresent() && vxlanPort(device.id()).isPresent()) {
                        ConnectPoint srcConnectPoint = createConnectPoint(targetvDevice.id());

                        ConnectPoint dstConnectPoint = createConnectPoint(device.id());

                        LinkDescription linkDescription = createLinkDescription(srcConnectPoint, dstConnectPoint);

                        linkStore.createOrUpdateLink(new ProviderId(SONA_GUI, APP_ID),
                                linkDescription);
                    }
                });
    }

    private ConnectPoint createConnectPoint(DeviceId deviceId) {
        try {
            return new ConnectPoint(deviceId, vxlanPort(deviceId).get().number());
        } catch (NoSuchElementException exception) {
            log.warn("Exception occured because of {}", exception.toString());
            return null;
        }
    }

    private LinkDescription createLinkDescription(ConnectPoint srcConnectPoint, ConnectPoint dstConnectPoint) {
        return new DefaultLinkDescription(srcConnectPoint, dstConnectPoint, Type.DIRECT, true);
    }
}
