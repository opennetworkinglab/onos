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
import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Intents targeting a domain network.
 */
@Beta
public abstract class DomainIntent extends Intent {

    private final Set<FilteredConnectPoint> filteredIngressPoints;
    private final Set<FilteredConnectPoint> filteredEgressPoints;
    private final TrafficTreatment treatment;
    private final List<Constraint> constraints;


    /**
     * @param appId                 application identifier
     * @param key                   explicit key to use for intent
     * @param resources             required network resources (optional)
     * @param priority              intent priority
     * @param filteredIngressPoints filtered ingress points
     * @param filteredEgressPoints  filtered egress points
     * @param treatment             action to be applied at the egress
     * @param constraints           constraints of the intent
     * @throws NullPointerException if {@code filteredIngressPoints} or
     *                              {@code filteredEgressPoints} or {@code appId}
     *                              or {@code constraints} is null.
     * @throws IllegalArgumentException if {@code filteredIngressPoints} or {@code filteredEgressPoints} is empty.
     */
    public DomainIntent(ApplicationId appId, Key key,
                        Collection<NetworkResource> resources,
                        int priority,
                        Set<FilteredConnectPoint> filteredIngressPoints,
                        Set<FilteredConnectPoint> filteredEgressPoints,
                        TrafficTreatment treatment,
                        List<Constraint> constraints) {
        super(appId, key, resources, priority);

        checkNotNull(filteredIngressPoints, "Ingress points cannot be null");
        checkArgument(!filteredIngressPoints.isEmpty(), "Ingress point cannot be empty");
        checkNotNull(filteredEgressPoints, "Egress points cannot be null");
        checkArgument(!filteredEgressPoints.isEmpty(), "Egress point cannot be empty");
        this.filteredIngressPoints = filteredIngressPoints;
        this.filteredEgressPoints = filteredEgressPoints;
        this.treatment = treatment;
        this.constraints = checkNotNull(constraints, "Constraints cannot be null");
    }

    /**
     * Constructor for serializer.
     */
    protected DomainIntent() {
        super();
        filteredIngressPoints = null;
        filteredEgressPoints = null;
        treatment = null;
        constraints = null;
    }

    /**
     * Abstract builder for connectivity intents.
     */
    public abstract static class Builder extends Intent.Builder {
        protected List<Constraint> constraints = ImmutableList.of();
        protected TrafficTreatment treatment;

        /**
         * Creates a new empty builder.
         */
        protected Builder() {
        }

        @Override
        public Builder appId(ApplicationId appId) {
            return (Builder) super.appId(appId);
        }

        @Override
        public Builder key(Key key) {
            return (Builder) super.key(key);
        }

        @Override
        public Builder priority(int priority) {
            return (Builder) super.priority(priority);
        }

        /**
         * Sets the traffic treatment for the intent that will be built.
         *
         * @param treatment treatment to use for built intent
         * @return this builder
         */
        public Builder treatment(TrafficTreatment treatment) {
            this.treatment = treatment;
            return this;
        }

        /**
         * Sets the constraints for the intent that will be built.
         *
         * @param constraints constraints to use for built intent
         * @return this builder
         */
        public Builder constraints(List<Constraint> constraints) {
            this.constraints = ImmutableList.copyOf(constraints);
            return this;
        }
    }

    @Override
    public boolean isInstallable() {
        return true;
    }

    /**
     * Returns the filtered connected points on which the ingress traffic
     * should be connected to the egress.
     *
     * @return filtered ingress connect points
     */
    public Set<FilteredConnectPoint> filteredIngressPoints() {
        return filteredIngressPoints;
    }

    /**
     * Returns the filtered connected points on which the traffic should egress.
     *
     * @return filtered egress connect points
     */
    public Set<FilteredConnectPoint> filteredEgressPoints() {
        return filteredEgressPoints;
    }

    /**
     * Returns the action applied to the traffic at the egress.
     *
     * @return applied action
     */
    public TrafficTreatment treatment() {
        return treatment;
    }

    /**
     * Returns the set of connectivity constraints.
     *
     * @return list of intent constraints
     */
    public List<Constraint> constraints() {
        return constraints;
    }

}
