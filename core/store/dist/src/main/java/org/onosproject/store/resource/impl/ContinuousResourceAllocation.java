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

    ContinuousResourceAllocation(ContinuousResource original,
                                 ImmutableList<ResourceAllocation> allocations) {
        this.original = original;
        this.allocations = allocations;
    }

    ContinuousResource original() {
        return original;
    }

    ImmutableList<ResourceAllocation> allocations() {
        return allocations;
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

        if (matched.size() > 1) {
            matched.remove(0);
        }

        return new ContinuousResourceAllocation(original,
                Stream.concat(nonMatched.stream(), matched.stream())
                        .collect(GuavaCollectors.toImmutableList()));
    }
}
