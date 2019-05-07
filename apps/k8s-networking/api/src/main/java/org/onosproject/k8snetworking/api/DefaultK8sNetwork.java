/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.apache.commons.net.util.SubnetUtils;
import org.onlab.packet.IpAddress;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of kubernetes network.
 */
public final class DefaultK8sNetwork implements K8sNetwork {

    private static final int DEFAULT_MTU = 1500;
    private static final Type DEFAULT_TYPE = Type.VXLAN;
    private static final String DEFAULT_SEGMENT_ID = String.valueOf(100);

    private final String networkId;
    private final String name;
    private final Type type;
    private final Integer mtu;
    private final String segmentId;
    private final IpAddress gatewayIp;
    private final String cidr;

    private static final String NOT_NULL_MSG = "Network % cannot be null";

    // private constructor not intended for external invocation
    private DefaultK8sNetwork(String networkId, String name, Type type, Integer mtu,
                              String segmentId, IpAddress gatewayIp, String cidr) {
        this.networkId = networkId;
        this.name = name;
        this.type = type;
        this.mtu = mtu;
        this.segmentId = segmentId;
        this.gatewayIp = gatewayIp;
        this.cidr = cidr;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Integer mtu() {
        return mtu;
    }

    @Override
    public String segmentId() {
        return segmentId;
    }

    @Override
    public IpAddress gatewayIp() {
        return gatewayIp;
    }

    @Override
    public String cidr() {
        return cidr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultK8sNetwork that = (DefaultK8sNetwork) o;
        return Objects.equal(networkId, that.networkId) &&
                Objects.equal(name, that.name) &&
                type == that.type &&
                Objects.equal(mtu, that.mtu) &&
                Objects.equal(segmentId, that.segmentId) &&
                Objects.equal(gatewayIp, that.gatewayIp) &&
                Objects.equal(cidr, that.cidr);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, name, type, mtu, segmentId, gatewayIp, cidr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("name", name)
                .add("type", type)
                .add("mtu", mtu)
                .add("segmentId", segmentId)
                .add("gatewayIp", gatewayIp)
                .add("cidr", cidr)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes network builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default implementation of kubernetes network builder.
     */
    public static final class Builder implements K8sNetwork.Builder {

        private String networkId;
        private String name;
        private Type type;
        private Integer mtu;
        private String segmentId;
        private IpAddress gatewayIp;
        private String cidr;

        @Override
        public K8sNetwork build() {
            checkArgument(networkId != null, NOT_NULL_MSG, "networkId");
            checkArgument(name != null, NOT_NULL_MSG, "name");

            // TODO: CIDR can be retrieve from k8s node info, therefore, such
            // value injection should be purged sooner
            checkArgument(cidr != null, NOT_NULL_MSG, "cidr");

            // gateway IP address is derived from subnet CIDR
            gatewayIp = getGatewayIp(cidr);

            if (segmentId == null) {
                segmentId = DEFAULT_SEGMENT_ID;
            }

            // VXLAN as the default tunneling protocol if not specified
            if (type == null) {
                type = DEFAULT_TYPE;
            }

            if (mtu == null) {
                mtu = DEFAULT_MTU;
            }

            return new DefaultK8sNetwork(networkId, name, type, mtu, segmentId, gatewayIp, cidr);
        }

        @Override
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder mtu(Integer mtu) {
            this.mtu = mtu;
            return this;
        }

        @Override
        public Builder segmentId(String segmentId) {
            this.segmentId = segmentId;
            return this;
        }

        @Override
        public Builder gatewayIp(IpAddress ipAddress) {
            this.gatewayIp = ipAddress;
            return this;
        }

        @Override
        public Builder cidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        private IpAddress getGatewayIp(String cidr) {
            SubnetUtils utils = new SubnetUtils(cidr);
            utils.setInclusiveHostCount(false);
            SubnetUtils.SubnetInfo info = utils.getInfo();

            return IpAddress.valueOf(info.getLowAddress());
        }
    }
}
