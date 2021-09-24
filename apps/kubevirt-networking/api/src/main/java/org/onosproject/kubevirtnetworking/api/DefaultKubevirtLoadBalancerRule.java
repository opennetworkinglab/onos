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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation class of kubevirt load balancer rule.
 */
public final class DefaultKubevirtLoadBalancerRule implements KubevirtLoadBalancerRule {
    private static final String NOT_NULL_MSG = "Load Balancer Rule % cannot be null";
    private static final String TCP = "TCP";
    private static final String UDP = "UDP";

    private final String protocol;
    private final Integer portRangeMax;
    private final Integer portRangeMin;

    /**
     * A default constructor.
     *
     * @param protocol          protocol
     * @param portRangeMax      port range max
     * @param portRangeMin      port range min
     */
    public DefaultKubevirtLoadBalancerRule(String protocol,
                                           Integer portRangeMax,
                                           Integer portRangeMin) {
        this.protocol = protocol;
        this.portRangeMax = portRangeMax;
        this.portRangeMin = portRangeMin;
    }

    @Override
    public String protocol() {
        return protocol;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtLoadBalancerRule that = (DefaultKubevirtLoadBalancerRule) o;

        return protocol.equals(that.protocol) &&
                Objects.equals(portRangeMax, that.portRangeMax) &&
                Objects.equals(portRangeMin, that.portRangeMin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, portRangeMax, portRangeMin);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("protocol", protocol)
                .add("portRangeMax", portRangeMax)
                .add("portRangeMin", portRangeMin)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt load balancer rule builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtLoadBalancerRule.Builder {

        private String protocol;
        private Integer portRangeMax;
        private Integer portRangeMin;

        @Override
        public KubevirtLoadBalancerRule build() {
            checkArgument(protocol != null, NOT_NULL_MSG, "protocol");

            if (protocol.equalsIgnoreCase(TCP) || protocol.equalsIgnoreCase(UDP)) {
                checkArgument(portRangeMax != null, NOT_NULL_MSG, "portRangeMax");
                checkArgument(portRangeMin != null, NOT_NULL_MSG, "portRangeMin");
            }
            return new DefaultKubevirtLoadBalancerRule(protocol, portRangeMax, portRangeMin);
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
    }
}
