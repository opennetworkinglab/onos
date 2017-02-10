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

package org.onosproject.reactive.routing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.intentsync.IntentSynchronizationService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.constraint.PartialFailureConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * FIB component for reactive routing intents.
 */
public class ReactiveRoutingFib implements IntentRequestListener {

    private static final int PRIORITY_OFFSET = 100;
    private static final int PRIORITY_MULTIPLIER = 5;
    protected static final ImmutableList<Constraint> CONSTRAINTS
            = ImmutableList.of(new PartialFailureConstraint());

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationId appId;
    private final HostService hostService;
    private final InterfaceService interfaceService;
    private final IntentSynchronizationService intentSynchronizer;

    private final Map<IpPrefix, MultiPointToSinglePointIntent> routeIntents;

    /**
     * Class constructor.
     *
     * @param appId application ID to use to generate intents
     * @param hostService host service
     * @param interfaceService interface service
     * @param intentSynchronizer intent synchronization service
     */
    public ReactiveRoutingFib(ApplicationId appId, HostService hostService,
                              InterfaceService interfaceService,
                              IntentSynchronizationService intentSynchronizer) {
        this.appId = appId;
        this.hostService = hostService;
        this.interfaceService = interfaceService;
        this.intentSynchronizer = intentSynchronizer;

        routeIntents = Maps.newConcurrentMap();
    }

    @Override
    public void setUpConnectivityInternetToHost(IpAddress hostIpAddress) {
        checkNotNull(hostIpAddress);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        if (hostIpAddress.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
        }

        // Match the destination IP prefix at the first hop
        IpPrefix ipPrefix = hostIpAddress.toIpPrefix();
        selector.matchIPDst(ipPrefix);

        // Rewrite the destination MAC address
        MacAddress hostMac = null;
        ConnectPoint egressPoint = null;
        for (Host host : hostService.getHostsByIp(hostIpAddress)) {
            if (host.mac() != null) {
                hostMac = host.mac();
                egressPoint = host.location();
                break;
            }
        }
        if (hostMac == null) {
            hostService.startMonitoringIp(hostIpAddress);
            return;
        }

        TrafficTreatment.Builder treatment =
                DefaultTrafficTreatment.builder().setEthDst(hostMac);
        Key key = Key.of(ipPrefix.toString(), appId);
        int priority = ipPrefix.prefixLength() * PRIORITY_MULTIPLIER
                + PRIORITY_OFFSET;

        Set<ConnectPoint> interfaceConnectPoints =
                interfaceService.getInterfaces().stream()
                .map(intf -> intf.connectPoint()).collect(Collectors.toSet());

        if (interfaceConnectPoints.isEmpty()) {
            log.error("The interface connect points are empty!");
            return;
        }

        Set<ConnectPoint> ingressPoints = new HashSet<>();

        for (ConnectPoint connectPoint : interfaceConnectPoints) {
            if (!connectPoint.equals(egressPoint)) {
                ingressPoints.add(connectPoint);
            }
        }

        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(appId)
                        .key(key)
                        .selector(selector.build())
                        .treatment(treatment.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(egressPoint)
                        .priority(priority)
                        .constraints(CONSTRAINTS)
                        .build();

        log.trace("Generates ConnectivityInternetToHost intent {}", intent);
        submitReactiveIntent(ipPrefix, intent);
    }

    @Override
    public void setUpConnectivityHostToInternet(IpAddress hostIp, IpPrefix prefix,
                                                IpAddress nextHopIpAddress) {
        // Find the attachment point (egress interface) of the next hop
        Interface egressInterface =
                interfaceService.getMatchingInterface(nextHopIpAddress);
        if (egressInterface == null) {
            log.warn("No outgoing interface found for {}",
                    nextHopIpAddress);
            return;
        }

        Set<Host> hosts = hostService.getHostsByIp(nextHopIpAddress);
        if (hosts.isEmpty()) {
            log.warn("No host found for next hop IP address");
            return;
        }
        MacAddress nextHopMacAddress = null;
        for (Host host : hosts) {
            nextHopMacAddress = host.mac();
            break;
        }

        hosts = hostService.getHostsByIp(hostIp);
        if (hosts.isEmpty()) {
            log.warn("No host found for host IP address");
            return;
        }
        Host host = hosts.stream().findFirst().get();
        ConnectPoint ingressPoint = host.location();

        // Generate the intent itself
        ConnectPoint egressPort = egressInterface.connectPoint();
        log.debug("Generating intent for prefix {}, next hop mac {}",
                prefix, nextHopMacAddress);

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

        int priority = prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;
        Key key = Key.of(prefix.toString() + "-reactive", appId);
        MultiPointToSinglePointIntent intent = MultiPointToSinglePointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .ingressPoints(Collections.singleton(ingressPoint))
                .egressPoint(egressPort)
                .priority(priority)
                .constraints(CONSTRAINTS)
                .build();

        submitReactiveIntent(prefix, intent);
    }

    @Override
    public void setUpConnectivityHostToHost(IpAddress dstIpAddress,
                                            IpAddress srcIpAddress,
                                            MacAddress srcMacAddress,
                                            ConnectPoint srcConnectPoint) {
        checkNotNull(dstIpAddress);
        checkNotNull(srcIpAddress);
        checkNotNull(srcMacAddress);
        checkNotNull(srcConnectPoint);

        IpPrefix srcIpPrefix = srcIpAddress.toIpPrefix();
        IpPrefix dstIpPrefix = dstIpAddress.toIpPrefix();
        ConnectPoint dstConnectPoint = null;
        MacAddress dstMacAddress = null;

        for (Host host : hostService.getHostsByIp(dstIpAddress)) {
            if (host.mac() != null) {
                dstMacAddress = host.mac();
                dstConnectPoint = host.location();
                break;
            }
        }
        if (dstMacAddress == null) {
            hostService.startMonitoringIp(dstIpAddress);
            return;
        }

        //
        // Handle intent from source host to destination host
        //
        MultiPointToSinglePointIntent srcToDstIntent =
                hostToHostIntentGenerator(dstIpAddress, dstConnectPoint,
                        dstMacAddress, srcConnectPoint);
        submitReactiveIntent(dstIpPrefix, srcToDstIntent);

        //
        // Handle intent from destination host to source host
        //

        // Since we proactively handle the intent from destination host to
        // source host, we should check whether there is an exiting intent
        // first.
        if (mp2pIntentExists(srcIpPrefix)) {
            updateExistingMp2pIntent(srcIpPrefix, dstConnectPoint);
            return;
        } else {
            // There is no existing intent, create a new one.
            MultiPointToSinglePointIntent dstToSrcIntent =
                    hostToHostIntentGenerator(srcIpAddress, srcConnectPoint,
                            srcMacAddress, dstConnectPoint);
            submitReactiveIntent(srcIpPrefix, dstToSrcIntent);
        }
    }

    /**
     * Generates MultiPointToSinglePointIntent for both source host and
     * destination host located in local SDN network.
     *
     * @param dstIpAddress the destination IP address
     * @param dstConnectPoint the destination host connect point
     * @param dstMacAddress the MAC address of destination host
     * @param srcConnectPoint the connect point where packet-in from
     * @return the generated MultiPointToSinglePointIntent
     */
    private MultiPointToSinglePointIntent hostToHostIntentGenerator(
            IpAddress dstIpAddress,
            ConnectPoint dstConnectPoint,
            MacAddress dstMacAddress,
            ConnectPoint srcConnectPoint) {
        checkNotNull(dstIpAddress);
        checkNotNull(dstConnectPoint);
        checkNotNull(dstMacAddress);
        checkNotNull(srcConnectPoint);

        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ingressPoints.add(srcConnectPoint);
        IpPrefix dstIpPrefix = dstIpAddress.toIpPrefix();

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        if (dstIpAddress.isIp4()) {
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(dstIpPrefix);
        } else {
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchIPv6Dst(dstIpPrefix);
        }

        // Rewrite the destination MAC address
        TrafficTreatment.Builder treatment =
                DefaultTrafficTreatment.builder().setEthDst(dstMacAddress);

        Key key = Key.of(dstIpPrefix.toString(), appId);
        int priority = dstIpPrefix.prefixLength() * PRIORITY_MULTIPLIER
                + PRIORITY_OFFSET;
        MultiPointToSinglePointIntent intent =
                MultiPointToSinglePointIntent.builder()
                        .appId(appId)
                        .key(key)
                        .selector(selector.build())
                        .treatment(treatment.build())
                        .ingressPoints(ingressPoints)
                        .egressPoint(dstConnectPoint)
                        .priority(priority)
                        .constraints(CONSTRAINTS)
                        .build();

        log.trace("Generates ConnectivityHostToHost = {} ", intent);
        return intent;
    }

    @Override
    public void updateExistingMp2pIntent(IpPrefix ipPrefix,
                                         ConnectPoint ingressConnectPoint) {
        checkNotNull(ipPrefix);
        checkNotNull(ingressConnectPoint);

        MultiPointToSinglePointIntent existingIntent =
                getExistingMp2pIntent(ipPrefix);
        if (existingIntent != null) {
            Set<ConnectPoint> ingressPoints = existingIntent.ingressPoints();
            // Add host connect point into ingressPoints of the existing intent
            if (ingressPoints.add(ingressConnectPoint)) {
                MultiPointToSinglePointIntent updatedMp2pIntent =
                        MultiPointToSinglePointIntent.builder()
                                .appId(appId)
                                .key(existingIntent.key())
                                .selector(existingIntent.selector())
                                .treatment(existingIntent.treatment())
                                .ingressPoints(ingressPoints)
                                .egressPoint(existingIntent.egressPoint())
                                .priority(existingIntent.priority())
                                .constraints(CONSTRAINTS)
                                .build();

                log.trace("Update an existing MultiPointToSinglePointIntent "
                        + "to new intent = {} ", updatedMp2pIntent);
                submitReactiveIntent(ipPrefix, updatedMp2pIntent);
            }
            // If adding ingressConnectPoint to ingressPoints failed, it
            // because between the time interval from checking existing intent
            // to generating new intent, onos updated this intent due to other
            // packet-in and the new intent also includes the
            // ingressConnectPoint. This will not affect reactive routing.
        }
    }

    /**
     * Submits a reactive intent to the intent synchronizer.
     *
     * @param ipPrefix IP prefix of the intent
     * @param intent intent to submit
     */
    void submitReactiveIntent(IpPrefix ipPrefix, MultiPointToSinglePointIntent intent) {
        routeIntents.put(ipPrefix, intent);

        intentSynchronizer.submit(intent);
    }

    /**
     * Gets the existing MultiPointToSinglePointIntent from memory for a given
     * IP prefix.
     *
     * @param ipPrefix the IP prefix used to find MultiPointToSinglePointIntent
     * @return the MultiPointToSinglePointIntent if found, otherwise null
     */
    private MultiPointToSinglePointIntent getExistingMp2pIntent(IpPrefix ipPrefix) {
        checkNotNull(ipPrefix);
        return routeIntents.get(ipPrefix);
    }

    @Override
    public boolean mp2pIntentExists(IpPrefix ipPrefix) {
        checkNotNull(ipPrefix);
        return routeIntents.get(ipPrefix) != null;
    }
}
