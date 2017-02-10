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
package org.onosproject.bgprouter;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.component.ComponentService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.routing.config.RoutingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BgpRouter component.
 */
@Component(immediate = true)
public class BgpRouter {

    private static final Logger log = LoggerFactory.getLogger(BgpRouter.class);

    public static final String BGP_ROUTER_APP = "org.onosproject.bgprouter";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentService componentService;

    private ApplicationId appId;

    // Device id of control-plane switch (OVS) connected to BGP Speaker - should be
    // learned from config
    private DeviceId ctrlDeviceId;

    // Responsible for handling BGP traffic (encapsulated within OF messages)
    // between the data-plane switch and the Quagga VM using a control plane OVS.
    private TunnellingConnectivityManager connectivityManager;

    private DeviceListener deviceListener;
    private IcmpHandler icmpHandler;

    private static List<String> components = new ArrayList<>();
    static {
        components.add("org.onosproject.routing.bgp.BgpSessionManager");
        components.add("org.onosproject.routing.impl.BgpSpeakerNeighbourHandler");
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(BGP_ROUTER_APP);

        RoutingConfiguration.register(networkConfigService);

        components.forEach(name -> componentService.activate(appId, name));

        ApplicationId routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        BgpConfig bgpConfig =
                networkConfigService.getConfig(routerAppId, RoutingService.CONFIG_CLASS);

        if (bgpConfig == null) {
            log.error("No BgpConfig found");
            return;
        }

        getDeviceConfiguration(bgpConfig);

        connectivityManager = new TunnellingConnectivityManager(appId,
                                                                bgpConfig,
                                                                interfaceService,
                                                                packetService,
                                                                flowObjectiveService);

        icmpHandler = new IcmpHandler(interfaceService, packetService);

        deviceListener = new InnerDeviceListener();
        deviceService.addListener(deviceListener);

        connectivityManager.start();
        icmpHandler.start();

        if (deviceService.isAvailable(ctrlDeviceId)) {
            connectivityManager.notifySwitchAvailable();
        }

        log.info("BgpRouter started");
    }

    @Deactivate
    protected void deactivate() {
        components.forEach(name -> componentService.deactivate(appId, name));

        RoutingConfiguration.unregister(networkConfigService);

        connectivityManager.stop();
        icmpHandler.stop();
        deviceService.removeListener(deviceListener);

        log.info("BgpRouter stopped");
    }

    private void getDeviceConfiguration(BgpConfig bgpConfig) {
        Optional<BgpConfig.BgpSpeakerConfig> bgpSpeaker =
                bgpConfig.bgpSpeakers().stream().findAny();

        if (!bgpSpeaker.isPresent()) {
            log.error("BGP speaker configuration not found");
            return;
        }

        ctrlDeviceId = bgpSpeaker.get().connectPoint().deviceId();

        log.info("Control Plane OVS dpid: {}", ctrlDeviceId);
    }

    // Triggers driver setup when a device is (re)detected.
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
            case DEVICE_AVAILABILITY_CHANGED:
                if (deviceService.isAvailable(event.subject().id())) {
                    log.info("Device connected {}", event.subject().id());

                    if (event.subject().id().equals(ctrlDeviceId)) {
                        connectivityManager.notifySwitchAvailable();
                    }
                }
                break;
            // TODO other cases
            case DEVICE_UPDATED:
            case DEVICE_REMOVED:
            case DEVICE_SUSPENDED:
            case PORT_ADDED:
            case PORT_UPDATED:
            case PORT_REMOVED:
            default:
                break;
            }
        }
    }

}
