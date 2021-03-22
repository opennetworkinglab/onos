/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpPrefix;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation class of kubevirt security group rule.
 */
public final class DefaultKubevirtSecurityGroupRule implements KubevirtSecurityGroupRule {

    private static final String NOT_NULL_MSG = "Security Group Rule % cannot be null";

    private final String id;
    private final String securityGroupId;
    private final String direction;
    private final String etherType;
    private final Integer portRangeMax;
    private final Integer portRangeMin;
    private final String protocol;
    private final IpPrefix remoteIpPrefix;
    private final String remoteGroupId;

    /**
     * A default constructor.
     *
     * @param id                security group rule identifier
     * @param securityGroupId   security group identifier
     * @param direction         traffic direction
     * @param etherType         ethernet type
     * @param portRangeMax      maximum port range
     * @param portRangeMin      minimum port range
     * @param protocol          network protocol
     * @param remoteIpPrefix    remote IP prefix
     * @param remoteGroupId     remote group identifier
     */
    public DefaultKubevirtSecurityGroupRule(String id, String securityGroupId,
                                            String direction, String etherType,
                                            Integer portRangeMax, Integer portRangeMin,
                                            String protocol, IpPrefix remoteIpPrefix,
                                            String remoteGroupId) {
        this.id = id;
        this.securityGroupId = securityGroupId;
        this.direction = direction;
        this.etherType = etherType;
        this.portRangeMax = portRangeMax;
        this.portRangeMin = portRangeMin;
        this.protocol = protocol;
        this.remoteIpPrefix = remoteIpPrefix;
        this.remoteGroupId = remoteGroupId;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String securityGroupId() {
        return securityGroupId;
    }

    @Override
    public String direction() {
        return direction;
    }

    @Override
    public String etherType() {
        return etherType;
    }

    @Override
    public Integer portRangeMax() {
        return portRangeMax;
    }

    @Override
    public Integer portRangeMin() {
        return portRangeMin;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public IpPrefix remoteIpPrefix() {
        return remoteIpPrefix;
    }

    @Override
    public String remoteGroupId() {
        return remoteGroupId;
    }

    @Override
    public KubevirtSecurityGroupRule updateDirection(String updated) {
        return DefaultKubevirtSecurityGroupRule.builder()
                .remoteGroupId(remoteGroupId)
                .etherType(etherType)
                .protocol(protocol)
                .portRangeMin(portRangeMin)
                .portRangeMax(portRangeMax)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .id(id)
                .direction(updated)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtSecurityGroupRule that = (DefaultKubevirtSecurityGroupRule) o;
        return id.equals(that.id) && securityGroupId.equals(that.securityGroupId) &&
                direction.equals(that.direction) &&
                Objects.equals(etherType, that.etherType) &&
                Objects.equals(portRangeMax, that.portRangeMax) &&
                Objects.equals(portRangeMin, that.portRangeMin) &&
                Objects.equals(protocol, that.protocol) &&
                Objects.equals(remoteIpPrefix, that.remoteIpPrefix) &&
                Objects.equals(remoteGroupId, that.remoteGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, securityGroupId, direction, etherType, portRangeMax,
                portRangeMin, protocol, remoteIpPrefix, remoteGroupId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("securityGroupId", securityGroupId)
                .add("direction", direction)
                .add("etherType", etherType)
                .add("portRangeMax", portRangeMax)
                .add("portRangeMin", portRangeMin)
                .add("protocol", protocol)
                .add("remoteIpPrefix", remoteIpPrefix)
                .add("remoteGroupId", remoteGroupId)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt security group rule builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtSecurityGroupRule.Builder {

        private String id;
        private String securityGroupId;
        private String direction;
        private String etherType;
        private Integer portRangeMax;
        private Integer portRangeMin;
        private String protocol;
        private IpPrefix remoteIpPrefix;
        private String remoteGroupId;

        @Override
        public KubevirtSecurityGroupRule build() {
            checkArgument(id != null, NOT_NULL_MSG, "id");
            checkArgument(securityGroupId != null, NOT_NULL_MSG, "securityGroupId");
            checkArgument(direction != null, NOT_NULL_MSG, "direction");

            return new DefaultKubevirtSecurityGroupRule(id, securityGroupId,
                    direction, etherType, portRangeMax, portRangeMin, protocol,
                    remoteIpPrefix, remoteGroupId);
        }

        @Override
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public Builder securityGroupId(String securityGroupId) {
            this.securityGroupId = securityGroupId;
            return this;
        }

        @Override
        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        @Override
        public Builder etherType(String etherType) {
            this.etherType = etherType;
            return this;
        }

        @Override
        public Builder portRangeMax(Integer portRangeMax) {
            this.portRangeMax = portRangeMax;
            return this;
        }

        @Override
        public Builder portRangeMin(Integer portRangeMin) {
            this.portRangeMin = portRangeMin;
            return this;
        }

        @Override
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public Builder remoteIpPrefix(IpPrefix remoteIpPrefix) {
            this.remoteIpPrefix = remoteIpPrefix;
            return this;
        }

        @Override
        public Builder remoteGroupId(String remoteGroupId) {
            this.remoteGroupId = remoteGroupId;
            return this;
        }
    }
}
