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
import com.google.common.collect.ImmutableSet;
import org.onlab.util.GuavaCollectors;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ContinuousResourceId;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.Versioned;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;

class ConsistentContinuousResourceSubStore {
    private ConsistentMap<ContinuousResourceId, ContinuousResourceAllocation> consumers;
    private ConsistentMap<DiscreteResourceId, Set<ContinuousResource>> childMap;

    ConsistentContinuousResourceSubStore(StorageService service) {
        this.consumers = service.<ContinuousResourceId, ContinuousResourceAllocation>consistentMapBuilder()
                .withName(MapNames.CONTINUOUS_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        this.childMap = service.<DiscreteResourceId, Set<ContinuousResource>>consistentMapBuilder()
                .withName(MapNames.CONTINUOUS_CHILD_MAP)
                .withSerializer(SERIALIZER)
                .build();

        childMap.put(Resource.ROOT.id(), new LinkedHashSet<>());
    }

    TransactionalContinuousResourceSubStore transactional(TransactionContext tx) {
        return new TransactionalContinuousResourceSubStore(tx);
    }

    // computational complexity: O(n) where n is the number of the existing allocations for the resource
    List<ResourceAllocation> getResourceAllocations(ContinuousResourceId resource) {
        Versioned<ContinuousResourceAllocation> allocations = consumers.get(resource);
        if (allocations == null) {
            return ImmutableList.of();
        }

        return allocations.value().allocations().stream()
                .filter(x -> x.resource().id().equals(resource))
                .collect(GuavaCollectors.toImmutableList());
    }

    Set<ContinuousResource> getChildResources(DiscreteResourceId parent) {
        Versioned<Set<ContinuousResource>> children = childMap.get(parent);

        if (children == null) {
            return ImmutableSet.of();
        }

        return children.value();
    }

    <T> Set<ContinuousResource> getChildResources(DiscreteResourceId parent, Class<T> cls) {
        // naive implementation
        return getChildResources(parent).stream()
                .filter(x -> x.isTypeOf(cls))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isAvailable(ContinuousResource resource) {
        // check if it's registered or not.
        Versioned<Set<ContinuousResource>> children = childMap.get(resource.parent().get().id());
        if (children == null) {
            return false;
        }

        ContinuousResource registered = children.value().stream()
                .filter(c -> c.id().equals(resource.id()))
                .findFirst()
                .get();
        if (registered.value() < resource.value()) {
            // Capacity < requested, can never satisfy
            return false;
        }

        // check if there's enough left
        Versioned<ContinuousResourceAllocation> allocation = consumers.get(resource.id());
        if (allocation == null) {
            // no allocation (=no consumer) full registered resources available
            return true;
        }

        return allocation.value().hasEnoughResource(resource);
    }

    <T> Stream<ContinuousResource> getAllocatedResources(DiscreteResourceId parent, Class<T> cls) {
        Set<ContinuousResource> children = getChildResources(parent);
        if (children.isEmpty()) {
            return Stream.of();
        }

        return children.stream()
                .filter(x -> x.id().equals(parent.child(cls)))
                // we don't use cascading simple predicates like follows to reduce accesses to consistent map
                // .filter(x -> continuousConsumers.containsKey(x.id()))
                // .filter(x -> continuousConsumers.get(x.id()) != null)
                // .filter(x -> !continuousConsumers.get(x.id()).value().allocations().isEmpty());
                .filter(resource -> {
                    Versioned<ContinuousResourceAllocation> allocation = consumers.get(resource.id());
                    if (allocation == null) {
                        return false;
                    }
                    return !allocation.value().allocations().isEmpty();
                });
    }

    Stream<ContinuousResource> getResources(ResourceConsumerId consumerId) {
        return consumers.values().stream()
                .flatMap(x -> x.value().allocations().stream())
                .filter(x -> x.consumerId().equals(consumerId))
                // this cast is safe because this class stores
                // continuous resource allocations only
                .map(x -> (ContinuousResource) x.resource());
    }
}
