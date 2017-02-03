/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent;

import java.util.Collections;
import java.util.List;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of point-to-point connectivity.
 */
@Beta
public final class PointToPointIntent extends ConnectivityIntent {

    private final FilteredConnectPoint ingressPoint;
    private final FilteredConnectPoint egressPoint;

    /**
     * Returns a new point to point intent builder. The application id,
     * ingress point and egress point are required fields.  If they are
     * not set by calls to the appropriate methods, an exception will
     * be thrown.
     *
     * @return point to point builder
     */
    public static PointToPointIntent.Builder builder() {
        return new Builder();
    }

    /**
     * Builder of a point to point intent.
     */
    public static final class Builder extends ConnectivityIntent.Builder {
        FilteredConnectPoint ingressPoint;
        FilteredConnectPoint egressPoint;

        private Builder() {
            // Hide constructor
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
        public Builder selector(TrafficSelector selector) {
            return (Builder) super.selector(selector);
        }

        @Override
        public Builder treatment(TrafficTreatment treatment) {
            return (Builder) super.treatment(treatment);
        }

        @Override
        public Builder constraints(List<Constraint> constraints) {
            return (Builder) super.constraints(constraints);
        }

        @Override
        public Builder priority(int priority) {
            return (Builder) super.priority(priority);
        }

        @Override
        public Builder resourceGroup(ResourceGroup resourceGroup) {
            return (Builder) super.resourceGroup(resourceGroup);
        }

        /**
         * Sets the ingress point of the point to point intent that will be built.
         *
         * @param ingressPoint ingress connect point
         * @return this builder
         */
        @Deprecated
        public Builder ingressPoint(ConnectPoint ingressPoint) {
            this.ingressPoint = new FilteredConnectPoint(ingressPoint);
            return this;
        }

        /**
         * Sets the egress point of the point to point intent that will be built.
         *
         * @param egressPoint egress connect point
         * @return this builder
         */
        @Deprecated
        public Builder egressPoint(ConnectPoint egressPoint) {
            this.egressPoint = new FilteredConnectPoint(egressPoint);
            return this;
        }

        /**
         * Sets the filtered ingress point of the point to
         * point intent that will be built.
         *
         * @param ingressPoint filtered ingress connect point
         * @return this builder
         */
        public Builder filteredIngressPoint(FilteredConnectPoint ingressPoint) {
            this.ingressPoint = ingressPoint;
            return this;
        }

        /**
         * Sets the filtered egress point of the point to
         * point intent that will be built.
         *
         * @param egressPoint filtered egress connect point
         * @return this builder
         */
        public Builder filteredEgressPoint(FilteredConnectPoint egressPoint) {
            this.egressPoint = egressPoint;
            return this;
        }

        /**
         * Builds a point to point intent from the accumulated parameters.
         *
         * @return point to point intent
         */
        public PointToPointIntent build() {

            return new PointToPointIntent(
                    appId,
                    key,
                    selector,
                    treatment,
                    ingressPoint,
                    egressPoint,
                    constraints,
                    priority,
                    resourceGroup
            );
        }
    }



    /**
     * Creates a new point-to-point intent with the supplied ingress/egress
     * ports and constraints.
     *
     * @param appId        application identifier
     * @param key          key of the intent
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint filtered ingress port
     * @param egressPoint  filtered egress port
     * @param constraints  optional list of constraints
     * @param priority     priority to use for flows generated by this intent
     * @throws NullPointerException if {@code ingressPoint} or
     *        {@code egressPoints} or {@code appId} is null.
     */
    private PointToPointIntent(ApplicationId appId,
                               Key key,
                               TrafficSelector selector,
                               TrafficTreatment treatment,
                               FilteredConnectPoint ingressPoint,
                               FilteredConnectPoint egressPoint,
                               List<Constraint> constraints,
                               int priority,
                               ResourceGroup resourceGroup) {
        super(appId, key, Collections.emptyList(), selector, treatment, constraints,
                priority, resourceGroup);

        checkArgument(!ingressPoint.equals(egressPoint),
                "ingress and egress should be different (ingress: %s, egress: %s)", ingressPoint, egressPoint);

        this.ingressPoint = checkNotNull(ingressPoint);
        this.egressPoint = checkNotNull(egressPoint);
    }

    /**
     * Constructor for serializer.
     */
    protected PointToPointIntent() {
        super();
        this.ingressPoint = null;
        this.egressPoint = null;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress port
     */
    @Deprecated
    public ConnectPoint ingressPoint() {
        return ingressPoint.connectPoint();
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    @Deprecated
    public ConnectPoint egressPoint() {
        return egressPoint.connectPoint();
    }

    /**
     * Returns the filtered port on which the ingress traffic should be connected to the
     * egress.
     *
     * @return ingress port
     */
    public FilteredConnectPoint filteredIngressPoint() {
        return ingressPoint;
    }

    /**
     * Return the filtered port on which the traffic should exit.
     *
     * @return egress port
     */
    public FilteredConnectPoint filteredEgressPoint() {
        return egressPoint;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingress", filteredIngressPoint())
                .add("egress", filteredEgressPoint())
                .add("constraints", constraints())
                .add("resourceGroup", resourceGroup())
                .toString();
    }

}
