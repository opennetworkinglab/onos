/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkStore;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkStoreDelegate;
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
import static org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent.Type.KUBEVIRT_NETWORK_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent.Type.KUBEVIRT_NETWORK_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent.Type.KUBEVIRT_NETWORK_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubevirt network store using consistent map.
 */
@Component(immediate = true, service = KubevirtNetworkStore.class)
public class DistributedKubevirtNetworkStore
        extends AbstractStore<KubevirtNetworkEvent, KubevirtNetworkStoreDelegate>
        implements KubevirtNetworkStore {
    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.kubevirtnetwork";

    private static final KryoNamespace SERIALIZER_KUBEVIRT_NETWORK = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtNetwork.class)
            .register(KubevirtNetwork.Type.class)
            .register(DefaultKubevirtNetwork.class)
            .register(KubevirtIpPool.class)
            .register(KubevirtHostRoute.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtNetwork> networkMapListener =
            new KubevirtNetworkMapListener();

    private ConsistentMap<String, KubevirtNetwork> networkStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        networkStore = storageService.<String, KubevirtNetwork>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_NETWORK))
                .withName("kubevirt-networkstore")
                .withApplicationId(appId)
                .build();

        networkStore.addListener(networkMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkStore.removeListener(networkMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNetwork(KubevirtNetwork network) {
        networkStore.compute(network.networkId(), (networkId, existing) -> {
            final String error = network.networkId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return network;
        });
    }

    @Override
    public void updateNetwork(KubevirtNetwork network) {
        networkStore.compute(network.networkId(), (networkId, existing) -> {
            final String error = network.networkId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return network;
        });
    }

    @Override
    public KubevirtNetwork removeNetwork(String networkId) {
        Versioned<KubevirtNetwork> network = networkStore.remove(networkId);
        if (network == null) {
            final String error = networkId + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return network.value();
    }

    @Override
    public KubevirtNetwork network(String networkId) {
        return networkStore.asJavaMap().get(networkId);
    }

    @Override
    public Set<KubevirtNetwork> networks() {
        return ImmutableSet.copyOf(networkStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        networkStore.clear();
    }

    private class KubevirtNetworkMapListener implements MapEventListener<String, KubevirtNetwork> {

        @Override
        public void event(MapEvent<String, KubevirtNetwork> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt network created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtNetworkEvent(
                            KUBEVIRT_NETWORK_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubevirt network updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtNetworkEvent(
                            KUBEVIRT_NETWORK_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubevirt network removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtNetworkEvent(
                            KUBEVIRT_NETWORK_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
