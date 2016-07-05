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
import org.onlab.util.GuavaCollectors;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// internal use only
final class ContinuousResourceAllocation {
    private final ContinuousResource original;
    private final ImmutableList<ResourceAllocation> allocations;

    static ContinuousResourceAllocation empty(ContinuousResource original) {
        return new ContinuousResourceAllocation(original, ImmutableList.of());
    }

    ContinuousResourceAllocation(ContinuousResource original,
                                 ImmutableList<ResourceAllocation> allocations) {
        this.original = original;
        this.allocations = allocations;
    }

    // for serializer
    private ContinuousResourceAllocation() {
        this.original = null;
        this.allocations = null;
    }

    /**
     * Checks if there is enough resource volume to allocated the requested resource
     * against the specified resource.
     *
     * @param request    requested resource
     * @return true if there is enough resource volume. Otherwise, false.
     */
    // computational complexity: O(n) where n is the number of allocations
    boolean hasEnoughResource(ContinuousResource request) {
        double allocated = allocations.stream()
                .filter(x -> x.resource() instanceof ContinuousResource)
                .map(x -> (ContinuousResource) x.resource())
                .mapToDouble(ContinuousResource::value)
                .sum();
        double left = original.value() - allocated;
        return request.value() <= left;
    }

    ImmutableList<ResourceAllocation> allocations() {
        return allocations;
    }

    ContinuousResourceAllocation allocate(ResourceAllocation value) {
        return new ContinuousResourceAllocation(original, ImmutableList.<ResourceAllocation>builder()
                .addAll(allocations)
                .add(value)
                .build());
    }

    ContinuousResourceAllocation release(ContinuousResource resource, ResourceConsumerId consumerId) {
        List<ResourceAllocation> nonMatched = allocations.stream()
                .filter(x -> !(x.consumerId().equals(consumerId) &&
                        ((ContinuousResource) x.resource()).value() == resource.value()))
                .collect(Collectors.toList());

        List<ResourceAllocation> matched = allocations.stream()
                .filter(x -> (x.consumerId().equals(consumerId) &&
                        ((ContinuousResource) x.resource()).value() == resource.value()))
                .collect(Collectors.toList());

        if (!matched.isEmpty()) {
            matched.remove(0);
        }

        return new ContinuousResourceAllocation(original,
                Stream.concat(nonMatched.stream(), matched.stream())
                        .collect(GuavaCollectors.toImmutableList()));
    }
}
