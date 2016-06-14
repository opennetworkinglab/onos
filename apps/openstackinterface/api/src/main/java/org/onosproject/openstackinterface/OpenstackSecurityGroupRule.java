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
package org.onosproject.openstackinterface;

import org.onlab.packet.IpPrefix;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents Openstack Security Group Rules.
 */
public final class OpenstackSecurityGroupRule {

    private final Direction direction;
    private final String ethertype;
    private final String id;
    private final int portRangeMax;
    private final int portRangeMin;
    private final String protocol;
    private final String remoteGroupId;
    private final IpPrefix remoteIpPrefix;
    private final String secuityGroupId;
    private final String tenantId;

    /**
     * Direction of the Security Group.
     *
     */
    public enum Direction {
        INGRESS,
        EGRESS
    }

    private OpenstackSecurityGroupRule(Direction direction,
                                       String ethertype,
                                       String id,
                                       int portRangeMax,
                                       int portRangeMin,
                                       String protocol,
                                       String remoteGroupId,
                                       IpPrefix remoteIpPrefix,
                                       String securityGroupId,
                                       String tenantId) {
        this.direction = direction;
        this.ethertype = ethertype;
        this.id = checkNotNull(id);
        this.portRangeMax = portRangeMax;
        this.portRangeMin = portRangeMin;
        this.protocol = protocol;
        this.remoteGroupId = remoteGroupId;
        this.remoteIpPrefix = remoteIpPrefix;
        this.secuityGroupId = securityGroupId;
        this.tenantId = tenantId;
    }

    /**
     * Returns the builder object for the OpenstackSecurityGroupRule.
     *
     * @return OpenstackSecurityGroupRule builder object
     */
    public static OpenstackSecurityGroupRule.Builder builder() {
        return new Builder();
    }

    /**
     * Returns the direction.
     *
     * @return direction
     */
    public Direction direction() {
        return direction;
    }

    /**
     * Returns the Ethernet type.
     *
     * @return Ethernet type
     */
    public String ethertype() {
        return ethertype;
    }

    /**
     * Returns the Security Group ID.
     *
     * @return Security Group ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns the max of the port range.
     *
     * @return max of the port range
     */
    public int portRangeMax() {
        return portRangeMax;
    }

    /**
     * Returns the min of the port range.
     *
     * @return min of the port range
     */
    public int portRangeMin() {
        return portRangeMin;
    }

    /**
     * Returns the IP protocol.
     *
     * @return IP protocol
     */
    public String protocol() {
        return protocol;
    }

    /**
     * Returns the remote group ID.
     *
     * @return remote group ID
     */
    public String remoteGroupId() {
        return remoteGroupId;
    }

    /**
     * Returns the remote IP address.
     *
     * @return remote IP address
     */
    public IpPrefix remoteIpPrefix() {
        return this.remoteIpPrefix;
    }

    /**
     * Returns the Security Group ID.
     *
     * @return security group ID
     */
    public String secuityGroupId() {
        return secuityGroupId;
    }

    /**
     * Returns the tenant ID.
     *
     * @return tenant ID
     */
    public String tenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return new StringBuilder(" [")
                .append(direction + ",")
                .append(ethertype + ",")
                .append(id + ",")
                .append(portRangeMax + ",")
                .append(portRangeMin + ",")
                .append(protocol + ",'")
                .append(remoteGroupId + ",")
                .append(remoteIpPrefix + ",")
                .append(secuityGroupId + ",")
                .append(tenantId + "] ")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (this instanceof OpenstackSecurityGroupRule) {
            OpenstackSecurityGroupRule that = (OpenstackSecurityGroupRule) o;
            return this.direction.equals(that.direction) &&
                    this.ethertype.equals(that.ethertype) &&
                    this.id.equals(that.id) &&
                    this.portRangeMax == that.portRangeMax &&
                    this.portRangeMin == that.portRangeMin &&
                    this.protocol.equals(that.protocol) &&
                    this.remoteGroupId.equals(that.remoteGroupId) &&
                    this.secuityGroupId.equals(that.secuityGroupId) &&
                    this.remoteIpPrefix.equals(that.remoteIpPrefix) &&
                    this.tenantId.equals(that.tenantId);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, ethertype, id, portRangeMax, portRangeMin, protocol,
                remoteGroupId, remoteIpPrefix, secuityGroupId, tenantId);
    }

    /**
     * Represents a security group rule builder object.
     */
    public static final class Builder {

        private String direction;
        private String etherType;
        private String id;
        private String portRangeMax;
        private String portRangeMin;
        private String protocol;
        private String remoteGroupId;
        private String remoteIpPrefix;
        private String secuityGroupId;
        private String tenantId;


        /**
         * Sets the direction of the security group rule.
         *
         * @param direction direction (ingress or egress)
         * @return builder object
         */
        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        /**
         * Sets the Ethernet Type.
         *
         * @param etherType Ethernet Type
         * @return builder object
         */
        public Builder etherType(String etherType) {
            this.etherType = etherType;
            return this;
        }

        /**
         * Sets the Security Group Rule ID.
         *
         * @param id security group rule ID
         * @return builder object
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the port range max value.
         *
         * @param portRangeMax port range max value
         * @return builder object
         */
        public Builder portRangeMax(String portRangeMax) {
            this.portRangeMax = portRangeMax;
            return this;
        }

        /**
         * Sets the port range min value.
         *
         * @param portRangeMin port range min value
         * @return builder object
         */
        public Builder portRangeMin(String portRangeMin) {
            this.portRangeMin = portRangeMin;
            return this;
        }

        /**
         * Sets the protocol.
         *
         * @param protocol protocol
         * @return builder object
         */
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Sets the remote security group ID.
         *
         * @param remoteGroupId remote security group ID
         * @return builder
         */
        public Builder remoteGroupId(String remoteGroupId) {
            this.remoteGroupId = remoteGroupId;
            return this;
        }

        /**
         * Sets the remote IP address as prefix.
         *
         * @param remoteIpPrefix remote IP address
         * @return builder object
         */
        public Builder remoteIpPrefix(String remoteIpPrefix) {
            this.remoteIpPrefix = remoteIpPrefix;
            return this;
        }

        /**
         * Sets the Security Group ID.
         *
         * @param securityGroupId security group ID
         * @return builder object
         */
        public Builder securityGroupId(String securityGroupId) {
            this.secuityGroupId = securityGroupId;
            return this;
        }

        /**
         * Sets the tenant ID.
         *
         * @param tenantId tenant ID
         * @return builder object
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Creates a OpenstackSecurityGroupRule instance.
         *
         * @return OpenstackSecurityGroupRule object
         */
        public OpenstackSecurityGroupRule build() {

            int portRangeMinInt = (portRangeMin == null || portRangeMin.equals("null")) ?
                    -1 : Integer.parseInt(portRangeMin);
            int portRangeMaxInt = (portRangeMax == null || portRangeMax.equals("null")) ?
                    -1 : Integer.parseInt(portRangeMax);
            IpPrefix ipPrefix = (remoteIpPrefix == null || remoteIpPrefix.equals("null")) ?
                    null : IpPrefix.valueOf(remoteIpPrefix);

            return new OpenstackSecurityGroupRule(Direction.valueOf(direction.toUpperCase()), etherType, id,
                    portRangeMaxInt, portRangeMinInt, protocol, remoteGroupId, ipPrefix, secuityGroupId, tenantId);
        }
    }
}
