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
import com.google.common.collect.Lists;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of ResourceService.
 */
@Component(immediate = true)
@Service
@Beta
public final class ResourceManager implements ResourceService, ResourceAdminService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceStore store;

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer,
                                             List<ResourcePath> resources) {
        checkNotNull(consumer);
        checkNotNull(resources);

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
    public boolean release(ResourceConsumer consumer) {
        checkNotNull(consumer);

        Collection<ResourceAllocation> allocations = getResourceAllocations(consumer);
        return release(ImmutableList.copyOf(allocations));
    }

    @Override
    public Optional<ResourceAllocation> getResourceAllocation(ResourcePath resource) {
        checkNotNull(resource);

        Optional<ResourceConsumer> consumer = store.getConsumer(resource);
        return consumer.map(x -> new ResourceAllocation(resource, x));
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
    public <T> boolean registerResources(ResourcePath parent, List<T> children) {
        checkNotNull(parent);
        checkNotNull(children);
        checkArgument(!children.isEmpty());

        List<ResourcePath> resources = Lists.transform(children, x -> ResourcePath.child(parent, x));
        return store.register(resources);
    }

    @Override
    public <T> boolean unregisterResources(ResourcePath parent, List<T> children) {
        checkNotNull(parent);
        checkNotNull(children);
        checkArgument(!children.isEmpty());

        List<ResourcePath> resources = Lists.transform(children, x -> ResourcePath.child(parent, x));
        return store.unregister(resources);
    }
}
