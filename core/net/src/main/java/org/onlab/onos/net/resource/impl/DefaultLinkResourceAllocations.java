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
package org.onlab.onos.net.resource.impl;

import com.google.common.base.MoreObjects;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.resource.LinkResourceAllocations;
import org.onlab.onos.net.resource.LinkResourceRequest;
import org.onlab.onos.net.resource.ResourceAllocation;
import org.onlab.onos.net.resource.ResourceRequest;
import org.onlab.onos.net.resource.ResourceType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link LinkResourceAllocations}.
 */
public class DefaultLinkResourceAllocations implements LinkResourceAllocations {
    private final LinkResourceRequest request;
    private final Map<Link, Set<ResourceAllocation>> allocations;

    /**
     * Creates a new link resource allocations.
     *
     * @param request     requested resources
     * @param allocations allocated resources
     */
    DefaultLinkResourceAllocations(LinkResourceRequest request,
                                   Map<Link, Set<ResourceAllocation>> allocations) {
        this.request = request;
        this.allocations = allocations;
    }

    @Override
    public IntentId intendId() {
        return request.intendId();
    }

    @Override
    public Collection<Link> links() {
        return request.links();
    }

    @Override
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
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("allocations", allocations)
                .toString();
    }
}
