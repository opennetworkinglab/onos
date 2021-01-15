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
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirement;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.v1.IPBlock;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPort;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicySpec;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyStore;
import org.onosproject.k8snetworking.api.K8sNetworkPolicyStoreDelegate;
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
import static org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent.Type.K8S_NETWORK_POLICY_CREATED;
import static org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent.Type.K8S_NETWORK_POLICY_REMOVED;
import static org.onosproject.k8snetworking.api.K8sNetworkPolicyEvent.Type.K8S_NETWORK_POLICY_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes network policy store using consistent map.
 */
@Component(immediate = true, service = K8sNetworkPolicyStore.class)
public class DistributedK8sNetworkPolicyStore
        extends AbstractStore<K8sNetworkPolicyEvent, K8sNetworkPolicyStoreDelegate>
        implements K8sNetworkPolicyStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_NETWORK_POLICY = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(NetworkPolicy.class)
            .register(ObjectMeta.class)
            .register(NetworkPolicySpec.class)
            .register(NetworkPolicyIngressRule.class)
            .register(NetworkPolicyEgressRule.class)
            .register(LabelSelector.class)
            .register(NetworkPolicyPeer.class)
            .register(NetworkPolicyPort.class)
            .register(IPBlock.class)
            .register(LabelSelector.class)
            .register(LabelSelectorRequirement.class)
            .register(ManagedFieldsEntry.class)
            .register(FieldsV1.class)
            .register(LinkedHashMap.class)
            .register(IntOrString.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, NetworkPolicy> networkPolicyMapListener = new K8sNetworkPolicyMapListener();

    private ConsistentMap<String, NetworkPolicy> networkPolicyStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        networkPolicyStore = storageService.<String, NetworkPolicy>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_NETWORK_POLICY))
                .withName("k8s-network-policy-store")
                .withApplicationId(appId)
                .build();

        networkPolicyStore.addListener(networkPolicyMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        networkPolicyStore.removeListener(networkPolicyMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createNetworkPolicy(NetworkPolicy networkPolicy) {
        networkPolicyStore.compute(networkPolicy.getMetadata().getUid(), (uid, existing) -> {
            final String error = networkPolicy.getMetadata().getUid() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return networkPolicy;
        });
    }

    @Override
    public void updateNetworkPolicy(NetworkPolicy networkPolicy) {
        networkPolicyStore.compute(networkPolicy.getMetadata().getUid(), (uid, existing) -> {
            final String error  = networkPolicy.getMetadata().getUid() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return networkPolicy;
        });
    }

    @Override
    public NetworkPolicy removeNetworkPolicy(String uid) {
        Versioned<NetworkPolicy> networkPolicy = networkPolicyStore.remove(uid);
        if (networkPolicy == null) {
            final String error = uid + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return networkPolicy.value();
    }

    @Override
    public NetworkPolicy networkPolicy(String uid) {
        return networkPolicyStore.asJavaMap().get(uid);
    }

    @Override
    public Set<NetworkPolicy> networkPolicies() {
        return ImmutableSet.copyOf(networkPolicyStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        networkPolicyStore.clear();
    }

    private class K8sNetworkPolicyMapListener implements MapEventListener<String, NetworkPolicy> {

        @Override
        public void event(MapEvent<String, NetworkPolicy> event) {

            switch (event.type()) {
                case INSERT:
                    log.debug("Kubernetes network policy created {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNetworkPolicyEvent(
                                    K8S_NETWORK_POLICY_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubernetes network policy updated {}", event.newValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNetworkPolicyEvent(
                                    K8S_NETWORK_POLICY_UPDATED, event.newValue().value())));
                    break;
                case REMOVE:
                    log.debug("Kubernetes network policy removed {}", event.oldValue());
                    eventExecutor.execute(() ->
                            notifyDelegate(new K8sNetworkPolicyEvent(
                                    K8S_NETWORK_POLICY_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }
    }
}
