/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.domain;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Key;

import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a point to point intent targeting a domain. It consists
 * into an ingress and an egress filtered connection points and
 * a set of links to represent the path.
 */
@Beta
public class DomainPointToPointIntent extends DomainIntent {

    private final List<Link> links;


    /**
     * Creates a new point-to-point domain intent with the supplied ingress/egress
     * ports and domainId.
     *
     * @param appId                application identifier
     * @param key                  explicit key to use for intent
     * @param priority             intent priority
     * @param filteredIngressPoint filtered ingress point
     * @param filteredEgressPoint  filtered egress point
     * @throws NullPointerException if {@code filteredIngressPoint} or
     *                              {@code filteredEgressPoint} or {@code appId}
     *                              or {@code links} or {@code constraints}
     *                              is null.
     */
    private DomainPointToPointIntent(ApplicationId appId, Key key,
                                     int priority,
                                     FilteredConnectPoint filteredIngressPoint,
                                     FilteredConnectPoint filteredEgressPoint,
                                     TrafficTreatment treatment,
                                     List<Constraint> constraints,
                                     List<Link> links) {

        super(appId, key, resources(links),
              priority, ImmutableSet.of(filteredIngressPoint),
              ImmutableSet.of(filteredEgressPoint), treatment, constraints);
        this.links = links;

    }

    /**
     * Constructor for serializer.
     */
    protected DomainPointToPointIntent() {
        super();
        this.links = null;
    }

    /**
     * Returns a new point to point domain intent builder. The application id,
     * ingress point, egress point and domainId are required fields.
     * If they are not set by calls to the appropriate methods,
     * an exception will be thrown.
     *
     * @return point to point builder
     */
    public static DomainPointToPointIntent.Builder builder() {
        return new Builder();
    }

    /**
     * Builder of a point to point domain intent.
     */
    public static final class Builder extends DomainIntent.Builder {
        private FilteredConnectPoint filteredIngressPoint;
        private FilteredConnectPoint filteredEgressPoint;
        private List<Link> links;

        private Builder() {
            // Hide constructor
        }

        @Override
        public Builder appId(ApplicationId appId) {
            super.appId = appId;
            return this;
        }

        @Override
        public Builder key(Key key) {
            super.key = key;
            return this;
        }

        @Override
        public Builder priority(int priority) {
            super.priority = priority;
            return this;
        }

        @Override
        public Builder treatment(TrafficTreatment treatment) {
            super.treatment = treatment;
            return this;
        }

        @Override
        public Builder constraints(List<Constraint> constraints) {
            super.constraints = ImmutableList.copyOf(constraints);
            return this;
        }

        /**
         * Sets the filtered ingress point of the domain point to point intent
         * that will be built.
         *
         * @param ingressPoint single ingress connect point
         * @return this builder
         */
        public Builder filteredIngressPoint(FilteredConnectPoint ingressPoint) {
            this.filteredIngressPoint = ingressPoint;
            return this;
        }

        /**
         * Sets the filtered egress point of the domain point to point intent
         * that will be built.
         *
         * @param egressPoint single egress connect point
         * @return this builder
         */
        public Builder filteredEgressPoint(FilteredConnectPoint egressPoint) {
            this.filteredEgressPoint = egressPoint;
            return this;
        }

        /**
         * Sets the links of the point to domain point intent that will be built.
         *
         * @param links links for the intent
         * @return this builder
         */
        public Builder links(List<Link> links) {
            this.links = ImmutableList.copyOf(links);
            return this;
        }

        /**
         * Builds a point to point domain intent from the accumulated parameters.
         *
         * @return point to point domain intent
         */
        public DomainPointToPointIntent build() {

            return new DomainPointToPointIntent(
                    appId,
                    key,
                    priority,
                    filteredIngressPoint,
                    filteredEgressPoint,
                    treatment,
                    constraints,
                    links
            );
        }
    }

    public List<Link> links() {
        return links;
    }

    /**
     * Produces a collection of network resources from the given links.
     *
     * @param links collection of links
     * @return collection of link resources
     */
    protected static Collection<NetworkResource> resources(Collection<Link> links) {
        checkNotNull(links, "links cannot be null");
        return ImmutableSet.copyOf(links);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("filtered ingress", filteredIngressPoints())
                .add("filtered egress", filteredEgressPoints())
                .toString();
    }

}

