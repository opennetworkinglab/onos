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
package org.onosproject.net.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onlab.util.Tools;
import org.onosproject.net.TributarySlot;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MockResourceService implements ResourceService {

    private double bandwidth = 1000.0;
    private final Map<Resource, ResourceConsumer> assignment = new HashMap<>();
    public Set<Short> availableVlanLabels = new HashSet<>();
    public Set<Integer> availableMplsLabels = new HashSet<>();

    public MockResourceService(){}

    // To express a custom bandwidth available (in bps)
    public static ResourceService makeCustomBandwidthResourceService(double bandwidth) {
        return new MockResourceService(bandwidth);
    }

    private MockResourceService(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    @Override
    public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<? extends Resource> resources) {
        assignment.putAll(
                resources.stream().collect(Collectors.toMap(Function.identity(), x -> consumer))
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
    public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
        if (id instanceof ContinuousResourceId) {
            return ImmutableList.of();
        }
        DiscreteResource discrete = Resources.discrete((DiscreteResourceId) id).resource();
        return Optional.ofNullable(assignment.get(discrete))
                .map(x -> ImmutableList.of(new ResourceAllocation(discrete, x)))
                .orElse(ImmutableList.of());
    }

    @Override
    public <T> Collection<ResourceAllocation> getResourceAllocations(DiscreteResourceId parent, Class<T> cls) {
        return assignment.entrySet().stream()
                .filter(x -> x.getKey().parent().isPresent())
                .filter(x -> x.getKey().parent().get().id().equals(parent))
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


    /**
     * Binds VLAN Ids to a parent resource, given a parent resource.
     *
     * @param parent the parent resource
     * @return the VLAN Ids allocated
     */
    private Collection<Resource> addVlanIds(DiscreteResourceId parent) {
        Collection<Resource> resources = new HashSet<>();
        if (!this.availableVlanLabels.isEmpty()) {
            this.availableVlanLabels.forEach(label -> {
                if (label > VlanId.NO_VID && label < VlanId.MAX_VLAN) {
                    resources.add(Resources.discrete(parent).resource().child(VlanId.vlanId(label)));
                }
            });
        } else {
            for (int i = VlanId.NO_VID + 1; i < 1000; i++) {
                resources.add(Resources.discrete(parent).resource().child(VlanId.vlanId((short) i)));
            }
        }
        return resources;
    }

    /**
     * Binds MPLS labels to a parent resource, given a parent resource.
     *
     * @param parent the parent resource
     * @return the MPLS labels allocated
     */
    private Collection<Resource> addMplsLabels(DiscreteResourceId parent) {
        Collection<Resource> resources = new HashSet<>();
        if (!this.availableMplsLabels.isEmpty()) {
            this.availableMplsLabels.forEach(label -> {
                if (label < MplsLabel.MAX_MPLS) {
                    resources.add(Resources.discrete(parent).resource().child(MplsLabel.mplsLabel(label)));
                }
            });
        } else {
            for (int i = 1; i < 1000; i++) {
                resources.add(Resources.discrete(parent).resource().child(MplsLabel.mplsLabel(i)));
            }
        }
        return resources;
    }

    @Override
    public Set<Resource> getAvailableResources(DiscreteResourceId parent) {
        Collection<Resource> resources = new HashSet<>();
        resources.addAll(addVlanIds(parent));
        resources.addAll(addMplsLabels(parent));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(1)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(2)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(3)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(4)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(5)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(6)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(7)));
        resources.add(Resources.discrete(parent).resource().child(TributarySlot.of(8)));
        return ImmutableSet.copyOf(resources);
    }

    @Override
    public <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls) {
        return getAvailableResources(parent).stream()
                .filter(x -> x.isTypeOf(cls))
                .collect(Collectors.toSet());
    }

    @Override
    public <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls) {
        return getAvailableResources(parent).stream()
                .filter(x -> x.isTypeOf(cls))
                .flatMap(x -> Tools.stream(x.valueAs(cls)))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Resource> getRegisteredResources(DiscreteResourceId parent) {
        return getAvailableResources(parent);
    }

    @Override
    public boolean isAvailable(Resource resource) {
        if (resource.isTypeOf(Bandwidth.class)) {
            // If there's is enough bandwidth available return true; false otherwise
            Optional<Double> value = resource.valueAs(Double.class);
            return value.filter(requested -> requested <= bandwidth).isPresent();
        }
        return true;
    }

    @Override
    public void addListener(ResourceListener listener) {}

    @Override
    public void removeListener(ResourceListener listener) {}
}
