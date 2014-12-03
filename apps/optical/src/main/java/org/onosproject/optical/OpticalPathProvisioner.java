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
import org.apache.felix.scr.annotations.Deactivate;
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
import org.onosproject.net.intent.IntentOperations;
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
import java.util.Set;

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

    //protected <IntentId> intentIdGenerator;

    private final InternalOpticalPathProvisioner pathProvisioner = new InternalOpticalPathProvisioner();

    @Activate
    protected void activate() {
        intentService.addListener(pathProvisioner);
        appId = coreService.registerApplication("org.onosproject.optical");
        log.info("Starting optical path provisoning...");
    }

    @Deactivate
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
                    teardownLightpath(event.subject());
                    break;
                default:
                    break;
            }
        }

        private void setupLightpath(Intent intent) {
            // TODO support more packet intent types
            List<Intent> intents = Lists.newArrayList();
            if (intent instanceof HostToHostIntent) {
                HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;
                Host one = hostService.getHost(hostToHostIntent.one());
                Host two = hostService.getHost(hostToHostIntent.two());
                // provision both directions
                intents.addAll(getOpticalPaths(one.location(), two.location()));
                intents.addAll(getOpticalPaths(two.location(), one.location()));
            } else if (intent instanceof PointToPointIntent) {
                PointToPointIntent p2pIntent = (PointToPointIntent) intent;
                intents.addAll(getOpticalPaths(p2pIntent.ingressPoint(), p2pIntent.egressPoint()));
            } else {
                log.info("Unsupported intent type: {}", intent.getClass());
            }

            // Build the intent batch
            IntentOperations.Builder ops = IntentOperations.builder(appId);
            for (Intent i : intents) {
                // TODO: don't allow duplicate intents between the same points for now
                // we may want to allow this carefully in future to increase capacity
                Intent existing = intentService.getIntent(i.id());
                if (existing == null ||
                    !IntentState.WITHDRAWN.equals(intentService.getIntentState(i.id()))) {
                    ops.addSubmitOperation(i);
                }
            }
            intentService.execute(ops.build());
        }

        private List<Intent> getOpticalPaths(ConnectPoint ingress, ConnectPoint egress) {
            Set<Path> paths = pathService.getPaths(ingress.deviceId(),
                                                   egress.deviceId(),
                                                   new OpticalLinkWeight());

            if (paths.isEmpty()) {
                return Lists.newArrayList();
            }

            ConnectPoint srcWdmPoint = null;
            ConnectPoint dstWdmPoint = null;
            Iterator<Path> itrPath = paths.iterator();
            Path firstPath = itrPath.next();
            log.info(firstPath.links().toString());

            List<Intent> connectionList = Lists.newArrayList();

            Iterator<Link> itrLink = firstPath.links().iterator();
            while (itrLink.hasNext()) {
                Link link1 = itrLink.next();
                if (!isOpticalLink(link1)) {
                    continue;
                } else {
                    srcWdmPoint = link1.dst();
                    dstWdmPoint = srcWdmPoint;
                }

                while (true) {
                    if (itrLink.hasNext()) {
                        Link link2 = itrLink.next();
                        dstWdmPoint = link2.src();
                    } else {
                        break;
                    }
                }

                Intent opticalIntent = new OpticalConnectivityIntent(appId,
                                                                     srcWdmPoint,
                                                                     dstWdmPoint);
                log.info("Creating optical intent from {} to {}", srcWdmPoint, dstWdmPoint);
                connectionList.add(opticalIntent);
            }
            return connectionList;
        }



        private void teardownLightpath(Intent intent) {
          // TODO: tear down the idle lightpath if the utilization is close to zero.
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
                return 1000.0;  // optical links
            } else {
                return 1.0;   // packet links
            }
        }
    }

}
