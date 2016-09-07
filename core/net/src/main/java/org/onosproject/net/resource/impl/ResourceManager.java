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
package org.onosproject.net.resource.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.GuavaCollectors;
import org.onlab.util.Tools;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.ResourceAdminService;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceEvent;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.net.resource.ResourceListener;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceStore;
import org.onosproject.net.resource.ResourceStoreDelegate;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.RESOURCE_WRITE;
import static org.onosproject.security.AppPermission.Type.RESOURCE_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An implementation of ResourceService.
 */
@Component(immediate = true)
@Service
@Beta
public final class ResourceManager extends AbstractListenerManager<ResourceEvent, ResourceListener>
        implements ResourceService, ResourceAdminService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceStore store;

    private final Logger log = getLogger(getClass());

    private final ResourceStoreDelegate delegate = new InternalStoreDelegate();

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(ResourceEvent.class, listenerRegistry);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(ResourceEvent.class);

        log.info("Stopped");
    }

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer,
                                             List<? extends Resource> resources) {
        checkPermission(RESOURCE_WRITE);
        checkNotNull(consumer);
        checkNotNull(resources);

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
        checkPermission(RESOURCE_WRITE);
        checkNotNull(allocations);

        return store.release(allocations);
    }

    @Override
    public boolean release(ResourceConsumer consumer) {
        checkNotNull(consumer);

        Collection<ResourceAllocation> allocations = getResourceAllocations(consumer);
        return release(ImmutableList.copyOf(allocations));
    }

    @Override
    public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
        checkPermission(RESOURCE_READ);
        checkNotNull(id);

        return store.getResourceAllocations(id);
    }

    @Override
    public <T> Collection<ResourceAllocation> getResourceAllocations(DiscreteResourceId parent, Class<T> cls) {
        checkPermission(RESOURCE_READ);
        checkNotNull(parent);
        checkNotNull(cls);

        // We access store twice in this method, then the store may be updated by others
        Collection<Resource> resources = store.getAllocatedResources(parent, cls);
        return resources.stream()
                .flatMap(resource -> store.getResourceAllocations(resource.id()).stream())
                .collect(GuavaCollectors.toImmutableList());
    }

    @Override
    public Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer) {
        checkPermission(RESOURCE_READ);
        checkNotNull(consumer);

        Collection<Resource> resources = store.getResources(consumer);
        return resources.stream()
                .map(x -> new ResourceAllocation(x, consumer))
                .collect(Collectors.toList());
    }

    @Override
    public Set<Resource> getAvailableResources(DiscreteResourceId parent) {
        checkPermission(RESOURCE_READ);
        checkNotNull(parent);

        Set<Resource> children = store.getChildResources(parent);
        return children.stream()
                // We access store twice in this method, then the store may be updated by others
                .filter(store::isAvailable)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls) {
        checkPermission(RESOURCE_READ);
        checkNotNull(parent);
        checkNotNull(cls);

        return store.getChildResources(parent, cls).stream()
                // We access store twice in this method, then the store may be updated by others
                .filter(store::isAvailable)
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls) {
        checkPermission(RESOURCE_READ);
        checkNotNull(parent);
        checkNotNull(cls);

        return store.getChildResources(parent, cls).stream()
                // We access store twice in this method, then the store may be updated by others
                .filter(store::isAvailable)
                .map(x -> x.valueAs(cls))
                .flatMap(Tools::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Resource> getRegisteredResources(DiscreteResourceId parent) {
        checkPermission(RESOURCE_READ);
        checkNotNull(parent);

        return store.getChildResources(parent);
    }

    @Override
    public boolean isAvailable(Resource resource) {
        checkPermission(RESOURCE_READ);
        checkNotNull(resource);

        return store.isAvailable(resource);
    }

    @Override
    public boolean register(List<? extends Resource> resources) {
        checkNotNull(resources);

        return store.register(resources);
    }

    @Override
    public boolean unregister(List<? extends ResourceId> ids) {
        checkNotNull(ids);

        return store.unregister(ids);
    }

    private class InternalStoreDelegate implements ResourceStoreDelegate {
        @Override
        public void notify(ResourceEvent event) {
            post(event);
        }
    }
}
