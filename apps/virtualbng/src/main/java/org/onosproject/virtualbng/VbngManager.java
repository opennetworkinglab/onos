/*
 * Copyright 2015-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a virtual Broadband Network Gateway (BNG) application. It mainly
 * has 3 functions:
 * (1) assigns and replies a public IP address to a REST request with a private
 * IP address
 * (2) maintains the mapping from the private IP address to the public IP address
 * (3) installs point to point intents for the host configured with private IP
 * address to access Internet
 */
@Component(immediate = true, service = VbngService.class)
public class VbngManager implements VbngService {

    private static final String APP_NAME = "org.onosproject.virtualbng";
    private static final String VBNG_MAP_NAME = "vbng_mapping";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VbngConfigurationService vbngConfigurationService;

    private ApplicationId appId;
    private Map<IpAddress, PointToPointIntent> p2pIntentsFromHost;
    private Map<IpAddress, PointToPointIntent> p2pIntentsToHost;

    // This map stores the mapping from the private IP addresses to VcpeHost.
    // The IP addresses in this map are all the private IP addresses we failed
    // to create vBNGs due to the next hop host was not in ONOS.
    private Map<IpAddress, VcpeHost> privateIpAddressMap;

    // Store the mapping from hostname to connect point
    private Map<String, ConnectPoint> nodeToPort;

    private HostListener hostListener;
    private IpAddress nextHopIpAddress;

    private static final DeviceId FABRIC_DEVICE_ID =
            DeviceId.deviceId("of:8f0e486e73000187");

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);
        p2pIntentsFromHost = new ConcurrentHashMap<>();
        p2pIntentsToHost = new ConcurrentHashMap<>();
        privateIpAddressMap = new ConcurrentHashMap<>();

        nextHopIpAddress = vbngConfigurationService.getNextHopIpAddress();
        nodeToPort = vbngConfigurationService.getNodeToPort();
        hostListener = new InternalHostListener();
        hostService.addListener(hostListener);

        log.info("vBNG Started");

        // Recover the status before vBNG restarts
        statusRecovery();
    }

    @Deactivate
    public void deactivate() {
        hostService.removeListener(hostListener);
        log.info("vBNG Stopped");
    }

    /**
     * Recovers from XOS record. Re-sets up the mapping between private IP
     * address and public IP address, re-calculates intents and re-installs
     * those intents.
     */
    private void statusRecovery() {
        log.info("vBNG starts to recover from XOS record......");
        ObjectNode map;
        try {
            RestClient restClient =
                    new RestClient(vbngConfigurationService.getXosIpAddress(),
                            vbngConfigurationService.getXosRestPort());
            map = restClient.getRest();
        } catch (Exception e) {
            log.warn("Could not contact XOS {}", e.getMessage());
            return;
        }
        if (map == null) {
            log.info("Stop to recover vBNG status due to the vBNG map "
                    + "is null!");
            return;
        }

        log.info("Get record from XOS: {}", map);

        ArrayNode array = (ArrayNode) map.get(VBNG_MAP_NAME);
        Iterator<JsonNode> entries = array.elements();
        while (entries.hasNext()) {
            ObjectNode entry = (ObjectNode) entries.next();

            IpAddress hostIpAdddress =
                    IpAddress.valueOf(entry.get("private_ip").asText());
            IpAddress publicIpAddress =
                    IpAddress.valueOf(entry.get("routeable_subnet").asText());
            MacAddress macAddress =
                    MacAddress.valueOf(entry.get("mac").asText());
            String hostName = entry.get("hostname").asText();

            // Create vBNG
            createVbng(hostIpAdddress, publicIpAddress, macAddress, hostName);

        }
    }

    /**
     * Creates a new vBNG.
     *
     * @param privateIpAddress a private IP address
     * @param publicIpAddress the public IP address for the private IP address
     * @param hostMacAddress the MAC address for the private IP address
     * @param hostName the host name for the private IP address
     */
    private void createVbng(IpAddress privateIpAddress,
                            IpAddress publicIpAddress,
                            MacAddress hostMacAddress,
                            String hostName) {
        boolean result = vbngConfigurationService
                .assignSpecifiedPublicIp(publicIpAddress, privateIpAddress);
        if (!result) {
            log.info("Assign public IP address {} for private IP address {} "
                    + "failed!", publicIpAddress, privateIpAddress);
            log.info("Failed to create vBNG for private IP address {}",
                     privateIpAddress);
            return;
        }
        log.info("[ADD] Private IP to Public IP mapping: {} --> {}",
                 privateIpAddress, publicIpAddress);

        // Setup paths between the host configured with private IP and
        // next hop
        if (!setupForwardingPaths(privateIpAddress, publicIpAddress,
                                  hostMacAddress, hostName)) {
            privateIpAddressMap.put(privateIpAddress,
                                    new VcpeHost(hostMacAddress, hostName));
        }
    }

    @Override
    public IpAddress createVbng(IpAddress privateIpAddress,
                                MacAddress hostMacAddress,
                                String hostName) {

        IpAddress publicIpAddress =
                vbngConfigurationService.getAvailablePublicIpAddress(
                                                     privateIpAddress);
        if (publicIpAddress == null) {
            log.info("Did not find an available public IP address to use.");
            return null;
        }
        log.info("[ADD] Private IP to Public IP mapping: {} --> {}",
                 privateIpAddress, publicIpAddress);

        // Setup paths between the host configured with private IP and
        // next hop
        if (!setupForwardingPaths(privateIpAddress, publicIpAddress,
                                  hostMacAddress, hostName)) {
            privateIpAddressMap.put(privateIpAddress,
                                    new VcpeHost(hostMacAddress, hostName));
        }
        return publicIpAddress;
    }

    @Override
    public IpAddress deleteVbng(IpAddress privateIpAddress) {
        // Recycle the public IP address assigned to this private IP address.
        // Recycling will also delete the mapping entry from the private IP
        // address to public IP address.
        IpAddress assignedPublicIpAddress = vbngConfigurationService
                .recycleAssignedPublicIpAddress(privateIpAddress);
        if (assignedPublicIpAddress == null) {
            return null;
        }

        // Remove the private IP address from privateIpAddressMap
        privateIpAddressMap.remove(privateIpAddress);

        // Remove intents
        removeForwardingPaths(privateIpAddress);

        return assignedPublicIpAddress;
    }

    /**
     * Removes the forwarding paths in both two directions between host
     * configured with private IP and next hop.
     *
     * @param privateIp the private IP address of a local host
     */
    private void removeForwardingPaths(IpAddress privateIp) {
        PointToPointIntent toNextHopIntent =
                p2pIntentsFromHost.remove(privateIp);
        if (toNextHopIntent != null) {
            intentService.withdraw(toNextHopIntent);
            //intentService.purge(toNextHopIntent);
        }
        PointToPointIntent toLocalHostIntent =
                p2pIntentsToHost.remove(privateIp);
        if (toLocalHostIntent != null) {
            intentService.withdraw(toLocalHostIntent);
            //intentService.purge(toLocalHostIntent);
        }
    }

    /**
     * Sets up forwarding paths in both two directions between host configured
     * with private IP and next hop.
     *
     * @param privateIp the private IP address of a local host
     * @param publicIp the public IP address assigned for the private IP address
     * @param hostMacAddress the MAC address for the IP address
     * @param hostName the host name for the IP address
     */
    private boolean setupForwardingPaths(IpAddress privateIp,
                                         IpAddress publicIp,
                                         MacAddress hostMacAddress,
                                         String hostName) {
        checkNotNull(privateIp);
        checkNotNull(publicIp);
        checkNotNull(hostMacAddress);
        checkNotNull(hostName);

        if (nextHopIpAddress == null) {
            log.warn("Did not find next hop IP address");
            return false;
        }

        // If there are already intents for private IP address in the system,
        // we will do nothing and directly return.
        if (p2pIntentsFromHost.containsKey(privateIp)
                && p2pIntentsToHost.containsKey(privateIp)) {
            return true;
        }

        Host nextHopHost = null;
        if (!hostService.getHostsByIp(nextHopIpAddress).isEmpty()) {
            nextHopHost = hostService.getHostsByIp(nextHopIpAddress)
                    .iterator().next();
        } else {
            hostService.startMonitoringIp(nextHopIpAddress);
            if (hostService.getHostsByIp(privateIp).isEmpty()) {
                hostService.startMonitoringIp(privateIp);
            }
            return false;
        }

        ConnectPoint nextHopConnectPoint =
                new ConnectPoint(nextHopHost.location().elementId(),
                                 nextHopHost.location().port());
        ConnectPoint localHostConnectPoint = nodeToPort.get(hostName);

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
                                              hostMacAddress,
                                              localHostConnectPoint,
                                              nextHopConnectPoint);
            p2pIntentsToHost.put(privateIp, toLocalHostIntent);
            intentService.submit(toLocalHostIntent);
        }

        return true;
    }

    /**
     * Listener for host events.
     */
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            log.debug("Received HostEvent {}", event);

            Host host = event.subject();
            if (event.type() != HostEvent.Type.HOST_ADDED) {
                return;
            }

            for (IpAddress ipAddress: host.ipAddresses()) {
                // The POST method from XOS gives us MAC and host name, so we
                // do not need to do anything after receive a vCPE host event
                // for now.
                /*if (privateIpAddressSet.contains(ipAddress)) {
                    createVbngAgain(ipAddress);
                }*/

                if (nextHopIpAddress != null &&
                        ipAddress.equals(nextHopIpAddress)) {

                    for (Entry<IpAddress, VcpeHost> entry:
                        privateIpAddressMap.entrySet()) {
                        createVbngAgain(entry.getKey());
                    }

                }
            }
        }
    }

    /**
     * Tries to create vBNG again after receiving a host event if the IP
     * address of the host is the next hop IP address.
     *
     * @param privateIpAddress the private IP address
     */
    private void createVbngAgain(IpAddress privateIpAddress) {
        IpAddress publicIpAddress = vbngConfigurationService
                .getAssignedPublicIpAddress(privateIpAddress);
        if (publicIpAddress == null) {
            // We only need to handle the private IP addresses for which we
            // already returned the REST replies with assigned public IP
            // addresses. If a private IP addresses does not have an assigned
            // public IP address, we should not get it an available public IP
            // address here, and we should delete it in the unhandled private
            // IP address map.
            privateIpAddressMap.remove(privateIpAddress);
            return;
        }
        VcpeHost vcpeHost = privateIpAddressMap.get(privateIpAddress);
        if (setupForwardingPaths(privateIpAddress, publicIpAddress,
                                 vcpeHost.macAddress, vcpeHost.hostName)) {
            privateIpAddressMap.remove(privateIpAddress);
        }
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
                .filteredEgressPoint(new FilteredConnectPoint(dstConnectPoint))
                .filteredIngressPoint(new FilteredConnectPoint(srcConnectPoint))
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
                .filteredEgressPoint(new FilteredConnectPoint(dstConnectPoint))
                .filteredIngressPoint(new FilteredConnectPoint(srcConnectPoint))
                .build();
        log.info("Generated a PointToPointIntent for traffic to local host "
                + ": {}", intent);

        return intent;
    }

    /**
     * Constructor to store the a vCPE host info.
     */
    private class VcpeHost {
        MacAddress macAddress;
        String hostName;
        public VcpeHost(MacAddress macAddress, String hostName) {
            this.macAddress = macAddress;
            this.hostName = hostName;
        }
    }
}
