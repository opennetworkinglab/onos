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
import org.onosproject.net.newresource.DefaultResource;
import org.onosproject.net.newresource.DefaultResourceAllocation;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceService;
import org.onosproject.net.newresource.ResourceStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of ResourceService.
 */
@Component(immediate = true, enabled = false)
@Service
@Beta
public final class ResourceManager implements ResourceService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceStore store;

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Optional<ResourceAllocation<S, T>> allocate(ResourceConsumer consumer, Resource<S, T> resource) {
        checkNotNull(consumer);
        checkNotNull(resource);

        List<ResourceAllocation<?, ?>> allocations = allocate(consumer, ImmutableList.of(resource));
        if (allocations.isEmpty()) {
            return Optional.empty();
        }

        assert allocations.size() == 1;

        ResourceAllocation<?, ?> allocation = allocations.get(0);

        assert allocation.subject().getClass() == resource.subject().getClass();
        assert allocation.resource().getClass() == resource.resource().getClass();

        // cast is ensured by the assertions above
        return Optional.of((ResourceAllocation<S, T>) allocation);
    }

    @Override
    public List<ResourceAllocation<?, ?>> allocate(ResourceConsumer consumer,
                                                   List<? extends Resource<?, ?>> resources) {
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
                .map(x -> new DefaultResourceAllocation<>(x.subject(), x.resource(), consumer))
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceAllocation<?, ?>> allocate(ResourceConsumer consumer, Resource<?, ?>... resources) {
        checkNotNull(consumer);
        checkNotNull(resources);

        return allocate(consumer, Arrays.asList(resources));
    }

    @Override
    public <S, T> boolean release(ResourceAllocation<S, T> allocation) {
        checkNotNull(allocation);

        return release(ImmutableList.of(allocation));
    }

    @Override
    public boolean release(List<? extends ResourceAllocation<?, ?>> allocations) {
        checkNotNull(allocations);

        List<DefaultResource<?, ?>> resources = allocations.stream()
                .map(x -> new DefaultResource<>(x.subject(), x.resource()))
                .collect(Collectors.toList());
        List<ResourceConsumer> consumers = allocations.stream()
                .map(ResourceAllocation::consumer)
                .collect(Collectors.toList());

        return store.release(resources, consumers);
    }

    @Override
    public boolean release(ResourceAllocation<?, ?>... allocations) {
        checkNotNull(allocations);

        return release(ImmutableList.copyOf(allocations));
    }

    @Override
    public boolean release(ResourceConsumer consumer) {
        checkNotNull(consumer);

        Collection<ResourceAllocation<?, ?>> allocations = getResourceAllocations(consumer);
        return release(ImmutableList.copyOf(allocations));
    }

    @Override
    public <S, T> Collection<ResourceAllocation<S, T>> getResourceAllocations(S subject, Class<T> cls) {
        checkNotNull(subject);
        checkNotNull(cls);

        Collection<Resource<S, T>> resources = store.getAllocatedResources(subject, cls);
        List<ResourceAllocation<S, T>> allocations = new ArrayList<>(resources.size());
        for (Resource<S, T> resource: resources) {
            // We access store twice in this method, then the store may be updated by others
            Optional<ResourceConsumer> consumer = store.getConsumer(resource);
            if (consumer.isPresent()) {
                allocations.add(
                        new DefaultResourceAllocation<>(resource.subject(), resource.resource(), consumer.get()));
            }
        }

        return allocations;
    }

    @Override
    public Collection<ResourceAllocation<?, ?>> getResourceAllocations(ResourceConsumer consumer) {
        checkNotNull(consumer);

        Collection<Resource<?, ?>> resources = store.getResources(consumer);
        return resources.stream()
                .map(x -> new DefaultResourceAllocation<>(x.subject(), x.resource(), consumer))
                .collect(Collectors.toList());
    }

    @Override
    public <S, T> boolean isAvailable(Resource<S, T> resource) {
        checkNotNull(resource);

        Optional<ResourceConsumer> consumer = store.getConsumer(resource);
        return !consumer.isPresent();
    }

    /**
     * Returns if the specified resource is in the resource range.
     * E.g. VLAN ID against a link must be within 12 bit address space.
     *
     * @param resource resource to be checked if it is within the resource range
     * @param <S> type of the subject
     * @param <T> type of the resource
     * @return true if the resource within the range, false otherwise
     */
    private <S, T> boolean isValid(Resource<S, T> resource) {
        // TODO: implement
        return true;
    }
}
