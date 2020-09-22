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
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.FieldsV1;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sEndpointsEvent;
import org.onosproject.k8snetworking.api.K8sEndpointsStore;
import org.onosproject.k8snetworking.api.K8sEndpointsStoreDelegate;
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
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.K8sEndpointsEvent.Type.K8S_ENDPOINTS_CREATED;
import static org.onosproject.k8snetworking.api.K8sEndpointsEvent.Type.K8S_ENDPOINTS_REMOVED;
import static org.onosproject.k8snetworking.api.K8sEndpointsEvent.Type.K8S_ENDPOINTS_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes service store using consistent map.
 */
@Component(immediate = true, service = K8sEndpointsStore.class)
public class DistributedK8sEndpointsStore
        extends AbstractStore<K8sEndpointsEvent, K8sEndpointsStoreDelegate>
        implements K8sEndpointsStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_ENDPOINTS = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Endpoints.class)
            .register(ObjectMeta.class)
            .register(EndpointSubset.class)
            .register(EndpointAddress.class)
            .register(ObjectReference.class)
            .register(EndpointPort.class)
            .register(ManagedFieldsEntry.class)
            .register(FieldsV1.class)
            .register(LinkedHashMap.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Endpoints> endpointsMapListener =
            new K8sEndpointsMapListener();

    private ConsistentMap<String, Endpoints> endpointsStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        endpointsStore = storageService.<String, Endpoints>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_ENDPOINTS))
                .withName("k8s-endpoints-store")
                .withApplicationId(appId)
                .build();

        endpointsStore.addListener(endpointsMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        endpointsStore.removeListener(endpointsMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createEndpoints(Endpoints endpoints) {
        endpointsStore.compute(endpoints.getMetadata().getUid(), (uid, existing) -> {
            final String error = endpoints.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return endpoints;
        });
    }

    @Override
    public void updateEndpoints(Endpoints endpoints) {
        endpointsStore.compute(endpoints.getMetadata().getUid(), (uid, existing) -> {
            final String error = endpoints.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return endpoints;
        });
    }

    @Override
    public Endpoints removeEndpoints(String uid) {
        Versioned<Endpoints> endpoints = endpointsStore.remove(uid);
        if (endpoints == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return endpoints.value();
    }

    @Override
    public Endpoints endpoints(String uid) {
        return endpointsStore.asJavaMap().get(uid);
    }

    @Override
    public Set<Endpoints> endpointses() {
        return ImmutableSet.copyOf(endpointsStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        endpointsStore.clear();
    }

    private class K8sEndpointsMapListener implements MapEventListener<String, Endpoints> {

        @Override
        public void event(MapEvent<String, Endpoints> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes endpoints created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sEndpointsEvent(
                                    K8S_ENDPOINTS_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes endpoints updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sEndpointsEvent(
                                    K8S_ENDPOINTS_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes endpoints removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sEndpointsEvent(
                                    K8S_ENDPOINTS_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
