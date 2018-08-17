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
package org.onosproject.vpls;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceEvent;
import org.onosproject.net.intf.InterfaceListener;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.host.HostService;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.VplsStore;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NeighbourResolutionService neighbourService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VplsStore vplsStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
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
        appId = coreService.registerApplication(VplsManager.VPLS_APP);
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

    /**
     * Registers neighbour handler to all available interfaces.
     */
    protected void configNeighbourHandler() {
        neighbourService.unregisterNeighbourHandlers(appId);
        interfaceService
                .getInterfaces()
                .forEach(intf -> neighbourService.registerNeighbourHandler(intf,
                                                                       neighbourHandler,
                                                                       appId));
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
        // Find target VPLS first, then broadcast to all interface of this VPLS
        VplsData vplsData = findVpls(context);
        if (vplsData != null) {
            vplsData.interfaces().stream()
                    .filter(intf -> !context.inPort().equals(intf.connectPoint()))
                    .forEach(context::forward);
        } else {
            log.warn(CAN_NOT_FIND_VPLS, context.inPort(), context.vlan());
            context.drop();
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
        // Find target VPLS, then reply to the host
        VplsData vplsData = findVpls(context);
        if (vplsData != null) {
            MacAddress dstMac = context.dstMac();
            Set<Host> hosts = hostService.getHostsByMac(dstMac);
            hosts = hosts.stream()
                    .filter(host -> vplsData.interfaces().contains(getHostInterface(host)))
                    .collect(Collectors.toSet());

            // reply to all host in same VPLS
            hosts.stream()
                    .map(this::getHostInterface)
                    .filter(Objects::nonNull)
                    .forEach(context::forward);
        } else {
            // this might be happened when we remove an interface from VPLS
            // just ignore this message
            log.warn(CAN_NOT_FIND_VPLS, context.inPort(), context.vlan());
            context.drop();
        }
    }

    /**
     * Finds the VPLS with given neighbour message context.
     *
     * @param context the neighbour message context
     * @return the VPLS for specific neighbour message context
     */
    private VplsData findVpls(NeighbourMessageContext context) {
        Collection<VplsData> vplses = vplsStore.getAllVpls();
        for (VplsData vplsData : vplses) {
            Set<Interface> interfaces = vplsData.interfaces();
            ConnectPoint port = context.inPort();
            VlanId vlanId = context.vlan();
            boolean match = interfaces.stream()
                    .anyMatch(iface -> iface.connectPoint().equals(port) &&
                            iface.vlan().equals(vlanId));
            if (match) {
                return vplsData;
            }
        }
        return null;
    }

    /**
     * Finds the network interface related to the host.
     *
     * @param host the host
     * @return the interface related to the host
     */
    private Interface getHostInterface(Host host) {
        Set<Interface> interfaces = interfaceService.getInterfaces();
        return interfaces.stream()
                .filter(iface -> iface.connectPoint().equals(host.location()) &&
                                 iface.vlan().equals(host.vlan()))
                .findFirst()
                .orElse(null);
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
