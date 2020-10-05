/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_FLOATING_IP_ASSOCIATED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_FLOATING_IP_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_FLOATING_IP_DISASSOCIATED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_FLOATING_IP_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_FLOATING_IP_UPDATED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_GATEWAY_ADDED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_GATEWAY_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_INTERFACE_ADDED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_INTERFACE_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_INTERFACE_UPDATED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackRouterEvent.Type.OPENSTACK_ROUTER_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of OpenStack router and floating IP using a {@code ConsistentMap}.
 */
@Component(immediate = true, service = OpenstackRouterStore.class)
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final MapEventListener<String, Router>
                            routerMapListener = new OpenstackRouterMapListener();
    private final MapEventListener<String, RouterInterface>
            routerInterfaceMapListener = new OpenstackRouterInterfaceMapListener();
    private final MapEventListener<String, NetFloatingIP>
            floatingIpMapListener = new OpenstackFloatingIpMapListener();

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
            final String error = osRouter.getId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osRouter;
        });
    }

    @Override
    public void updateRouter(Router osRouter) {
        osRouterStore.compute(osRouter.getId(), (id, existing) -> {
            final String error = osRouter.getId() + ERR_NOT_FOUND;
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
        return osRouterStore.asJavaMap().get(routerId);
    }

    @Override
    public Set<Router> routers() {
        return ImmutableSet.copyOf(osRouterStore.asJavaMap().values());
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
        return osRouterInterfaceStore.asJavaMap().get(routerIfaceId);
    }

    @Override
    public Set<RouterInterface> routerInterfaces() {
        return ImmutableSet.copyOf(osRouterInterfaceStore.asJavaMap().values());
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
        return osFloatingIpStore.asJavaMap().get(floatingIpId);
    }

    @Override
    public Set<NetFloatingIP> floatingIps() {
        return ImmutableSet.copyOf(osFloatingIpStore.asJavaMap().values());
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
                    eventExecutor.execute(() -> processRouterMapUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processRouterMapInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processRouterMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported openstack router event type");
                    break;
            }
        }

        private void processRouterMapUpdate(MapEvent<String, Router> event) {
            log.debug("OpenStack router updated");
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_ROUTER_UPDATED,
                    event.newValue().value()));
            processGatewayUpdate(event);
        }

        private void processRouterMapInsertion(MapEvent<String, Router> event) {
            log.debug("OpenStack router created");
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_ROUTER_CREATED,
                    event.newValue().value()));
        }

        private void processRouterMapRemoval(MapEvent<String, Router> event) {
            log.debug("OpenStack router removed");
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_ROUTER_REMOVED,
                    event.oldValue().value()));
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

    private class OpenstackRouterInterfaceMapListener
                        implements MapEventListener<String, RouterInterface> {

        @Override
        public void event(MapEvent<String, RouterInterface> event) {
            switch (event.type()) {
                case UPDATE:
                    eventExecutor.execute(() -> processRouterIntfUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processRouterIntfInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processRouterIntfRemoval(event));
                    break;
                default:
                    log.error("Unsupported openstack router interface event type");
                    break;
            }
        }

        private void processRouterIntfUpdate(MapEvent<String, RouterInterface> event) {
            log.debug("OpenStack router interface updated");
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_ROUTER_INTERFACE_UPDATED,
                    router(event.newValue().value().getId()),
                    event.newValue().value()));
        }

        private void processRouterIntfInsertion(MapEvent<String, RouterInterface> event) {
            log.debug("OpenStack router interface created");
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_ROUTER_INTERFACE_ADDED,
                    router(event.newValue().value().getId()),
                    event.newValue().value()));
        }

        private void processRouterIntfRemoval(MapEvent<String, RouterInterface> event) {
            log.debug("OpenStack router interface removed");
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_ROUTER_INTERFACE_REMOVED,
                    router(event.oldValue().value().getId()),
                    event.oldValue().value()));
        }
    }

    private class OpenstackFloatingIpMapListener
                            implements MapEventListener<String, NetFloatingIP> {

        @Override
        public void event(MapEvent<String, NetFloatingIP> event) {
            switch (event.type()) {
                case UPDATE:
                    eventExecutor.execute(() -> processFloatingIpMapUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processFloatingIpMapInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processFloatingIpMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported openstack floating IP event type");
                    break;
            }
        }

        private void processFloatingIpMapUpdate(MapEvent<String, NetFloatingIP> event) {
            log.debug("OpenStack floating IP updated");
            Router osRouter = Strings.isNullOrEmpty(
                    event.newValue().value().getRouterId()) ?
                    null :
                    router(event.newValue().value().getRouterId());
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_FLOATING_IP_UPDATED,
                    osRouter,
                    event.newValue().value()));
            processFloatingIpUpdate(event, osRouter);
        }

        private void processFloatingIpMapInsertion(MapEvent<String, NetFloatingIP> event) {
            log.debug("OpenStack floating IP created");
            Router osRouter = Strings.isNullOrEmpty(
                    event.newValue().value().getRouterId()) ?
                    null :
                    router(event.newValue().value().getRouterId());
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_FLOATING_IP_CREATED,
                    osRouter,
                    event.newValue().value()));
        }

        private void processFloatingIpMapRemoval(MapEvent<String, NetFloatingIP> event) {
            log.debug("OpenStack floating IP removed");
            Router osRouter = Strings.isNullOrEmpty(
                    event.oldValue().value().getRouterId()) ?
                    null :
                    router(event.oldValue().value().getRouterId());
            notifyDelegate(new OpenstackRouterEvent(
                    OPENSTACK_FLOATING_IP_REMOVED,
                    osRouter,
                    event.oldValue().value()));
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
