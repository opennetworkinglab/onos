/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tunnel Policy.
 */
public final class TunnelPolicy implements Policy {

    private final Type type;
    private final String id;
    private final int priority;
    private final String tunnelId;
    private String dstIp;
    private String srcIp;
    private String ipProto;
    private short srcPort;
    private short dstPort;

    private TunnelPolicy(String policyId, Type type, int priority, String tunnelId, String srcIp,
                         String dstIp, String ipProto, short srcPort, short dstPort) {
        this.id = checkNotNull(policyId);
        this.type = type;
        this.tunnelId = tunnelId;
        this.priority = priority;
        this.dstIp = dstIp;
        this.srcIp = srcIp;
        this.ipProto = ipProto;
        this.srcPort = srcPort;
        this.dstPort = dstPort;

    }

    /**
     * Creates a TunnelPolicy reference.
     *
     * @param p TunnelPolicy reference
     */
    public TunnelPolicy(TunnelPolicy p) {
        this.id = p.id;
        this.type = p.type;
        this.tunnelId = p.tunnelId;
        this.priority = p.priority;
        this.srcIp = p.srcIp;
        this.dstIp = p.dstIp;
        this.ipProto = p.ipProto;
        this.srcPort = p.srcPort;
        this.dstPort = p.dstPort;
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
    public int priority() {
        return priority;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String srcIp() {
        return srcIp;
    }

    @Override
    public String dstIp() {
        return dstIp;
    }

    @Override
    public String ipProto() {
        return ipProto;
    }

    @Override
    public short srcPort() {
        return srcPort;
    }

    @Override
    public short dstPort() {
        return dstPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof TunnelPolicy) {
            TunnelPolicy that = (TunnelPolicy) o;
            // We do not compare the policy ID
            if (this.type.equals(that.type) &&
                    this.tunnelId.equals(that.tunnelId) &&
                    this.priority == that.priority &&
                    this.srcIp.equals(that.srcIp) &&
                    this.dstIp.equals(that.dstIp) &&
                    this.srcPort == that.srcPort &&
                    this.dstPort == that.dstPort &&
                    this.ipProto.equals(that.ipProto)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, tunnelId, srcIp, dstIp, ipProto,
                srcPort, dstPort, priority);
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

        private String id;
        private Type type;
        private int priority;
        private String tunnelId;
        private String dstIp;
        private String srcIp;
        private String ipProto;
        private short srcPort;
        private short dstPort;

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
         * Sets the source IP address.
         *
         * @param srcIp source IP address
         * @return Builder object
         */
        public Builder setSrcIp(String srcIp) {
            this.srcIp = srcIp;

            return this;
        }

        /**
         * Sets the destination IP address.
         *
         * @param dstIp destination IP address
         * @return Builder object
         */
        public Builder setDstIp(String dstIp) {
            this.dstIp = dstIp;

            return this;
        }

        /**
         * Sets the IP protocol.
         *
         * @param proto IP protocol
         * @return Builder object
         */
        public Builder setIpProto(String proto) {
            this.ipProto = proto;

            return this;
        }

        /**
         * Sets the source port.
         *
         * @param srcPort source port
         * @return Builder object
         */
        public Builder setSrcPort(short srcPort) {
            this.srcPort = srcPort;

            return this;
        }

        /**
         * Sets the destination port.
         *
         * @param dstPort destination port
         * @return Builder object
         */
        public Builder setDstPort(short dstPort) {
            this.dstPort = dstPort;

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
         * Builds the policy.
         *
         * @return Tunnel Policy reference
         */
        public Policy build() {
            return new TunnelPolicy(id, type, priority, tunnelId, srcIp, dstIp,
                    ipProto, srcPort, dstPort);
        }
    }
}
