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
package org.onosproject.pce.pceservice;

import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.net.resource.ResourceListener;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Adapter for resource service for path computation.
 */
public class ResourceServiceAdapter implements ResourceService {

    @Override
    public void addListener(ResourceListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeListener(ResourceListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<? extends Resource> resources) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean release(List<ResourceAllocation> allocations) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean release(ResourceConsumer consumer) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> Collection<ResourceAllocation> getResourceAllocations(DiscreteResourceId parent, Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Resource> getAvailableResources(DiscreteResourceId parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Resource> getRegisteredResources(DiscreteResourceId parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAvailable(Resource resource) {
        // TODO Auto-generated method stub
        return false;
    }
}
