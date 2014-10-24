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
     * Updates previously made allocations with a new resource request.
     *
     * @param req            updated resource request
     * @param oldAllocations old resource allocations
     * @return new resource allocations
     */
    LinkResourceAllocations updateResources(LinkResourceRequest req,
                                            LinkResourceAllocations oldAllocations);

    /**
     * Returns all allocated resources.
     *
     * @return allocated resources
     */
    Iterable<LinkResourceAllocations> getAllocations();

    /**
     * Returns the resources allocated for an Intent.
     *
     * @param intentId the target Intent's id
     * @return allocated resources for Intent
     */
    LinkResourceAllocations getAllocations(IntentId intentId);

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

    /**
     * Returns available resources for given link.
     *
     * @param link        a target link
     * @param allocations allocations to be included as available
     * @return available resources for the target link
     */
    ResourceRequest getAvailableResources(Link link,
                                          LinkResourceAllocations allocations);

}
