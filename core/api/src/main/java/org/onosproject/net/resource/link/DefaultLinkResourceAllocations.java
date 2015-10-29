/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.onosproject.net.Link;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Implementation of {@link LinkResourceAllocations}.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public class DefaultLinkResourceAllocations implements LinkResourceAllocations {
    private final LinkResourceRequest request;
    // TODO: probably should be using LinkKey instead
    private final Map<Link, Set<ResourceAllocation>> allocations;

    /**
     * Creates a new link resource allocations.
     *
     * @param request     requested resources
     * @param allocations allocated resources
     */
    public DefaultLinkResourceAllocations(LinkResourceRequest request,
                                   Map<Link, Set<ResourceAllocation>> allocations) {
        this.request = checkNotNull(request);
        ImmutableMap.Builder<Link, Set<ResourceAllocation>> builder
            = ImmutableMap.builder();
        for (Entry<Link, Set<ResourceAllocation>> e : allocations.entrySet()) {
            builder.put(e.getKey(), ImmutableSet.copyOf(e.getValue()));
        }
        this.allocations = builder.build();
    }

    public IntentId intentId() {
        return request.intentId();
    }

    public Collection<Link> links() {
        return request.links();
    }

    public Set<ResourceRequest> resources() {
        return request.resources();
    }

    @Override
    public ResourceType type() {
        return null;
    }

    @Override
    public Set<ResourceAllocation> getResourceAllocation(Link link) {
        Set<ResourceAllocation> result = allocations.get(link);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(request, allocations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DefaultLinkResourceAllocations other = (DefaultLinkResourceAllocations) obj;
        return Objects.equals(this.request, other.request)
                && Objects.equals(this.allocations, other.allocations);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("allocations", allocations)
                .toString();
    }
}
