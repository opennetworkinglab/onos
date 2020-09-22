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
import io.fabric8.kubernetes.api.model.FieldsV1;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceSpec;
import io.fabric8.kubernetes.api.model.NamespaceStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNamespaceEvent;
import org.onosproject.k8snetworking.api.K8sNamespaceStore;
import org.onosproject.k8snetworking.api.K8sNamespaceStoreDelegate;
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

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.K8sNamespaceEvent.Type.K8S_NAMESPACE_CREATED;
import static org.onosproject.k8snetworking.api.K8sNamespaceEvent.Type.K8S_NAMESPACE_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNamespaceEvent.Type.K8S_NAMESPACE_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes namespace store using consistent map.
 */
@Component(immediate = true, service = K8sNamespaceStore.class)
public class DistributedK8sNamespaceStore
        extends AbstractStore<K8sNamespaceEvent, K8sNamespaceStoreDelegate>
        implements K8sNamespaceStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_NAMESPACE = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Namespace.class)
            .register(ObjectMeta.class)
            .register(NamespaceSpec.class)
            .register(NamespaceStatus.class)
            .register(ManagedFieldsEntry.class)
            .register(FieldsV1.class)
            .register(LinkedHashMap.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Namespace> namespaceMapListener =
            new K8sNamespaceMapListener();

    private ConsistentMap<String, Namespace> namespaceStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        namespaceStore = storageService.<String, Namespace>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_NAMESPACE))
                .withName("k8s-namespace-store")
                .withApplicationId(appId)
                .build();

        namespaceStore.addListener(namespaceMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        namespaceStore.removeListener(namespaceMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNamespace(Namespace namespace) {
        namespaceStore.compute(namespace.getMetadata().getUid(), (uid, existing) -> {
            final String error = namespace.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return namespace;
        });
    }

    @Override
    public void updateNamespace(Namespace namespace) {
        namespaceStore.compute(namespace.getMetadata().getUid(), (uid, existing) -> {
            final String error  = namespace.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return namespace;
        });
    }

    @Override
    public Namespace removeNamespace(String uid) {
        Versioned<Namespace> namespace = namespaceStore.remove(uid);
        if (namespace == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return namespace.value();
    }

    @Override
    public Namespace namespace(String uid) {
        return namespaceStore.asJavaMap().get(uid);
    }

    @Override
    public Set<Namespace> namespaces() {
        return ImmutableSet.copyOf(namespaceStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        namespaceStore.clear();
    }

    private class K8sNamespaceMapListener implements MapEventListener<String, Namespace> {

        @Override
        public void event(MapEvent<String, Namespace> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes namespace created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNamespaceEvent(
                                    K8S_NAMESPACE_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes namespace updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNamespaceEvent(
                                    K8S_NAMESPACE_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes namespace removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNamespaceEvent(
                                    K8S_NAMESPACE_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
