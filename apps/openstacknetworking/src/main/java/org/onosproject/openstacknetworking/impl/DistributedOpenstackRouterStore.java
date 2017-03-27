/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterStore;
import org.onosproject.openstacknetworking.api.OpenstackRouterStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.State;
import org.openstack4j.openstack.networking.domain.NeutronExternalGateway;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.openstack4j.openstack.networking.domain.NeutronRouter;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of OpenStack router and floating IP using a {@code ConsistentMap}.
 */
@Service
@Component(immediate = true)
public class DistributedOpenstackRouterStore
        extends AbstractStore<OpenstackRouterEvent, OpenstackRouterStoreDelegate>
        implements OpenstackRouterStore {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final KryoNamespace SERIALIZER_NEUTRON_L3 = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Router.class)
            .register(NeutronRouter.class)
            .register(State.class)
            .register(NeutronHostRoute.class)
            .register(ExternalGateway.class)
            .register(NeutronExternalGateway.class)
            .register(RouterInterface.class)
            .register(NeutronRouterInterface.class)
            .register(NetFloatingIP.class)
            .register(NeutronFloatingIP.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final MapEventListener<String, Router> routerMapListener = new OpenstackRouterMapListener();
    private final MapEventListener<String, RouterInterface> routerInterfaceMapListener =
            new OpenstackRouterInterfaceMapListener();
    private final MapEventListener<String, NetFloatingIP> floatingIpMapListener =
            new OpenstackFloatingIpMapListener();

    private ConsistentMap<String, Router> osRouterStore;
    private ConsistentMap<String, RouterInterface> osRouterInterfaceStore;
    private ConsistentMap<String, NetFloatingIP> osFloatingIpStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        osRouterStore = storageService.<String, Router>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_NEUTRON_L3))
                .withName("openstack-routerstore")
                .withApplicationId(appId)
                .build();
        osRouterStore.addListener(routerMapListener);

        osRouterInterfaceStore = storageService.<String, RouterInterface>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_NEUTRON_L3))
                .withName("openstack-routerifacestore")
                .withApplicationId(appId)
                .build();
        osRouterInterfaceStore.addListener(routerInterfaceMapListener);

        osFloatingIpStore = storageService.<String, NetFloatingIP>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_NEUTRON_L3))
                .withName("openstack-floatingipstore")
                .withApplicationId(appId)
                .build();
        osFloatingIpStore.addListener(floatingIpMapListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osRouterStore.removeListener(routerMapListener);
        osRouterInterfaceStore.removeListener(routerInterfaceMapListener);
        osFloatingIpStore.removeListener(floatingIpMapListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createRouter(Router osRouter) {
        osRouterStore.compute(osRouter.getId(), (id, existing) -> {
            final String error = osRouter.getName() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osRouter;
        });
    }

    @Override
    public void updateRouter(Router osRouter) {
        osRouterStore.compute(osRouter.getId(), (id, existing) -> {
            final String error = osRouter.getName() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osRouter;
        });
    }

    @Override
    public Router removeRouter(String routerId) {
        Versioned<Router> osRouter = osRouterStore.remove(routerId);
        return osRouter == null ? null : osRouter.value();
    }

    @Override
    public Router router(String routerId) {
        Versioned<Router> versioned = osRouterStore.get(routerId);
        return versioned == null ? null : versioned.value();
    }

    @Override
    public Set<Router> routers() {
        Set<Router> osRouters = osRouterStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osRouters);
    }

    @Override
    public void addRouterInterface(RouterInterface osRouterIface) {
        osRouterInterfaceStore.compute(osRouterIface.getPortId(), (id, existing) -> {
            final String error = osRouterIface.getPortId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osRouterIface;
        });
    }

    @Override
    public void updateRouterInterface(RouterInterface osRouterIface) {
        osRouterInterfaceStore.compute(osRouterIface.getPortId(), (id, existing) -> {
            final String error = osRouterIface.getPortId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osRouterIface;
        });
    }

    @Override
    public RouterInterface removeRouterInterface(String routerIfaceId) {
        Versioned<RouterInterface> osRouterIface = osRouterInterfaceStore.remove(routerIfaceId);
        return osRouterIface == null ? null : osRouterIface.value();
    }

    @Override
    public RouterInterface routerInterface(String routerIfaceId) {
        Versioned<RouterInterface> osRouterIface = osRouterInterfaceStore.get(routerIfaceId);
        return osRouterIface == null ? null : osRouterIface.value();
    }

    @Override
    public Set<RouterInterface> routerInterfaces() {
        Set<RouterInterface> osRouterIfaces = osRouterInterfaceStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osRouterIfaces);
    }

    @Override
    public void createFloatingIp(NetFloatingIP osFloatingIp) {
        osFloatingIpStore.compute(osFloatingIp.getId(), (id, existing) -> {
            final String error = osFloatingIp.getId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osFloatingIp;
        });
    }

    @Override
    public void updateFloatingIp(NetFloatingIP osFloatingIp) {
        osFloatingIpStore.compute(osFloatingIp.getId(), (id, existing) -> {
            final String error = osFloatingIp.getId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osFloatingIp;
        });
    }

    @Override
    public NetFloatingIP removeFloatingIp(String floatingIpId) {
        Versioned<NetFloatingIP> osFloatingIp = osFloatingIpStore.remove(floatingIpId);
        return osFloatingIp == null ? null : osFloatingIp.value();
    }

    @Override
    public NetFloatingIP floatingIp(String floatingIpId) {
        Versioned<NetFloatingIP> osFloatingIp = osFloatingIpStore.get(floatingIpId);
        return osFloatingIp == null ? null : osFloatingIp.value();
    }

    @Override
    public Set<NetFloatingIP> floatingIps() {
        Set<NetFloatingIP> osFloatingIps = osFloatingIpStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osFloatingIps);
    }

    @Override
    public void clear() {
        osFloatingIpStore.clear();
        osRouterInterfaceStore.clear();
        osRouterStore.clear();
    }

    private class OpenstackRouterMapListener implements MapEventListener<String, Router> {

        @Override
        public void event(MapEvent<String, Router> event) {
            switch (event.type()) {
                case UPDATE:
                    log.debug("OpenStack router updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_ROUTER_UPDATED,
                                event.newValue().value()));
                        processGatewayUpdate(event);
                    });
                    break;
                case INSERT:
                    log.debug("OpenStack router created {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_ROUTER_CREATED,
                                event.newValue().value()));
                    });
                    break;
                case REMOVE:
                    log.debug("OpenStack router removed {}", event.oldValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_ROUTER_REMOVED,
                                event.oldValue().value()));
                    });
                    break;
                default:
                    log.error("Unsupported event type");
                    break;
            }
        }

        private void processGatewayUpdate(MapEvent<String, Router> event) {
            ExternalGateway oldGateway = event.oldValue().value().getExternalGatewayInfo();
            ExternalGateway newGateway = event.newValue().value().getExternalGatewayInfo();

            if (oldGateway == null && newGateway != null) {
                notifyDelegate(new OpenstackRouterEvent(
                        OPENSTACK_ROUTER_GATEWAY_ADDED,
                        event.newValue().value(), newGateway));
            }
            if (oldGateway != null && newGateway == null) {
                notifyDelegate(new OpenstackRouterEvent(
                        OPENSTACK_ROUTER_GATEWAY_REMOVED,
                        event.newValue().value(), oldGateway));
            }
        }
    }

    private class OpenstackRouterInterfaceMapListener implements MapEventListener<String, RouterInterface> {

        @Override
        public void event(MapEvent<String, RouterInterface> event) {
            switch (event.type()) {
                case UPDATE:
                    log.debug("OpenStack router interface updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_ROUTER_INTERFACE_UPDATED,
                                router(event.newValue().value().getId()),
                                event.newValue().value()));
                    });
                    break;
                case INSERT:
                    log.debug("OpenStack router interface created {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_ROUTER_INTERFACE_ADDED,
                                router(event.newValue().value().getId()),
                                event.newValue().value()));
                    });
                    break;
                case REMOVE:
                    log.debug("OpenStack router interface removed {}", event.oldValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_ROUTER_INTERFACE_REMOVED,
                                router(event.oldValue().value().getId()),
                                event.oldValue().value()));
                    });
                    break;
                default:
                    log.error("Unsupported event type");
                    break;
            }
        }
    }

    private class OpenstackFloatingIpMapListener implements MapEventListener<String, NetFloatingIP> {

        @Override
        public void event(MapEvent<String, NetFloatingIP> event) {
            switch (event.type()) {
                case UPDATE:
                    log.debug("OpenStack floating IP updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        Router osRouter = Strings.isNullOrEmpty(
                                event.newValue().value().getRouterId()) ?
                                null :
                                router(event.newValue().value().getRouterId());
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_FLOATING_IP_UPDATED,
                                osRouter,
                                event.newValue().value()));
                        processFloatingIpUpdate(event, osRouter);
                    });
                    break;
                case INSERT:
                    log.debug("OpenStack floating IP created {}", event.newValue());
                    eventExecutor.execute(() -> {
                        Router osRouter = Strings.isNullOrEmpty(
                                event.newValue().value().getRouterId()) ?
                                null :
                                router(event.newValue().value().getRouterId());
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_FLOATING_IP_CREATED,
                                osRouter,
                                event.newValue().value()));
                    });
                    break;
                case REMOVE:
                    log.debug("OpenStack floating IP removed {}", event.oldValue());
                    eventExecutor.execute(() -> {
                        Router osRouter = Strings.isNullOrEmpty(
                                event.oldValue().value().getRouterId()) ?
                                null :
                                router(event.oldValue().value().getRouterId());
                        notifyDelegate(new OpenstackRouterEvent(
                                OPENSTACK_FLOATING_IP_REMOVED,
                                osRouter,
                                event.oldValue().value()));
                    });
                    break;
                default:
                    log.error("Unsupported event type");
                    break;
            }
        }

        private void processFloatingIpUpdate(MapEvent<String, NetFloatingIP> event,
                                             Router osRouter) {
            String oldPortId = event.oldValue().value().getPortId();
            String newPortId = event.newValue().value().getPortId();

            if (Strings.isNullOrEmpty(oldPortId) && !Strings.isNullOrEmpty(newPortId)) {
                notifyDelegate(new OpenstackRouterEvent(
                        OPENSTACK_FLOATING_IP_ASSOCIATED,
                        osRouter,
                        event.newValue().value(), newPortId));
            }
            if (!Strings.isNullOrEmpty(oldPortId) && Strings.isNullOrEmpty(newPortId)) {
                notifyDelegate(new OpenstackRouterEvent(
                        OPENSTACK_FLOATING_IP_DISASSOCIATED,
                        osRouter,
                        event.newValue().value(), oldPortId));
            }
        }
    }
}
