/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

import org.onlab.util.Bandwidth;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentId;

import com.google.common.collect.ImmutableSet;

import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;

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
    public ResourceType type() {
        return null;
    }

    @Override
    public IntentId intentId() {
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
         * Adds Mpls request.
         *
         * @return self
         */
        @Override
        public Builder addMplsRequest() {
            resources.add(new MplsLabelResourceRequest());
            return this;
        }

        /**
         * Adds bandwidth request with bandwidth value.
         *
         * @param bandwidth bandwidth value in bits per second to be requested
         * @return self
         */
        @Override
        public Builder addBandwidthRequest(double bandwidth) {
            resources.add(new BandwidthResourceRequest(new BandwidthResource(Bandwidth.bps(bandwidth))));
            return this;
        }

        @Override
        public LinkResourceRequest.Builder addConstraint(Constraint constraint) {
            if (constraint instanceof LambdaConstraint) {
                return addLambdaRequest();
            } else if (constraint instanceof BandwidthConstraint) {
                BandwidthConstraint bw = (BandwidthConstraint) constraint;
                return addBandwidthRequest(bw.bandwidth().toDouble());
            }
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

    @Override
    public int hashCode() {
        return Objects.hash(intentId, links);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DefaultLinkResourceRequest other = (DefaultLinkResourceRequest) obj;
        return Objects.equals(this.intentId, other.intentId)
                && Objects.equals(this.links, other.links);
    }
}
