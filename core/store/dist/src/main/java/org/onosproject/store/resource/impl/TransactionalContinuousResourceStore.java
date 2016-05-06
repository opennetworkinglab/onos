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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.onlab.util.GuavaCollectors;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ContinuousResourceId;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.store.resource.impl.ConsistentResourceStore.ContinuousResourceAllocation;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;
import static org.onosproject.store.resource.impl.ResourceStoreUtil.hasEnoughResource;

class TransactionalContinuousResourceStore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TransactionalMap<DiscreteResourceId, Set<ContinuousResource>> childMap;
    private final TransactionalMap<ContinuousResourceId, ContinuousResourceAllocation> consumers;

    TransactionalContinuousResourceStore(TransactionContext tx) {
        this.childMap = tx.getTransactionalMap(MapNames.CONTINUOUS_CHILD_MAP, SERIALIZER);
        this.consumers = tx.getTransactionalMap(MapNames.CONTINUOUS_CONSUMER_MAP, SERIALIZER);
    }

    // iterate over the values in the set: O(n) operation
    Optional<Resource> lookup(ContinuousResourceId id) {
        if (!id.parent().isPresent()) {
            return Optional.of(Resource.ROOT);
        }

        Set<ContinuousResource> values = childMap.get(id.parent().get());
        if (values == null) {
            return Optional.empty();
        }

        return values.stream()
                .filter(x -> x.id().equals(id))
                .map(x -> (Resource) x)
                .findFirst();
    }

    boolean register(DiscreteResourceId key, List<ContinuousResource> values) {
        Set<ContinuousResource> requested = new LinkedHashSet<>(values);
        Set<ContinuousResource> oldValues = childMap.putIfAbsent(key, requested);
        if (oldValues == null) {
            return true;
        }

        Set<ContinuousResource> addedValues = Sets.difference(requested, oldValues);
        // no new value, then no-op
        if (addedValues.isEmpty()) {
            // don't write to map because all values are already stored
            return true;
        }

        Set<ContinuousResourceId> addedIds = addedValues.stream()
                .map(ContinuousResource::id)
                .collect(Collectors.toSet());
        // if the value is not found but the same ID is found
        // (this happens only when being continuous resource)
        if (oldValues.stream().anyMatch(x -> addedIds.contains(x.id()))) {
            // no-op, but indicating failure (reject the request)
            return false;
        }
        Set<ContinuousResource> newValues = new LinkedHashSet<>(oldValues);
        newValues.addAll(addedValues);
        return childMap.replace(key, oldValues, newValues);
    }

    boolean unregister(DiscreteResourceId key, List<ContinuousResource> values) {
        Set<ContinuousResource> oldValues = childMap.putIfAbsent(key, new LinkedHashSet<>());
        if (oldValues == null) {
            log.trace("No-Op removing values. key {} did not exist", key);
            return true;
        }

        if (values.stream().allMatch(x -> !oldValues.contains(x))) {
            // don't write map because none of the values are stored
            log.trace("No-Op removing values. key {} did not contain {}", key, values);
            return true;
        }

        LinkedHashSet<ContinuousResource> newValues = new LinkedHashSet<>(oldValues);
        newValues.removeAll(values);
        return childMap.replace(key, oldValues, newValues);
    }

    boolean isAllocated(ContinuousResourceId id) {
        ContinuousResourceAllocation allocations = consumers.get(id);
        return allocations != null && !allocations.allocations().isEmpty();
    }

    boolean allocate(ResourceConsumer consumer, ContinuousResource request) {
        // if the resource is not registered, then abort
        Optional<Resource> lookedUp = lookup(request.id());
        if (!lookedUp.isPresent()) {
            return false;
        }
        // Down cast: this must be safe as ContinuousResource is associated with ContinuousResourceId
        ContinuousResource original = (ContinuousResource) lookedUp.get();
        ContinuousResourceAllocation allocations = consumers.get(request.id());
        if (!hasEnoughResource(original, request, allocations)) {
            return false;
        }

        boolean success = appendValue(original, new ResourceAllocation(request, consumer));
        if (!success) {
            return false;
        }

        return true;
    }

    // Appends the specified ResourceAllocation to the existing values stored in the map
    // computational complexity: O(n) where n is the number of the elements in the associated allocation
    private boolean appendValue(ContinuousResource original, ResourceAllocation value) {
        ContinuousResourceAllocation oldValue = consumers.putIfAbsent(original.id(),
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
        return consumers.replace(original.id(), oldValue, newValue);
    }

    boolean release(ContinuousResource resource, ResourceConsumer consumer) {
        ContinuousResourceAllocation oldAllocation = consumers.get(resource.id());
        ImmutableList<ResourceAllocation> newAllocations = oldAllocation.allocations().stream()
                .filter(x -> !(x.consumer().equals(consumer) &&
                        ((ContinuousResource) x.resource()).value() == resource.value()))
                .collect(GuavaCollectors.toImmutableList());

        if (!consumers.replace(resource.id(), oldAllocation,
                new ContinuousResourceAllocation(oldAllocation.original(), newAllocations))) {
            return false;
        }

        return true;
    }
}
