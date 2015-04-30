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
package org.onosproject.virtualbng;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a virtual Broadband Network Gateway (BNG) application. It mainly
 * has 3 functions:
 * (1) assigns and replies a public IP address to a REST request with a private
 * IP address
 * (2) maintains the mapping from the private IP address to the public IP address
 * (3) installs point to point intents for the host configured with private IP
 * address to access Internet
 */
@Component(immediate = true)
@Service
public class VbngManager implements VbngService {

    private static final String APP_NAME = "org.onosproject.virtualbng";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VbngConfigurationService vbngConfigurationService;

    private ApplicationId appId;
    private Map<IpAddress, PointToPointIntent> p2pIntentsFromHost;
    private Map<IpAddress, PointToPointIntent> p2pIntentsToHost;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);
        p2pIntentsFromHost = new ConcurrentHashMap<>();
        p2pIntentsToHost = new ConcurrentHashMap<>();
        log.info("vBNG Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("vBNG Stopped");
    }

    @Override
    public IpAddress createVbng(IpAddress privateIpAddress) {

        IpAddress nextHopIpAddress =
                vbngConfigurationService.getNextHopIpAddress();
        if (nextHopIpAddress == null) {
            log.info("Did not find next hop IP address");
            return null;
        }

        IpAddress publicIpAddress =
                vbngConfigurationService.getAvailablePublicIpAddress(
                                                     privateIpAddress);
        if (publicIpAddress == null) {
            log.info("Did not find an available public IP address to use.");
            return null;
        }
        log.info("Private IP to Public IP mapping: {} --> {}",
                 privateIpAddress, publicIpAddress);

        // Setup paths between the host configured with private IP and
        // next hop
        setupForwardingPaths(privateIpAddress, publicIpAddress,
                             nextHopIpAddress);
        return publicIpAddress;
    }

    /**
     * Sets up forwarding paths in both two directions between host configured
     * with private IP and next hop.
     *
     * @param privateIp the private IP address of a local host
     * @param publicIp the public IP address assigned for the private IP address
     * @param nextHopIpAddress the next hop IP address outside local SDN network
     */
    private void setupForwardingPaths(IpAddress privateIp, IpAddress publicIp,
                                      IpAddress nextHopIpAddress) {
        checkNotNull(privateIp);
        checkNotNull(publicIp);
        checkNotNull(nextHopIpAddress);

        // If there are already intents for private IP address in the system,
        // we will do nothing and directly return.
        if (p2pIntentsFromHost.containsKey(privateIp)
                && p2pIntentsToHost.containsKey(privateIp)) {
            return;
        }

        Host localHost = null;
        Host nextHopHost = null;
        if (!hostService.getHostsByIp(nextHopIpAddress).isEmpty()) {
            nextHopHost = hostService.getHostsByIp(nextHopIpAddress)
                    .iterator().next();
        } else {
            // TODO to write a new thread to install intents after ONOS
            // discovers the next hop host
            hostService.startMonitoringIp(nextHopIpAddress);
            if (hostService.getHostsByIp(privateIp).isEmpty()) {
                hostService.startMonitoringIp(privateIp);
            }
            return;
        }

        if (!hostService.getHostsByIp(privateIp).isEmpty()) {
            localHost =
                    hostService.getHostsByIp(privateIp).iterator().next();
        } else {
            // TODO to write a new thread to install intents after ONOS
            // discovers the next hop host
            hostService.startMonitoringIp(privateIp);
            return;
        }

        ConnectPoint nextHopConnectPoint =
                new ConnectPoint(nextHopHost.location().elementId(),
                                 nextHopHost.location().port());
        ConnectPoint localHostConnectPoint =
                new ConnectPoint(localHost.location().elementId(),
                                 localHost.location().port());

        // Generate and install intent for traffic from host configured with
        // private IP
        if (!p2pIntentsFromHost.containsKey(privateIp)) {
            PointToPointIntent toNextHopIntent
                    = srcMatchIntentGenerator(privateIp,
                                              publicIp,
                                              nextHopHost.mac(),
                                              nextHopConnectPoint,
                                              localHostConnectPoint
                                              );
            p2pIntentsFromHost.put(privateIp, toNextHopIntent);
            intentService.submit(toNextHopIntent);
        }

        // Generate and install intent for traffic to host configured with
        // private IP
        if (!p2pIntentsToHost.containsKey(privateIp)) {
            PointToPointIntent toLocalHostIntent
                    = dstMatchIntentGenerator(publicIp,
                                              privateIp,
                                              localHost.mac(),
                                              localHostConnectPoint,
                                              nextHopConnectPoint);
            p2pIntentsToHost.put(nextHopIpAddress, toLocalHostIntent);
            intentService.submit(toLocalHostIntent);
        }

        return;
    }

    /**
     * PointToPointIntent Generator.
     * <p>
     * The intent will match the source IP address in packet, rewrite the
     * source IP address, and rewrite the destination MAC address.
     * </p>
     *
     * @param srcIpAddress the source IP address in packet to match
     * @param newSrcIpAddress the new source IP address to set
     * @param dstMacAddress the destination MAC address to set
     * @param dstConnectPoint the egress point
     * @param srcConnectPoint the ingress point
     * @return a PointToPointIntent
     */
    private PointToPointIntent srcMatchIntentGenerator(
                                             IpAddress srcIpAddress,
                                             IpAddress newSrcIpAddress,
                                             MacAddress dstMacAddress,
                                             ConnectPoint dstConnectPoint,
                                             ConnectPoint srcConnectPoint) {
        checkNotNull(srcIpAddress);
        checkNotNull(newSrcIpAddress);
        checkNotNull(dstMacAddress);
        checkNotNull(dstConnectPoint);
        checkNotNull(srcConnectPoint);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPSrc(IpPrefix.valueOf(srcIpAddress,
                                             IpPrefix.MAX_INET_MASK_LENGTH));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setEthDst(dstMacAddress);
        treatment.setIpSrc(newSrcIpAddress);

        Key key = Key.of(srcIpAddress.toString() + "MatchSrc", appId);
        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .egressPoint(dstConnectPoint)
                .ingressPoint(srcConnectPoint)
                .build();

        log.info("Generated a PointToPointIntent for traffic from local host "
                + ": {}", intent);
        return intent;
    }

    /**
     * PointToPointIntent Generator.
     * <p>
     * The intent will match the destination IP address in packet, rewrite the
     * destination IP address, and rewrite the destination MAC address.
     * </p>
     *
     * @param dstIpAddress the destination IP address in packet to match
     * @param newDstIpAddress the new destination IP address to set
     * @param dstMacAddress the destination MAC address to set
     * @param dstConnectPoint the egress point
     * @param srcConnectPoint the ingress point
     * @return a PointToPointIntent
     */
    private PointToPointIntent dstMatchIntentGenerator(
                                                IpAddress dstIpAddress,
                                                IpAddress newDstIpAddress,
                                                MacAddress dstMacAddress,
                                                ConnectPoint dstConnectPoint,
                                                ConnectPoint srcConnectPoint) {
        checkNotNull(dstIpAddress);
        checkNotNull(newDstIpAddress);
        checkNotNull(dstMacAddress);
        checkNotNull(dstConnectPoint);
        checkNotNull(srcConnectPoint);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPDst(IpPrefix.valueOf(dstIpAddress,
                                             IpPrefix.MAX_INET_MASK_LENGTH));

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.setEthDst(dstMacAddress);
        treatment.setIpDst(newDstIpAddress);

        Key key = Key.of(newDstIpAddress.toString() + "MatchDst", appId);
        PointToPointIntent intent = PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector.build())
                .treatment(treatment.build())
                .egressPoint(dstConnectPoint)
                .ingressPoint(srcConnectPoint)
                .build();
        log.info("Generated a PointToPointIntent for traffic to local host "
                + ": {}", intent);

        return intent;
    }


}
