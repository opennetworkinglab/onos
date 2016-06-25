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

import org.onlab.packet.Ip4Address;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents the subnet information given by Neutron.
 *
 */
public final class OpenstackSubnet {
    private String name;
    private boolean enableHhcp;
    private String networkId;
    private String tenantId;
    private List<Ip4Address> dnsNameservers;
    private String gatewayIp;
    private String cidr;
    private String id;
    private Collection<String> securityGroups;

    private OpenstackSubnet(String name, boolean enableHhcp, String networkId,
                            String tenantId, List<Ip4Address> dnsNameservers, String gatewayIp,
                            String cidr, String id, Collection<String> securityGroups) {
        this.name = name;
        this.enableHhcp = enableHhcp;
        this.networkId = checkNotNull(networkId);
        this.tenantId = checkNotNull(tenantId);
        this.dnsNameservers = dnsNameservers;
        this.gatewayIp = gatewayIp;
        this.cidr = checkNotNull(cidr);
        this.id = checkNotNull(id);
        this.securityGroups = securityGroups;
    }

    /**
     * Returns OpenstackSubnet builder object.
     *
     * @return OpenstackSubnet builder
     */
    public static OpenstackSubnet.Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public boolean enableHhcp() {
        return enableHhcp;
    }

    public String networkId() {
        return networkId;
    }

    public String tenantId() {
        return tenantId;
    }

    public List<Ip4Address> dnsNameservers() {
        return dnsNameservers;
    }

    public String gatewayIp() {
        return gatewayIp;
    }

    public String cidr() {
        return cidr;
    }

    public String id() {
        return id;
    }

    public Collection<String> securityGroups() {
        return Collections.unmodifiableCollection(this.securityGroups);
    }

    /**
     * OpenstackSubnet Builder class.
     *
     */
    public static final class Builder {
        private String name;
        private boolean enableDhcp;
        private String networkId;
        private String tenantId;
        private List<Ip4Address> dnsNameservers;
        private String gatewayIp;
        private String cidr;
        private String id;
        private Collection<String> securityGroups;

        Builder() {}

        public Builder setName(String name) {
            this.name = name;

            return this;
        }

        public Builder setEnableDhcp(boolean enableDhcp) {
            this.enableDhcp = enableDhcp;

            return this;
        }

        public Builder setNetworkId(String networkId) {
            this.networkId = networkId;

            return this;
        }

        public Builder setTenantId(String tenantId) {
            this.tenantId = tenantId;

            return this;
        }

        public Builder setDnsNameservers(List<Ip4Address> dnsNameservers) {
            this.dnsNameservers = dnsNameservers;

            return this;
        }

        public Builder setGatewayIp(String gatewayIp) {
            this.gatewayIp = gatewayIp;

            return this;
        }

        public Builder setCidr(String cidr) {
            this.cidr = cidr;

            return this;
        }

        public Builder setId(String id) {
            this.id = id;

            return this;
        }

        public Builder securityGroups(Collection<String> securityGroups) {
            this.securityGroups = securityGroups;

            return this;
        }

        public OpenstackSubnet build() {
            return new OpenstackSubnet(name, enableDhcp, networkId, tenantId,
                    dnsNameservers, gatewayIp, cidr, id, securityGroups);
        }
    }
}
