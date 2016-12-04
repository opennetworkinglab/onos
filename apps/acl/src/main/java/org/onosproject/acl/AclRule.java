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
 *
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li, Heng Qi and Haisheng Yu
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */
package org.onosproject.acl;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.core.IdGenerator;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * ACL rule class.
 */
public final class AclRule {

    private final RuleId id;

    private final Ip4Prefix srcIp;
    private final Ip4Prefix dstIp;
    private final byte ipProto;
    private final short dstTpPort;
    private final Action action;

    private static IdGenerator idGenerator;

    /**
     * Enum type for ACL rule's action.
     */
    public enum Action {
        DENY, ALLOW
    }

    /**
     * Constructor for serializer.
     */
    private AclRule() {
        this.id = null;
        this.srcIp = null;
        this.dstIp = null;
        this.ipProto = 0;
        this.dstTpPort = 0;
        this.action = null;
    }

    /**
     * Create a new ACL rule.
     *
     * @param srcIp     source IP address
     * @param dstIp     destination IP address
     * @param ipProto   IP protocol
     * @param dstTpPort destination transport layer port
     * @param action    ACL rule's action
     */
    private AclRule(Ip4Prefix srcIp, Ip4Prefix dstIp, byte ipProto,
                    short dstTpPort, Action action) {
        checkState(idGenerator != null, "Id generator is not bound.");
        this.id = RuleId.valueOf(idGenerator.getNewId());
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.ipProto = ipProto;
        this.dstTpPort = dstTpPort;
        this.action = action;
    }

    /**
     * Check if the first CIDR address is in (or the same as) the second CIDR address.
     */
    private boolean checkCidrInCidr(Ip4Prefix cidrAddr1, Ip4Prefix cidrAddr2) {
        if (cidrAddr2 == null) {
            return true;
        } else if (cidrAddr1 == null) {
            return false;
        }
        if (cidrAddr1.prefixLength() < cidrAddr2.prefixLength()) {
            return false;
        }
        int offset = 32 - cidrAddr2.prefixLength();

        int cidr1Prefix = cidrAddr1.address().toInt();
        int cidr2Prefix = cidrAddr2.address().toInt();
        cidr1Prefix = cidr1Prefix >> offset;
        cidr2Prefix = cidr2Prefix >> offset;
        cidr1Prefix = cidr1Prefix << offset;
        cidr2Prefix = cidr2Prefix << offset;

        return (cidr1Prefix == cidr2Prefix);
    }

    /**
     * Check if this ACL rule match the given ACL rule.
     *
     * @param r ACL rule to check against
     * @return true if this ACL rule matches the given ACL ruleule.
     */
    public boolean checkMatch(AclRule r) {
        return (this.dstTpPort == r.dstTpPort || r.dstTpPort == 0)
                && (this.ipProto == r.ipProto || r.ipProto == 0)
                && (checkCidrInCidr(this.srcIp(), r.srcIp()))
                && (checkCidrInCidr(this.dstIp(), r.dstIp()));
    }

    /**
     * Returns a new ACL rule builder.
     *
     * @return ACL rule builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of an ACL rule.
     */
    public static final class Builder {

        private Ip4Prefix srcIp = null;
        private Ip4Prefix dstIp = null;
        private byte ipProto = 0;
        private short dstTpPort = 0;
        private Action action = Action.DENY;

        private Builder() {
            // Hide constructor
        }

        /**
         * Sets the source IP address for the ACL rule that will be built.
         *
         * @param srcIp source IP address to use for built ACL rule
         * @return this builder
         */
        public Builder srcIp(Ip4Prefix srcIp) {
            this.srcIp = srcIp;
            return this;
        }

        /**
         * Sets the destination IP address for the ACL rule that will be built.
         *
         * @param dstIp destination IP address to use for built ACL rule
         * @return this builder
         */
        public Builder dstIp(Ip4Prefix dstIp) {
            this.dstIp = dstIp;
            return this;
        }

        /**
         * Sets the IP protocol for the ACL rule that will be built.
         *
         * @param ipProto IP protocol to use for built ACL rule
         * @return this builder
         */
        public Builder ipProto(byte ipProto) {
            this.ipProto = ipProto;
            return this;
        }

        /**
         * Sets the destination transport layer port for the ACL rule that will be built.
         *
         * @param dstTpPort destination transport layer port to use for built ACL rule
         * @return this builder
         */
        public Builder dstTpPort(short dstTpPort) {
            if ((ipProto == IPv4.PROTOCOL_TCP || ipProto == IPv4.PROTOCOL_UDP)) {
                this.dstTpPort = dstTpPort;
            }
            return this;
        }

        /**
         * Sets the action for the ACL rule that will be built.
         *
         * @param action action to use for built ACL rule
         * @return this builder
         */
        public Builder action(Action action) {
            this.action = action;
            return this;
        }

        /**
         * Builds an ACL rule from the accumulated parameters.
         *
         * @return ACL rule instance
         */
        public AclRule build() {
            checkState(srcIp != null && dstIp != null, "Either srcIp or dstIp must be assigned.");
            checkState(ipProto == 0 || ipProto == IPv4.PROTOCOL_ICMP
                               || ipProto == IPv4.PROTOCOL_TCP || ipProto == IPv4.PROTOCOL_UDP,
                       "ipProto must be assigned to TCP, UDP, or ICMP.");
            return new AclRule(srcIp, dstIp, ipProto, dstTpPort, action);
        }

    }

    /**
     * Binds an id generator for unique ACL rule id generation.
     * <p>
     * Note: A generator cannot be bound if there is already a generator bound.
     *
     * @param newIdGenerator id generator
     */
    public static void bindIdGenerator(IdGenerator newIdGenerator) {
        checkState(idGenerator == null, "Id generator is already bound.");
        idGenerator = checkNotNull(newIdGenerator);
    }

    public RuleId id() {
        return id;
    }

    public Ip4Prefix srcIp() {
        return srcIp;
    }

    public Ip4Prefix dstIp() {
        return this.dstIp;
    }

    public byte ipProto() {
        return ipProto;
    }

    public short dstTpPort() {
        return dstTpPort;
    }

    public Action action() {
        return action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, id.fingerprint(), ipProto, srcIp, dstIp, dstTpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AclRule) {
            AclRule that = (AclRule) obj;
            return Objects.equals(id, that.id) &&
                    Objects.equals(srcIp, that.srcIp) &&
                    Objects.equals(dstIp, that.dstIp) &&
                    Objects.equals(ipProto, that.ipProto) &&
                    Objects.equals(dstTpPort, that.dstTpPort) &&
                    Objects.equals(action, that.action);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("srcIp", srcIp)
                .add("dstIp", dstIp)
                .add("ipProto", ipProto)
                .add("dstTpPort", dstTpPort)
                .add("action", action)
                .toString();
    }

}
