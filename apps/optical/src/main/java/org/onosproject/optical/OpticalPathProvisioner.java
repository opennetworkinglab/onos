/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.optical;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * OpticalPathProvisioner listens for event notifications from the Intent F/W.
 * It generates one or more opticalConnectivityIntent(s) and submits (or withdraws) to Intent F/W
 * for adding/releasing capacity at the packet layer.
 *
 * @deprecated in Goldeneye (1.6.0)
 */

@Deprecated
@Component(immediate = true)
public class OpticalPathProvisioner {

    protected static final Logger log = LoggerFactory
            .getLogger(OpticalPathProvisioner.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    private final InternalOpticalPathProvisioner pathProvisioner = new InternalOpticalPathProvisioner();

    @Activate
    protected void activate() {
        deviceService = opticalView(deviceService);
        intentService.addListener(pathProvisioner);
        appId = coreService.registerApplication("org.onosproject.optical");
        initOpticalPorts();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        intentService.removeListener(pathProvisioner);
        log.info("Stopped");
    }

    /**
     * Initialize availability of optical ports.
     */
    private void initOpticalPorts() {
        // TODO: check for existing optical intents
        return;
    }

    public class InternalOpticalPathProvisioner implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            switch (event.type()) {
                case INSTALL_REQ:
                    break;
                case INSTALLED:
                    break;
                case FAILED:
                    log.info("Intent {} failed, calling optical path provisioning app.", event.subject());
                    setupLightpath(event.subject());
                    break;
                default:
                    break;
            }
        }

        private void setupLightpath(Intent intent) {
            checkNotNull(intent);

            // TODO change the coordination approach between packet intents and optical intents
            // Low speed LLDP may cause multiple calls which are not expected

            if (intentService.getIntentState(intent.key()) != IntentState.FAILED) {
                return;
            }

            // Get source and destination based on intent type
            ConnectPoint src;
            ConnectPoint dst;
            if (intent instanceof HostToHostIntent) {
                HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;

                Host one = hostService.getHost(hostToHostIntent.one());
                Host two = hostService.getHost(hostToHostIntent.two());

                checkNotNull(one);
                checkNotNull(two);

                src = one.location();
                dst = two.location();
            } else if (intent instanceof PointToPointIntent) {
                PointToPointIntent p2pIntent = (PointToPointIntent) intent;

                src = p2pIntent.ingressPoint();
                dst = p2pIntent.egressPoint();
            } else {
                return;
            }

            if (src == null || dst == null) {
                return;
            }

            // Ignore if we're not the master for the intent's origin device
            NodeId localNode = clusterService.getLocalNode().id();
            NodeId sourceMaster = mastershipService.getMasterFor(src.deviceId());
            if (!localNode.equals(sourceMaster)) {
                return;
            }

            // Generate optical connectivity intents
            List<Intent> intents = getOpticalIntents(src, dst);

            // Submit the intents
            for (Intent i : intents) {
                intentService.submit(i);
                log.debug("Submitted an intent: {}", i);
            }
        }

        /**
         * Returns list of cross connection points of missing optical path sections.
         *
         * Scans the given multi-layer path and looks for sections that use cross connect links.
         * The ingress and egress points in the optical layer are returned in a list.
         *
         * @param path the multi-layer path
         * @return list of cross connection points on the optical layer
         */
        private List<ConnectPoint> getCrossConnectPoints(Path path) {
            boolean scanning = false;
            List<ConnectPoint> connectPoints = new LinkedList<>();

            for (Link link : path.links()) {
                if (!isCrossConnectLink(link)) {
                    continue;
                }

                if (scanning) {
                    connectPoints.add(checkNotNull(link.src()));
                    scanning = false;
                } else {
                    connectPoints.add(checkNotNull(link.dst()));
                    scanning = true;
                }
            }

            return connectPoints;
        }

        /**
         * Checks if cross connect points are of same type.
         *
         * @param crossConnectPoints list of cross connection points
         * @return true if cross connect point pairs are of same type, false otherwise
         */
        private boolean checkCrossConnectPoints(List<ConnectPoint> crossConnectPoints) {
            checkArgument(crossConnectPoints.size() % 2 == 0);

            Iterator<ConnectPoint> itr = crossConnectPoints.iterator();

            while (itr.hasNext()) {
                // checkArgument at start ensures we'll always have pairs of connect points
                ConnectPoint src = itr.next();
                ConnectPoint dst = itr.next();

                Device.Type srcType = deviceService.getDevice(src.deviceId()).type();
                Device.Type dstType = deviceService.getDevice(dst.deviceId()).type();

                // Only support connections between identical port types
                if (srcType != dstType) {
                    log.warn("Unsupported mix of cross connect points");
                    return false;
                }
            }

            return true;
        }

        /**
         * Scans the list of cross connection points and returns a list of optical connectivity intents.
         *
         * @param crossConnectPoints list of cross connection points
         * @return list of optical connectivity intents
         */
        private List<Intent> getIntents(List<ConnectPoint> crossConnectPoints) {
            checkArgument(crossConnectPoints.size() % 2 == 0);

            List<Intent> intents = new LinkedList<>();
            Iterator<ConnectPoint> itr = crossConnectPoints.iterator();

            while (itr.hasNext()) {
                // checkArgument at start ensures we'll always have pairs of connect points
                ConnectPoint src = itr.next();
                ConnectPoint dst = itr.next();

                Port srcPort = deviceService.getPort(src.deviceId(), src.port());
                Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());

                if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
                    // Create OTN circuit
                    Intent circuitIntent = OpticalCircuitIntent.builder()
                            .appId(appId)
                            .src(src)
                            .dst(dst)
                            .signalType(CltSignalType.CLT_10GBE)
                            .bidirectional(true)
                            .build();
                    intents.add(circuitIntent);
                } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
                    // Create lightpath
                    // FIXME: hardcoded ODU signal type
                    Intent opticalIntent = OpticalConnectivityIntent.builder()
                            .appId(appId)
                            .src(src)
                            .dst(dst)
                            .signalType(OduSignalType.ODU4)
                            .bidirectional(true)
                            .build();
                    intents.add(opticalIntent);
                } else {
                    log.warn("Unsupported cross connect point types {} {}", srcPort.type(), dstPort.type());
                    return Collections.emptyList();
                }
            }

            return intents;
        }

        /**
         * Returns list of optical connectivity intents needed to create connectivity
         * between ingress and egress.
         *
         * @param ingress the ingress connect point
         * @param egress the egress connect point
         * @return list of optical connectivity intents, empty list if no path was found
         */
        private List<Intent> getOpticalIntents(ConnectPoint ingress, ConnectPoint egress) {
            Set<Path> paths = pathService.getPaths(ingress.deviceId(),
                    egress.deviceId(),
                    new OpticalLinkWeight());

            if (paths.isEmpty()) {
                return Collections.emptyList();
            }

            // Search path with available cross connect points
            for (Path path : paths) {
                List<ConnectPoint> crossConnectPoints = getCrossConnectPoints(path);

                // Skip to next path if cross connect points are mismatched
                if (!checkCrossConnectPoints(crossConnectPoints)) {
                    continue;
                }

                return getIntents(crossConnectPoints);
            }

            log.warn("Unable to find multi-layer path.");
            return Collections.emptyList();
        }

        /**
         * Link weight function that emphasizes re-use of packet links.
         */
        private class OpticalLinkWeight implements LinkWeight {
            @Override
            public double weight(TopologyEdge edge) {
                // Ignore inactive links
                if (edge.link().state() == Link.State.INACTIVE) {
                    return -1;
                }

                // TODO: Ignore cross connect links with used ports

                // Transport links have highest weight
                if (edge.link().type() == Link.Type.OPTICAL) {
                    return 1000;
                }

                // Packet links
                return 1;
            }
        }

    }

    /**
     * Verifies if given device type is in packet layer, i.e., ROADM, OTN or ROADM_OTN device.
     *
     * @param type device type
     * @return true if in packet layer, false otherwise
     */
    private boolean isPacketLayer(Device.Type type) {
        return type == Device.Type.SWITCH || type == Device.Type.ROUTER || type == Device.Type.VIRTUAL;
    }

    /**
     * Verifies if given device type is in packet layer, i.e., switch or router device.
     *
     * @param type device type
     * @return true if in packet layer, false otherwise
     */
    private boolean isTransportLayer(Device.Type type) {
        return type == Device.Type.ROADM || type == Device.Type.OTN || type == Device.Type.ROADM_OTN;
    }

    /**
     * Verifies if given link forms a cross-connection between packet and optical layer.
     *
     * @param link the link
     * @return true if the link is a cross-connect link, false otherwise
     */
    private boolean isCrossConnectLink(Link link) {
        if (link.type() != Link.Type.OPTICAL) {
            return false;
        }

        Device.Type src = deviceService.getDevice(link.src().deviceId()).type();
        Device.Type dst = deviceService.getDevice(link.dst().deviceId()).type();

        return src != dst &&
                ((isPacketLayer(src) && isTransportLayer(dst)) || (isPacketLayer(dst) && isTransportLayer(src)));
    }

}
