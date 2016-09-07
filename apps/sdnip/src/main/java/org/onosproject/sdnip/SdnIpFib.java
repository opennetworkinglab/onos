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

package org.onosproject.sdnip;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.constraint.PartialFailureConstraint;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIB component of SDN-IP.
 */
@Component(immediate = true, enabled = false)
public class SdnIpFib {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentSynchronizationService intentSynchronizer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteService routeService;

    private final InternalRouteListener routeListener = new InternalRouteListener();
    private final InternalInterfaceListener interfaceListener = new InternalInterfaceListener();

    private static final int PRIORITY_OFFSET = 100;
    private static final int PRIORITY_MULTIPLIER = 5;
    protected static final ImmutableList<Constraint> CONSTRAINTS
            = ImmutableList.of(new PartialFailureConstraint());

    private final Map<IpPrefix, MultiPointToSinglePointIntent> routeIntents
            = new ConcurrentHashMap<>();

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.getAppId(SdnIp.SDN_IP_APP);

        interfaceService.addListener(interfaceListener);

        routeService.addListener(routeListener);
    }

    @Deactivate
    public void deactivate() {
        interfaceService.removeListener(interfaceListener);
        routeService.removeListener(routeListener);
    }

    private void update(ResolvedRoute route) {
        synchronized (this) {
            IpPrefix prefix = route.prefix();
            MultiPointToSinglePointIntent intent =
                    generateRouteIntent(prefix, route.nextHop(), route.nextHopMac());

            if (intent == null) {
                log.debug("SDN-IP no interface found for route {}", route);
                return;
            }

            routeIntents.put(prefix, intent);
            intentSynchronizer.submit(intent);
        }
    }

    private void withdraw(ResolvedRoute route) {
        synchronized (this) {
            IpPrefix prefix = route.prefix();
            MultiPointToSinglePointIntent intent = routeIntents.remove(prefix);
            if (intent == null) {
                log.trace("SDN-IP no intent in routeIntents to delete " +
                        "for prefix: {}", prefix);
                return;
            }
            intentSynchronizer.withdraw(intent);
        }
    }

    /**
     * Generates a route intent for a prefix, the next hop IP address, and
     * the next hop MAC address.
     * <p/>
     * This method will find the egress interface for the intent.
     * Intent will match dst IP prefix and rewrite dst MAC address at all other
     * border switches, then forward packets according to dst MAC address.
     *
     * @param prefix            IP prefix of the route to add
     * @param nextHopIpAddress  IP address of the next hop
     * @param nextHopMacAddress MAC address of the next hop
     * @return the generated intent, or null if no intent should be submitted
     */
    private MultiPointToSinglePointIntent generateRouteIntent(
            IpPrefix prefix,
            IpAddress nextHopIpAddress,
            MacAddress nextHopMacAddress) {

        // Find the attachment point (egress interface) of the next hop
        Interface egressInterface =
                interfaceService.getMatchingInterface(nextHopIpAddress);
        if (egressInterface == null) {
            log.warn("No outgoing interface found for {}",
                    nextHopIpAddress);
            return null;
        }
        ConnectPoint egressPort = egressInterface.connectPoint();

        Set<Interface> ingressInterfaces = new HashSet<>();
        Set<ConnectPoint> ingressPorts = new HashSet<>();
        log.debug("Generating intent for prefix {}, next hop mac {}",
                prefix, nextHopMacAddress);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        // Get ingress interfaces and ports
        // TODO this should be only peering interfaces
        interfaceService.getInterfaces().stream()
                .filter(intf -> !intf.equals(egressInterface))
                .filter(intf -> !intf.connectPoint().equals(egressPort))
                .forEach(intf -> {
                    ingressInterfaces.add(intf);
                    ConnectPoint ingressPort = intf.connectPoint();
                    ingressPorts.add(ingressPort);
                });

        // By default the ingress traffic is not tagged
        VlanId ingressVlanId = VlanId.NONE;

        // Match VLAN Id ANY if the source VLAN Id is not null
        // TODO need to be able to set a different VLAN Id per ingress interface
        for (Interface intf : ingressInterfaces) {
            if (!intf.vlan().equals(VlanId.NONE)) {
                selector.matchVlanId(VlanId.ANY);
                ingressVlanId = intf.vlan();
            }
        }

        // Match the destination IP prefix at the first hop
        if (prefix.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            // if it is default route, then we do not need match destination
            // IP address
            if (prefix.prefixLength() != 0) {
                selector.matchIPDst(prefix);
            }
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            // if it is default route, then we do not need match destination
            // IP address
            if (prefix.prefixLength() != 0) {
                selector.matchIPv6Dst(prefix);
            }
        }

        // Rewrite the destination MAC address
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthDst(nextHopMacAddress);

        // Set egress VLAN Id
        // TODO need to make the comparison with different ingress VLAN Ids
        if (!ingressVlanId.equals(egressInterface.vlan())) {
            if (egressInterface.vlan().equals(VlanId.NONE)) {
                treatment.popVlan();
            } else {
                treatment.setVlanId(egressInterface.vlan());
            }
        }

        // Set priority
        int priority =
                prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;

        // Set key
        Key key = Key.of(prefix.toString(), appId);

        return MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .ingressPoints(ingressPorts)
                .egressPoint(egressPort)
                .priority(priority)
                .constraints(CONSTRAINTS)
                .build();
    }

    private void updateInterface(Interface intf) {
        synchronized (this) {
            for (Map.Entry<IpPrefix, MultiPointToSinglePointIntent> entry : routeIntents.entrySet()) {
                MultiPointToSinglePointIntent intent = entry.getValue();
                Set<ConnectPoint> ingress = Sets.newHashSet(intent.ingressPoints());
                ingress.add(intf.connectPoint());

                MultiPointToSinglePointIntent newIntent =
                        MultiPointToSinglePointIntent.builder(intent)
                                .ingressPoints(ingress)
                                .build();

                routeIntents.put(entry.getKey(), newIntent);
                intentSynchronizer.submit(newIntent);
            }
        }
    }

    private void removeInterface(Interface intf) {
        synchronized (this) {
            for (Map.Entry<IpPrefix, MultiPointToSinglePointIntent> entry : routeIntents.entrySet()) {
                MultiPointToSinglePointIntent intent = entry.getValue();
                if (intent.egressPoint().equals(intf.connectPoint())) {
                    // This intent just lost its head. Remove it and let
                    // higher layer routing reroute.
                    intentSynchronizer.withdraw(routeIntents.remove(entry.getKey()));
                } else {
                    if (intent.ingressPoints().contains(intf.connectPoint())) {

                        Set<ConnectPoint> ingress = Sets.newHashSet(intent.ingressPoints());
                        ingress.remove(intf.connectPoint());

                        MultiPointToSinglePointIntent newIntent =
                                MultiPointToSinglePointIntent.builder(intent)
                                .ingressPoints(ingress)
                                .build();

                        routeIntents.put(entry.getKey(), newIntent);
                        intentSynchronizer.submit(newIntent);
                    }
                }
            }
        }
    }

    private class InternalRouteListener implements RouteListener {
        @Override
        public void event(RouteEvent event) {
            switch (event.type()) {
            case ROUTE_ADDED:
            case ROUTE_UPDATED:
                update(event.subject());
                break;
            case ROUTE_REMOVED:
                withdraw(event.subject());
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
