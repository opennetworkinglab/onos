/*
 * Copyright 2014 Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onosproject.net.intent.IntentState.INSTALLED;

/**
 * OpticalPathProvisioner listens event notifications from the Intent F/W.
 * It generates one or more opticalConnectivityIntent(s) and submits (or withdraws) to Intent F/W
 * for adding/releasing capacity at the packet layer.
 *
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

    private ApplicationId appId;

    // TODO use a shared map for distributed operation
    protected final Map<ConnectPoint, OpticalConnectivityIntent> inStatusTportMap =
            new ConcurrentHashMap<>();
    protected final Map<ConnectPoint, OpticalConnectivityIntent> outStatusTportMap =
            new ConcurrentHashMap<>();

    protected final Map<ConnectPoint, Map<ConnectPoint, Intent>> intentMap =
            new ConcurrentHashMap<>();

    private final InternalOpticalPathProvisioner pathProvisioner = new InternalOpticalPathProvisioner();

    @Activate
    protected void activate() {
        // TODO elect a leader and have one instance do the provisioning
        intentService.addListener(pathProvisioner);
        appId = coreService.registerApplication("org.onosproject.optical");
        initTport();
        log.info("Starting optical path provisoning...");
    }

    protected void initTport() {
        inStatusTportMap.clear();
        outStatusTportMap.clear();
        for (Intent intent : intentService.getIntents()) {
            if (intentService.getIntentState(intent.id()) == INSTALLED) {
                if (intent instanceof OpticalConnectivityIntent) {
                    inStatusTportMap.put(((OpticalConnectivityIntent) intent).getSrc(),
                            (OpticalConnectivityIntent) intent);
                    outStatusTportMap.put(((OpticalConnectivityIntent) intent).getDst(),
                            (OpticalConnectivityIntent) intent);
                }
            }
        }
    }

    protected void deactivate() {
        intentService.removeListener(pathProvisioner);
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
                    log.info("packet intent {} failed, calling optical path provisioning APP.", event.subject());
                    setupLightpath(event.subject());
                    break;
                case WITHDRAWN:
                    log.info("intent {} withdrawn.", event.subject());
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

            if (!IntentState.FAILED.equals(intentService.getIntentState(intent.id()))) {
                   return;
             }

            List<Intent> intents = Lists.newArrayList();
            if (intent instanceof HostToHostIntent) {
                HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;
                Host one = hostService.getHost(hostToHostIntent.one());
                Host two = hostService.getHost(hostToHostIntent.two());
                if (one == null || two == null) {
                    return; //FIXME
                }
                // provision both directions
                intents.addAll(getOpticalPath(one.location(), two.location()));
                // note: bi-directional intent is set up
                // HostToHost Intent requires symmetric path!
                //intents.addAll(getOpticalPath(two.location(), one.location()));
            } else if (intent instanceof PointToPointIntent) {
                PointToPointIntent p2pIntent = (PointToPointIntent) intent;
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

        private List<Intent> getOpticalPath(ConnectPoint ingress, ConnectPoint egress) {
            Set<Path> paths = pathService.getPaths(ingress.deviceId(),
                                                   egress.deviceId(),
                                                   new OpticalLinkWeight());

            if (paths.isEmpty()) {
                return Lists.newArrayList();
            }

            List<Intent> connectionList = Lists.newArrayList();

            Iterator<Path> itrPath = paths.iterator();
            while (itrPath.hasNext()) {
                boolean usedTportFound = false;
                Path nextPath = itrPath.next();
                log.info(nextPath.links().toString()); // TODO drop log level

                Iterator<Link> itrLink = nextPath.links().iterator();
                while (itrLink.hasNext()) {
                    ConnectPoint srcWdmPoint, dstWdmPoint;
                    Link link1 = itrLink.next();
                    if (!isOpticalLink(link1)) {
                        continue;
                    } else {
                        srcWdmPoint = link1.dst();
                        dstWdmPoint = srcWdmPoint;
                    }

                    while (itrLink.hasNext()) {
                        Link link2 = itrLink.next();
                        if (isOpticalLink(link2)) {
                            dstWdmPoint = link2.src();
                        } else {
                            break;
                        }
                    }

                    if (inStatusTportMap.get(srcWdmPoint) != null ||
                            outStatusTportMap.get(dstWdmPoint) != null) {
                        usedTportFound = true;
                        // log.info("used ConnectPoint {} to {} were found", srcWdmPoint, dstWdmPoint);
                        break;
                    }

                    Intent opticalIntent = new OpticalConnectivityIntent(appId,
                                                                         srcWdmPoint,
                                                                         dstWdmPoint);
                    Intent opticalIntent2 = new OpticalConnectivityIntent(appId,
                                                                         dstWdmPoint,
                                                                         srcWdmPoint);
                    log.info("Creating optical intent from {} to {}", srcWdmPoint, dstWdmPoint);
                    log.info("Creating optical intent from {} to {}", dstWdmPoint, srcWdmPoint);
                    connectionList.add(opticalIntent);
                    connectionList.add(opticalIntent2);

                    break;
                }

                if (!usedTportFound) {
                    break;
                } else {
                    // reset the connection list
                    connectionList = Lists.newArrayList();
                }

            }

            return connectionList;
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

    private static boolean isOpticalLink(Link link) {
        boolean isOptical = false;
        Link.Type lt = link.type();
        if (lt == Link.Type.OPTICAL) {
            isOptical = true;
        }
        return isOptical;
    }

    private static class OpticalLinkWeight implements LinkWeight {
        @Override
        public double weight(TopologyEdge edge) {
            if (edge.link().state() == Link.State.INACTIVE) {
                return -1; // ignore inactive links
            }
            if (isOpticalLink(edge.link())) {
                return 1000;  // optical links
            } else {
                return 1;     // packet links
            }
        }
    }

}
