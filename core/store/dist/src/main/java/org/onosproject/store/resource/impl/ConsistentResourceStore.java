/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.resource.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ContinuousResourceId;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.net.resource.ResourceEvent;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.net.resource.ResourceStore;
import org.onosproject.net.resource.ResourceStoreDelegate;
import org.onosproject.net.resource.Resources;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.CommitStatus;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.resource.ResourceEvent.Type.RESOURCE_ADDED;
import static org.onosproject.net.resource.ResourceEvent.Type.RESOURCE_REMOVED;

/**
 * Implementation of ResourceStore using TransactionalMap.
 */
@Component(immediate = true)
@Service
@Beta
public class ConsistentResourceStore extends AbstractStore<ResourceEvent, ResourceStoreDelegate>
        implements ResourceStore {
    private static final Logger log = LoggerFactory.getLogger(ConsistentResourceStore.class);

    static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(UnifiedDiscreteResources.class)
            .register(new EncodableDiscreteResourcesSerializer(), EncodableDiscreteResources.class)
            .register(GenericDiscreteResources.class)
            .register(EmptyDiscreteResources.class)
            .register(new EncodedResourcesSerializer(), EncodedDiscreteResources.class)
            .register(ContinuousResourceAllocation.class)
            .register(PortNumberCodec.class)
            .register(VlanIdCodec.class)
            .register(MplsLabelCodec.class)
            .build());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService service;

    private ConsistentDiscreteResourceSubStore discreteStore;
    private ConsistentContinuousResourceSubStore continuousStore;

    @Activate
    public void activate() {
        discreteStore = new ConsistentDiscreteResourceSubStore(service);
        continuousStore = new ConsistentContinuousResourceSubStore(service);

        log.info("Started");
    }

    // Computational complexity: O(1) if the resource is discrete type.
    // O(n) if the resource is continuous type where n is the number of the existing allocations for the resource
    @Override
    public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
        checkNotNull(id);
        checkArgument(id instanceof DiscreteResourceId || id instanceof ContinuousResourceId);

        if (id instanceof DiscreteResourceId) {
            return discreteStore.getResourceAllocations((DiscreteResourceId) id);
        } else {
            return continuousStore.getResourceAllocations((ContinuousResourceId) id);
        }
    }

    @Override
    public boolean register(List<Resource> resources) {
        checkNotNull(resources);
        if (log.isTraceEnabled()) {
            resources.forEach(r -> log.trace("registering {}", r));
        }

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        // the order is preserved by LinkedHashMap
        Map<DiscreteResource, List<Resource>> resourceMap = resources.stream()
                .filter(x -> x.parent().isPresent())
                .collect(Collectors.groupingBy(x -> x.parent().get(), LinkedHashMap::new, Collectors.toList()));

        TransactionalDiscreteResourceSubStore discreteTxStore = discreteStore.transactional(tx);
        TransactionalContinuousResourceSubStore continuousTxStore = continuousStore.transactional(tx);
        for (Map.Entry<DiscreteResource, List<Resource>> entry : resourceMap.entrySet()) {
            DiscreteResourceId parentId = entry.getKey().id();
            if (!discreteTxStore.lookup(parentId).isPresent()) {
                return abortTransaction(tx);
            }

            if (!register(discreteTxStore, continuousTxStore, parentId, entry.getValue())) {
                return abortTransaction(tx);
            }
        }

        return tx.commit().whenComplete((status, error) -> {
            if (status == CommitStatus.SUCCESS) {
                log.trace("Transaction commit succeeded on registration: resources={}", resources);
                List<ResourceEvent> events = resources.stream()
                        .filter(x -> x.parent().isPresent())
                        .map(x -> new ResourceEvent(RESOURCE_ADDED, x))
                        .collect(Collectors.toList());
                notifyDelegate(events);
            } else {
                log.warn("Transaction commit failed on registration", error);
            }
        }).join() == CommitStatus.SUCCESS;
    }

    @Override
    public boolean unregister(List<ResourceId> ids) {
        checkNotNull(ids);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalDiscreteResourceSubStore discreteTxStore = discreteStore.transactional(tx);
        TransactionalContinuousResourceSubStore continuousTxStore = continuousStore.transactional(tx);
        // Look up resources by resource IDs
        List<Resource> resources = ids.stream()
                .filter(x -> x.parent().isPresent())
                .map(x -> {
                    // avoid access to consistent map in the case of discrete resource
                    if (x instanceof DiscreteResourceId) {
                        return Optional.of(Resources.discrete((DiscreteResourceId) x).resource());
                    } else {
                        return continuousTxStore.lookup((ContinuousResourceId) x);
                    }
                })
                .flatMap(Tools::stream)
                .collect(Collectors.toList());
        // the order is preserved by LinkedHashMap
        Map<DiscreteResourceId, List<Resource>> resourceMap = resources.stream()
                .collect(Collectors.groupingBy(x -> x.parent().get().id(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<DiscreteResourceId, List<Resource>> entry : resourceMap.entrySet()) {
            if (!unregister(discreteTxStore, continuousTxStore, entry.getKey(), entry.getValue())) {
                log.warn("Failed to unregister {}: Failed to remove {} values.",
                        entry.getKey(), entry.getValue().size());
                log.debug("Failed to unregister {}: Failed to remove values: {}",
                        entry.getKey(), entry.getValue());
                return abortTransaction(tx);
            }
        }

        return tx.commit().whenComplete((status, error) -> {
            if (status == CommitStatus.SUCCESS) {
                List<ResourceEvent> events = resources.stream()
                        .filter(x -> x.parent().isPresent())
                        .map(x -> new ResourceEvent(RESOURCE_REMOVED, x))
                        .collect(Collectors.toList());
                notifyDelegate(events);
            } else {
                log.warn("Failed to unregister {}: Commit failed.", ids, error);
            }
        }).join() == CommitStatus.SUCCESS;
    }

    @Override
    public boolean allocate(List<Resource> resources, ResourceConsumer consumer) {
        checkNotNull(resources);
        checkNotNull(consumer);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalDiscreteResourceSubStore discreteTxStore = discreteStore.transactional(tx);
        TransactionalContinuousResourceSubStore continuousTxStore = continuousStore.transactional(tx);
        for (Resource resource : resources) {
            if (resource instanceof DiscreteResource) {
                if (!discreteTxStore.allocate(consumer.consumerId(), (DiscreteResource) resource)) {
                    return abortTransaction(tx);
                }
            } else if (resource instanceof ContinuousResource) {
                if (!continuousTxStore.allocate(consumer.consumerId(), (ContinuousResource) resource)) {
                    return abortTransaction(tx);
                }
            }
        }

        return tx.commit().join() == CommitStatus.SUCCESS;
    }

    @Override
    public boolean release(List<ResourceAllocation> allocations) {
        checkNotNull(allocations);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalDiscreteResourceSubStore discreteTxStore = discreteStore.transactional(tx);
        TransactionalContinuousResourceSubStore continuousTxStore = continuousStore.transactional(tx);
        for (ResourceAllocation allocation : allocations) {
            Resource resource = allocation.resource();
            ResourceConsumerId consumerId = allocation.consumerId();

            if (resource instanceof DiscreteResource) {
                if (!discreteTxStore.release((DiscreteResource) resource, consumerId)) {
                    return abortTransaction(tx);
                }
            } else if (resource instanceof ContinuousResource) {
                if (!continuousTxStore.release((ContinuousResource) resource, consumerId)) {
                    return abortTransaction(tx);
                }
            }
        }

        return tx.commit().join() == CommitStatus.SUCCESS;
    }

    // computational complexity: O(1) if the resource is discrete type.
    // O(n) if the resource is continuous type where n is the number of the children of
    // the specified resource's parent
    @Override
    public boolean isAvailable(Resource resource) {
        checkNotNull(resource);
        checkArgument(resource instanceof DiscreteResource || resource instanceof ContinuousResource);

        if (resource instanceof DiscreteResource) {
            // check if already consumed
            return discreteStore.isAvailable((DiscreteResource) resource);
        } else {
            return continuousStore.isAvailable((ContinuousResource) resource);
        }
    }

    // computational complexity: O(n + m) where n is the number of entries in discreteConsumers
    // and m is the number of allocations for all continuous resources
    @Override
    public Collection<Resource> getResources(ResourceConsumer consumer) {
        checkNotNull(consumer);

        // NOTE: getting all entries may become performance bottleneck
        // TODO: revisit for better backend data structure
        Stream<DiscreteResource> discrete = discreteStore.getResources(consumer.consumerId());
        Stream<ContinuousResource> continuous = continuousStore.getResources(consumer.consumerId());

        return Stream.concat(discrete, continuous).collect(Collectors.toList());
    }

    // computational complexity: O(1)
    @Override
    public Set<Resource> getChildResources(DiscreteResourceId parent) {
        checkNotNull(parent);

        return ImmutableSet.<Resource>builder()
                .addAll(discreteStore.getChildResources(parent))
                .addAll(continuousStore.getChildResources(parent))
                .build();
    }

    @Override
    public <T> Set<Resource> getChildResources(DiscreteResourceId parent, Class<T> cls) {
        checkNotNull(parent);
        checkNotNull(cls);

        return ImmutableSet.<Resource>builder()
                .addAll(discreteStore.getChildResources(parent, cls))
                .addAll(continuousStore.getChildResources(parent, cls))
                .build();
    }

    // computational complexity: O(n) where n is the number of the children of the parent
    @Override
    public <T> Collection<Resource> getAllocatedResources(DiscreteResourceId parent, Class<T> cls) {
        checkNotNull(parent);
        checkNotNull(cls);

        Stream<DiscreteResource> discrete = discreteStore.getAllocatedResources(parent, cls);
        Stream<ContinuousResource> continuous = continuousStore.getAllocatedResources(parent, cls);

        return Stream.concat(discrete, continuous).collect(Collectors.toList());
    }

    /**
     * Abort the transaction.
     *
     * @param tx transaction context
     * @return always false
     */
    private boolean abortTransaction(TransactionContext tx) {
        tx.abort();
        return false;
    }

    /**
     * Appends the values to the existing values associated with the specified key.
     * If the map already has all the given values, appending will not happen.
     *
     * @param key    key specifying values
     * @param values values to be appended
     * @return true if the operation succeeds, false otherwise.
     */
    // computational complexity: O(n) where n is the number of the specified value
    private boolean register(TransactionalDiscreteResourceSubStore discreteTxStore,
                             TransactionalContinuousResourceSubStore continuousTxStore,
                             DiscreteResourceId key, List<Resource> values) {
        // it's assumed that the passed "values" is non-empty

        // This is 2-pass scan. Nicer to have 1-pass scan
        Set<DiscreteResource> discreteValues = values.stream()
                .filter(x -> x instanceof DiscreteResource)
                .map(x -> (DiscreteResource) x)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<ContinuousResource> continuousValues = values.stream()
                .filter(x -> x instanceof ContinuousResource)
                .map(x -> (ContinuousResource) x)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return discreteTxStore.register(key, discreteValues)
                && continuousTxStore.register(key, continuousValues);
    }

    /**
     * Removes the values from the existing values associated with the specified key.
     * If the map doesn't contain the given values, removal will not happen.
     *
     * @param discreteTxStore   map holding multiple discrete resources for a key
     * @param continuousTxStore map holding multiple continuous resources for a key
     * @param key               key specifying values
     * @param values            values to be removed
     * @return true if the operation succeeds, false otherwise
     */
    private boolean unregister(TransactionalDiscreteResourceSubStore discreteTxStore,
                               TransactionalContinuousResourceSubStore continuousTxStore,
                               DiscreteResourceId key, List<Resource> values) {
        // it's assumed that the passed "values" is non-empty

        // This is 2-pass scan. Nicer to have 1-pass scan
        Set<DiscreteResource> discreteValues = values.stream()
                .filter(x -> x instanceof DiscreteResource)
                .map(x -> (DiscreteResource) x)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<ContinuousResource> continuousValues = values.stream()
                .filter(x -> x instanceof ContinuousResource)
                .map(x -> (ContinuousResource) x)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return discreteTxStore.unregister(key, discreteValues)
                && continuousTxStore.unregister(key, continuousValues);
    }
}
