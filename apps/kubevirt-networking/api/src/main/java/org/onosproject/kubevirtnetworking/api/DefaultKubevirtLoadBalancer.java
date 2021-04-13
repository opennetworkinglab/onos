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
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of kubevirt load balancer.
 */
public final class DefaultKubevirtLoadBalancer implements KubevirtLoadBalancer {

    private static final String NOT_NULL_MSG = "Loadbalancer % cannot be null";

    private final String name;
    private final String description;
    private final String networkId;
    private final IpAddress vip;
    private final Set<IpAddress> members;
    private final Set<KubevirtLoadBalancerRule> rules;

    /**
     * Default constructor.
     *
     * @param name              load balancer name
     * @param description       load balancer description
     * @param networkId         load balancer network identifier
     * @param vip               load balancer virtual IP address
     * @param members           load balancer members
     * @param rules             load balancer rules
     */
    public DefaultKubevirtLoadBalancer(String name, String description, String networkId,
                                       IpAddress vip, Set<IpAddress> members,
                                       Set<KubevirtLoadBalancerRule> rules) {
        this.name = name;
        this.description = description;
        this.networkId = networkId;
        this.vip = vip;
        this.members = members;
        this.rules = rules;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public IpAddress vip() {
        return vip;
    }

    @Override
    public Set<IpAddress> members() {
        if (members == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(members);
        }
    }

    @Override
    public Set<KubevirtLoadBalancerRule> rules() {
        if (rules == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(rules);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtLoadBalancer that = (DefaultKubevirtLoadBalancer) o;
        return name.equals(that.name) && Objects.equals(description, that.description) &&
                networkId.equals(that.networkId) && vip.equals(that.vip) &&
                Objects.equals(members, that.members) && Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, networkId, vip, members, rules);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("networkId", networkId)
                .add("vip", vip)
                .add("members", members)
                .add("rules", rules)
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtLoadBalancer.Builder {

        private String name;
        private String description;
        private String networkId;
        private IpAddress vip;
        private Set<IpAddress> members;
        private Set<KubevirtLoadBalancerRule> rules;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public KubevirtLoadBalancer build() {
            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(name != null, NOT_NULL_MSG, "name");
            checkArgument(vip != null, NOT_NULL_MSG, "vip");

            return new DefaultKubevirtLoadBalancer(name, description, networkId, vip, members, rules);
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public Builder vip(IpAddress vip) {
            this.vip = vip;
            return this;
        }

        @Override
        public Builder members(Set<IpAddress> members) {
            this.members = members;
            return this;
        }

        @Override
        public Builder rules(Set<KubevirtLoadBalancerRule> rules) {
            this.rules = rules;
            return this;
        }
    }
}
