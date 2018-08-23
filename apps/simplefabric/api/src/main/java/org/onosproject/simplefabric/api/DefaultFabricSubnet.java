/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.simplefabric.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.EncapsulationType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Configuration details for an ip subnet entry.
 */
public final class DefaultFabricSubnet implements FabricSubnet {

    private static final String NOT_NULL_MSG = "FabricSubnet % cannot be null";

    private final IpPrefix prefix;
    private final IpAddress gatewayIp;
    private final MacAddress gatewayMac;
    private EncapsulationType encapsulation;
    private final String name;

    /**
     * Creates a new subnet entry.
     *
     * @param prefix  an ip subnet
     * @param gatewayIp IP of the virtual gateway
     * @param gatewayMac MacAddress of the virtual gateway
     * @param encapsulation encapsulation type
     * @param name subnet name
     */
    private DefaultFabricSubnet(IpPrefix prefix, IpAddress gatewayIp,
                                MacAddress gatewayMac, EncapsulationType encapsulation,
                                String name) {
        this.prefix = prefix;
        this.gatewayIp = gatewayIp;
        this.gatewayMac = gatewayMac;
        this.encapsulation = encapsulation;
        this.name = name;
    }

    @Override
    public IpPrefix prefix() {
        return prefix;
    }

    @Override
    public IpAddress gatewayIp() {
        return gatewayIp;
    }

    @Override
    public MacAddress gatewayMac() {
        return gatewayMac;
    }

    @Override
    public EncapsulationType encapsulation() {
        return encapsulation;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isIp4() {
        return prefix.isIp4();
    }

    @Override
    public boolean isIp6() {
        return prefix.isIp6();
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, gatewayIp, gatewayMac, encapsulation, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DefaultFabricSubnet)) {
            return false;
        }
        DefaultFabricSubnet that = (DefaultFabricSubnet) obj;
        return Objects.equals(this.prefix, that.prefix)
               && Objects.equals(this.gatewayIp, that.gatewayIp)
               && Objects.equals(this.gatewayMac, that.gatewayMac)
               && Objects.equals(this.encapsulation, that.encapsulation)
               && Objects.equals(this.name, that.name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("prefix", prefix)
                .add("gatewayIp", gatewayIp)
                .add("gatewayMac", gatewayMac)
                .add("encapsulation", encapsulation)
                .add("name", name)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return fabric IP subnet builder
     */
    public static DefaultSubnetBuilder builder() {
        return new DefaultSubnetBuilder();
    }

    /**
     * A builder class for Ip Subnet.
     */
    public static final class DefaultSubnetBuilder implements Builder {
        private IpPrefix ipPrefix;
        private IpAddress gatewayIp;
        private MacAddress gatewayMac;
        private EncapsulationType encapsulation;
        private String name;

        private DefaultSubnetBuilder() {
        }

        @Override
        public Builder ipPrefix(IpPrefix ipPrefix) {
            this.ipPrefix = ipPrefix;
            return this;
        }

        @Override
        public Builder gatewayIp(IpAddress gatewayIp) {
            this.gatewayIp = gatewayIp;
            return this;
        }

        @Override
        public Builder gatewayMac(MacAddress gatewayMac) {
            this.gatewayMac = gatewayMac;
            return this;
        }

        @Override
        public Builder encapsulation(EncapsulationType encapsulation) {
            this.encapsulation = encapsulation;
            return this;
        }

        @Override
        public Builder name(String networkName) {
            this.name = networkName;
            return this;
        }

        @Override
        public FabricSubnet build() {
            checkArgument(ipPrefix != null, NOT_NULL_MSG, "prefix");
            checkArgument(gatewayIp != null, NOT_NULL_MSG, "gatewayIp");
            checkArgument(gatewayMac != null, NOT_NULL_MSG, "gatewayMac");
            checkArgument(name != null, NOT_NULL_MSG, "name");

            if (this.encapsulation == null) {
                encapsulation = EncapsulationType.NONE;
            }

            return new DefaultFabricSubnet(ipPrefix, gatewayIp, gatewayMac,
                                                            encapsulation, name);
        }
    }
}
