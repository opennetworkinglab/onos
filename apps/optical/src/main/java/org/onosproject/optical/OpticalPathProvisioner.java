/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.collect.Lists;
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.intent.IntentState.INSTALLED;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OpticalPathProvisioner listens for event notifications from the Intent F/W.
 * It generates one or more opticalConnectivityIntent(s) and submits (or withdraws) to Intent F/W
 * for adding/releasing capacity at the packet layer.
 */

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

    private ApplicationId appId;

    // TODO use a shared map for distributed operation
    private final Map<ConnectPoint, OpticalConnectivityIntent> inStatusTportMap =
            new ConcurrentHashMap<>();
    private final Map<ConnectPoint, OpticalConnectivityIntent> outStatusTportMap =
            new ConcurrentHashMap<>();

    private final Map<ConnectPoint, Map<ConnectPoint, Intent>> intentMap =
            new ConcurrentHashMap<>();

    private final InternalOpticalPathProvisioner pathProvisioner = new InternalOpticalPathProvisioner();

    @Activate
    protected void activate() {
        intentService.addListener(pathProvisioner);
        appId = coreService.registerApplication("org.onosproject.optical");
        initTport();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        intentService.removeListener(pathProvisioner);
        log.info("Stopped");
    }

    protected void initTport() {
        inStatusTportMap.clear();
        outStatusTportMap.clear();
        for (Intent intent : intentService.getIntents()) {
            if (intentService.getIntentState(intent.key()) == INSTALLED) {
                if (intent instanceof OpticalConnectivityIntent) {
                    inStatusTportMap.put(((OpticalConnectivityIntent) intent).getSrc(),
                            (OpticalConnectivityIntent) intent);
                    outStatusTportMap.put(((OpticalConnectivityIntent) intent).getDst(),
                            (OpticalConnectivityIntent) intent);
                }
            }
        }
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
                case WITHDRAWN:
                    log.info("Intent {} withdrawn.", event.subject());
                    //FIXME
                    //teardownLightpath(event.subject());
                    break;
                default:
                    break;
            }
        }

        private void reserveTport(Intent intent) {
            // TODO move to resourceManager
            if (intent instanceof OpticalConnectivityIntent) {
                OpticalConnectivityIntent opticalIntent =
                        (OpticalConnectivityIntent) intent;
                if (inStatusTportMap.containsKey(opticalIntent.getSrc()) ||
                        outStatusTportMap.containsKey(opticalIntent.getDst())) {
                    //TODO throw an exception, perhaps
                    log.warn("Overlapping reservation: {}", opticalIntent);
                }
                inStatusTportMap.put(opticalIntent.getSrc(), opticalIntent);
                outStatusTportMap.put(opticalIntent.getDst(), opticalIntent);
            }
        }

        /**
         * Registers an intent from src to dst.
         *
         * @param src source point
         * @param dst destination point
         * @param intent intent to be registered
         * @return true if intent has not been previously added, false otherwise
         */
        private boolean addIntent(ConnectPoint src, ConnectPoint dst, Intent intent) {
            Map<ConnectPoint, Intent> srcMap = intentMap.get(src);
            if (srcMap == null) {
                srcMap = new ConcurrentHashMap<>();
                intentMap.put(src, srcMap);
            }
            if (srcMap.containsKey(dst)) {
                return false;
            } else {
                srcMap.put(dst, intent);
                return true;
            }
        }

        private void setupLightpath(Intent intent) {
            // TODO change the coordination approach between packet intents and optical intents
            // Low speed LLDP may cause multiple calls which are not expected

            if (!IntentState.FAILED.equals(intentService.getIntentState(intent.key()))) {
                return;
            }

            NodeId localNode = clusterService.getLocalNode().id();

            List<Intent> intents = Lists.newArrayList();
            if (intent instanceof HostToHostIntent) {
                HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;

                Host one = hostService.getHost(hostToHostIntent.one());
                Host two = hostService.getHost(hostToHostIntent.two());
                if (one == null || two == null) {
                    return; //FIXME
                }

                // Ignore if we're not the master for the intent's origin device
                NodeId sourceMaster = mastershipService.getMasterFor(one.location().deviceId());
                if (!localNode.equals(sourceMaster)) {
                    return;
                }

                // provision both directions
                intents.addAll(getOpticalPath(one.location(), two.location()));
                // note: bi-directional intent is set up
                // HostToHost Intent requires symmetric path!
                //intents.addAll(getOpticalPath(two.location(), one.location()));
            } else if (intent instanceof PointToPointIntent) {
                PointToPointIntent p2pIntent = (PointToPointIntent) intent;

                // Ignore if we're not the master for the intent's origin device
                NodeId sourceMaster = mastershipService.getMasterFor(p2pIntent.ingressPoint().deviceId());
                if (!localNode.equals(sourceMaster)) {
                    return;
                }

                intents.addAll(getOpticalPath(p2pIntent.ingressPoint(), p2pIntent.egressPoint()));
            } else {
                log.info("Unsupported intent type: {}", intent.getClass());
            }

            // Create the intents
            for (Intent i : intents) {
                // TODO: don't allow duplicate intents between the same points for now
                //       we may want to allow this carefully in future to increase capacity
                if (i instanceof OpticalConnectivityIntent) {
                    OpticalConnectivityIntent oi = (OpticalConnectivityIntent) i;
                    if (addIntent(oi.getSrc(), oi.getDst(), oi)) {
                        intentService.submit(i);
                        reserveTport(i);
                    }
                } else {
                    log.warn("Invalid intent type: {} for {}", i.getClass(), i);
                }
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
            List<ConnectPoint> connectPoints = new LinkedList<ConnectPoint>();

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
         * Checks availability of cross connect points by verifying T port status.
         * TODO: refactor after rewriting OpticalConnectivityIntentCompiler
         *
         * @param crossConnectPoints list of cross connection points
         * @return true if all cross connect points are available, false otherwise
         */
        private boolean checkCrossConnectPoints(List<ConnectPoint> crossConnectPoints) {
            checkArgument(crossConnectPoints.size() % 2 == 0);

            Iterator<ConnectPoint> itr = crossConnectPoints.iterator();

            while (itr.hasNext()) {
                // checkArgument at start ensures we'll always have pairs of connect points
                ConnectPoint src = itr.next();
                ConnectPoint dst = itr.next();

                if (inStatusTportMap.get(src) != null || outStatusTportMap.get(dst) != null) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Scans the list of cross connection points and returns a list of optical connectivity intents
         * in both directions.
         *
         * @param crossConnectPoints list of cross connection points
         * @return list of optical connectivity intents
         */
        private List<Intent> getIntents(List<ConnectPoint> crossConnectPoints) {
            checkArgument(crossConnectPoints.size() % 2 == 0);

            List<Intent> intents = new LinkedList<Intent>();
            Iterator<ConnectPoint> itr = crossConnectPoints.iterator();

            while (itr.hasNext()) {
                // checkArgument at start ensures we'll always have pairs of connect points
                ConnectPoint src = itr.next();
                ConnectPoint dst = itr.next();

                // TODO: should have option for bidirectional OpticalConnectivityIntent
                Intent opticalIntent = OpticalConnectivityIntent.builder()
                        .appId(appId)
                        .src(src)
                        .dst(dst)
                        .build();
                Intent opticalIntentRev = OpticalConnectivityIntent.builder()
                        .appId(appId)
                        .src(dst)
                        .dst(src)
                        .build();
                intents.add(opticalIntent);
                intents.add(opticalIntentRev);
            }

            return intents;
        }

        private List<Intent> getOpticalPath(ConnectPoint ingress, ConnectPoint egress) {
            Set<Path> paths = pathService.getPaths(ingress.deviceId(),
                    egress.deviceId(),
                    new OpticalLinkWeight());

            if (paths.isEmpty()) {
                return Collections.emptyList();
            }

            List<Intent> connectionList = Lists.newArrayList();

            // Iterate over all paths until a suitable one has been found
            Iterator<Path> itrPath = paths.iterator();
            while (itrPath.hasNext()) {
                Path nextPath = itrPath.next();

                List<ConnectPoint> crossConnectPoints = getCrossConnectPoints(nextPath);

                // Skip to next path if not all connect points are available
                if (!checkCrossConnectPoints(crossConnectPoints)) {
                    continue;
                }

                return getIntents(crossConnectPoints);
            }

            return Collections.emptyList();
        }

        private void teardownLightpath(Intent intent) {
            /* FIXME this command doesn't make much sense. we need to define the semantics
            // TODO move to resourceManager
            if (intent instanceof OpticalConnectivityIntent) {
                inStatusTportMap.remove(((OpticalConnectivityIntent) intent).getSrc());
                outStatusTportMap.remove(((OpticalConnectivityIntent) intent).getDst());
                // TODO tear down the idle lightpath if the utilization is zero.

            }
            */ //end-FIXME
        }

    }

    /**
     * Verifies if given link is cross-connect between packet and optical layer.
     *
     * @param link the link
     * @return true if the link is a cross-connect link
     */
    public static boolean isCrossConnectLink(Link link) {
        if (link.type() != Link.Type.OPTICAL) {
            return false;
        }

        checkNotNull(link.annotations());
        checkNotNull(link.annotations().value("optical.type"));

        if (link.annotations().value("optical.type").equals("cross-connect")) {
            return true;
        }

        return false;
    }

    /**
     * Link weight function that emphasizes re-use of packet links.
     */
    private static class OpticalLinkWeight implements LinkWeight {
        @Override
        public double weight(TopologyEdge edge) {
            // Ignore inactive links
            if (edge.link().state() == Link.State.INACTIVE) {
                return -1;
            }

            // Transport links have highest weight
            if (edge.link().type() == Link.Type.OPTICAL) {
                return 1000;
            }

            // Packet links
            return 1;
        }
    }

}
