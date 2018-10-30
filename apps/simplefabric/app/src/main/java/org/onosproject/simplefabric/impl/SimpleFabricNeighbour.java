/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.simplefabric.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.simplefabric.api.FabricNetwork;
import org.onosproject.simplefabric.api.SimpleFabricEvent;
import org.onosproject.simplefabric.api.SimpleFabricListener;
import org.onosproject.simplefabric.api.SimpleFabricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


/**
 * Handles neighbour messages for on behalf of the L2 Network application. Handlers
 * will be changed automatically by interface or network configuration events.
 */
@Component(immediate = true, enabled = false)
public class SimpleFabricNeighbour {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NeighbourResolutionService neighbourService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected SimpleFabricService simpleFabric;

    private final InternalSimpleFabricListener simpleFabricListener =
            new InternalSimpleFabricListener();

    private L2NetworkNeighbourMessageHandler neighbourHandler =
            new L2NetworkNeighbourMessageHandler();

    private Set<Interface> monitoredInterfaces = new HashSet<>();

    @Activate
    public void activate() {
        appId = simpleFabric.appId();
        simpleFabric.addListener(simpleFabricListener);
        refresh();
        log.info("simple fabric neighbour started");
    }

    @Deactivate
    public void deactivate() {
        simpleFabric.removeListener(simpleFabricListener);
        unregister();
        monitoredInterfaces.clear();
        log.info("simple fabric neighbour stoped");
    }

    /**
     * Registers neighbour handler to all available interfaces.
     */
    protected void refresh() {
        Set<Interface> interfaces = interfaceService.getInterfaces();
        // check for new interfaces
        for (Interface intf : interfaces) {
            if (!monitoredInterfaces.contains(intf) && simpleFabric.isFabricNetworkInterface(intf)) {
               log.info("simple fabric neighbour register handler: {}", intf);
               neighbourService.registerNeighbourHandler(intf, neighbourHandler, appId);
               monitoredInterfaces.add(intf);
            } else {
               log.debug("simple fabric neighobur unknown interface: {}", intf);
            }
        }
        // check for removed interfaces
        Set<Interface> monitoredInterfacesToBeRemoved = new HashSet<>();
        for (Interface intf : monitoredInterfaces) {
            if (!interfaces.contains(intf)) {
               log.info("simple fabric neighbour unregister handler: {}", intf);
               neighbourService.unregisterNeighbourHandler(intf, neighbourHandler, appId);
               monitoredInterfacesToBeRemoved.add(intf);
            }
        }
        for (Interface intf : monitoredInterfacesToBeRemoved) {
            monitoredInterfaces.remove(intf);
        }
    }

    /**
     * Unregisters neighbour handler to all available interfaces.
     */
    protected void unregister() {
        log.info("simple fabric neighbour unregister handler");
        neighbourService.unregisterNeighbourHandlers(appId);
    }

    /**
     * Handles request messages.
     *
     * @param context the message context
     */
    protected void handleRequest(NeighbourMessageContext context) {
        MacAddress mac = simpleFabric.vMacForIp(context.target());
        if (mac != null) {
            log.trace("simple fabric neightbour request on virtualGatewayAddress {}; response to {} {} mac={}",
                      context.target(), context.inPort(), context.vlan(), mac);
            context.reply(mac);
            return;
        }
        // else forward to corresponding host

        FabricNetwork fabricNetwork = simpleFabric.fabricNetwork(context.inPort(), context.vlan());
        if (fabricNetwork != null) {
            int numForwards = 0;
            if (!context.dstMac().isBroadcast() && !context.dstMac().isMulticast()) {
                for (Host host : hostService.getHostsByMac(context.dstMac())) {
                    log.trace("simple fabric neightbour request forward unicast to {}", host.location());
                    context.forward(host.location());  // ASSUME: vlan is same
                    // TODO: may need to check host.location().time()
                    numForwards++;
                }
                if (numForwards > 0) {
                    return;
                }
            }
            // else do broadcast to all host in the same l2 network
            log.trace("simple fabric neightbour request forward broadcast: {} {}",
                     context.inPort(), context.vlan());
            for (Interface iface : fabricNetwork.interfaces()) {
                if (!context.inPort().equals(iface.connectPoint())) {
                    log.trace("simple fabric forward neighbour request broadcast to {}", iface);
                    context.forward(iface);
                }
            }
        } else {
            log.warn("simple fabric neightbour request drop: {} {}",
                     context.inPort(), context.vlan());
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
        // Find target L2 Network, then reply to the host
        FabricNetwork fabricNetwork = simpleFabric.fabricNetwork(context.inPort(), context.vlan());
        if (fabricNetwork != null) {
            // TODO: need to check and update simpleFabric.DefaultFabricNetwork
            MacAddress mac = simpleFabric.vMacForIp(context.target());
            if (mac != null) {
                log.trace("simple fabric neightbour response message to virtual gateway; drop: {} {} target={}",
                          context.inPort(), context.vlan(), context.target());
                context.drop();
            } else {
                // forward reply to the hosts of the dstMac
                Set<Host> hosts = hostService.getHostsByMac(context.dstMac());
                log.trace("simple fabric neightbour response message forward: {} {} target={} -> {}",
                          context.inPort(), context.vlan(), context.target(), hosts);
                hosts.stream()
                        .map(host -> simpleFabric.hostInterface(host))
                        .filter(Objects::nonNull)
                        .forEach(context::forward);
            }
        } else {
            // this might be happened when we remove an interface from L2 Network
            // just ignore this message
            log.warn("simple fabric neightbour response message drop for unknown fabricNetwork: {} {}",
                     context.inPort(), context.vlan());
            context.drop();
        }
    }

    private class L2NetworkNeighbourMessageHandler implements NeighbourMessageHandler {
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
                    log.warn("simple fabric neightor unknown context type: {}", context.type());
                    break;
            }
        }
    }

    private class InternalSimpleFabricListener implements SimpleFabricListener {
        @Override
        public void event(SimpleFabricEvent event) {
            switch (event.type()) {
            case SIMPLE_FABRIC_UPDATED:
                refresh();
                break;
            default:
                break;
            }
        }
    }

}

