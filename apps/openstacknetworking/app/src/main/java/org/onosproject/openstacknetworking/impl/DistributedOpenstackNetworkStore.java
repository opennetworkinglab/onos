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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkStore;
import org.onosproject.openstacknetworking.api.OpenstackNetworkStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Ipv6AddressMode;
import org.openstack4j.model.network.Ipv6RaMode;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.State;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.networking.domain.NeutronAllowedAddressPair;
import org.openstack4j.openstack.networking.domain.NeutronExtraDhcpOptCreate;
import org.openstack4j.openstack.networking.domain.NeutronHostRoute;
import org.openstack4j.openstack.networking.domain.NeutronIP;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPool;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.EXTERNAL_PEER_ROUTER_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.EXTERNAL_PEER_ROUTER_MAC_UPDATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.EXTERNAL_PEER_ROUTER_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.EXTERNAL_PEER_ROUTER_UPDATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_NETWORK_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_NETWORK_PRE_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_NETWORK_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_NETWORK_UPDATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_PRE_REMOVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_SECURITY_GROUP_ADDED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_SECURITY_GROUP_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_UPDATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_SUBNET_CREATED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_SUBNET_REMOVED;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_SUBNET_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of OpenStack network, subnet, and port using a {@code ConsistentMap}.
 */
@Component(immediate = true, service = OpenstackNetworkStore.class)
public class DistributedOpenstackNetworkStore
        extends AbstractStore<OpenstackNetworkEvent, OpenstackNetworkStoreDelegate>
        implements OpenstackNetworkStore {

    protected final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final KryoNamespace SERIALIZER_NEUTRON_L2 = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Network.class)
            .register(NeutronNetwork.class)
            .register(State.class)
            .register(NetworkType.class)
            .register(Port.class)
            .register(NeutronPort.class)
            .register(NeutronIP.class)
            .register(NeutronAllowedAddressPair.class)
            .register(NeutronExtraDhcpOptCreate.class)
            .register(Subnet.class)
            .register(NeutronSubnet.class)
            .register(NeutronPool.class)
            .register(NeutronHostRoute.class)
            .register(IPVersionType.class)
            .register(Ipv6AddressMode.class)
            .register(Ipv6RaMode.class)
            .register(LinkedHashMap.class)
            .build();

    private static final KryoNamespace
            SERIALIZER_EXTERNAL_PEER_ROUTER_MAP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(ExternalPeerRouter.class)
            .register(DefaultExternalPeerRouter.class)
            .register(MacAddress.class)
            .register(IpAddress.class)
            .register(VlanId.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Network>
                        networkMapListener = new OpenstackNetworkMapListener();
    private final MapEventListener<String, Subnet>
                        subnetMapListener = new OpenstackSubnetMapListener();
    private final MapEventListener<String, Port>
                        portMapListener = new OpenstackPortMapListener();
    private final MapEventListener<String, ExternalPeerRouter>
                        peerRouterListener = new ExternalPeerRouterMapListener();

    private ConsistentMap<String, ExternalPeerRouter> externalPeerRouterStore;
    private ConsistentMap<String, Network> osNetworkStore;
    private ConsistentMap<String, Subnet> osSubnetStore;
    private ConsistentMap<String, Port> osPortStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);

        osNetworkStore = storageService.<String, Network>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_NEUTRON_L2))
                .withName("openstack-networkstore")
                .withApplicationId(appId)
                .build();
        osNetworkStore.addListener(networkMapListener);

        osSubnetStore = storageService.<String, Subnet>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_NEUTRON_L2))
                .withName("openstack-subnetstore")
                .withApplicationId(appId)
                .build();
        osSubnetStore.addListener(subnetMapListener);

        osPortStore = storageService.<String, Port>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_NEUTRON_L2))
                .withName("openstack-portstore")
                .withApplicationId(appId)
                .build();
        osPortStore.addListener(portMapListener);

        externalPeerRouterStore = storageService.<String, ExternalPeerRouter>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_EXTERNAL_PEER_ROUTER_MAP))
                .withName("external-routermap")
                .withApplicationId(appId)
                .build();
        externalPeerRouterStore.addListener(peerRouterListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNetworkStore.removeListener(networkMapListener);
        osSubnetStore.removeListener(subnetMapListener);
        osPortStore.removeListener(portMapListener);
        externalPeerRouterStore.removeListener(peerRouterListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createNetwork(Network osNet) {
        osNetworkStore.compute(osNet.getId(), (id, existing) -> {
            final String error = osNet.getId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osNet;
        });
    }

    @Override
    public void updateNetwork(Network osNet) {
        osNetworkStore.compute(osNet.getId(), (id, existing) -> {
            final String error = osNet.getId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osNet;
        });
    }

    @Override
    public Network removeNetwork(String netId) {
        Versioned<Network> osNet = osNetworkStore.remove(netId);
        return osNet == null ? null : osNet.value();
    }

    @Override
    public Network network(String netId) {
        return osNetworkStore.asJavaMap().get(netId);
    }

    @Override
    public Set<Network> networks() {
        return ImmutableSet.copyOf(osNetworkStore.asJavaMap().values());
    }

    @Override
    public void createSubnet(Subnet osSubnet) {
        osSubnetStore.compute(osSubnet.getId(), (id, existing) -> {
            final String error = osSubnet.getId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osSubnet;
        });
    }

    @Override
    public void updateSubnet(Subnet osSubnet) {
        osSubnetStore.compute(osSubnet.getId(), (id, existing) -> {
            final String error = osSubnet.getId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osSubnet;
        });
    }

    @Override
    public Subnet removeSubnet(String subnetId) {
        Versioned<Subnet> osSubnet = osSubnetStore.remove(subnetId);
        return osSubnet == null ? null : osSubnet.value();
    }

    @Override
    public Subnet subnet(String subnetId) {
        return osSubnetStore.asJavaMap().get(subnetId);
    }

    @Override
    public Set<Subnet> subnets() {
        return ImmutableSet.copyOf(osSubnetStore.asJavaMap().values());
    }

    @Override
    public void createPort(Port osPort) {
        osPortStore.compute(osPort.getId(), (id, existing) -> {
            final String error = osPort.getId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osPort;
        });
    }

    @Override
    public void updatePort(Port osPort) {
        osPortStore.compute(osPort.getId(), (id, existing) -> {
            final String error = osPort.getId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return osPort;
        });
    }

    @Override
    public Port removePort(String portId) {
        Versioned<Port> osPort = osPortStore.remove(portId);
        return osPort == null ? null : osPort.value();
    }

    @Override
    public Port port(String portId) {
        return osPortStore.asJavaMap().get(portId);
    }

    @Override
    public Set<Port> ports() {
        return ImmutableSet.copyOf(osPortStore.asJavaMap().values());
    }

    @Override
    public ExternalPeerRouter externalPeerRouter(String ipAddress) {
        return externalPeerRouterStore.asJavaMap().get(ipAddress);
    }

    @Override
    public Set<ExternalPeerRouter> externalPeerRouters() {
        return new HashSet<>(externalPeerRouterStore.asJavaMap().values());
    }

    @Override
    public void createExternalPeerRouter(ExternalPeerRouter peerRouter) {
        externalPeerRouterStore.compute(
                peerRouter.ipAddress().toString(), (id, existing) -> {
            final String error = peerRouter.ipAddress().toString() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return peerRouter;
        });
    }

    @Override
    public void updateExternalPeerRouter(ExternalPeerRouter peerRouter) {
        externalPeerRouterStore.compute(
                peerRouter.ipAddress().toString(), (id, existing) -> {
            final String error = peerRouter.ipAddress() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return peerRouter;
        });
    }

    @Override
    public ExternalPeerRouter removeExternalPeerRouter(String ipAddress) {
        Versioned<ExternalPeerRouter> peerRouter =
                externalPeerRouterStore.remove(ipAddress);
        return peerRouter == null ? null : peerRouter.value();
    }

    @Override
    public void clear() {
        osPortStore.clear();
        osSubnetStore.clear();
        osNetworkStore.clear();
        externalPeerRouterStore.clear();
    }

    private class OpenstackNetworkMapListener
                                implements MapEventListener<String, Network> {

        @Override
        public void event(MapEvent<String, Network> event) {
            switch (event.type()) {
                case UPDATE:
                    eventExecutor.execute(() -> processNetworkMapUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processNetworkMapInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processNetworkMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported openstack network event type");
                    break;
            }
        }

        private void processNetworkMapUpdate(MapEvent<String, Network> event) {
            log.debug("OpenStack network updated");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_NETWORK_UPDATED,
                    event.newValue().value()));

            Network oldValue = event.oldValue().value();
            Network newValue = event.newValue().value();

            // FIXME: before the network get removed eventually, neutron always
            // issue network update event with removed (empty) segmentation ID
            // this might be a bug of openstack or openstack4j, need to revisit later
            if (oldValue.getProviderSegID() != null &&
                    newValue.getProviderSegID() == null) {
                log.debug("OpenStack network pre-removed");
                notifyDelegate(new OpenstackNetworkEvent(
                        OPENSTACK_NETWORK_PRE_REMOVED,
                        event.oldValue().value()));
            }
        }

        private void processNetworkMapInsertion(MapEvent<String, Network> event) {
            log.debug("OpenStack network created");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_NETWORK_CREATED,
                    event.newValue().value()));
        }

        private void processNetworkMapRemoval(MapEvent<String, Network> event) {
            log.debug("OpenStack network removed");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_NETWORK_REMOVED,
                    event.oldValue().value()));
        }
    }

    private class OpenstackSubnetMapListener implements MapEventListener<String, Subnet> {

        @Override
        public void event(MapEvent<String, Subnet> event) {
            switch (event.type()) {
                case UPDATE:
                    eventExecutor.execute(() -> processSubnetMapUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processSubnetMapInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processSubnetMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported openstack subnet event type");
                    break;
            }
        }

        private void processSubnetMapUpdate(MapEvent<String, Subnet> event) {
            log.debug("OpenStack subnet updated");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_SUBNET_UPDATED,
                    network(event.newValue().value().getNetworkId()),
                    event.newValue().value()));
        }

        private void processSubnetMapInsertion(MapEvent<String, Subnet> event) {
            log.debug("OpenStack subnet created");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_SUBNET_CREATED,
                    network(event.newValue().value().getNetworkId()),
                    event.newValue().value()));
        }

        private void processSubnetMapRemoval(MapEvent<String, Subnet> event) {
            log.debug("OpenStack subnet removed");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_SUBNET_REMOVED,
                    network(event.oldValue().value().getNetworkId()),
                    event.oldValue().value()));
        }
    }

    private class ExternalPeerRouterMapListener
            implements MapEventListener<String, ExternalPeerRouter> {

        @Override
        public void event(MapEvent<String, ExternalPeerRouter> event) {
            switch (event.type()) {
                case UPDATE:
                    eventExecutor.execute(() -> processPeerRouterUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processPeerRouterInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processPeerRouterRemoval(event));
                    break;
                default:
                    log.error("Unsupported external peer router event type");
                    break;
            }
        }

        private void processPeerRouterUpdate(
                MapEvent<String, ExternalPeerRouter> event) {
            log.debug("External peer router updated");
            notifyDelegate(new OpenstackNetworkEvent(
                    EXTERNAL_PEER_ROUTER_UPDATED, event.newValue().value()));

            processPeerRouterMacUpdated(event);
        }

        private void processPeerRouterInsertion(
                MapEvent<String, ExternalPeerRouter> event) {
            log.debug("External peer router inserted");
            notifyDelegate(new OpenstackNetworkEvent(
                    EXTERNAL_PEER_ROUTER_CREATED, event.newValue().value()));
        }

        private void processPeerRouterRemoval(
                MapEvent<String, ExternalPeerRouter> event) {
            log.debug("External peer router removed");
            notifyDelegate(new OpenstackNetworkEvent(
                    EXTERNAL_PEER_ROUTER_REMOVED, event.oldValue().value()));
        }

        private void processPeerRouterMacUpdated(
                MapEvent<String, ExternalPeerRouter> event) {
            ExternalPeerRouter oldPeerRouter = event.oldValue().value();
            ExternalPeerRouter newPeerRouter = event.newValue().value();

            if (!Objects.equals(oldPeerRouter.macAddress(), newPeerRouter.macAddress())) {
                notifyDelegate(new OpenstackNetworkEvent(
                        EXTERNAL_PEER_ROUTER_MAC_UPDATED, newPeerRouter));
            }
        }
     }

    private class OpenstackPortMapListener implements MapEventListener<String, Port> {

        @Override
        public void event(MapEvent<String, Port> event) {
            switch (event.type()) {
                case UPDATE:
                    eventExecutor.execute(() -> processPortMapUpdate(event));
                    break;
                case INSERT:
                    eventExecutor.execute(() -> processPortMapInsertion(event));
                    break;
                case REMOVE:
                    eventExecutor.execute(() -> processPortMapRemoval(event));
                    break;
                default:
                    log.error("Unsupported openstack port event type");
                    break;
            }
        }

        private void processPortMapUpdate(MapEvent<String, Port> event) {
            log.debug("OpenStack port updated");
            Port oldPort = event.oldValue().value();
            Port newPort = event.newValue().value();
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_PORT_UPDATED,
                    network(event.newValue().value().getNetworkId()), newPort));
            processSecurityGroupUpdate(oldPort, newPort);
        }

        private void processPortMapInsertion(MapEvent<String, Port> event) {
            log.debug("OpenStack port created");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_PORT_CREATED,
                    network(event.newValue().value().getNetworkId()),
                    event.newValue().value()));
        }

        private void processPortMapRemoval(MapEvent<String, Port> event) {
            log.debug("OpenStack port removed");
            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_PORT_PRE_REMOVE,
                    network(event.oldValue().value().getNetworkId()),
                    event.oldValue().value()));

            notifyDelegate(new OpenstackNetworkEvent(
                    OPENSTACK_PORT_REMOVED,
                    network(event.oldValue().value().getNetworkId()),
                    event.oldValue().value()));
        }

        private void processSecurityGroupUpdate(Port oldPort, Port newPort) {
            List<String> oldSecurityGroups = oldPort.getSecurityGroups() == null ?
                    ImmutableList.of() : oldPort.getSecurityGroups();
            List<String> newSecurityGroups = newPort.getSecurityGroups() == null ?
                    ImmutableList.of() : newPort.getSecurityGroups();

            oldSecurityGroups.stream()
                    .filter(sgId -> !Objects.requireNonNull(
                                    newPort.getSecurityGroups()).contains(sgId))
                    .forEach(sgId -> notifyDelegate(new OpenstackNetworkEvent(
                            OPENSTACK_PORT_SECURITY_GROUP_REMOVED, newPort, sgId
                    )));

            newSecurityGroups.stream()
                    .filter(sgId -> !oldPort.getSecurityGroups().contains(sgId))
                    .forEach(sgId -> notifyDelegate(new OpenstackNetworkEvent(
                            OPENSTACK_PORT_SECURITY_GROUP_ADDED, newPort, sgId
                    )));
        }
    }
}
