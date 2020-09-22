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
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressStatus;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sIngressEvent;
import org.onosproject.k8snetworking.api.K8sIngressStore;
import org.onosproject.k8snetworking.api.K8sIngressStoreDelegate;
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

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.K8sIngressEvent.Type.K8S_INGRESS_CREATED;
import static org.onosproject.k8snetworking.api.K8sIngressEvent.Type.K8S_INGRESS_REMOVED;
import static org.onosproject.k8snetworking.api.K8sIngressEvent.Type.K8S_INGRESS_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes ingress store using consistent map.
 */
@Component(immediate = true, service = K8sIngressStore.class)
public class DistributedK8sIngressStore
        extends AbstractStore<K8sIngressEvent, K8sIngressStoreDelegate>
        implements K8sIngressStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
        SERIALIZER_K8S_INGRESS = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Ingress.class)
            .register(ObjectMeta.class)
            .register(IngressSpec.class)
            .register(IngressStatus.class)
            .register(IngressBackend.class)
            .register(IngressRule.class)
            .register(IngressTLS.class)
            .register(LoadBalancerStatus.class)
            .register(LoadBalancerIngress.class)
            .register(ManagedFieldsEntry.class)
            .register(FieldsV1.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, Ingress> ingressMapListener = new K8sIngressMapListener();
    private ConsistentMap<String, Ingress> ingressStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        ingressStore = storageService.<String, Ingress>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_INGRESS))
                .withName("k8s-ingress-store")
                .withApplicationId(appId)
                .build();

        ingressStore.addListener(ingressMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        ingressStore.removeListener(ingressMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createIngress(Ingress ingress) {
        ingressStore.compute(ingress.getMetadata().getUid(), (uid, existing) -> {
            final String error = ingress.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return ingress;
        });
    }

    @Override
    public void updateIngress(Ingress ingress) {
        ingressStore.compute(ingress.getMetadata().getUid(), (uid, existing) -> {
            final String error = ingress.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return ingress;
        });
    }

    @Override
    public Ingress removeIngress(String uid) {
        Versioned<Ingress> ingress = ingressStore.remove(uid);
        if (ingress == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return ingress.value();
    }

    @Override
    public Ingress ingress(String uid) {
        return ingressStore.asJavaMap().get(uid);
    }

    @Override
    public Set<Ingress> ingresses() {
        return ImmutableSet.copyOf(ingressStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        ingressStore.clear();
    }

    private class K8sIngressMapListener implements MapEventListener<String, Ingress> {

        @Override
        public void event(MapEvent<String, Ingress> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes ingress created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sIngressEvent(
                                    K8S_INGRESS_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes ingress updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sIngressEvent(
                                    K8S_INGRESS_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes ingress removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sIngressEvent(
                                    K8S_INGRESS_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
