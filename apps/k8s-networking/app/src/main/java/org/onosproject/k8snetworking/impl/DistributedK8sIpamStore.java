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
import org.onosproject.k8snetworking.api.DefaultK8sIpam;
import org.onosproject.k8snetworking.api.K8sIpam;
import org.onosproject.k8snetworking.api.K8sIpamEvent;
import org.onosproject.k8snetworking.api.K8sIpamStore;
import org.onosproject.k8snetworking.api.K8sIpamStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
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
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of kubernetes IP address management store using consistent map.
 */
@Component(immediate = true, service = K8sIpamStore.class)
public class DistributedK8sIpamStore
        extends AbstractStore<K8sIpamEvent, K8sIpamStoreDelegate>
        implements K8sIpamStore {

    private final Logger log = getLogger(getClass());

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String APP_ID = "org.onosproject.k8snetwork";

    private static final KryoNamespace
            SERIALIZER_K8S_IPAM = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(K8sIpam.class)
            .register(DefaultK8sIpam.class)
            .register(Collection.class)
            .build();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private ConsistentMap<String, K8sIpam> allocatedStore;
    private ConsistentMap<String, K8sIpam> availableStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication(APP_ID);
        allocatedStore = storageService.<String, K8sIpam>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_IPAM))
                .withName("k8s-ipam-allocated-store")
                .withApplicationId(appId)
                .build();
        availableStore = storageService.<String, K8sIpam>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_K8S_IPAM))
                .withName("k8s-ipam-available-store")
                .withApplicationId(appId)
                .build();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    @Override
    public void createAllocatedIp(K8sIpam ipam) {
        allocatedStore.compute(ipam.ipamId(), (ipamId, existing) -> {
            final String error = ipam.ipamId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return ipam;
        });
    }

    @Override
    public void updateAllocatedIp(K8sIpam ipam) {
        allocatedStore.compute(ipam.ipamId(), (ipamId, existing) -> {
            final String error = ipam.ipamId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return ipam;
        });
    }

    @Override
    public K8sIpam removeAllocatedIp(String ipamId) {
        Versioned<K8sIpam> ipam = allocatedStore.remove(ipamId);
        if (ipam == null) {
            final String error = ipamId + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return ipam.value();
    }

    @Override
    public K8sIpam allocatedIp(String ipamId) {
        return allocatedStore.asJavaMap().get(ipamId);
    }

    @Override
    public Set<K8sIpam> allocatedIps() {
        return ImmutableSet.copyOf(allocatedStore.asJavaMap().values());
    }

    @Override
    public void createAvailableIp(K8sIpam ipam) {
        availableStore.compute(ipam.ipamId(), (ipamId, existing) -> {
            final String error = ipam.ipamId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return ipam;
        });
    }

    @Override
    public void updateAvailableIp(K8sIpam ipam) {
        availableStore.compute(ipam.ipamId(), (ipamId, existing) -> {
            final String error = ipam.ipamId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return ipam;
        });
    }

    @Override
    public K8sIpam removeAvailableIp(String ipamId) {
        Versioned<K8sIpam> ipam = availableStore.remove(ipamId);
        if (ipam == null) {
            final String error = ipamId + ERR_NOT_FOUND;
            throw new IllegalArgumentException(error);
        }
        return ipam.value();
    }

    @Override
    public K8sIpam availableIp(String ipamId) {
        return availableStore.asJavaMap().get(ipamId);
    }

    @Override
    public Set<K8sIpam> availableIps() {
        return ImmutableSet.copyOf(availableStore.asJavaMap().values());
    }

    @Override
    public void clear() {
        allocatedStore.clear();
        availableStore.clear();
    }

    @Override
    public void clear(String networkId) {
        Set<K8sIpam> ipams = allocatedStore.asJavaMap().values().stream()
                            .filter(i -> i.networkId().equals(networkId))
                            .collect(Collectors.toSet());
        ipams.forEach(i -> allocatedStore.remove(i.ipamId()));
    }
}
