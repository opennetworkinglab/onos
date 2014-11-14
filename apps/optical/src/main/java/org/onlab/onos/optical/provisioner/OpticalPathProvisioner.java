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
package org.onlab.onos.optical.provisioner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.OpticalConnectivityIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.resource.LinkResourceService;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyEdge;

import org.onlab.onos.net.topology.TopologyService;
import org.onlab.packet.Ethernet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;

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
    private IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private ApplicationId appId;

    //protected <IntentId> intentIdGenerator;

    private final InternalOpticalPathProvisioner pathProvisioner = new InternalOpticalPathProvisioner();

    @Activate
    protected void activate() {
        intentService.addListener(pathProvisioner);
        appId = coreService.registerApplication("org.onlab.onos.optical");
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
                case SUBMITTED:
                    break;
                case INSTALLED:
                    break;
                case FAILED:
                    log.info("packet intent {} failed, calling optical path provisioning APP.", event.subject());
                    setuplightpath(event.subject());
                    break;
                case WITHDRAWN:
                    log.info("intent {} withdrawn.", event.subject());
                    teardownLightpath(event.subject());
                    break;
                default:
                    break;
            }
        }

        private void setuplightpath(Intent intent) {
           // TODO support more packet intent types

           if (intent instanceof HostToHostIntent) {
               HostToHostIntent hostToHostIntent = (HostToHostIntent) intent;
               Host one = hostService.getHost(hostToHostIntent.one());
               Host two = hostService.getHost(hostToHostIntent.two());

               TrafficSelector selector = buildTrafficSelector();
               TrafficTreatment treatment = builder().build();

               PointToPointIntent intentOneToTwo =
                       new PointToPointIntent(appId, selector, treatment,
                                              one.location(), two.location());
               intentService.submit(intentOneToTwo);
               log.info("Submitting P2P intent {} ", intentOneToTwo);

               PointToPointIntent intentTwoToOne =
                       new PointToPointIntent(appId, selector, treatment,
                                              two.location(), one.location());
               intentService.submit(intentTwoToOne);
               log.info("Submitting P2P intent for {} ", intentTwoToOne);
               return;
           } else if (!intent.getClass().equals(PointToPointIntent.class)) {
               return;
           }

           PointToPointIntent pktIntent = (PointToPointIntent) intent;
           if (pktIntent.ingressPoint() == null || pktIntent.egressPoint() == null) {
               return;
           }

           Topology topology = topologyService.currentTopology();

           LinkWeight weight = new LinkWeight() {
               @Override
               public double weight(TopologyEdge edge) {
                   if (isOpticalLink(edge.link())) {
                       return 1000.0;  // optical links
                   } else {
                       return 1.0;   // packet links
                   }
               }
           };

           Set<Path> paths = topologyService.getPaths(topology,
                   pktIntent.ingressPoint().deviceId(),
                   pktIntent.egressPoint().deviceId(),
                   weight);

           if (paths.isEmpty()) {
               return;
           }

           ConnectPoint srcWdmPoint = null;
           ConnectPoint dstWdmPoint = null;
           Iterator<Path> itrPath = paths.iterator();
           Path firstPath = itrPath.next();
           log.info(firstPath.links().toString());

           ArrayList<Map<ConnectPoint, ConnectPoint>> connectionList =
                   new ArrayList<>();


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

                   /*
                   if (itrLink.hasNext()) {
                       Link link3 = itrLink.next();
                       if (isOpticalLink(link3)) {
                          break;
                       }
                   } else {
                       break;
                   }*/

               }

               Map<ConnectPoint, ConnectPoint> pair =
                       new HashMap<ConnectPoint, ConnectPoint>();
               pair.put(srcWdmPoint, dstWdmPoint);

               connectionList.add(pair);
           }

           for (Map<ConnectPoint, ConnectPoint> map : connectionList) {
               for (Entry<ConnectPoint, ConnectPoint> entry : map.entrySet()) {

                   ConnectPoint src = entry.getKey();
                   ConnectPoint dst = entry.getValue();

                   Intent opticalIntent = new OpticalConnectivityIntent(appId,
                          srcWdmPoint,
                          dstWdmPoint);

                   intentService.submit(opticalIntent);

                   log.info(srcWdmPoint.toString());
                   log.info(dstWdmPoint.toString());
               }
           }

        }

        private boolean isOpticalLink(Link link) {
            boolean isOptical = false;
            Link.Type lt = link.type();
            if (lt == Link.Type.OPTICAL) {
                isOptical = true;
            }
            return isOptical;
          }

        private void teardownLightpath(Intent intent) {
          // TODO: tear down the idle lightpath if the utilization is close to zero.
        }

        private TrafficSelector buildTrafficSelector() {
            TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
            Short ethType = Ethernet.TYPE_IPV4;

            selectorBuilder.matchEthType(ethType);

            return selectorBuilder.build();
        }

    }

}
