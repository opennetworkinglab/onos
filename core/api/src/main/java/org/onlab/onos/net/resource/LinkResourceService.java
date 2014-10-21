package org.onlab.onos.net.resource;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;

/**
 * Service for providing link resource allocation.
 */
public interface LinkResourceService {

    /**
     * Requests resources.
     *
     * @param req resources to be allocated
     * @return allocated resources
     */
    LinkResourceAllocations requestResources(LinkResourceRequest req);

    /**
     * Releases resources.
     *
     * @param allocations resources to be released
     */
    void releaseResources(LinkResourceAllocations allocations);

    /**
     * Returns all allocated resources.
     *
     * @return allocated resources
     */
    Iterable<LinkResourceAllocations> getAllocations();

    /**
     * Returns all allocated resources to given link.
     *
     * @param link a target link
     * @return allocated resources
     */
    Iterable<LinkResourceAllocations> getAllocations(Link link);

    /**
     * Returns all IDs of intents using the given link.
     *
     * @param link a target link
     * @return IDs of intents using the link
     */
    Iterable<IntentId> getIntents(Link link);

    /**
     * Returns available resources for given link.
     *
     * @param link a target link
     * @return available resources for the target link
     */
    ResourceRequest getAvailableResources(Link link);
}
