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
import org.onlab.packet.VlanId;
import org.onosproject.net.newresource.ResourceAllocation;
import org.onosproject.net.newresource.ResourceConsumer;
import org.onosproject.net.newresource.ResourceListener;
import org.onosproject.net.newresource.Resource;
import org.onosproject.net.newresource.ResourceService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class MockResourceService implements ResourceService {

    private final Map<Resource, ResourceConsumer> assignment = new HashMap<>();

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<Resource> resources) {
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
        List<Resource> resources = assignment.entrySet().stream()
                .filter(x -> x.getValue().equals(consumer))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<ResourceAllocation> allocations = resources.stream()
                .map(x -> new ResourceAllocation(x, consumer))
                .collect(Collectors.toList());

        return release(allocations);
    }

    @Override
    public List<ResourceAllocation> getResourceAllocations(Resource resource) {
        return Optional.ofNullable(assignment.get(resource))
                .map(x -> ImmutableList.of(new ResourceAllocation(resource, x)))
                .orElse(ImmutableList.of());
    }

    @Override
    public <T> Collection<ResourceAllocation> getResourceAllocations(Resource parent, Class<T> cls) {
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
    public Collection<Resource> getAvailableResources(Resource parent) {

        Collection<Resource> resources = new HashSet<Resource>();
        resources.add(parent.child(VlanId.vlanId((short) 10)));
        resources.add(parent.child(MplsLabel.mplsLabel(10)));
        return ImmutableList.copyOf(resources);
    }

    @Override
    public Collection<Resource> getRegisteredResources(Resource parent) {
        return getAvailableResources(parent);
    }

    @Override
    public boolean isAvailable(Resource resource) {
        return true;
    }

    @Override
    public void addListener(ResourceListener listener) {}

    @Override
    public void removeListener(ResourceListener listener) {}
}
