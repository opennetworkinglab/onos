package org.onlab.onos.net.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;

/**
 * Implementation of {@link LinkResourceAllocations}.
 */
public class DefaultLinkResourceAllocations implements LinkResourceAllocations {
    private final LinkResourceRequest request;
    private final Map<Link, Set<ResourceAllocation>> allocations;

    /**
     * Creates a new link resource allocations.
     *
     * @param request requested resources
     * @param allocations allocated resources
     */
    protected DefaultLinkResourceAllocations(LinkResourceRequest request,
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

}
