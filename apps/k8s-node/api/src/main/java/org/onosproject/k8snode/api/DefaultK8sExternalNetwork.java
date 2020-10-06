/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * Representation of a external network.
 */
public class DefaultK8sExternalNetwork implements K8sExternalNetwork {

    private final IpAddress extBridgeIp;
    private final IpAddress extGatewayIp;
    private final MacAddress extGatewayMac;
    private final String extIntf;

    protected DefaultK8sExternalNetwork(IpAddress extBridgeIp, IpAddress extGatewayIp,
                                        MacAddress extGatewayMac, String extIntf) {
        this.extBridgeIp = extBridgeIp;
        this.extGatewayIp = extGatewayIp;
        this.extGatewayMac = extGatewayMac;
        this.extIntf = extIntf;
    }

    @Override
    public IpAddress extBridgeIp() {
        return extBridgeIp;
    }

    @Override
    public IpAddress extGatewayIp() {
        return extGatewayIp;
    }

    @Override
    public MacAddress extGatewayMac() {
        return extGatewayMac;
    }

    @Override
    public String extIntf() {
        return extIntf;
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes node builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultK8sExternalNetwork that = (DefaultK8sExternalNetwork) o;
        return extBridgeIp.equals(that.extBridgeIp) &&
                extGatewayIp.equals(that.extGatewayIp) &&
                extIntf.equals(that.extIntf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extBridgeIp, extGatewayIp, extGatewayMac, extIntf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("extBridgeIp", extBridgeIp)
                .add("extGatewayIp", extGatewayIp)
                .add("extGatewayMac", extGatewayMac)
                .add("extIntf", extIntf)
                .toString();
    }

    public static final class Builder implements K8sExternalNetwork.Builder {

        private IpAddress extBridgeIp;
        private IpAddress extGatewayIp;
        private MacAddress extGatewayMac;
        private String extIntf;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public K8sExternalNetwork build() {
            return new DefaultK8sExternalNetwork(extBridgeIp, extGatewayIp, extGatewayMac, extIntf);
        }

        @Override
        public Builder extBridgeIp(IpAddress extBridgeIp) {
            this.extBridgeIp = extBridgeIp;
            return this;
        }

        @Override
        public Builder extGatewayIp(IpAddress extGatewayIp) {
            this.extGatewayIp = extGatewayIp;
            return this;
        }

        @Override
        public Builder extGatewayMac(MacAddress extGatewayMac) {
            this.extGatewayMac = extGatewayMac;
            return this;
        }

        @Override
        public Builder extIntf(String extIntf) {
            this.extIntf = extIntf;
            return this;
        }
    }
}
