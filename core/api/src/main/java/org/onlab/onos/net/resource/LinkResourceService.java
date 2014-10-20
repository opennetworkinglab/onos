package org.onlab.onos.net.resource;

import java.util.Map;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.PathIntent;

/**
 * Service for providing link resource allocation.
 */
public interface LinkResourceService {

    /**
     * Allocates resources along the path.
     * <p>
     * Tries to allocate given resources on the links along the path specified
     * by the given intent.
     *
     * @param res resources to be allocated
     * @param intent an intent to be used for specifying the path
     */
    void allocateResource(LinkResources res, PathIntent intent);

    /**
     * Releases resources along the path.
     *
     * @param intentId an ID for the intent for specifying the path
     */
    void releaseResource(IntentId intentId);

    /**
     * Returns all allocated resources to each link.
     *
     * @return allocated resources to each link with {@link IntentId}
     */
    Map<Link, Map<IntentId, LinkResources>> allocatedResources();

    /**
     * Returns all allocated resources to given link.
     *
     * @param link a target link
     * @return allocated resources to the target link with {@link IntentId}
     */
    Map<IntentId, LinkResources> allocatedResources(Link link);

    /**
     * Returns available resources for each link.
     *
     * @return available resources for each link
     */
    Map<Link, LinkResources> availableResources();

    /**
     * Returns available resources for given link.
     * @param link a target link
     * @return available resources for the target link
     */
    LinkResource availableResources(Link link);
}
