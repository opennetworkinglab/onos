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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of external peer router.
 */
public final class DefaultExternalPeerRouter implements ExternalPeerRouter {

    private final IpAddress ipAddress;
    private final MacAddress macAddress;
    private final VlanId vlanId;

    private static final String NOT_NULL_MSG = "External Peer Router % cannot be null";

    // private constructor not intended for invoked from external
    private DefaultExternalPeerRouter(IpAddress ipAddress,
                                     MacAddress macAddress,
                                     VlanId vlanId) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.vlanId = vlanId;
    }

    @Override
    public IpAddress ipAddress() {
        return this.ipAddress;
    }

    @Override
    public MacAddress macAddress() {
        return this.macAddress;
    }

    @Override
    public VlanId vlanId() {
        return this.vlanId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultExternalPeerRouter) {
            DefaultExternalPeerRouter that = (DefaultExternalPeerRouter) obj;
            return Objects.equals(ipAddress, that.ipAddress) &&
                    Objects.equals(macAddress, that.macAddress) &&
                    Objects.equals(vlanId, that.vlanId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, macAddress, vlanId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ipAddress", ipAddress)
                .add("macAddress", macAddress)
                .add("vlanId", vlanId)
                .toString();
    }

    /**
     * Obtains an external peer router builder.
     *
     * @return external peer router builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for external peer router.
     */
    public static final class Builder implements ExternalPeerRouter.Builder {

        private IpAddress ipAddress;
        private MacAddress macAddress;
        private VlanId vlanId;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public ExternalPeerRouter build() {

            checkArgument(ipAddress != null, NOT_NULL_MSG, "IP address");
            checkArgument(macAddress != null, NOT_NULL_MSG, "MAC address");
            checkArgument(vlanId != null, NOT_NULL_MSG, "VLAN ID");

            return new DefaultExternalPeerRouter(ipAddress, macAddress, vlanId);
        }

        @Override
        public Builder ipAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        @Override
        public Builder macAddress(MacAddress macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        @Override
        public Builder vlanId(VlanId vlanId) {
            this.vlanId = vlanId;
            return this;
        }
    }
}
