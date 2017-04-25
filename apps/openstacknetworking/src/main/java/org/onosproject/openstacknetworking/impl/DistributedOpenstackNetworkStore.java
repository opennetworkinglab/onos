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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages the inventory of OpenStack network, subnet, and port using a {@code ConsistentMap}.
 */
@Service
@Component(immediate = true)
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
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Network> networkMapListener = new OpenstackNetworkMapListener();
    private final MapEventListener<String, Subnet> subnetMapListener = new OpenstackSubnetMapListener();
    private final MapEventListener<String, Port> portMapListener = new OpenstackPortMapListener();

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

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNetworkStore.removeListener(networkMapListener);
        osSubnetStore.removeListener(subnetMapListener);
        osPortStore.removeListener(portMapListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createNetwork(Network osNet) {
        osNetworkStore.compute(osNet.getId(), (id, existing) -> {
            final String error = osNet.getName() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return osNet;
        });
    }

    @Override
    public void updateNetwork(Network osNet) {
        osNetworkStore.compute(osNet.getId(), (id, existing) -> {
            final String error = osNet.getName() + ERR_NOT_FOUND;
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
        Versioned<Network> versioned = osNetworkStore.get(netId);
        return versioned == null ? null : versioned.value();
    }

    @Override
    public Set<Network> networks() {
        Set<Network> osNets = osNetworkStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osNets);
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
        Versioned<Subnet> osSubnet = osSubnetStore.get(subnetId);
        return osSubnet == null ? null : osSubnet.value();
    }

    @Override
    public Set<Subnet> subnets() {
        Set<Subnet> osSubnets = osSubnetStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSubnets);
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
        Versioned<Port> osPort = osPortStore.get(portId);
        return osPort == null ? null : osPort.value();
    }

    @Override
    public Set<Port> ports() {
        Set<Port> osPorts = osPortStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osPorts);
    }

    @Override
    public void clear() {
        osPortStore.clear();
        osSubnetStore.clear();
        osNetworkStore.clear();
    }

    private class OpenstackNetworkMapListener implements MapEventListener<String, Network> {

        @Override
        public void event(MapEvent<String, Network> event) {
            switch (event.type()) {
                case UPDATE:
                    log.debug("OpenStack network updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_NETWORK_UPDATED,
                                event.newValue().value()));
                    });
                    break;
                case INSERT:
                    log.debug("OpenStack network created {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_NETWORK_CREATED,
                                event.newValue().value()));
                    });
                    break;
                case REMOVE:
                    log.debug("OpenStack network removed {}", event.oldValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_NETWORK_REMOVED,
                                event.oldValue().value()));
                    });
                    break;
                default:
                    log.error("Unsupported event type");
                    break;
            }
        }
    }

    private class OpenstackSubnetMapListener implements MapEventListener<String, Subnet> {

        @Override
        public void event(MapEvent<String, Subnet> event) {
            switch (event.type()) {
                case UPDATE:
                    log.debug("OpenStack subnet updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_SUBNET_UPDATED,
                                network(event.newValue().value().getNetworkId()),
                                event.newValue().value()));
                    });
                    break;
                case INSERT:
                    log.debug("OpenStack subnet created {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_SUBNET_CREATED,
                                network(event.newValue().value().getNetworkId()),
                                event.newValue().value()));
                    });
                    break;
                case REMOVE:
                    log.debug("OpenStack subnet removed {}", event.oldValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_SUBNET_REMOVED,
                                network(event.oldValue().value().getNetworkId()),
                                event.oldValue().value()));
                    });
                    break;
                default:
                    log.error("Unsupported event type");
                    break;
            }
        }
    }

    private class OpenstackPortMapListener implements MapEventListener<String, Port> {

        @Override
        public void event(MapEvent<String, Port> event) {
            switch (event.type()) {
                case UPDATE:
                    log.debug("OpenStack port updated {}", event.newValue());
                    eventExecutor.execute(() -> {
                        Port oldPort = event.oldValue().value();
                        Port newPort = event.newValue().value();
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_PORT_UPDATED,
                                network(event.newValue().value().getNetworkId()), newPort));
                        processSecurityGroupUpdate(oldPort, newPort);
                    });
                    break;
                case INSERT:
                    log.debug("OpenStack port created {}", event.newValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_PORT_CREATED,
                                network(event.newValue().value().getNetworkId()),
                                event.newValue().value()));
                    });
                    break;
                case REMOVE:
                    log.debug("OpenStack port removed {}", event.oldValue());
                    eventExecutor.execute(() -> {
                        notifyDelegate(new OpenstackNetworkEvent(
                                OPENSTACK_PORT_REMOVED,
                                network(event.oldValue().value().getNetworkId()),
                                event.oldValue().value()));
                    });
                    break;
                default:
                    log.error("Unsupported event type");
                    break;
            }
        }

        private void processSecurityGroupUpdate(Port oldPort, Port newPort) {
            List<String> oldSecurityGroups = oldPort.getSecurityGroups() == null ?
                    ImmutableList.of() : oldPort.getSecurityGroups();
            List<String> newSecurityGroups = newPort.getSecurityGroups() == null ?
                    ImmutableList.of() : newPort.getSecurityGroups();

            oldSecurityGroups.stream()
                    .filter(sgId -> !newPort.getSecurityGroups().contains(sgId))
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
