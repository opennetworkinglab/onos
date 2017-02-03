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

package org.onosproject.incubator.net.virtual;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Key;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of VirtualNetworkIntent connectivity.
 */
@Beta
public final class VirtualNetworkIntent extends ConnectivityIntent {

    private final NetworkId networkId;
    private final ConnectPoint ingressPoint;
    private final ConnectPoint egressPoint;

    private static final String NETWORK_ID_NULL = "Network ID cannot be null";

    /**
     * Returns a new point to point intent builder. The application id,
     * ingress point and egress point are required fields.  If they are
     * not set by calls to the appropriate methods, an exception will
     * be thrown.
     *
     * @return point to point builder
     */
    public static VirtualNetworkIntent.Builder builder() {
        return new VirtualNetworkIntent.Builder();
    }

    /**
     * Builder of a point to point intent.
     */
    public static final class Builder extends ConnectivityIntent.Builder {
        NetworkId networkId;
        ConnectPoint ingressPoint;
        ConnectPoint egressPoint;

        /**
         * Builder constructor.
         */
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
         * Sets the virtual network of the virtual network intent.
         *
         * @param networkId virtual network identifier
         * @return this builder
         */
        public VirtualNetworkIntent.Builder networkId(NetworkId networkId) {
            this.networkId = networkId;
            return this;
        }

        /**
         * Sets the ingress point of the virtual network intent that will be built.
         *
         * @param ingressPoint ingress connect point
         * @return this builder
         */
        public VirtualNetworkIntent.Builder ingressPoint(ConnectPoint ingressPoint) {
            this.ingressPoint = ingressPoint;
            return this;
        }

        /**
         * Sets the egress point of the virtual network intent that will be built.
         *
         * @param egressPoint egress connect point
         * @return this builder
         */
        public VirtualNetworkIntent.Builder egressPoint(ConnectPoint egressPoint) {
            this.egressPoint = egressPoint;
            return this;
        }

        /**
         * Builds a virtual network intent from the accumulated parameters.
         *
         * @return virtual network intent
         */
        public VirtualNetworkIntent build() {

            return new VirtualNetworkIntent(
                    networkId,
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
     * @param networkId    virtual network identifier
     * @param appId        application identifier
     * @param key          key of the intent
     * @param selector     traffic selector
     * @param treatment    treatment
     * @param ingressPoint ingress port
     * @param egressPoint  egress port
     * @param constraints  optional list of constraints
     * @param priority     priority to use for flows generated by this intent
     * @throws NullPointerException if {@code ingressPoint} or
     *                              {@code egressPoints} or {@code appId} is null.
     */
    private VirtualNetworkIntent(NetworkId networkId,
                                 ApplicationId appId,
                                 Key key,
                                 TrafficSelector selector,
                                 TrafficTreatment treatment,
                                 ConnectPoint ingressPoint,
                                 ConnectPoint egressPoint,
                                 List<Constraint> constraints,
                                 int priority,
                                 ResourceGroup resourceGroup) {
        super(appId, key, Collections.emptyList(), selector, treatment, constraints,
              priority, resourceGroup);

        checkNotNull(networkId, NETWORK_ID_NULL);
        checkArgument(!ingressPoint.equals(egressPoint),
                      "ingress and egress should be different (ingress: %s, egress: %s)", ingressPoint, egressPoint);

        this.networkId = networkId;
        this.ingressPoint = checkNotNull(ingressPoint);
        this.egressPoint = checkNotNull(egressPoint);
    }

    /**
     * Constructor for serializer.
     */
    protected VirtualNetworkIntent() {
        super();
        this.networkId = null;
        this.ingressPoint = null;
        this.egressPoint = null;
    }

    /**
     * Returns the virtual network identifier.
     *
     * @return network identifier
     */
    public NetworkId networkId() {
        return networkId;
    }

    /**
     * Returns the port on which the ingress traffic should be connected to
     * the egress.
     *
     * @return ingress port
     */
    public ConnectPoint ingressPoint() {
        return ingressPoint;
    }

    /**
     * Returns the port on which the traffic should egress.
     *
     * @return egress port
     */
    public ConnectPoint egressPoint() {
        return egressPoint;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("networkId", networkId)
                .add("id", id())
                .add("key", key())
                .add("appId", appId())
                .add("priority", priority())
                .add("resources", resources())
                .add("selector", selector())
                .add("treatment", treatment())
                .add("ingress", ingressPoint)
                .add("egress", egressPoint)
                .add("constraints", constraints())
                .add("resourceGroup", resourceGroup())
                .toString();
    }

}
