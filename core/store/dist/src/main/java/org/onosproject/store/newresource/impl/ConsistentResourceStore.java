/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.store.newresource.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.GuavaCollectors;
import org.onlab.util.Tools;
import org.onosproject.net.newresource.ContinuousResource;
import org.onosproject.net.newresource.ContinuousResourceId;
import org.onosproject.net.newresource.DiscreteResource;
import org.onosproject.net.newresource.DiscreteResourceId;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceEvent;
import org.onosproject.net.newresource.ResourceId;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceStore;
import org.onosproject.net.newresource.ResourceStoreDelegate;
import org.onosproject.net.newresource.Resources;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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
import static org.onosproject.net.newresource.ResourceEvent.Type.*;

/**
 * Implementation of ResourceStore using TransactionalMap.
 */
@Component(immediate = true)
@Service
@Beta
public class ConsistentResourceStore extends AbstractStore<ResourceEvent, ResourceStoreDelegate>
        implements ResourceStore {
    private static final Logger log = LoggerFactory.getLogger(ConsistentResourceStore.class);

    private static final String DISCRETE_CONSUMER_MAP = "onos-discrete-consumers";
    private static final String CONTINUOUS_CONSUMER_MAP = "onos-continuous-consumers";
    private static final String CHILD_MAP = "onos-resource-children";
    private static final Serializer SERIALIZER = Serializer.using(
            Arrays.asList(KryoNamespaces.BASIC, KryoNamespaces.API),
            ContinuousResourceAllocation.class);

    // TODO: We should provide centralized values for this
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 1_000; // millis

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService service;

    private ConsistentMap<DiscreteResourceId, ResourceConsumer> discreteConsumers;
    private ConsistentMap<ContinuousResourceId, ContinuousResourceAllocation> continuousConsumers;
    private ConsistentMap<DiscreteResourceId, Set<Resource>> childMap;

    @Activate
    public void activate() {
        discreteConsumers = service.<DiscreteResourceId, ResourceConsumer>consistentMapBuilder()
                .withName(DISCRETE_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        continuousConsumers = service.<ContinuousResourceId, ContinuousResourceAllocation>consistentMapBuilder()
                .withName(CONTINUOUS_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        childMap = service.<DiscreteResourceId, Set<Resource>>consistentMapBuilder()
                .withName(CHILD_MAP)
                .withSerializer(SERIALIZER)
                .build();

        Tools.retryable(() -> childMap.put(Resource.ROOT.id(), new LinkedHashSet<>()),
                        ConsistentMapException.class, MAX_RETRIES, RETRY_DELAY);
        log.info("Started");
    }

    // Computational complexity: O(1) if the resource is discrete type.
    // O(n) if the resource is continuous type where n is the number of the existing allocations for the resource
    @Override
    public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
        checkNotNull(id);
        checkArgument(id instanceof DiscreteResourceId || id instanceof ContinuousResourceId);

        if (id instanceof DiscreteResourceId) {
            return getResourceAllocations((DiscreteResourceId) id);
        } else {
            return getResourceAllocations((ContinuousResourceId) id);
        }
    }

    // computational complexity: O(1)
    private List<ResourceAllocation> getResourceAllocations(DiscreteResourceId resource) {
        Versioned<ResourceConsumer> consumer = discreteConsumers.get(resource);
        if (consumer == null) {
            return ImmutableList.of();
        }

        return ImmutableList.of(new ResourceAllocation(Resources.discrete(resource).resource(), consumer.value()));
    }

    // computational complexity: O(n) where n is the number of the existing allocations for the resource
    private List<ResourceAllocation> getResourceAllocations(ContinuousResourceId resource) {
        Versioned<ContinuousResourceAllocation> allocations = continuousConsumers.get(resource);
        if (allocations == null) {
            return ImmutableList.of();
        }

        return allocations.value().allocations().stream()
                .filter(x -> x.resource().id().equals(resource))
                .collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public boolean register(List<Resource> resources) {
        checkNotNull(resources);
        if (log.isTraceEnabled()) {
            resources.forEach(r -> log.trace("registering {}", r));
        }

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<DiscreteResourceId, Set<Resource>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);

        // the order is preserved by LinkedHashMap
        Map<DiscreteResource, List<Resource>> resourceMap = resources.stream()
                .filter(x -> x.parent().isPresent())
                .collect(Collectors.groupingBy(x -> x.parent().get(), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<DiscreteResource, List<Resource>> entry: resourceMap.entrySet()) {
            if (!lookup(childTxMap, entry.getKey().id()).isPresent()) {
                return abortTransaction(tx);
            }

            if (!appendValues(childTxMap, entry.getKey().id(), entry.getValue())) {
                return abortTransaction(tx);
            }
        }

        boolean success = tx.commit();
        if (success) {
            log.trace("Transaction commit succeeded on registration: resources={}", resources);
            List<ResourceEvent> events = resources.stream()
                    .filter(x -> x.parent().isPresent())
                    .map(x -> new ResourceEvent(RESOURCE_ADDED, x))
                    .collect(Collectors.toList());
            notifyDelegate(events);
        } else {
            log.debug("Transaction commit failed on registration: resources={}", resources);
        }
        return success;
    }

    @Override
    public boolean unregister(List<ResourceId> ids) {
        checkNotNull(ids);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<DiscreteResourceId, Set<Resource>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);
        TransactionalMap<DiscreteResourceId, ResourceConsumer> discreteConsumerTxMap =
                tx.getTransactionalMap(DISCRETE_CONSUMER_MAP, SERIALIZER);
        TransactionalMap<ContinuousResourceId, ContinuousResourceAllocation> continuousConsumerTxMap =
                tx.getTransactionalMap(CONTINUOUS_CONSUMER_MAP, SERIALIZER);

        // Look up resources by resource IDs
        List<Resource> resources = ids.stream()
                .filter(x -> x.parent().isPresent())
                .map(x -> {
                    // avoid access to consistent map in the case of discrete resource
                    if (x instanceof DiscreteResourceId) {
                        return Optional.of(Resources.discrete((DiscreteResourceId) x).resource());
                    } else {
                        return lookup(childTxMap, x);
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        // the order is preserved by LinkedHashMap
        Map<DiscreteResourceId, List<Resource>> resourceMap = resources.stream()
                .collect(Collectors.groupingBy(x -> x.parent().get().id(), LinkedHashMap::new, Collectors.toList()));

        // even if one of the resources is allocated to a consumer,
        // all unregistrations are regarded as failure
        for (Map.Entry<DiscreteResourceId, List<Resource>> entry: resourceMap.entrySet()) {
            boolean allocated = entry.getValue().stream().anyMatch(x -> {
                if (x instanceof DiscreteResource) {
                    return discreteConsumerTxMap.get(((DiscreteResource) x).id()) != null;
                } else if (x instanceof ContinuousResource) {
                    ContinuousResourceAllocation allocations =
                            continuousConsumerTxMap.get(((ContinuousResource) x).id());
                    return allocations != null && !allocations.allocations().isEmpty();
                } else {
                    return false;
                }
            });
            if (allocated) {
                log.warn("Failed to unregister {}: allocation exists", entry.getKey());
                return abortTransaction(tx);
            }

            if (!removeValues(childTxMap, entry.getKey(), entry.getValue())) {
                log.warn("Failed to unregister {}: Failed to remove {} values.",
                          entry.getKey(), entry.getValue().size());
                log.debug("Failed to unregister {}: Failed to remove values: {}",
                         entry.getKey(), entry.getValue());
                return abortTransaction(tx);
            }
        }

        boolean success = tx.commit();
        if (success) {
            List<ResourceEvent> events = resources.stream()
                    .filter(x -> x.parent().isPresent())
                    .map(x -> new ResourceEvent(RESOURCE_REMOVED, x))
                    .collect(Collectors.toList());
            notifyDelegate(events);
        } else {
            log.warn("Failed to unregister {}: Commit failed.", ids);
        }
        return success;
    }

    @Override
    public boolean allocate(List<Resource> resources, ResourceConsumer consumer) {
        checkNotNull(resources);
        checkNotNull(consumer);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<DiscreteResourceId, Set<Resource>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);
        TransactionalMap<DiscreteResourceId, ResourceConsumer> discreteConsumerTxMap =
                tx.getTransactionalMap(DISCRETE_CONSUMER_MAP, SERIALIZER);
        TransactionalMap<ContinuousResourceId, ContinuousResourceAllocation> continuousConsumerTxMap =
                tx.getTransactionalMap(CONTINUOUS_CONSUMER_MAP, SERIALIZER);

        for (Resource resource: resources) {
            // if the resource is not registered, then abort
            Optional<Resource> lookedUp = lookup(childTxMap, resource.id());
            if (!lookedUp.isPresent()) {
                return abortTransaction(tx);
            }

            if (resource instanceof DiscreteResource) {
                ResourceConsumer oldValue = discreteConsumerTxMap.put(((DiscreteResource) resource).id(), consumer);
                if (oldValue != null) {
                    return abortTransaction(tx);
                }
            } else if (resource instanceof ContinuousResource) {
                // Down cast: this must be safe as ContinuousResource is associated with ContinuousResourceId
                ContinuousResource continuous = (ContinuousResource) lookedUp.get();
                ContinuousResourceAllocation allocations = continuousConsumerTxMap.get(continuous.id());
                if (!hasEnoughResource(continuous, (ContinuousResource) resource, allocations)) {
                    return abortTransaction(tx);
                }

                boolean success = appendValue(continuousConsumerTxMap,
                        continuous, new ResourceAllocation(continuous, consumer));
                if (!success) {
                    return abortTransaction(tx);
                }
            }
        }

        return tx.commit();
    }

    @Override
    public boolean release(List<ResourceAllocation> allocations) {
        checkNotNull(allocations);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<DiscreteResourceId, ResourceConsumer> discreteConsumerTxMap =
                tx.getTransactionalMap(DISCRETE_CONSUMER_MAP, SERIALIZER);
        TransactionalMap<ContinuousResourceId, ContinuousResourceAllocation> continuousConsumerTxMap =
                tx.getTransactionalMap(CONTINUOUS_CONSUMER_MAP, SERIALIZER);

        for (ResourceAllocation allocation : allocations) {
            Resource resource = allocation.resource();
            ResourceConsumer consumer = allocation.consumer();

            if (resource instanceof DiscreteResource) {
                // if this single release fails (because the resource is allocated to another consumer,
                // the whole release fails
                if (!discreteConsumerTxMap.remove(((DiscreteResource) resource).id(), consumer)) {
                    return abortTransaction(tx);
                }
            } else if (resource instanceof ContinuousResource) {
                ContinuousResource continuous = (ContinuousResource) resource;
                ContinuousResourceAllocation continuousAllocation = continuousConsumerTxMap.get(continuous.id());
                ImmutableList<ResourceAllocation> newAllocations = continuousAllocation.allocations().stream()
                        .filter(x -> !(x.consumer().equals(consumer) &&
                                ((ContinuousResource) x.resource()).value() == continuous.value()))
                        .collect(GuavaCollectors.toImmutableList());

                if (!continuousConsumerTxMap.replace(continuous.id(), continuousAllocation,
                        new ContinuousResourceAllocation(continuousAllocation.original(), newAllocations))) {
                    return abortTransaction(tx);
                }
            }
        }

        return tx.commit();
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
            return getResourceAllocations(resource.id()).isEmpty();
        } else {
            return isAvailable((ContinuousResource) resource);
        }
    }

    // computational complexity: O(n) where n is the number of existing allocations for the resource
    private boolean isAvailable(ContinuousResource resource) {
        // check if it's registered or not.
        Versioned<Set<Resource>> children = childMap.get(resource.parent().get().id());
        if (children == null) {
            return false;
        }

        ContinuousResource registered = children.value().stream()
                .filter(c -> c.id().equals(resource.id()))
                .findFirst()
                .map(c -> (ContinuousResource) c)
                .get();
        if (registered.value() < resource.value()) {
            // Capacity < requested, can never satisfy
            return false;
        }

        // check if there's enough left
        Versioned<ContinuousResourceAllocation> allocation = continuousConsumers.get(resource.id());
        if (allocation == null) {
            // no allocation (=no consumer) full registered resources available
            return true;
        }

        return hasEnoughResource(allocation.value().original(), resource, allocation.value());
    }

    // computational complexity: O(n + m) where n is the number of entries in discreteConsumers
    // and m is the number of allocations for all continuous resources
    @Override
    public Collection<Resource> getResources(ResourceConsumer consumer) {
        checkNotNull(consumer);

        // NOTE: getting all entries may become performance bottleneck
        // TODO: revisit for better backend data structure
        Stream<DiscreteResource> discreteStream = discreteConsumers.entrySet().stream()
                .filter(x -> x.getValue().value().equals(consumer))
                .map(Map.Entry::getKey)
                .map(x -> Resources.discrete(x).resource());

        Stream<ContinuousResource> continuousStream = continuousConsumers.values().stream()
                .flatMap(x -> x.value().allocations().stream()
                        .map(y -> Maps.immutableEntry(x.value().original(), y)))
                .filter(x -> x.getValue().consumer().equals(consumer))
                .map(x -> x.getKey());

        return Stream.concat(discreteStream, continuousStream).collect(Collectors.toList());
    }

    // computational complexity: O(1)
    @Override
    public Set<Resource> getChildResources(DiscreteResourceId parent) {
        checkNotNull(parent);

        Versioned<Set<Resource>> children = childMap.get(parent);
        if (children == null) {
            return ImmutableSet.of();
        }

        return children.value();
    }

    // computational complexity: O(n) where n is the number of the children of the parent
    @Override
    public <T> Collection<Resource> getAllocatedResources(DiscreteResourceId parent, Class<T> cls) {
        checkNotNull(parent);
        checkNotNull(cls);

        Versioned<Set<Resource>> children = childMap.get(parent);
        if (children == null) {
            return ImmutableList.of();
        }

        Stream<DiscreteResource> discrete = children.value().stream()
                .filter(x -> x.isTypeOf(cls))
                .filter(x -> x instanceof DiscreteResource)
                .map(x -> ((DiscreteResource) x))
                .filter(x -> discreteConsumers.containsKey(x.id()));

        Stream<ContinuousResource> continuous = children.value().stream()
                .filter(x -> x.id().equals(parent.child(cls)))
                .filter(x -> x instanceof ContinuousResource)
                .map(x -> (ContinuousResource) x)
                // we don't use cascading simple predicates like follows to reduce accesses to consistent map
                // .filter(x -> continuousConsumers.containsKey(x.id()))
                // .filter(x -> continuousConsumers.get(x.id()) != null)
                // .filter(x -> !continuousConsumers.get(x.id()).value().allocations().isEmpty());
                .filter(resource -> {
                    Versioned<ContinuousResourceAllocation> allocation = continuousConsumers.get(resource.id());
                    if (allocation == null) {
                        return false;
                    }
                    return !allocation.value().allocations().isEmpty();
                });

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

    // Appends the specified ResourceAllocation to the existing values stored in the map
    // computational complexity: O(n) where n is the number of the elements in the associated allocation
    private boolean appendValue(TransactionalMap<ContinuousResourceId, ContinuousResourceAllocation> map,
                                ContinuousResource original, ResourceAllocation value) {
        ContinuousResourceAllocation oldValue = map.putIfAbsent(original.id(),
                new ContinuousResourceAllocation(original, ImmutableList.of(value)));
        if (oldValue == null) {
            return true;
        }

        if (oldValue.allocations().contains(value)) {
            // don't write to map because all values are already stored
            return true;
        }

        ContinuousResourceAllocation newValue = new ContinuousResourceAllocation(original,
                ImmutableList.<ResourceAllocation>builder()
                        .addAll(oldValue.allocations())
                        .add(value)
                        .build());
        return map.replace(original.id(), oldValue, newValue);
    }
    /**
     * Appends the values to the existing values associated with the specified key.
     * If the map already has all the given values, appending will not happen.
     *
     * @param map map holding multiple values for a key
     * @param key key specifying values
     * @param values values to be appended
     * @return true if the operation succeeds, false otherwise.
     */
    // computational complexity: O(n) where n is the number of the specified value
    private boolean appendValues(TransactionalMap<DiscreteResourceId, Set<Resource>> map,
                                 DiscreteResourceId key, List<Resource> values) {
        Set<Resource> requested = new LinkedHashSet<>(values);
        Set<Resource> oldValues = map.putIfAbsent(key, requested);
        if (oldValues == null) {
            return true;
        }

        Set<Resource> addedValues = Sets.difference(requested, oldValues);
        // no new value, then no-op
        if (addedValues.isEmpty()) {
            // don't write to map because all values are already stored
            return true;
        }

        Set<ResourceId> addedIds = addedValues.stream()
                .map(Resource::id)
                .collect(Collectors.toSet());
        // if the value is not found but the same ID is found
        // (this happens only when being continuous resource)
        if (oldValues.stream().anyMatch(x -> addedIds.contains(x.id()))) {
            // no-op, but indicating failure (reject the request)
            return false;
        }
        Set<Resource> newValues = new LinkedHashSet<>(oldValues);
        newValues.addAll(addedValues);
        return map.replace(key, oldValues, newValues);
    }

    /**
     * Removes the values from the existing values associated with the specified key.
     * If the map doesn't contain the given values, removal will not happen.
     *
     * @param map map holding multiple values for a key
     * @param key key specifying values
     * @param values values to be removed
     * @return true if the operation succeeds, false otherwise
     */
    // computational complexity: O(n) where n is the number of the specified values
    private boolean removeValues(TransactionalMap<DiscreteResourceId, Set<Resource>> map,
                                 DiscreteResourceId key, List<Resource> values) {
        Set<Resource> oldValues = map.putIfAbsent(key, new LinkedHashSet<>());
        if (oldValues == null) {
            log.trace("No-Op removing values. key {} did not exist", key);
            return true;
        }

        if (values.stream().allMatch(x -> !oldValues.contains(x))) {
            // don't write map because none of the values are stored
            log.trace("No-Op removing values. key {} did not contain {}", key, values);
            return true;
        }

        LinkedHashSet<Resource> newValues = new LinkedHashSet<>(oldValues);
        newValues.removeAll(values);
        return map.replace(key, oldValues, newValues);
    }

    /**
     * Returns the resource which has the same key as the specified resource ID
     * in the set as a value of the map.
     *
     * @param childTxMap map storing parent - child relationship of resources
     * @param id ID of resource to be checked
     * @return the resource which is regarded as the same as the specified resource
     */
    // Naive implementation, which traverses all elements in the set when continuous resource
    // computational complexity: O(1) when discrete resource. O(n) when continuous resource
    // where n is the number of elements in the associated set
    private Optional<Resource> lookup(TransactionalMap<DiscreteResourceId, Set<Resource>> childTxMap, ResourceId id) {
        if (!id.parent().isPresent()) {
            return Optional.of(Resource.ROOT);
        }

        Set<Resource> values = childTxMap.get(id.parent().get());
        if (values == null) {
            return Optional.empty();
        }

        // short-circuit if discrete resource
        // check the existence in the set: O(1) operation
        if (id instanceof DiscreteResourceId) {
            DiscreteResource discrete = Resources.discrete((DiscreteResourceId) id).resource();
            if (values.contains(discrete)) {
                return Optional.of(discrete);
            } else {
                return Optional.empty();
            }
        }

        // continuous resource case
        // iterate over the values in the set: O(n) operation
        return values.stream()
                .filter(x -> x.id().equals(id))
                .findFirst();
    }

    /**
     * Checks if there is enough resource volume to allocated the requested resource
     * against the specified resource.
     *
     * @param original original resource
     * @param request requested resource
     * @param allocation current allocation of the resource
     * @return true if there is enough resource volume. Otherwise, false.
     */
    // computational complexity: O(n) where n is the number of allocations
    private boolean hasEnoughResource(ContinuousResource original,
                                      ContinuousResource request,
                                      ContinuousResourceAllocation allocation) {
        if (allocation == null) {
            return request.value() <= original.value();
        }

        double allocated = allocation.allocations().stream()
                .filter(x -> x.resource() instanceof ContinuousResource)
                .map(x -> (ContinuousResource) x.resource())
                .mapToDouble(ContinuousResource::value)
                .sum();
        double left = original.value() - allocated;
        return request.value() <= left;
    }

    // internal use only
    private static final class ContinuousResourceAllocation {
        private final ContinuousResource original;
        private final ImmutableList<ResourceAllocation> allocations;

        private ContinuousResourceAllocation(ContinuousResource original,
                                             ImmutableList<ResourceAllocation> allocations) {
            this.original = original;
            this.allocations = allocations;
        }

        private ContinuousResource original() {
            return original;
        }

        private ImmutableList<ResourceAllocation> allocations() {
            return allocations;
        }
    }
}
