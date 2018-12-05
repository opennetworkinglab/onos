/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.IdGenerator;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * ACL rule class.
 */
public final class AclRule {

    private final RuleId id;

    private final MacAddress srcMac;
    private final MacAddress dstMac;
    private final Ip4Prefix srcIp;
    private final Ip4Prefix dstIp;
    private final Ip6Prefix srcIp6;
    private final Ip6Prefix dstIp6;
    private final byte ipProto;
    private final short srcTpPort;
    private final short dstTpPort;
    private final byte dscp;
    private final Action action;

    protected static IdGenerator idGenerator;
    private static final Object ID_GENERATOR_LOCK = new Object();

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
        this.srcMac = null;
        this.dstMac = null;
        this.srcIp = null;
        this.dstIp = null;
        this.srcIp6 = null;
        this.dstIp6 = null;
        this.dscp = 0;
        this.ipProto = 0;
        this.dstTpPort = 0;
        this.srcTpPort = 0;
        this.action = null;
    }

    /**
     * Create a new ACL rule.
     *
     * @param srcIp     source IP address
     * @param srcMac    source Mac address
     * @param dstMac    destination Mac address
     * @param dstIp     destination IP address
     * @param ipProto   IP protocol
     * @param dscp      IP dscp
     * @param dstTpPort destination transport layer port
     * @param srcTpPort source transport layer port
     * @param action    ACL rule's action
     */
    private AclRule(MacAddress srcMac, MacAddress dstMac, Ip4Prefix srcIp, Ip4Prefix dstIp,
                    Ip6Prefix srcIp6, Ip6Prefix dstIp6, byte ipProto, byte dscp,
                    short dstTpPort, short srcTpPort, Action action) {
        synchronized (ID_GENERATOR_LOCK) {
            checkState(idGenerator != null, "Id generator is not bound.");
            this.id = RuleId.valueOf(idGenerator.getNewId());
        }
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcIp6 = srcIp6;
        this.dstIp6 = dstIp6;
        this.ipProto = ipProto;
        this.dscp = dscp;
        this.dstTpPort = dstTpPort;
        this.srcTpPort = srcTpPort;
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
                && (this.srcTpPort == r.srcTpPort || r.srcTpPort == 0)
                && (this.ipProto == r.ipProto || r.ipProto == 0)
                && (this.dscp == r.dscp || r.dscp == 0)
                && (this.srcMac == r.srcMac || r.srcMac == null)
                && (this.dstMac == r.dstMac || r.dstMac == null)
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
        private Ip6Prefix srcIp6 = null;
        private Ip6Prefix dstIp6 =  null;
        private MacAddress srcMac = null;
        private MacAddress dstMac = null;
        private byte ipProto = 0;
        private byte dscp = 0;
        private short dstTpPort = 0;
        private short srcTpPort = 0;
        private Action action = Action.DENY;

        private Builder() {
            // Hide constructor
        }

        /**
         * Sets the source Mac address for the ACL rule that will be built.
         *
         * @param srcMac source Mac address to use for built ACL rule
         * @return this builder
         */
        public Builder srcMac(MacAddress srcMac) {
            this.srcMac = srcMac;
            return this;
        }

        /**
         * Sets the destination Mac address for the ACL rule that will be built.
         *
         * @param dstMac destination Mac address to use for built ACL rule
         * @return this builder
         */
        public Builder dstMac(MacAddress dstMac) {
            this.dstMac = dstMac;
            return this;
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
         * Sets the source IP address for the ACL rule that will be built.
         *
         * @param srcIp6 source IP address to use for built ACL rule
         * @return this builder
         */
        public Builder srcIp6(Ip6Prefix srcIp6) {
            this.srcIp6 = srcIp6;
            return this;
        }

        /**
         * Sets the destination IP address for the ACL rule that will be built.
         *
         * @param dstIp6 destination IP address to use for built ACL rule
         * @return this builder
         */
        public Builder dstIp6(Ip6Prefix dstIp6) {
            this.dstIp6 = dstIp6;
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
         * Sets the IP dscp for the ACL rule that will be built.
         *
         * @param dscp IP dscp to use for built ACL rule
         * @return this builder
         */
        public Builder dscp(byte dscp) {
            this.dscp = dscp;
            return this;
        }

        /**
         * Sets the destination transport layer port for the ACL rule that will be built.
         *
         * @param dstTpPort destination transport layer port to use for built ACL rule
         * @return this builder
         */
        public Builder dstTpPort(short dstTpPort) {
            if (ipProto == IPv4.PROTOCOL_TCP || ipProto == IPv4.PROTOCOL_UDP ||
                    ipProto == IPv6.PROTOCOL_TCP || ipProto == IPv6.PROTOCOL_UDP) {
                this.dstTpPort = dstTpPort;
            }
            return this;
        }

        /**
         * Sets the source transport layer port for the ACL rule that will be built.
         *
         * @param srcTpPort destination transport layer port to use for built ACL rule
         * @return this builder
         */
        public Builder srcTpPort(short srcTpPort) {
            if (ipProto == IPv4.PROTOCOL_TCP || ipProto == IPv4.PROTOCOL_UDP ||
                    ipProto == IPv6.PROTOCOL_TCP || ipProto == IPv6.PROTOCOL_UDP) {
                this.srcTpPort = srcTpPort;
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
            boolean assigned = true, notAssigned = false;
            checkState(!((srcIp != null || dstIp != null) && (srcIp6 != null || dstIp6 != null)),
                       "Either Ipv4 or Ipv6 must be assigned.");
            checkState((srcIp != null || dstIp != null) ?
                               assigned : (srcIp6 != null || dstIp6 != null) ? assigned : notAssigned,
                       "Either srcIp or dstIp must be assigned.");
            checkState(ipProto == 0 || ipProto == IPv4.PROTOCOL_ICMP || ipProto == IPv4.PROTOCOL_TCP ||
                               ipProto == IPv4.PROTOCOL_UDP || ipProto == IPv6.PROTOCOL_ICMP6 ||
                               ipProto == IPv6.PROTOCOL_TCP || ipProto == IPv6.PROTOCOL_UDP,
                       "ipProto must be assigned to TCP, UDP, or ICMP.");
            return new AclRule(srcMac, dstMac, srcIp, dstIp, srcIp6, dstIp6,
                               ipProto, dscp, dstTpPort, srcTpPort, action);
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
        synchronized (ID_GENERATOR_LOCK) {
            checkState(idGenerator == null, "Id generator is already bound.");
            idGenerator = checkNotNull(newIdGenerator);
        }
    }

    public RuleId id() {
        return id;
    }

    public MacAddress srcMac() {
        return srcMac;
    }

    public MacAddress dstMac() {
        return dstMac;
    }

    public Ip4Prefix srcIp() {
        return srcIp;
    }

    public Ip4Prefix dstIp() {
        return this.dstIp;
    }

    public Ip6Prefix srcIp6() {
        return srcIp6;
    }

    public Ip6Prefix dstIp6() {
        return dstIp6;
    }
    public byte ipProto() {
        return ipProto;
    }

    public byte dscp() {
        return dscp;
    }

    public short dstTpPort() {
        return dstTpPort;
    }

    public short srcTpPort() {
        return srcTpPort;
    }

    public Action action() {
        return action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, id.fingerprint(), srcMac, dstMac, ipProto, dscp,
                srcIp, dstIp, srcIp6, dstIp6, dstTpPort, srcTpPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AclRule) {
            AclRule that = (AclRule) obj;
            return Objects.equals(id, that.id) &&
                    Objects.equals(srcMac, that.srcMac) &&
                    Objects.equals(dstMac, that.dstMac) &&
                    Objects.equals(srcIp, that.srcIp) &&
                    Objects.equals(dstIp, that.dstIp) &&
                    Objects.equals(srcIp6, that.srcIp6) &&
                    Objects.equals(dstIp6, that.dstIp6) &&
                    Objects.equals(ipProto, that.ipProto) &&
                    Objects.equals(dscp, that.dscp) &&
                    Objects.equals(dstTpPort, that.dstTpPort) &&
                    Objects.equals(srcTpPort, that.srcTpPort) &&
                    Objects.equals(action, that.action);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("srcMac", srcMac)
                .add("dstMac", dstMac)
                .add("srcIp", srcIp)
                .add("dstIp", dstIp)
                .add("srcIp6", srcIp6)
                .add("dstIp6", dstIp6)
                .add("ipProto", ipProto)
                .add("dscp", dscp)
                .add("dstTpPort", dstTpPort)
                .add("srcTpPort", srcTpPort)
                .add("action", action)
                .toString();
    }

}
