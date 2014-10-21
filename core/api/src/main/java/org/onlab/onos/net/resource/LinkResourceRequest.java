package org.onlab.onos.net.resource;

import java.util.Collection;
import java.util.Set;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;

/**
 * Representation of a request for link resource.
 */
public interface LinkResourceRequest extends ResourceRequest {

    /**
     * Returns the {@link IntentId} associated with the request.
     *
     * @return the {@link IntentId} associated with the request
     */
    IntentId intendId();

    /**
     * Returns the set of target links.
     *
     * @return the set of target links
     */
    Collection<Link> links();

    /**
     * Returns the set of resource requests.
     *
     * @return the set of resource requests
     */
    Set<ResourceRequest> resources();
}
