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
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ContinuousResourceId;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;

class TransactionalContinuousResourceSubStore {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TransactionalMap<DiscreteResourceId, Set<ContinuousResource>> childMap;
    private final TransactionalMap<ContinuousResourceId, ContinuousResourceAllocation> consumers;

    TransactionalContinuousResourceSubStore(TransactionContext tx) {
        this.childMap = tx.getTransactionalMap(MapNames.CONTINUOUS_CHILD_MAP, SERIALIZER);
        this.consumers = tx.getTransactionalMap(MapNames.CONTINUOUS_CONSUMER_MAP, SERIALIZER);
    }

    // iterate over the values in the set: O(n) operation
    Optional<ContinuousResource> lookup(ContinuousResourceId id) {
        // continuous resource always has its parent
        checkArgument(id.parent().isPresent());

        Set<ContinuousResource> values = childMap.get(id.parent().get());
        if (values == null) {
            return Optional.empty();
        }

        return values.stream()
                .filter(x -> x.id().equals(id))
                .findFirst();
    }

    boolean register(DiscreteResourceId parent, Set<ContinuousResource> resources) {
        // short-circuit: receiving empty resource is regarded as success
        if (resources.isEmpty()) {
            return true;
        }

        Set<ContinuousResource> oldValues = childMap.putIfAbsent(parent, resources);
        if (oldValues == null) {
            return true;
        }

        Set<ContinuousResource> addedValues = Sets.difference(resources, oldValues);
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
        return childMap.replace(parent, oldValues, newValues);
    }

    boolean unregister(DiscreteResourceId parent, Set<ContinuousResource> resources) {
        // short-circuit: receiving empty resource is regarded as success
        if (resources.isEmpty()) {
            return true;
        }

        // even if one of the resources is allocated to a consumer,
        // all unregistrations are regarded as failure
        boolean allocated = resources.stream().anyMatch(x -> isAllocated(x.id()));
        if (allocated) {
            log.warn("Failed to unregister {}: allocation exists", parent);
            return false;
        }

        Set<ContinuousResource> oldValues = childMap.putIfAbsent(parent, new LinkedHashSet<>());
        if (oldValues == null) {
            log.trace("No-Op removing values. key {} did not exist", parent);
            return true;
        }

        if (resources.stream().allMatch(x -> !oldValues.contains(x))) {
            // don't write map because none of the values are stored
            log.trace("No-Op removing values. key {} did not contain {}", parent, resources);
            return true;
        }

        LinkedHashSet<ContinuousResource> newValues = new LinkedHashSet<>(oldValues);
        newValues.removeAll(resources);
        return childMap.replace(parent, oldValues, newValues);
    }

    private boolean isAllocated(ContinuousResourceId id) {
        ContinuousResourceAllocation allocations = consumers.get(id);
        return allocations != null && !allocations.allocations().isEmpty();
    }

    boolean allocate(ResourceConsumerId consumerId, ContinuousResource request) {
        // if the resource is not registered, then abort
        Optional<ContinuousResource> lookedUp = lookup(request.id());
        if (!lookedUp.isPresent()) {
            return false;
        }
        // Down cast: this must be safe as ContinuousResource is associated with ContinuousResourceId
        ContinuousResource original = lookedUp.get();
        ContinuousResourceAllocation allocations = consumers.get(request.id());
        if (!Optional.ofNullable(allocations)
                .orElse(ContinuousResourceAllocation.empty(original))
                .hasEnoughResource(request)) {
            return false;
        }

        return appendValue(original, new ResourceAllocation(request, consumerId));
    }

    // Appends the specified ResourceAllocation to the existing values stored in the map
    // computational complexity: O(n) where n is the number of the elements in the associated allocation
    private boolean appendValue(ContinuousResource original, ResourceAllocation value) {
        ContinuousResourceAllocation oldValue = consumers.putIfAbsent(original.id(),
                new ContinuousResourceAllocation(original, ImmutableList.of(value)));
        if (oldValue == null) {
            return true;
        }

        ContinuousResourceAllocation newValue = oldValue.allocate(value);
        return consumers.replace(original.id(), oldValue, newValue);
    }

    boolean release(ContinuousResource resource, ResourceConsumerId consumerId) {
        ContinuousResourceAllocation oldAllocation = consumers.get(resource.id());
        ContinuousResourceAllocation newAllocation = oldAllocation.release(resource, consumerId);

        return consumers.replace(resource.id(), oldAllocation, newAllocation);
    }
}
