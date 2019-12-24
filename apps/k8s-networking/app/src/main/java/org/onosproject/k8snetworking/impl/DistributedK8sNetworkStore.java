/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.DefaultK8sNetwork;
import org.onosproject.k8snetworking.api.DefaultK8sPort;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkEvent;
import org.onosproject.k8snetworking.api.K8sNetworkStore;
import org.onosproject.k8snetworking.api.K8sNetworkStoreDelegate;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_NETWORK_CREATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_NETWORK_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_NETWORK_UPDATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_ACTIVATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_CREATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_INACTIVATED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNetworkEvent.Type.K8S_PORT_UPDATED;
import static org.onosproject.k8snetworking.api.K8sPort.State.ACTIVE;
import static org.onosproject.k8snetworking.api.K8sPort.State.INACTIVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes network store using consistent map.
 */
@Component(immediate = true, service = K8sNetworkStore.class)
public class DistributedK8sNetworkStore
    extends AbstractStore<K8sNetworkEvent, K8sNetworkStoreDelegate>
    implements K8sNetworkStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_NETWORK_PORT = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(K8sNetwork.class)
            .register(K8sNetwork.Type.class)
            .register(DefaultK8sNetwork.class)
            .register(K8sPort.class)
            .register(K8sPort.State.class)
            .register(DefaultK8sPort.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, K8sNetwork> networkMapListener =
            new K8sNetworkMapListener();
    private final MapEventListener<String, K8sPort> portMapListener =
            new K8sPortMapListener();

    private ConsistentMap<String, K8sNetwork> networkStore;
    private ConsistentMap<String, K8sPort> portStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        networkStore = storageService.<String, K8sNetwork>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_NETWORK_PORT))
                .withName("k8s-networkstore")
                .withApplicationId(appId)
                .build();
        portStore = storageService.<String, K8sPort>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_NETWORK_PORT))
                .withName("k8s-portstore")
                .withApplicationId(appId)
                .build();
        networkStore.addListener(networkMapListener);
        portStore.addListener(portMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkStore.removeListener(networkMapListener);
        portStore.removeListener(portMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNetwork(K8sNetwork network) {
        networkStore.compute(network.networkId(), (networkId, existing) -> {
            final String error = network.networkId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return network;
        });
    }

    @Override
    public void updateNetwork(K8sNetwork network) {
        networkStore.compute(network.networkId(), (networkId, existing) -> {
            final String error = network.networkId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return network;
        });
    }

    @Override
    public K8sNetwork removeNetwork(String networkId) {
        Versioned<K8sNetwork> network = networkStore.remove(networkId);
        if (network == null) {
            final String error = networkId + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return network.value();
    }

    @Override
    public K8sNetwork network(String networkId) {
        return networkStore.asJavaMap().get(networkId);
    }

    @Override
    public Set<K8sNetwork> networks() {
        return ImmutableSet.copyOf(networkStore.asJavaMap().values());
    }

    @Override
    public void createPort(K8sPort port) {
        portStore.compute(port.portId(), (portId, existing) -> {
            final String error = port.portId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return port;
        });
    }

    @Override
    public void updatePort(K8sPort port) {
        portStore.compute(port.portId(), (portId, existing) -> {
            final String error = port.portId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return port;
        });
    }

    @Override
    public K8sPort removePort(String portId) {
        Versioned<K8sPort> port = portStore.remove(portId);
        if (port == null) {
            final String error = portId + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return port.value();
    }

    @Override
    public Set<K8sPort> ports() {
        return ImmutableSet.copyOf(portStore.asJavaMap().values());
    }

    @Override
    public K8sPort port(String portId) {
        return portStore.asJavaMap().get(portId);
    }

    @Override
    public void clear() {
        portStore.clear();
        networkStore.clear();
    }

    private class K8sNetworkMapListener implements MapEventListener<String, K8sNetwork> {

        @Override
        public void event(MapEvent<String, K8sNetwork> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes network created {}", event.newValue());
                    eventExecutor.execute(() ->
                        notifyDelegate(new K8sNetworkEvent(
                                K8S_NETWORK_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes network updated {}", event.newValue());
                    eventExecutor.execute(() ->
                        notifyDelegate(new K8sNetworkEvent(
                                K8S_NETWORK_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes network removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNetworkEvent(
                                    K8S_NETWORK_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class K8sPortMapListener implements MapEventListener<String, K8sPort> {

        @Override
        public void event(MapEvent<String, K8sPort> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes port created");
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNetworkEvent(
                                K8S_PORT_CREATED,
                                network(event.newValue().value().networkId()),
                                event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes port updated");
                    eventExecutor.execute(() -> processPortUpdate(event));
                    break;
                case REMOVE:
                    log.debug("Kubernetes port removed");

                    // if the event object has invalid port value, we do not
                    // propagate K8S_PORT_REMOVED event.
                    if (event.oldValue() != null &&
                            event.oldValue().value() != null) {
                        notifyDelegate(new K8sNetworkEvent(
                                K8S_PORT_REMOVED,
                                network(event.oldValue().value().networkId()),
                                event.oldValue().value()));
                    }

                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processPortUpdate(MapEvent<String, K8sPort> event) {
            K8sPort.State oldState = event.oldValue().value().state();
            K8sPort.State newState = event.newValue().value().state();

            eventExecutor.execute(() ->
                    notifyDelegate(new K8sNetworkEvent(
                            K8S_PORT_UPDATED,
                            network(event.newValue().value().networkId()),
                            event.newValue().value())));

            if (oldState == INACTIVE && newState == ACTIVE) {
                notifyDelegate(new K8sNetworkEvent(
                        K8S_PORT_ACTIVATED,
                        network(event.newValue().value().networkId()),
                        event.newValue().value()));
            }

            if (oldState == ACTIVE && newState == INACTIVE) {
                notifyDelegate(new K8sNetworkEvent(
                        K8S_PORT_INACTIVATED,
                        network(event.newValue().value().networkId()),
                        event.newValue().value()));
            }
        }
    }
}
