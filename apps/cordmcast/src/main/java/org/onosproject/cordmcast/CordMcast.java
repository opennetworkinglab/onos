/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.cordmcast;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.mcast.MulticastRouteService;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * CORD multicast provisoning application. Operates by listening to
 * events on the multicast rib and provsioning groups to program multicast
 * flows on the dataplane.
 */
@Component(immediate = true)
public class CordMcast {

    private static final int DEFAULT_PRIORITY = 1000;
    private static final short DEFAULT_MCAST_VLAN = 4000;
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService mcastService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    protected McastListener listener = new InternalMulticastListener();



    //TODO: move this to a ec map
    private Map<IpAddress, Integer> groups = Maps.newConcurrentMap();

    //TODO: move this to distributed atomic long
    private AtomicInteger channels = new AtomicInteger(0);

    private ApplicationId appId;

    //TODO: network config this
    private short mcastVlan = DEFAULT_MCAST_VLAN;

    // TODO component config this
    private int priority = DEFAULT_PRIORITY;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.cordmcast");
        mcastService.addListener(listener);
        //TODO: obtain all existing mcast routes
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        mcastService.removeListener(listener);
        log.info("Stopped");
    }

    private class InternalMulticastListener implements McastListener {
        @Override
        public void event(McastEvent event) {
            switch (event.type()) {
                case ROUTE_ADDED:
                    break;
                case ROUTE_REMOVED:
                    break;
                case SOURCE_ADDED:
                    break;
                case SINK_ADDED:
                    provisionGroup(event.subject());
                    break;
                case SINK_REMOVED:
                    break;
                default:
                    log.warn("Unknown mcast event {}", event.type());
            }
        }
    }

    private void provisionGroup(McastRouteInfo info) {
        if (!info.sink().isPresent()) {
            log.warn("No sink given after sink added event: {}", info);
            return;
        }
        ConnectPoint loc = info.sink().get();


        Integer nextId = groups.computeIfAbsent(info.route().group(), (g) -> {
            Integer id = allocateId(g);

            TrafficSelector mcast = DefaultTrafficSelector.builder()
                    .matchVlanId(VlanId.vlanId(mcastVlan))
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_IGMP)
                    .matchIPDst(g.toIpPrefix())
                    .build();


            ForwardingObjective fwd = DefaultForwardingObjective.builder()
                    .fromApp(appId)
                    .nextStep(id)
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(priority)
                    .withSelector(mcast)
                    .add(new ObjectiveContext() {
                        @Override
                        public void onSuccess(Objective objective) {
                            //TODO: change to debug
                            log.info("Forwarding objective installed {}", objective);
                        }

                        @Override
                        public void onError(Objective objective, ObjectiveError error) {
                            //TODO: change to debug
                            log.info("Forwarding objective failed {}", objective);
                        }
                    });

            flowObjectiveService.forward(loc.deviceId(), fwd);

           return id;
        });

        NextObjective next = DefaultNextObjective.builder()
                .fromApp(appId)
                .addTreatment(DefaultTrafficTreatment.builder().setOutput(loc.port()).build())
                .withType(NextObjective.Type.BROADCAST)
                .withId(nextId)
                .addToExisting(new ObjectiveContext() {
                    @Override
                    public void onSuccess(Objective objective) {
                        //TODO: change to debug
                        log.info("Next Objective {} installed", objective.id());
                    }

                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        //TODO: change to debug
                        log.info("Next Objective {} failed, because {}",
                                 objective.id(),
                                 error);
                    }
                });

        flowObjectiveService.next(loc.deviceId(), next);
    }

    private Integer allocateId(IpAddress group) {
        Integer channel = groups.putIfAbsent(group, channels.getAndIncrement());
        return channel == null ? groups.get(group) : channel;
    }
}
