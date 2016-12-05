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
package org.onosproject.vpls;

import com.google.common.collect.SetMultimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageHandler;
import org.onosproject.incubator.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.Host;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.vpls.config.VplsConfigService;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles neighbour messages for on behalf of the VPLS application. Handlers
 * will be changed automatically by interface or network configuration events.
 */
@Component(immediate = true)
public class VplsNeighbourHandler {
    private static final String UNKNOWN_CONTEXT = "Unknown context type: {}";

    private static final String CAN_NOT_FIND_VPLS =
            "Cannot find VPLS for port {} with VLAN Id {}.";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NeighbourResolutionService neighbourService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VplsConfigService vplsConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    private VplsInterfaceListener interfaceListener =
            new VplsInterfaceListener();

    protected VplsNeighbourMessageHandler neighbourHandler =
            new VplsNeighbourMessageHandler();

    protected VplsConfigListener configListener =
            new VplsConfigListener();

    private final Logger log = getLogger(getClass());

    private ApplicationId appId;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Vpls.VPLS_APP);
        interfaceService.addListener(interfaceListener);
        configService.addListener(configListener);
        configNeighbourHandler();
    }

    @Deactivate
    protected void deactivate() {
        interfaceService.removeListener(interfaceListener);
        configService.removeListener(configListener);
        neighbourService.unregisterNeighbourHandlers(appId);
    }

    private void configNeighbourHandler() {
        neighbourService.unregisterNeighbourHandlers(appId);
        Set<Interface> interfaces = vplsConfigService.allIfaces();

        interfaceService.getInterfaces()
                .stream()
                .filter(interfaces::contains)
                .forEach(intf -> {
                    neighbourService.registerNeighbourHandler(intf,
                                                              neighbourHandler,
                                                              appId);
                });
    }

    /**
     * Handler for neighbour messages.
     */
    private class VplsNeighbourMessageHandler implements NeighbourMessageHandler {

        @Override
        public void handleMessage(NeighbourMessageContext context,
                                  HostService hostService) {

            switch (context.type()) {
                case REQUEST:
                    handleRequest(context);
                    break;

                case REPLY:
                    handleReply(context, hostService);
                    break;

                default:
                    log.warn(UNKNOWN_CONTEXT, context.type());
                    break;
            }
        }
    }

    /**
     * Handles request messages.
     *
     * @param context the message context
     */
    protected void handleRequest(NeighbourMessageContext context) {
        SetMultimap<String, Interface> interfaces =
                vplsConfigService.ifacesByVplsName(context.vlan(),
                                                   context.inPort());
        if (interfaces != null) {
            interfaces.values().stream()
                    .filter(intf -> !context.inPort().equals(intf.connectPoint()))
                    .forEach(context::forward);

        } else {
            log.debug(CAN_NOT_FIND_VPLS, context.inPort(), context.vlan());
        }
    }

    /**
     * Handles reply messages between VLAN tagged interfaces.
     *
     * @param context the message context
     * @param hostService the host service
     */
    protected void handleReply(NeighbourMessageContext context,
                               HostService hostService) {
        Set<Host> hosts = hostService.getHostsByMac(context.dstMac());
        SetMultimap<String, Interface> interfaces =
                vplsConfigService.ifacesByVplsName(context.vlan(),
                                                   context.inPort());
        if (interfaces != null) {
            hosts.forEach(host -> interfaces.values().stream()
                    .filter(intf -> intf.connectPoint().equals(host.location()))
                    .filter(intf -> intf.vlan().equals(host.vlan()))
                    .forEach(context::forward));
        } else {
            log.debug(CAN_NOT_FIND_VPLS, context.inPort(), context.vlan());
        }
    }

    /**
     * Listener for interface configuration events.
     */
    private class VplsInterfaceListener implements InterfaceListener {

        @Override
        public void event(InterfaceEvent event) {
            configNeighbourHandler();
        }
    }

    /**
     * Listener for network configuration events.
     */
    private class VplsConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            configNeighbourHandler();
        }
    }
}