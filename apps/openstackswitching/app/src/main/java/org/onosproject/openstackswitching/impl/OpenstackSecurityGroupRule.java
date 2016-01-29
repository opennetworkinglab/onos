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
package org.onosproject.openstackswitching.impl;

/**
 * Represents Openstack Security Group Rules.
 */
public final class OpenstackSecurityGroupRule {

    private String direction;
    private String ethertype;
    private String id;
    private String portRangeMax;
    private String portRangeMin;
    private String protocol;
    private String remoteGroupId;
    private String remoteIpPrefix;
    private String secuityGroupId;
    private String tenantId;

    private OpenstackSecurityGroupRule(String direction,
                                       String ethertype,
                                       String id,
                                       String portRangeMax,
                                       String portRangeMin,
                                       String protocol,
                                       String remoteGroupId,
                                       String remoteIpPrefix,
                                       String securityGroupId,
                                       String tenantId) {
        this.direction = direction;
        this.ethertype = ethertype;
        this.id = id;
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
            return new OpenstackSecurityGroupRule(direction, etherType, id, portRangeMax,
                    portRangeMin, protocol, remoteGroupId, remoteIpPrefix, secuityGroupId, tenantId);
        }
    }
}
