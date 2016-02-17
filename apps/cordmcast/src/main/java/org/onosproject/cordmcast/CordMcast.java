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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.codec.CodecService;
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
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.rest.AbstractWebResource;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * CORD multicast provisoning application. Operates by listening to
 * events on the multicast rib and provisioning groups to program multicast
 * flows on the dataplane.
 */
@Component(immediate = true)
public class CordMcast {

    private static final int DEFAULT_PRIORITY = 1000;
    private static final short DEFAULT_MCAST_VLAN = 4000;
    private static final String DEFAULT_SYNC_HOST = "10.90.0.8:8181";
    private static final String DEFAULT_USER = "karaf";
    private static final String DEFAULT_PASSWORD = "karaf";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService mcastService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    protected McastListener listener = new InternalMulticastListener();

    //TODO: move this to a ec map
    private Map<IpAddress, Integer> groups = Maps.newConcurrentMap();

    //TODO: move this to distributed atomic long
    private AtomicInteger channels = new AtomicInteger(0);

    private ApplicationId appId;

    @Property(name = "mcastVlan", intValue = DEFAULT_MCAST_VLAN,
            label = "VLAN for multicast traffic")
    private int mcastVlan = DEFAULT_MCAST_VLAN;

    @Property(name = "vlanEnabled", boolValue = false,
            label = "Use vlan for multicast traffic")
    private boolean vlanEnabled = false;

    @Property(name = "priority", intValue = DEFAULT_PRIORITY,
            label = "Priority for multicast rules")
    private int priority = DEFAULT_PRIORITY;

    @Property(name = "syncHost", value = DEFAULT_SYNC_HOST,
            label = "host:port to synchronize routes to")
    private String syncHost = DEFAULT_SYNC_HOST;

    @Property(name = "username", value = DEFAULT_USER,
            label = "Username for REST password authentication")
    private String user = DEFAULT_USER;

    @Property(name = "password", value = DEFAULT_PASSWORD,
            label = "Password for REST authentication")
    private String password = DEFAULT_PASSWORD;

    private String fabricOnosUrl;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.cordmcast");
        componentConfigService.registerProperties(getClass());
        mcastService.addListener(listener);

        fabricOnosUrl = "http://" + syncHost + "/onos/v1/mcast";

        //TODO: obtain all existing mcast routes
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        componentConfigService.unregisterProperties(getClass(), true);
        mcastService.removeListener(listener);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();


        try {
            String s = get(properties, "username");
            user = isNullOrEmpty(s) ? DEFAULT_USER : s.trim();

            s = get(properties, "password");
            password = isNullOrEmpty(s) ? DEFAULT_PASSWORD : s.trim();

            s = get(properties, "mcastVlan");
            mcastVlan = isNullOrEmpty(s) ? DEFAULT_MCAST_VLAN : Short.parseShort(s.trim());

            s = get(properties, "vlanEnabled");
            vlanEnabled = isNullOrEmpty(s) || Boolean.parseBoolean(s.trim());

            s = get(properties, "priority");
            priority = isNullOrEmpty(s) ? DEFAULT_PRIORITY : Integer.parseInt(s.trim());

            s = get(properties, syncHost);
            syncHost = isNullOrEmpty(s) ? DEFAULT_SYNC_HOST : s.trim();
        } catch (Exception e) {
            user = DEFAULT_USER;
            password = DEFAULT_PASSWORD;
            syncHost = DEFAULT_SYNC_HOST;
            mcastVlan = DEFAULT_MCAST_VLAN;
            vlanEnabled = false;
            priority = DEFAULT_PRIORITY;
        }


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
                    unprovisionGroup(event.subject());
                    break;
                default:
                    log.warn("Unknown mcast event {}", event.type());
            }
        }
    }

    private void unprovisionGroup(McastRouteInfo info) {
        if (!info.sink().isPresent()) {
            log.warn("No sink given after sink removed event: {}", info);
            return;
        }
        ConnectPoint loc = info.sink().get();

        NextObjective next = DefaultNextObjective.builder()
                .fromApp(appId)
                .addTreatment(DefaultTrafficTreatment.builder().setOutput(loc.port()).build())
                .withType(NextObjective.Type.BROADCAST)
                .withId(groups.get(info.route().group()))
                .removeFromExisting(new ObjectiveContext() {
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

    private void provisionGroup(McastRouteInfo info) {
        if (!info.sink().isPresent()) {
            log.warn("No sink given after sink added event: {}", info);
            return;
        }
        ConnectPoint loc = info.sink().get();

        final AtomicBoolean sync = new AtomicBoolean(false);

        Integer nextId = groups.computeIfAbsent(info.route().group(), (g) -> {
            Integer id = allocateId();

            NextObjective next = DefaultNextObjective.builder()
                    .fromApp(appId)
                    .addTreatment(DefaultTrafficTreatment.builder().setOutput(loc.port()).build())
                    .withType(NextObjective.Type.BROADCAST)
                    .withId(id)
                    .add(new ObjectiveContext() {
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

            TrafficSelector.Builder mcast = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(g.toIpPrefix());


            if (vlanEnabled) {
                mcast.matchVlanId(VlanId.vlanId((short) mcastVlan));
            }


            ForwardingObjective fwd = DefaultForwardingObjective.builder()
                    .fromApp(appId)
                    .nextStep(id)
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(priority)
                    .withSelector(mcast.build())
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

            sync.set(true);

           return id;
        });

        if (!sync.get()) {
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

        if (sync.get()) {
            syncRoute(info);
        }
    }

    private void syncRoute(McastRouteInfo info) {
        if (syncHost == null) {
            log.warn("No host configured for synchronization; route will be dropped");
            return;
        }

        log.debug("Sending route to other ONOS: {}", info.route());

        WebResource.Builder builder = getClientBuilder(fabricOnosUrl);

        ObjectNode json = codecService.getCodec(McastRoute.class)
                .encode(info.route(), new AbstractWebResource());
        builder.post(json.toString());
    }

    private Integer allocateId() {
        return channels.getAndIncrement();
    }

    private WebResource.Builder getClientBuilder(String uri) {
        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        WebResource resource = client.resource(uri);
        return resource.accept(JSON_UTF_8.toString())
                .type(JSON_UTF_8.toString());
    }

}
