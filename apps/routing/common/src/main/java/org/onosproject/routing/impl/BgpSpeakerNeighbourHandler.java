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

package org.onosproject.routing.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
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
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages neighbour message handlers for the use case of internal BGP speakers
 * connected to the network at some point that are exchanging neighbour
 * resolution messages with external routers that are connected behind interfaces.
 * <p>
 * For each internal speaker port we use a handler that proxies packets from
 * that port to the appropriate external-facing interface port.
 * For each external interface, we use a handler that responds to requests based
 * on the interface configuration and proxies replies back the the internal BGP
 * speaker.
 * </p>
 */
@Component(immediate = true, enabled = false)
public class BgpSpeakerNeighbourHandler {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NeighbourResolutionService neighbourService;

    private ApplicationId appId;

    private Set<ConnectPoint> speakerConnectPoints = new HashSet<>();


    private InternalNetworkConfigListener configListener = new InternalNetworkConfigListener();
    private InternalInterfaceListener interfaceListener = new InternalInterfaceListener();

    private ExternalInterfaceNeighbourHandler externalHandler = new ExternalInterfaceNeighbourHandler();
    private InternalSpeakerNeighbourHandler internalHandler = new InternalSpeakerNeighbourHandler();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(RoutingService.ROUTER_APP_ID);
        configService.addListener(configListener);
        interfaceService.addListener(interfaceListener);

        interfaceService.getInterfaces().forEach(
                intf -> neighbourService.registerNeighbourHandler(intf, externalHandler, appId));
        configureSpeakerHandlers();
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        interfaceService.removeListener(interfaceListener);

        neighbourService.unregisterNeighbourHandlers(appId);
    }

    private void configureSpeakerHandlers() {
        BgpConfig config = configService.getConfig(appId, RoutingService.CONFIG_CLASS);

        if (config == null) {
            return;
        }

        speakerConnectPoints.forEach(
                cp -> neighbourService.unregisterNeighbourHandler(cp, internalHandler, appId));
        speakerConnectPoints.clear();

        config.bgpSpeakers().forEach(speaker -> {
            neighbourService.registerNeighbourHandler(speaker.connectPoint(), internalHandler, appId);
            speakerConnectPoints.add(speaker.connectPoint());
        });
    }

    private void updateInterface(Interface intf) {
        // Only use interfaces that have an IP address
        if (!intf.ipAddressesList().isEmpty()) {
            neighbourService.registerNeighbourHandler(intf, externalHandler, appId);
        }
    }

    private void removeInterface(Interface intf) {
        neighbourService.unregisterNeighbourHandler(intf, externalHandler, appId);
    }

    /**
     * Neighbour message handler for external facing ports that have interface
     * configuration.
     */
    public class ExternalInterfaceNeighbourHandler implements
            NeighbourMessageHandler {

        @Override
        public void handleMessage(NeighbourMessageContext context, HostService hostService) {
            switch (context.type()) {
            case REQUEST:
                // Reply to requests that target our configured interface IP
                // address on this port. Drop all other requests.
                interfaceService.getInterfacesByPort(context.inPort())
                        .stream()
                        .filter(intf -> intf.ipAddressesList()
                                .stream()
                                .anyMatch(ia -> ia.ipAddress().equals(context.target()) &&
                                        ia.subnetAddress().contains(context.sender())))
                        .forEach(intf -> context.reply(intf.mac()));

                break;
            case REPLY:
                // Proxy replies over to our internal BGP speaker if the host
                // is known to us
                Set<Host> hosts = hostService.getHostsByMac(context.dstMac());

                if (hosts.isEmpty()) {
                    context.drop();
                } else {
                    Host h = hosts.stream().findFirst().get();
                    VlanId bgpSpeakerVlanId = h.vlan();

                    if (!bgpSpeakerVlanId.equals(VlanId.NONE)) {
                        context.packet().setVlanID(bgpSpeakerVlanId.toShort());
                    } else {
                        context.packet().setVlanID(Ethernet.VLAN_UNTAGGED);
                    }
                    context.forward(h.location());
                }
                break;
            default:
                break;
            }
        }

    }

    /**
     * Neighbour message handler for ports connected to the internal BGP speakers.
     */
    private class InternalSpeakerNeighbourHandler implements
            NeighbourMessageHandler {
        @Override
        public void handleMessage(NeighbourMessageContext context, HostService hostService) {
            // For messages coming from a BGP speaker, look at the sender address
            // to find the interface to proxy to
            interfaceService.getInterfacesByIp(context.sender())
                    .forEach(context::forward);
        }
    }

    private class InternalNetworkConfigListener implements
            NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
            case CONFIG_REGISTERED:
                break;
            case CONFIG_UNREGISTERED:
                break;
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
            case CONFIG_REMOVED:
                if (event.configClass() == RoutingService.CONFIG_CLASS) {
                    configureSpeakerHandlers();
                }
                break;
            default:
                break;
            }
        }
    }

    private class InternalInterfaceListener implements InterfaceListener {

        @Override
        public void event(InterfaceEvent event) {
            switch (event.type()) {
            case INTERFACE_ADDED:
                updateInterface(event.subject());
                break;
            case INTERFACE_UPDATED:
                break;
            case INTERFACE_REMOVED:
                removeInterface(event.subject());
                break;
            default:
                break;
            }
        }
    }
}
