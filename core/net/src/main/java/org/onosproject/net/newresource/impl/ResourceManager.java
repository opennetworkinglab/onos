/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.newresource.ResourceAdminService;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceService;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.newresource.ResourceStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of ResourceService.
 */
@Component(immediate = true, enabled = false)
@Service
@Beta
public final class ResourceManager implements ResourceService, ResourceAdminService {

    private final ConcurrentMap<Class<?>, Predicate<?>> boundaries = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceStore store;

    @Override
    public Optional<ResourceAllocation> allocate(ResourceConsumer consumer, ResourcePath resource) {
        checkNotNull(consumer);
        checkNotNull(resource);

        List<ResourceAllocation> allocations = allocate(consumer, ImmutableList.of(resource));
        if (allocations.isEmpty()) {
            return Optional.empty();
        }

        assert allocations.size() == 1;

        ResourceAllocation allocation = allocations.get(0);

        assert allocation.resource().equals(resource);

        // cast is ensured by the assertions above
        return Optional.of(allocation);
    }

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer,
                                             List<ResourcePath> resources) {
        checkNotNull(consumer);
        checkNotNull(resources);

        if (resources.stream().anyMatch(x -> !isValid(x))) {
            return ImmutableList.of();
        }

        // TODO: implement support of resource hierarchy
        // allocation for a particular resource implies allocations for all of the sub-resources need to be done

        boolean success = store.allocate(resources, consumer);
        if (!success) {
            return ImmutableList.of();
        }

        return resources.stream()
                .map(x -> new ResourceAllocation(x, consumer))
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer, ResourcePath... resources) {
        checkNotNull(consumer);
        checkNotNull(resources);

        return allocate(consumer, Arrays.asList(resources));
    }

    @Override
    public boolean release(ResourceAllocation allocation) {
        checkNotNull(allocation);

        return release(ImmutableList.of(allocation));
    }

    @Override
    public boolean release(List<ResourceAllocation> allocations) {
        checkNotNull(allocations);

        List<ResourcePath> resources = allocations.stream()
                .map(ResourceAllocation::resource)
                .collect(Collectors.toList());
        List<ResourceConsumer> consumers = allocations.stream()
                .map(ResourceAllocation::consumer)
                .collect(Collectors.toList());

        return store.release(resources, consumers);
    }

    @Override
    public boolean release(ResourceAllocation... allocations) {
        checkNotNull(allocations);

        return release(ImmutableList.copyOf(allocations));
    }

    @Override
    public boolean release(ResourceConsumer consumer) {
        checkNotNull(consumer);

        Collection<ResourceAllocation> allocations = getResourceAllocations(consumer);
        return release(ImmutableList.copyOf(allocations));
    }

    @Override
    public <T> Collection<ResourceAllocation> getResourceAllocations(ResourcePath parent, Class<T> cls) {
        checkNotNull(parent);
        checkNotNull(cls);

        Collection<ResourcePath> resources = store.getAllocatedResources(parent, cls);
        List<ResourceAllocation> allocations = new ArrayList<>(resources.size());
        for (ResourcePath resource: resources) {
            // We access store twice in this method, then the store may be updated by others
            Optional<ResourceConsumer> consumer = store.getConsumer(resource);
            if (consumer.isPresent()) {
                allocations.add(new ResourceAllocation(resource, consumer.get()));
            }
        }

        return allocations;
    }

    @Override
    public Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer) {
        checkNotNull(consumer);

        Collection<ResourcePath> resources = store.getResources(consumer);
        return resources.stream()
                .map(x -> new ResourceAllocation(x, consumer))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAvailable(ResourcePath resource) {
        checkNotNull(resource);

        Optional<ResourceConsumer> consumer = store.getConsumer(resource);
        return !consumer.isPresent();
    }

    @Override
    public <T> void defineResourceBoundary(Class<T> cls, Predicate<T> predicate) {
        boundaries.put(cls, predicate);
    }

    /**
     * Returns the predicate associated with the specified resource.
     *
     * @param resource resource whose associated predicate is to be returned
     * @param <T> type of the resource
     * @return predicate associated with the resource
     * Null if the resource doesn't have an associated predicate.
     */
    @SuppressWarnings("unchecked")
    private <T> Predicate<T> lookupPredicate(T resource) {
        return (Predicate<T>) boundaries.get(resource.getClass());
    }

    /**
     * Returns if the specified resource is in the resource range.
     * E.g. VLAN ID against a link must be within 12 bit address space.
     *
     * @param resource resource to be checked if it is within the resource range
     * @return true if the resource within the range, false otherwise
     */
    boolean isValid(ResourcePath resource) {
        List<Object> flatten = resource.components();
        Object bottom = flatten.get(flatten.size() - 1);
        Predicate<Object> predicate = lookupPredicate(bottom);
        if (predicate == null) {
            return true;
        }

        return predicate.test(bottom);
    }
}
