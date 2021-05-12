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
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancerRule;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerStore;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerStoreDelegate;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_CREATED;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_MEMBER_ADDED;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_MEMBER_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_REMOVED;
import static org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerEvent.Type.KUBEVIRT_LOAD_BALANCER_UPDATED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubevirt load balancer store using consistent map.
 */
@Component(immediate = true, service = KubevirtLoadBalancerStore.class)
public class DistributedKubevirtLoadBalancerStore
        extends AbstractStore<KubevirtLoadBalancerEvent, KubevirtLoadBalancerStoreDelegate>
        implements KubevirtLoadBalancerStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final String APP_ID = "org.onosproject.kubevirtnetwork";

    private static final KryoNamespace
            SERIALIZER_KUBEVIRT_LOAD_BALANCER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KubevirtLoadBalancer.class)
            .register(DefaultKubevirtLoadBalancer.class)
            .register(KubevirtLoadBalancerRule.class)
            .register(DefaultKubevirtLoadBalancerRule.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final MapEventListener<String, KubevirtLoadBalancer> loadBalancerMapListener =
            new KubevirtLoadBalancerMapListener();

    private ConsistentMap<String, KubevirtLoadBalancer> loadBalancerStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        loadBalancerStore = storageService.<String, KubevirtLoadBalancer>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_KUBEVIRT_LOAD_BALANCER))
                .withName("kubevirt-loadbalancerstore")
                .withApplicationId(appId)
                .build();
        loadBalancerStore.addListener(loadBalancerMapListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        loadBalancerStore.removeListener(loadBalancerMapListener);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createLoadBalancer(KubevirtLoadBalancer lb) {
        loadBalancerStore.compute(lb.name(), (name, existing) -> {
            final String error = lb.name() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return lb;
        });
    }

    @Override
    public void updateLoadBalancer(KubevirtLoadBalancer lb) {
        loadBalancerStore.compute(lb.name(), (name, existing) -> {
            final String error = lb.name() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return lb;
        });
    }

    @Override
    public KubevirtLoadBalancer removeLoadBalancer(String name) {
        Versioned<KubevirtLoadBalancer> lb = loadBalancerStore.remove(name);
        if (lb == null) {
            final String error = name + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return lb.value();
    }

    @Override
    public KubevirtLoadBalancer loadBalancer(String name) {
        return loadBalancerStore.asJavaMap().get(name);
    }

    @Override
    public Set<KubevirtLoadBalancer> loadBalancers() {
        return ImmutableSet.copyOf(loadBalancerStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        loadBalancerStore.clear();
    }

    private class KubevirtLoadBalancerMapListener
            implements MapEventListener<String, KubevirtLoadBalancer> {

        @Override
        public void event(MapEvent<String, KubevirtLoadBalancer> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("Kubevirt load balancer created");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtLoadBalancerEvent(
                                    KUBEVIRT_LOAD_BALANCER_CREATED, event.newValue().value())));
                    break;
                case UPDATE:
                    log.debug("Kubevirt load balancer updated");
                    eventExecutor.execute(() -> processLoadBalancerMapUpdate(event));
                    break;
                case REMOVE:
                    log.debug("Kubevirt load balancer removed");
                    eventExecutor.execute(() ->
                            notifyDelegate(new KubevirtLoadBalancerEvent(
                                    KUBEVIRT_LOAD_BALANCER_REMOVED, event.oldValue().value())));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processLoadBalancerMapUpdate(MapEvent<String, KubevirtLoadBalancer> event) {
            KubevirtLoadBalancer oldLb = event.oldValue().value();
            KubevirtLoadBalancer newLb = event.newValue().value();

            Set<IpAddress> added = new HashSet<>(newLb.members());
            Set<IpAddress> oldSet = oldLb.members();

            added.removeAll(oldSet);

            if (added.size() > 0) {
                notifyDelegate(new KubevirtLoadBalancerEvent(
                        KUBEVIRT_LOAD_BALANCER_MEMBER_ADDED,
                        newLb,
                        added
                ));
            }

            Set<IpAddress> removed = new HashSet<>(oldLb.members());
            Set<IpAddress> newSet = newLb.members();
            removed.removeAll(newSet);

            if (removed.size() > 0) {
                notifyDelegate(new KubevirtLoadBalancerEvent(
                        KUBEVIRT_LOAD_BALANCER_MEMBER_REMOVED,
                        newLb,
                        removed
                ));
            }

            notifyDelegate(new KubevirtLoadBalancerEvent(
                    KUBEVIRT_LOAD_BALANCER_UPDATED,
                    newLb,
                    oldLb
            ));
        }
    }
}
