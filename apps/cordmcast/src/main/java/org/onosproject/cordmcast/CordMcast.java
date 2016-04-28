/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.cordconfig.access.AccessDeviceConfig;
import org.onosproject.cordconfig.access.AccessDeviceData;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
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
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.McastRouteInfo;
import org.onosproject.net.mcast.MulticastRouteService;

import org.onosproject.rest.AbstractWebResource;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * CORD multicast provisioning application. Operates by listening to
 * events on the multicast rib and provisioning groups to program multicast
 * flows on the dataplane.
 */
@Component(immediate = true)
public class CordMcast {


    private static final int DEFAULT_REST_TIMEOUT_MS = 2000;
    private static final int DEFAULT_PRIORITY = 500;
    private static final short DEFAULT_MCAST_VLAN = 4000;
    private static final String DEFAULT_SYNC_HOST = "10.90.0.8:8181";
    private static final String DEFAULT_USER = "karaf";
    private static final String DEFAULT_PASSWORD = "karaf";
    private static final boolean DEFAULT_VLAN_ENABLED = true;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService mcastService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    protected McastListener listener = new InternalMulticastListener();
    private InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    //TODO: move this to a ec map
    private Map<IpAddress, Integer> groups = Maps.newConcurrentMap();

    private ApplicationId appId;

    @Property(name = "mcastVlan", intValue = DEFAULT_MCAST_VLAN,
            label = "VLAN for multicast traffic")
    private int mcastVlan = DEFAULT_MCAST_VLAN;

    @Property(name = "vlanEnabled", boolValue = DEFAULT_VLAN_ENABLED,
            label = "Use vlan for multicast traffic?")
    private boolean vlanEnabled = DEFAULT_VLAN_ENABLED;

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

    private Map<DeviceId, AccessDeviceData> oltData = new ConcurrentHashMap<>();

    private static final Class<AccessDeviceConfig> CONFIG_CLASS =
            AccessDeviceConfig.class;

    private ConfigFactory<DeviceId, AccessDeviceConfig> configFactory =
            new ConfigFactory<DeviceId, AccessDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY, CONFIG_CLASS, "accessDevice") {
                @Override
                public AccessDeviceConfig createConfig() {
                    return new AccessDeviceConfig();
                }
            };

    @Activate
    public void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        modified(context);

        appId = coreService.registerApplication("org.onosproject.cordmcast");


        clearRemoteRoutes();

        networkConfig.registerConfigFactory(configFactory);
        networkConfig.addListener(configListener);

        networkConfig.getSubjects(DeviceId.class, AccessDeviceConfig.class).forEach(
                subject -> {
                    AccessDeviceConfig config = networkConfig.getConfig(subject, AccessDeviceConfig.class);
                    if (config != null) {
                        AccessDeviceData data = config.getOlt();
                        oltData.put(data.deviceId(), data);
                    }
                }
        );


        mcastService.addListener(listener);

        mcastService.getRoutes().stream()
                .map(r -> new ImmutablePair<>(r, mcastService.fetchSinks(r)))
                .filter(pair -> pair.getRight() != null && !pair.getRight().isEmpty())
                .forEach(pair -> pair.getRight().forEach(sink -> provisionGroup(pair.getLeft(),
                        sink)));

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        mcastService.removeListener(listener);
        networkConfig.unregisterConfigFactory(configFactory);
        networkConfig.removeListener(configListener);
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
            vlanEnabled = isNullOrEmpty(s) ? DEFAULT_VLAN_ENABLED : Boolean.parseBoolean(s.trim());

            s = get(properties, "priority");
            priority = isNullOrEmpty(s) ? DEFAULT_PRIORITY : Integer.parseInt(s.trim());

            s = get(properties, "syncHost");
            syncHost = isNullOrEmpty(s) ? DEFAULT_SYNC_HOST : s.trim();
        } catch (Exception e) {
            user = DEFAULT_USER;
            password = DEFAULT_PASSWORD;
            syncHost = DEFAULT_SYNC_HOST;
            mcastVlan = DEFAULT_MCAST_VLAN;
            vlanEnabled = false;
            priority = DEFAULT_PRIORITY;
        }
        fabricOnosUrl = createRemoteUrl(syncHost);
    }

    private static String createRemoteUrl(String remoteHost) {
        return "http://" + remoteHost + "/onos/v1/mcast";
    }

    private class InternalMulticastListener implements McastListener {
        @Override
        public void event(McastEvent event) {
            McastRouteInfo info = event.subject();
            switch (event.type()) {
                case ROUTE_ADDED:
                    break;
                case ROUTE_REMOVED:
                    break;
                case SOURCE_ADDED:
                    break;
                case SINK_ADDED:
                    if (!info.sink().isPresent()) {
                        log.warn("No sink given after sink added event: {}", info);
                        return;
                    }
                    provisionGroup(info.route(), info.sink().get());
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
        if (info.sinks().isEmpty()) {
            removeRemoteRoute(info.route());
        }

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

    private void provisionGroup(McastRoute route, ConnectPoint sink) {
        checkNotNull(route, "Route cannot be null");
        checkNotNull(sink, "Sink cannot be null");

        AccessDeviceData oltInfo = oltData.get(sink.deviceId());

        if (oltInfo == null) {
            log.warn("Unknown OLT device : {}", sink.deviceId());
            return;
        }

        final AtomicBoolean sync = new AtomicBoolean(false);

        Integer nextId = groups.computeIfAbsent(route.group(), (g) -> {
            Integer id = flowObjectiveService.allocateNextId();

            NextObjective next = DefaultNextObjective.builder()
                    .fromApp(appId)
                    .addTreatment(DefaultTrafficTreatment.builder().setOutput(sink.port()).build())
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

            flowObjectiveService.next(sink.deviceId(), next);

            TrafficSelector.Builder mcast = DefaultTrafficSelector.builder()
                    .matchInPort(oltInfo.uplink())
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

            flowObjectiveService.forward(sink.deviceId(), fwd);

            sync.set(true);

            return id;
        });

        if (!sync.get()) {
            NextObjective next = DefaultNextObjective.builder()
                    .fromApp(appId)
                    .addTreatment(DefaultTrafficTreatment.builder().setOutput(sink.port()).build())
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

            flowObjectiveService.next(sink.deviceId(), next);
        }


        addRemoteRoute(route);
    }

    private void addRemoteRoute(McastRoute route) {
        checkNotNull(route);
        if (syncHost == null) {
            log.warn("No host configured for synchronization; route will be dropped");
            return;
        }

        log.debug("Sending route {} to other ONOS {}", route, fabricOnosUrl);

        Invocation.Builder builder = getClientBuilder(fabricOnosUrl);

        ObjectNode json = codecService.getCodec(McastRoute.class)
                .encode(route, new AbstractWebResource());

        try {
            builder.post(Entity.json(json.toString()));
        } catch (ProcessingException e) {
            log.warn("Unable to send route to remote controller: {}", e.getMessage());
        }
    }

    private void removeRemoteRoute(McastRoute route) {
        if (syncHost == null) {
            log.warn("No host configured for synchronization; route will be dropped");
            return;
        }

        log.debug("Removing route {} from other ONOS {}", route, fabricOnosUrl);

        Invocation.Builder builder = getClientBuilder(fabricOnosUrl)
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);

        ObjectNode json = codecService.getCodec(McastRoute.class)
                .encode(route, new AbstractWebResource());

        builder.method("DELETE", Entity.entity(json.asText(),
                MediaType.APPLICATION_OCTET_STREAM));
    }

    private void clearRemoteRoutes() {
        if (syncHost == null) {
            log.warn("No host configured for synchronization");
            return;
        }

        log.debug("Clearing remote multicast routes from {}", fabricOnosUrl);

        Invocation.Builder builder = getClientBuilder(fabricOnosUrl);
        List<McastRoute> mcastRoutes = Lists.newArrayList();

        try {
            String response = builder
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);

            JsonCodec<McastRoute> routeCodec = codecService.getCodec(McastRoute.class);
            ObjectMapper mapper = new ObjectMapper();


            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode list = (ArrayNode) node.path("routes");

            list.forEach(n -> mcastRoutes.add(
                    routeCodec.decode((ObjectNode) n, new AbstractWebResource())));

        } catch (IOException e) {
            log.warn("Error clearing remote routes", e);
        }

        mcastRoutes.forEach(this::removeRemoteRoute);
    }

    private Invocation.Builder getClientBuilder(String uri) {
        ClientConfig config = new ClientConfig();
        Client client = ClientBuilder.newClient(config);

        client.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_REST_TIMEOUT_MS);
        client.property(ClientProperties.READ_TIMEOUT,    DEFAULT_REST_TIMEOUT_MS);
        client.register(HttpAuthenticationFeature.basic(user, password));

        WebTarget wt = client.target(uri);
        return wt.request(JSON_UTF_8.toString());
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {

                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    AccessDeviceConfig config =
                            networkConfig.getConfig((DeviceId) event.subject(), CONFIG_CLASS);
                    if (config != null) {
                        oltData.put(config.getOlt().deviceId(), config.getOlt());
                    }

                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                    break;
                case CONFIG_REMOVED:
                    oltData.remove(event.subject());
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(CONFIG_CLASS);
        }
    }
}
