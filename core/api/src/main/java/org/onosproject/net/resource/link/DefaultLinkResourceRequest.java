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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import org.onlab.util.Bandwidth;
import org.onosproject.net.Link;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentId;

import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LambdaConstraint;
import org.onosproject.net.resource.ResourceRequest;
import org.onosproject.net.resource.ResourceType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link LinkResourceRequest}.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public final class DefaultLinkResourceRequest implements LinkResourceRequest {

    private final IntentId intentId;
    protected final Map<Link, Set<ResourceRequest>> requests;

    /**
     * Creates a new link resource request with the specified Intent ID,
     * and resource requests over links.
     *
     * @param intentId intent ID associated with this request
     * @param requests resource requests over links
     */
    private DefaultLinkResourceRequest(IntentId intentId, Map<Link, Set<ResourceRequest>> requests) {
        this.intentId = checkNotNull(intentId);
        this.requests = checkNotNull(ImmutableMap.copyOf(requests));
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
        return requests.keySet();
    }

    @Override
    public Set<ResourceRequest> resources() {
        return requests.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<ResourceRequest> resources(Link link) {
        return requests.get(link);
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
        private Map<Link, Set<ResourceRequest>> requests;

        /**
         * Creates a new link resource request.
         *
         * @param intentId intent ID related to this request
         * @param links a set of links for the request
         */
        private Builder(IntentId intentId, Collection<Link> links) {
            this.intentId = intentId;
            this.requests = new HashMap<>();
            for (Link link : links) {
                requests.put(link, new HashSet<>());
            }
        }

        /**
         * Adds lambda request.
         *
         * @return self
         * @deprecated in Emu Release
         */
        @Deprecated
        @Override
        public Builder addLambdaRequest() {
            for (Link link : requests.keySet()) {
                requests.get(link).add(new LambdaResourceRequest());
            }
            return this;
        }

        @Beta
        @Override
        public LinkResourceRequest.Builder addLambdaRequest(LambdaResource lambda) {
            for (Link link : requests.keySet()) {
                requests.get(link).add(new LambdaResourceRequest(lambda));
            }
            return this;
        }

        /**
         * Adds Mpls request.
         *
         * @return self
         * @deprecated in Emu Release
         */
        @Deprecated
        @Override
        public Builder addMplsRequest() {
            for (Link link : requests.keySet()) {
                requests.get(link).add(new MplsLabelResourceRequest());
            }
            return this;
        }

        @Beta
        @Override
        public Builder addMplsRequest(MplsLabel label) {
            for (Link link : requests.keySet()) {
                requests.get(link).add(new MplsLabelResourceRequest(label));
            }
            return this;
        }

        @Beta
        @Override
        public LinkResourceRequest.Builder addMplsRequest(Map<Link, MplsLabel> labels) {
            for (Link link : labels.keySet()) {
                if (!requests.containsKey(link)) {
                    requests.put(link, new HashSet<>());
                }
                requests.get(link).add(new MplsLabelResourceRequest(labels.get(link)));
            }

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
            for (Link link : requests.keySet()) {
                requests.get(link).add(new BandwidthResourceRequest(new BandwidthResource(Bandwidth.bps(bandwidth))));
            }
            return this;
        }

        @Override
        public LinkResourceRequest.Builder addConstraint(Constraint constraint) {
            if (constraint instanceof LambdaConstraint) {
                return addLambdaRequest();
            } else if (constraint instanceof BandwidthConstraint) {
                BandwidthConstraint bw = (BandwidthConstraint) constraint;
                return addBandwidthRequest(bw.bandwidth().bps());
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
            return new DefaultLinkResourceRequest(intentId, requests);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(intentId, links());
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
                && Objects.equals(this.links(), other.links());
    }
}
