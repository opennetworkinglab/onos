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

import com.google.common.collect.Maps;

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
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles neighbour messages for VPLS use case.
 * Handlers will be changed automatically by interface or network configuration
 * events.
 */
@Component(immediate = true)
public class VplsNeighbourHandler {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NeighbourResolutionService neighbourService;

    private VplsInterfaceListener interfaceListener
            = new VplsInterfaceListener();

    private VplsNeighbourMessageHandler neighbourHandler =
            new VplsNeighbourMessageHandler();

    private final Logger log = getLogger(getClass());

    private Map<Interface, NeighbourMessageHandler> neighbourHandlers =
            Maps.newHashMap();

    private ApplicationId appId;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Vpls.VPLS_APP);
        interfaceService.addListener(interfaceListener);

        interfaceService.getInterfaces().forEach(intf -> {
            neighbourHandlers.put(intf, neighbourHandler);

            neighbourService.registerNeighbourHandler(intf, neighbourHandler, appId);
        });

        log.debug("Activated");
    }

    @Deactivate
    protected void deactivate() {
        interfaceService.removeListener(interfaceListener);
        neighbourHandlers.entrySet().forEach(e -> {
            neighbourService.unregisterNeighbourHandler(e.getKey(), e.getValue(), appId);
        });
        log.debug("Deactivated");
    }

    private void configNeighbourHandler(Interface intf,
                                          NeighbourMessageHandler handler,
                                          InterfaceEvent.Type eventType) {
        switch (eventType) {
            case INTERFACE_ADDED:
                neighbourHandlers.put(intf, handler);
                neighbourService.registerNeighbourHandler(intf, handler, appId);
                break;
            case INTERFACE_REMOVED:
                neighbourHandlers.remove(intf, handler);
                neighbourService.unregisterNeighbourHandler(intf, handler, appId);
                break;
            case INTERFACE_UPDATED:
                break;
            default:
                break;
        }
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
                    interfaceService.getInterfacesByVlan(context.vlan())
                            .stream()
                            .map(Interface::connectPoint)
                            .forEach(context::forward);
                    break;
                case REPLY:
                    hostService.getHostsByMac(context.dstMac())
                            .stream()
                            .filter(host -> host.vlan().equals(context.vlan()))
                            .map(Host::location)
                            .forEach(context::forward);
                    break;

                default:
                    log.warn("Unknown context type: {}", context.type());
                    break;
            }
        }
    }

    /**
     * Listener for interface configuration events.
     */
    private class VplsInterfaceListener implements InterfaceListener {

        @Override
        public void event(InterfaceEvent event) {
            configNeighbourHandler(event.subject(), neighbourHandler, event.type());
        }
    }

}
