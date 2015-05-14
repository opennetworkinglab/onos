/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.segmentrouting;

import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tunnel Policy.
 */
public final class TunnelPolicy implements Policy {

    private final SegmentRoutingManager srManager;
    private final Type type;
    private final String id;
    private final TrafficSelector selector;
    private final int priority;
    private final String tunnelId;

    private TunnelPolicy(SegmentRoutingManager srm, String policyId, Type type,
                         TrafficSelector selector, int priority, String tunnelId) {
        this.srManager = srm;
        this.id = checkNotNull(policyId);
        this.type = type;
        this.tunnelId = tunnelId;
        this.priority = priority;
        this.selector = selector;
    }

    /**
     * Creates a TunnelPolicy reference.
     *
     * @param p TunnelPolicy reference
     */
    public TunnelPolicy(TunnelPolicy p) {
        this.srManager = p.srManager;
        this.id = p.id;
        this.type = p.type;
        this.tunnelId = p.tunnelId;
        this.priority = p.priority;
        this.selector = p.selector;
    }

    /**
     * Creates a TunnelPolicy reference.
     *
     * @param p TunnelPolicy reference
     */
    public TunnelPolicy(SegmentRoutingManager srm, TunnelPolicy p) {
        this.srManager = srm;
        this.id = p.id;
        this.type = p.type;
        this.tunnelId = p.tunnelId;
        this.priority = p.priority;
        this.selector = p.selector;
    }

    /**
     * Returns the TunnelPolicy builder reference.
     *
     * @return TunnelPolicy builder
     */
    public static TunnelPolicy.Builder builder() {
        return new Builder();
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public TrafficSelector selector() {
        return selector;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean create() {

        Tunnel tunnel = srManager.getTunnel(tunnelId);

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId)
                .makePermanent()
                .nextStep(tunnel.groupId())
                .withPriority(priority)
                .withSelector(selector)
                .withFlag(ForwardingObjective.Flag.VERSATILE);

        srManager.flowObjectiveService.forward(tunnel.source(), fwdBuilder.add());

        return true;
    }

    @Override
    public boolean remove() {

        Tunnel tunnel = srManager.getTunnel(tunnelId);

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId)
                .makePermanent()
                .withSelector(selector)
                .withPriority(priority)
                .nextStep(tunnel.groupId())
                .withFlag(ForwardingObjective.Flag.VERSATILE);

        srManager.flowObjectiveService.forward(tunnel.source(), fwdBuilder.remove());

        return true;
    }

    /**
     * Returns the tunnel ID of the policy.
     *
     * @return Tunnel ID
     */
    public String tunnelId() {
        return this.tunnelId;
    }

    /**
     * Tunnel Policy Builder.
     */
    public static final class Builder {

        private SegmentRoutingManager srManager;
        private String id;
        private Type type;
        private TrafficSelector selector;
        private int priority;
        private String tunnelId;

        /**
         * Sets the policy Id.
         *
         * @param id policy Id
         * @return Builder object
         */
        public Builder setPolicyId(String id) {
            this.id = id;

            return this;
        }

        /**
         * Sets the policy type.
         *
         * @param type policy type
         * @return Builder object
         */
        public Builder setType(Type type) {
            this.type = type;

            return this;
        }

        /**
         * Sets the TrafficSelector.
         *
         * @param selector TrafficSelector
         * @return Builder object
         */
        public Builder setSelector(TrafficSelector selector) {
            this.selector = selector;

            return this;
        }

        /**
         * Sets the priority of the policy.
         *
         * @param p priority
         * @return Builder object
         */
        public Builder setPriority(int p) {
            this.priority = p;

            return this;
        }

        /**
         * Sets the tunnel Id.
         *
         * @param tunnelId tunnel Id
         * @return Builder object
         */
        public Builder setTunnelId(String tunnelId) {
            this.tunnelId = tunnelId;

            return this;
        }

        /**
         * Sets the Segment Routing Manager reference.
         *
         * @param srm Segment Routing Manager reference
         * @return Builder object
         */
        public Builder setManager(SegmentRoutingManager srm) {
            this.srManager = srm;

            return this;
        }

        /**
         * Builds the policy.
         *
         * @return Tunnel Policy reference
         */
        public Policy build() {
            return new TunnelPolicy(srManager, id, type, selector, priority, tunnelId);
        }
    }
}
