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
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.net.resource.Resources;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.Versioned;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.onosproject.store.resource.impl.ConsistentResourceStore.SERIALIZER;

/**
 * Consistent substore for discrete resources.
 */
class ConsistentDiscreteResourceSubStore implements ConsistentResourceSubStore
        <DiscreteResourceId, DiscreteResource, TransactionalDiscreteResourceSubStore> {
    private ConsistentMap<DiscreteResourceId, ResourceConsumerId> consumers;
    private ConsistentMap<DiscreteResourceId, DiscreteResources> childMap;

    ConsistentDiscreteResourceSubStore(StorageService service) {
        this.consumers = service.<DiscreteResourceId, ResourceConsumerId>consistentMapBuilder()
                .withName(MapNames.DISCRETE_CONSUMER_MAP)
                .withSerializer(SERIALIZER)
                .build();
        this.childMap = service.<DiscreteResourceId, DiscreteResources>consistentMapBuilder()
                .withName(MapNames.DISCRETE_CHILD_MAP)
                .withSerializer(SERIALIZER)
                .build();

        childMap.put(Resource.ROOT.id(), DiscreteResources.empty());
    }

    @Override
    public TransactionalDiscreteResourceSubStore transactional(TransactionContext tx) {
        return new TransactionalDiscreteResourceSubStore(tx);
    }

    // computational complexity: O(1)
    @Override
    public List<ResourceAllocation> getResourceAllocations(DiscreteResourceId resource) {
        Versioned<ResourceConsumerId> consumerId = consumers.get(resource);
        if (consumerId == null) {
            return ImmutableList.of();
        }

        return ImmutableList.of(new ResourceAllocation(Resources.discrete(resource).resource(), consumerId.value()));
    }

    @Override
    public Set<DiscreteResource> getChildResources(DiscreteResourceId parent) {
        Versioned<DiscreteResources> children = childMap.get(parent);

        if (children == null) {
            return ImmutableSet.of();
        }

        return children.value().values();
    }

    @Override
    public Set<DiscreteResource> getChildResources(DiscreteResourceId parent, Class<?> cls) {
        Versioned<DiscreteResources> children = childMap.get(parent);

        if (children == null) {
            return ImmutableSet.of();
        }

        return children.value().valuesOf(cls);
    }

    @Override
    public boolean isAvailable(DiscreteResource resource) {
        return getResourceAllocations(resource.id()).isEmpty();
    }

    @Override
    public Stream<DiscreteResource> getAllocatedResources(DiscreteResourceId parent, Class<?> cls) {
        Set<DiscreteResource> children = getChildResources(parent);
        if (children.isEmpty()) {
            return Stream.of();
        }

        return children.stream()
                .filter(x -> x.isTypeOf(cls))
                .filter(x -> consumers.containsKey(x.id()));
    }

    @Override
    public Stream<DiscreteResource> getResources(ResourceConsumerId consumerId) {
        return consumers.entrySet().stream()
                .filter(x -> x.getValue().value().equals(consumerId))
                .map(Map.Entry::getKey)
                .map(x -> Resources.discrete(x).resource());
    }
}
