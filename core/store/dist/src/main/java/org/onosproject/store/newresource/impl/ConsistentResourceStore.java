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
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.GuavaCollectors;
import org.onlab.util.Tools;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceEvent;
import org.onosproject.net.newresource.ResourceId;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceStore;
import org.onosproject.net.newresource.ResourceStoreDelegate;
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
import java.util.Iterator;
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

    private ConsistentMap<Resource.Discrete, ResourceConsumer> discreteConsumers;
    private ConsistentMap<ResourceId, ContinuousResourceAllocation> continuousConsumers;
    private ConsistentMap<Resource.Discrete, Set<Resource>> childMap;

    @Activate
    public void activate() {
        discreteConsumers = service.<Resource.Discrete, ResourceConsumer>consistentMapBuilder()
                .withName(DISCRETE_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        continuousConsumers = service.<ResourceId, ContinuousResourceAllocation>consistentMapBuilder()
                .withName(CONTINUOUS_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        childMap = service.<Resource.Discrete, Set<Resource>>consistentMapBuilder()
                .withName(CHILD_MAP)
                .withSerializer(SERIALIZER)
                .build();

        Tools.retryable(() -> childMap.put(Resource.ROOT, new LinkedHashSet<>()),
                        ConsistentMapException.class, MAX_RETRIES, RETRY_DELAY);
        log.info("Started");
    }

    @Override
    public List<ResourceConsumer> getConsumers(Resource resource) {
        checkNotNull(resource);
        checkArgument(resource instanceof Resource.Discrete || resource instanceof Resource.Continuous);

        if (resource instanceof Resource.Discrete) {
            return getConsumer((Resource.Discrete) resource);
        } else {
            return getConsumer((Resource.Continuous) resource);
        }
    }

    private List<ResourceConsumer> getConsumer(Resource.Discrete resource) {
        Versioned<ResourceConsumer> consumer = discreteConsumers.get(resource);
        if (consumer == null) {
            return ImmutableList.of();
        }

        return ImmutableList.of(consumer.value());
    }

    private List<ResourceConsumer> getConsumer(Resource.Continuous resource) {
        Versioned<ContinuousResourceAllocation> allocations = continuousConsumers.get(resource.id());
        if (allocations == null) {
            return ImmutableList.of();
        }

        return allocations.value().allocations().stream()
                .filter(x -> x.resource().equals(resource))
                .map(ResourceAllocation::consumer)
                .collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public boolean register(List<Resource> resources) {
        checkNotNull(resources);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<Resource.Discrete, Set<Resource>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);

        Map<Resource.Discrete, List<Resource>> resourceMap = resources.stream()
                .filter(x -> x.parent().isPresent())
                .collect(Collectors.groupingBy(x -> x.parent().get()));

        for (Map.Entry<Resource.Discrete, List<Resource>> entry: resourceMap.entrySet()) {
            Optional<Resource.Discrete> child = lookup(childTxMap, entry.getKey());
            if (!child.isPresent()) {
                return abortTransaction(tx);
            }

            if (!appendValues(childTxMap, entry.getKey(), entry.getValue())) {
                return abortTransaction(tx);
            }
        }

        boolean success = tx.commit();
        if (success) {
            List<ResourceEvent> events = resources.stream()
                    .filter(x -> x.parent().isPresent())
                    .map(x -> new ResourceEvent(RESOURCE_ADDED, x))
                    .collect(Collectors.toList());
            notifyDelegate(events);
        }
        return success;
    }

    @Override
    public boolean unregister(List<Resource> resources) {
        checkNotNull(resources);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<Resource.Discrete, Set<Resource>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);
        TransactionalMap<Resource.Discrete, ResourceConsumer> discreteConsumerTxMap =
                tx.getTransactionalMap(DISCRETE_CONSUMER_MAP, SERIALIZER);
        TransactionalMap<ResourceId, ContinuousResourceAllocation> continuousConsumerTxMap =
                tx.getTransactionalMap(CONTINUOUS_CONSUMER_MAP, SERIALIZER);

        // Extract Discrete instances from resources
        Map<Resource.Discrete, List<Resource>> resourceMap = resources.stream()
                .filter(x -> x.parent().isPresent())
                .collect(Collectors.groupingBy(x -> x.parent().get()));

        // even if one of the resources is allocated to a consumer,
        // all unregistrations are regarded as failure
        for (Map.Entry<Resource.Discrete, List<Resource>> entry: resourceMap.entrySet()) {
            boolean allocated = entry.getValue().stream().anyMatch(x -> {
                if (x instanceof Resource.Discrete) {
                    return discreteConsumerTxMap.get((Resource.Discrete) x) != null;
                } else if (x instanceof Resource.Continuous) {
                    ContinuousResourceAllocation allocations = continuousConsumerTxMap.get(x.id());
                    return allocations != null && !allocations.allocations().isEmpty();
                } else {
                    return false;
                }
            });
            if (allocated) {
                return abortTransaction(tx);
            }

            if (!removeValues(childTxMap, entry.getKey(), entry.getValue())) {
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
        }
        return success;
    }

    @Override
    public boolean allocate(List<Resource> resources, ResourceConsumer consumer) {
        checkNotNull(resources);
        checkNotNull(consumer);

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<Resource.Discrete, Set<Resource>> childTxMap =
                tx.getTransactionalMap(CHILD_MAP, SERIALIZER);
        TransactionalMap<Resource.Discrete, ResourceConsumer> discreteConsumerTxMap =
                tx.getTransactionalMap(DISCRETE_CONSUMER_MAP, SERIALIZER);
        TransactionalMap<ResourceId, ContinuousResourceAllocation> continuousConsumerTxMap =
                tx.getTransactionalMap(CONTINUOUS_CONSUMER_MAP, SERIALIZER);

        for (Resource resource: resources) {
            if (resource instanceof Resource.Discrete) {
                if (!lookup(childTxMap, resource).isPresent()) {
                    return abortTransaction(tx);
                }

                ResourceConsumer oldValue = discreteConsumerTxMap.put((Resource.Discrete) resource, consumer);
                if (oldValue != null) {
                    return abortTransaction(tx);
                }
            } else if (resource instanceof Resource.Continuous) {
                Optional<Resource.Continuous> continuous = lookup(childTxMap, (Resource.Continuous) resource);
                if (!continuous.isPresent()) {
                    return abortTransaction(tx);
                }

                ContinuousResourceAllocation allocations = continuousConsumerTxMap.get(continuous.get().id());
                if (!hasEnoughResource(continuous.get(), (Resource.Continuous) resource, allocations)) {
                    return abortTransaction(tx);
                }

                boolean success = appendValue(continuousConsumerTxMap,
                        continuous.get(), new ResourceAllocation(continuous.get(), consumer));
                if (!success) {
                    return abortTransaction(tx);
                }
            }
        }

        return tx.commit();
    }

    @Override
    public boolean release(List<Resource> resources, List<ResourceConsumer> consumers) {
        checkNotNull(resources);
        checkNotNull(consumers);
        checkArgument(resources.size() == consumers.size());

        TransactionContext tx = service.transactionContextBuilder().build();
        tx.begin();

        TransactionalMap<Resource.Discrete, ResourceConsumer> discreteConsumerTxMap =
                tx.getTransactionalMap(DISCRETE_CONSUMER_MAP, SERIALIZER);
        TransactionalMap<ResourceId, ContinuousResourceAllocation> continuousConsumerTxMap =
                tx.getTransactionalMap(CONTINUOUS_CONSUMER_MAP, SERIALIZER);
        Iterator<Resource> resourceIte = resources.iterator();
        Iterator<ResourceConsumer> consumerIte = consumers.iterator();

        while (resourceIte.hasNext() && consumerIte.hasNext()) {
            Resource resource = resourceIte.next();
            ResourceConsumer consumer = consumerIte.next();

            if (resource instanceof Resource.Discrete) {
                // if this single release fails (because the resource is allocated to another consumer,
                // the whole release fails
                if (!discreteConsumerTxMap.remove((Resource.Discrete) resource, consumer)) {
                    return abortTransaction(tx);
                }
            } else if (resource instanceof Resource.Continuous) {
                Resource.Continuous continuous = (Resource.Continuous) resource;
                ContinuousResourceAllocation allocation = continuousConsumerTxMap.get(continuous.id());
                ImmutableList<ResourceAllocation> newAllocations = allocation.allocations().stream()
                        .filter(x -> !(x.consumer().equals(consumer) &&
                                ((Resource.Continuous) x.resource()).value() == continuous.value()))
                        .collect(GuavaCollectors.toImmutableList());

                if (!continuousConsumerTxMap.replace(continuous.id(), allocation,
                        new ContinuousResourceAllocation(allocation.original(), newAllocations))) {
                    return abortTransaction(tx);
                }
            }
        }

        return tx.commit();
    }

    @Override
    public boolean isAvailable(Resource resource) {
        checkNotNull(resource);
        checkArgument(resource instanceof Resource.Discrete || resource instanceof Resource.Continuous);

        if (resource instanceof Resource.Discrete) {
            return getConsumer((Resource.Discrete) resource).isEmpty();
        } else {
            return isAvailable((Resource.Continuous) resource);
        }
    }

    private boolean isAvailable(Resource.Continuous resource) {
        Versioned<ContinuousResourceAllocation> allocation = continuousConsumers.get(resource.id());
        if (allocation == null) {
            return false;
        }

        return hasEnoughResource(allocation.value().original(), resource, allocation.value());
    }

    @Override
    public Collection<Resource> getResources(ResourceConsumer consumer) {
        checkNotNull(consumer);

        // NOTE: getting all entries may become performance bottleneck
        // TODO: revisit for better backend data structure
        Stream<Resource.Discrete> discreteStream = discreteConsumers.entrySet().stream()
                .filter(x -> x.getValue().value().equals(consumer))
                .map(Map.Entry::getKey);

        Stream<Resource.Continuous> continuousStream = continuousConsumers.values().stream()
                .flatMap(x -> x.value().allocations().stream()
                        .map(y -> Maps.immutableEntry(x.value().original(), y)))
                .filter(x -> x.getValue().consumer().equals(consumer))
                .map(x -> x.getKey());

        return Stream.concat(discreteStream, continuousStream).collect(Collectors.toList());
    }

    @Override
    public Collection<Resource> getChildResources(Resource parent) {
        checkNotNull(parent);
        checkArgument(parent instanceof Resource.Discrete);

        Versioned<Set<Resource>> children = childMap.get((Resource.Discrete) parent);
        if (children == null) {
            return ImmutableList.of();
        }

        return children.value();
    }

    @Override
    public <T> Collection<Resource> getAllocatedResources(Resource parent, Class<T> cls) {
        checkNotNull(parent);
        checkNotNull(cls);
        checkArgument(parent instanceof Resource.Discrete);

        Versioned<Set<Resource>> children = childMap.get((Resource.Discrete) parent);
        if (children == null) {
            return ImmutableList.of();
        }

        Stream<Resource.Discrete> discrete = children.value().stream()
                .filter(x -> x.last().getClass().equals(cls))
                .filter(x -> x instanceof Resource.Discrete)
                .map(x -> (Resource.Discrete) x)
                .filter(discreteConsumers::containsKey);

        Stream<Resource.Continuous> continuous = children.value().stream()
                .filter(x -> x.last().getClass().equals(cls))
                .filter(x -> x instanceof Resource.Continuous)
                .map(x -> (Resource.Continuous) x)
                .filter(x -> continuousConsumers.containsKey(x.id()))
                .filter(x -> continuousConsumers.get(x.id()) != null)
                .filter(x -> !continuousConsumers.get(x.id()).value().allocations().isEmpty());

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
    private boolean appendValue(TransactionalMap<ResourceId, ContinuousResourceAllocation> map,
                                Resource.Continuous original, ResourceAllocation value) {
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
     * @param <K> type of the key
     * @param <V> type of the element of the list
     * @return true if the operation succeeds, false otherwise.
     */
    private <K, V> boolean appendValues(TransactionalMap<K, Set<V>> map, K key, List<V> values) {
        Set<V> oldValues = map.putIfAbsent(key, new LinkedHashSet<>(values));
        if (oldValues == null) {
            return true;
        }

        if (oldValues.containsAll(values)) {
            // don't write to map because all values are already stored
            return true;
        }

        LinkedHashSet<V> newValues = new LinkedHashSet<>(oldValues);
        newValues.addAll(values);
        return map.replace(key, oldValues, newValues);
    }

    /**
     * Removes the values from the existing values associated with the specified key.
     * If the map doesn't contain the given values, removal will not happen.
     *
     * @param map map holding multiple values for a key
     * @param key key specifying values
     * @param values values to be removed
     * @param <K> type of the key
     * @param <V> type of the element of the list
     * @return true if the operation succeeds, false otherwise
     */
    private <K, V> boolean removeValues(TransactionalMap<K, Set<V>> map, K key, List<? extends V> values) {
        Set<V> oldValues = map.putIfAbsent(key, new LinkedHashSet<>());
        if (oldValues == null) {
            return true;
        }

        if (values.stream().allMatch(x -> !oldValues.contains(x))) {
            // don't write map because none of the values are stored
            return true;
        }

        LinkedHashSet<V> newValues = new LinkedHashSet<>(oldValues);
        newValues.removeAll(values);
        return map.replace(key, oldValues, newValues);
    }

    /**
     * Returns the resource which has the same key as the key of the specified resource
     * in the list as a value of the map.
     *
     * @param map map storing parent - child relationship of resources
     * @param resource resource to be checked for its key
     * @return the resource which is regarded as the same as the specified resource
     */
    // Naive implementation, which traverses all elements in the list
    private <T extends Resource> Optional<T> lookup(
            TransactionalMap<Resource.Discrete, Set<Resource>> map, T resource) {
        // if it is root, always returns itself
        if (!resource.parent().isPresent()) {
            return Optional.of(resource);
        }

        Set<Resource> values = map.get(resource.parent().get());
        if (values == null) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        Optional<T> result = values.stream()
                .filter(x -> x.id().equals(resource.id()))
                .map(x -> (T) x)
                .findFirst();
        return result;
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
    private boolean hasEnoughResource(Resource.Continuous original,
                                      Resource.Continuous request,
                                      ContinuousResourceAllocation allocation) {
        if (allocation == null) {
            return request.value() <= original.value();
        }

        double allocated = allocation.allocations().stream()
                .filter(x -> x.resource() instanceof Resource.Continuous)
                .map(x -> (Resource.Continuous) x.resource())
                .mapToDouble(Resource.Continuous::value)
                .sum();
        double left = original.value() - allocated;
        return request.value() <= left;
    }

    // internal use only
    private static final class ContinuousResourceAllocation {
        private final Resource.Continuous original;
        private final ImmutableList<ResourceAllocation> allocations;

        private ContinuousResourceAllocation(Resource.Continuous original,
                                             ImmutableList<ResourceAllocation> allocations) {
            this.original = original;
            this.allocations = allocations;
        }

        private Resource.Continuous original() {
            return original;
        }

        private ImmutableList<ResourceAllocation> allocations() {
            return allocations;
        }
    }
}
