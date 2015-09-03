/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.constraint.PartialFailureConstraint;
import org.onosproject.routing.FibListener;
import org.onosproject.routing.FibUpdate;
import org.onosproject.routing.IntentSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * FIB component of SDN-IP.
 */
public class SdnIpFib implements FibListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    private static final int PRIORITY_OFFSET = 100;
    private static final int PRIORITY_MULTIPLIER = 5;
    protected static final ImmutableList<Constraint> CONSTRAINTS
            = ImmutableList.of(new PartialFailureConstraint());

    private final Map<IpPrefix, MultiPointToSinglePointIntent> routeIntents;

    private final ApplicationId appId;
    private final InterfaceService interfaceService;
    private final IntentSynchronizationService intentSynchronizer;

    /**
     * Class constructor.
     *
     * @param appId application ID to use when generating intents
     * @param interfaceService interface service
     * @param intentSynchronizer intent synchronizer
     */
    public SdnIpFib(ApplicationId appId, InterfaceService interfaceService,
                    IntentSynchronizationService intentSynchronizer) {
        routeIntents = new ConcurrentHashMap<>();

        this.appId = appId;
        this.interfaceService = interfaceService;
        this.intentSynchronizer = intentSynchronizer;
    }


    @Override
    public void update(Collection<FibUpdate> updates, Collection<FibUpdate> withdraws) {
        int submitCount = 0, withdrawCount = 0;
        //
        // NOTE: Semantically, we MUST withdraw existing intents before
        // submitting new intents.
        //
        synchronized (this) {
            MultiPointToSinglePointIntent intent;

            //
            // Prepare the Intent batch operations for the intents to withdraw
            //
            for (FibUpdate withdraw : withdraws) {
                checkArgument(withdraw.type() == FibUpdate.Type.DELETE,
                        "FibUpdate with wrong type in withdraws list");

                IpPrefix prefix = withdraw.entry().prefix();
                intent = routeIntents.remove(prefix);
                if (intent == null) {
                    log.trace("SDN-IP No intent in routeIntents to delete " +
                            "for prefix: {}", prefix);
                    continue;
                }
                intentSynchronizer.withdraw(intent);
                withdrawCount++;
            }

            //
            // Prepare the Intent batch operations for the intents to submit
            //
            for (FibUpdate update : updates) {
                checkArgument(update.type() == FibUpdate.Type.UPDATE,
                        "FibUpdate with wrong type in updates list");

                IpPrefix prefix = update.entry().prefix();
                intent = generateRouteIntent(prefix, update.entry().nextHopIp(),
                        update.entry().nextHopMac());

                if (intent == null) {
                    // This preserves the old semantics - if an intent can't be
                    // generated, we don't do anything with that prefix. But
                    // perhaps we should withdraw the old intent anyway?
                    continue;
                }

                routeIntents.put(prefix, intent);
                intentSynchronizer.submit(intent);
                submitCount++;
            }

            log.debug("SDN-IP submitted {}/{}, withdrew = {}/{}", submitCount,
                    updates.size(), withdrawCount, withdraws.size());
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
        Interface egressInterface = interfaceService.getMatchingInterface(nextHopIpAddress);
        if (egressInterface == null) {
            log.warn("No outgoing interface found for {}",
                    nextHopIpAddress);
            return null;
        }

        // Generate the intent itself
        Set<ConnectPoint> ingressPorts = new HashSet<>();
        ConnectPoint egressPort = egressInterface.connectPoint();
        log.debug("Generating intent for prefix {}, next hop mac {}",
                prefix, nextHopMacAddress);

        for (Interface intf : interfaceService.getInterfaces()) {
            // TODO this should be only peering interfaces
            if (!intf.connectPoint().equals(egressInterface.connectPoint())) {
                ConnectPoint srcPort = intf.connectPoint();
                ingressPorts.add(srcPort);
            }
        }

        // Match the destination IP prefix at the first hop
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (prefix.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(prefix);
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchIPv6Dst(prefix);
        }

        // Rewrite the destination MAC address
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setEthDst(nextHopMacAddress);
        if (!egressInterface.vlan().equals(VlanId.NONE)) {
            treatment.setVlanId(egressInterface.vlan());
            // If we set VLAN ID, we have to make sure a VLAN tag exists.
            // TODO support no VLAN -> VLAN routing
            selector.matchVlanId(VlanId.ANY);
        }

        int priority =
                prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;
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

}
