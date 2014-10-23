package org.onlab.onos.net.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;

import com.google.common.collect.ImmutableSet;

/**
 * Implementation of {@link LinkResourceRequest}.
 */
public final class DefaultLinkResourceRequest implements LinkResourceRequest {

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
    private DefaultLinkResourceRequest(IntentId intentId,
            Collection<Link> links,
            Set<ResourceRequest> resources) {
        this.intentId = intentId;
        this.links = ImmutableSet.copyOf(links);
        this.resources = ImmutableSet.copyOf(resources);
    }

    @Override
    public IntentId intendId() {
        return intentId;
    }

    @Override
    public Collection<Link> links() {
        return links;
    }

    @Override
    public Set<ResourceRequest> resources() {
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
    public static final class Builder implements LinkResourceRequest.Builder {
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
        @Override
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
        @Override
        public Builder addBandwidthRequest(double bandwidth) {
            resources.add(new BandwidthResourceRequest(bandwidth));
            return this;
        }

        /**
         * Returns link resource request.
         *
         * @return link resource request
         */
        @Override
        public LinkResourceRequest build() {
            return new DefaultLinkResourceRequest(intentId, links, resources);
        }
    }

}
