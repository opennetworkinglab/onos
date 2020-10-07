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
import io.fabric8.kubernetes.api.model.ClientIPConfig;
import io.fabric8.kubernetes.api.model.FieldsV1;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceStatus;
import io.fabric8.kubernetes.api.model.SessionAffinityConfig;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sServiceEvent;
import org.onosproject.k8snetworking.api.K8sServiceStore;
import org.onosproject.k8snetworking.api.K8sServiceStoreDelegate;
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
import static org.onosproject.k8snetworking.api.K8sServiceEvent.Type.K8S_SERVICE_CREATED;
import static org.onosproject.k8snetworking.api.K8sServiceEvent.Type.K8S_SERVICE_REMOVED;
import static org.onosproject.k8snetworking.api.K8sServiceEvent.Type.K8S_SERVICE_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes service store using consistent map.
 */
@Component(immediate = true, service = K8sServiceStore.class)
public class DistributedK8sServiceStore
        extends AbstractStore<K8sServiceEvent, K8sServiceStoreDelegate>
        implements K8sServiceStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_SERVICE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Service.class)
            .register(ObjectMeta.class)
            .register(ServiceSpec.class)
            .register(ServiceStatus.class)
            .register(LoadBalancerStatus.class)
            .register(LoadBalancerIngress.class)
            .register(ServicePort.class)
            .register(IntOrString.class)
            .register(SessionAffinityConfig.class)
            .register(ClientIPConfig.class)
            .register(ManagedFieldsEntry.class)
            .register(FieldsV1.class)
            .register(OwnerReference.class)
            .register(LinkedHashMap.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Service> serviceMapListener =
            new K8sServiceMapListener();

    private ConsistentMap<String, Service> serviceStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        serviceStore = storageService.<String, Service>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_SERVICE))
                .withName("k8s-service-store")
                .withApplicationId(appId)
                .build();

        serviceStore.addListener(serviceMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        serviceStore.removeListener(serviceMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createService(Service service) {
        serviceStore.compute(service.getMetadata().getUid(), (uid, existing) -> {
            final String error = service.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return service;
        });
    }

    @Override
    public void updateService(Service service) {
        serviceStore.compute(service.getMetadata().getUid(), (uid, existing) -> {
            final String error = service.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return service;
        });
    }

    @Override
    public Service removeService(String uid) {
        Versioned<Service> service = serviceStore.remove(uid);
        if (service == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return service.value();
    }

    @Override
    public Service service(String uid) {
        return serviceStore.asJavaMap().get(uid);
    }

    @Override
    public Set<Service> services() {
        return ImmutableSet.copyOf(serviceStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        serviceStore.clear();
    }

    private class K8sServiceMapListener implements MapEventListener<String, Service> {

        @Override
        public void event(MapEvent<String, Service> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes service created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sServiceEvent(
                                K8S_SERVICE_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes service updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sServiceEvent(
                                    K8S_SERVICE_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes service removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sServiceEvent(
                                    K8S_SERVICE_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
