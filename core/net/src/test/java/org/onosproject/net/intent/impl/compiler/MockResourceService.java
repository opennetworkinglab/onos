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
package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.MplsLabel;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceListener;
import org.onosproject.net.newresource.ResourcePath;
import org.onosproject.net.newresource.ResourceService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class MockResourceService implements ResourceService {

    private final Map<ResourcePath, ResourceConsumer> assignment = new HashMap<>();

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<ResourcePath> resources) {
        assignment.putAll(
                resources.stream().collect(Collectors.toMap(x -> x, x -> consumer))
        );

        return resources.stream()
                .map(x -> new ResourceAllocation(x, consumer))
                .collect(Collectors.toList());
    }

    @Override
    public boolean release(List<ResourceAllocation> allocations) {
        allocations.forEach(x -> assignment.remove(x.resource()));

        return true;
    }

    @Override
    public boolean release(ResourceConsumer consumer) {
        List<ResourcePath> resources = assignment.entrySet().stream()
                .filter(x -> x.getValue().equals(consumer))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<ResourceAllocation> allocations = resources.stream()
                .map(x -> new ResourceAllocation(x, consumer))
                .collect(Collectors.toList());

        return release(allocations);
    }

    @Override
    public Optional<ResourceAllocation> getResourceAllocation(ResourcePath resource) {
        return Optional.ofNullable(assignment.get(resource))
                .map(x -> new ResourceAllocation(resource, x));
    }

    @Override
    public <T> Collection<ResourceAllocation> getResourceAllocations(ResourcePath parent, Class<T> cls) {
        return assignment.entrySet().stream()
                .filter(x -> x.getKey().parent().isPresent())
                .filter(x -> x.getKey().parent().get().equals(parent))
                .map(x -> new ResourceAllocation(x.getKey(), x.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ResourceAllocation> getResourceAllocations(ResourceConsumer consumer) {
        return assignment.entrySet().stream()
                .filter(x -> x.getValue().equals(consumer))
                .map(x -> new ResourceAllocation(x.getKey(), x.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ResourcePath> getAvailableResources(ResourcePath parent) {
        ResourcePath resource = parent.child(MplsLabel.mplsLabel(10));
        return ImmutableList.of(resource);
    }

    @Override
    public boolean isAvailable(ResourcePath resource) {
        return true;
    }

    @Override
    public void addListener(ResourceListener listener) {}

    @Override
    public void removeListener(ResourceListener listener) {}
}
