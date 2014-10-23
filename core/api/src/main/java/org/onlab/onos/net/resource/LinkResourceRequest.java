package org.onlab.onos.net.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;

import com.google.common.collect.ImmutableSet;

/**
 * Representation of a request for link resource.
 */
public final class LinkResourceRequest implements ResourceRequest {
    // TODO: should this class be interface?

    private final IntentId intentId;
    private final Collection<Link> links;
    private final Set<ResourceRequest> resources;

    /**
     * Creates a new link resource request with the given ID, links, and
     * resource requests.
     *
     * @param intentId intent ID related to this request
     * @param links a set of links for the request
     * @param resources a set of resources to be requested
     */
    private LinkResourceRequest(IntentId intentId,
            Collection<Link> links,
            Set<ResourceRequest> resources) {
        this.intentId = intentId;
        this.links = ImmutableSet.copyOf(links);
        this.resources = ImmutableSet.copyOf(resources);
    }

    /**
     * Returns the {@link IntentId} associated with the request.
     *
     * @return the {@link IntentId} associated with the request
     */
    IntentId intendId() {
        return intentId;
    }

    /**
     * Returns the set of target links.
     *
     * @return the set of target links
     */
    Collection<Link> links() {
        return links;
    }

    /**
     * Returns the set of resource requests.
     *
     * @return the set of resource requests
     */
    Set<ResourceRequest> resources() {
        return resources;
    }

    /**
     * Returns builder of link resource request.
     *
     * @param intentId intent ID related to this request
     * @param links a set of links for the request
     * @return builder of link resource request
     */
    public static LinkResourceRequest.Builder builder(
            IntentId intentId, Collection<Link> links) {
        return new Builder(intentId, links);
    }

    /**
     * Builder of link resource request.
     */
    public static final class Builder {
        private IntentId intentId;
        private Collection<Link> links;
        private Set<ResourceRequest> resources;

        /**
         * Creates a new link resource request.
         *
         * @param intentId intent ID related to this request
         * @param links a set of links for the request
         */
        private Builder(IntentId intentId, Collection<Link> links) {
            this.intentId = intentId;
            this.links = links;
            this.resources = new HashSet<>();
        }

        /**
         * Adds lambda request.
         *
         * @return self
         */
        public Builder addLambdaRequest() {
            resources.add(new LambdaResourceRequest());
            return this;
        }

        /**
         * Adds bandwidth request with bandwidth value.
         *
         * @param bandwidth bandwidth value to be requested
         * @return self
         */
        public Builder addBandwidthRequest(double bandwidth) {
            resources.add(new BandwidthResourceRequest(bandwidth));
            return this;
        }

        /**
         * Returns link resource request.
         *
         * @return link resource request
         */
        public LinkResourceRequest build() {
            return new LinkResourceRequest(intentId, links, resources);
        }
    }
}
