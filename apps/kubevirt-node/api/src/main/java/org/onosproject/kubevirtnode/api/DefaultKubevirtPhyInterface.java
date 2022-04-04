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
package org.onosproject.kubevirtnode.api;

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation class of kubevirt physical interface.
 */
public class DefaultKubevirtPhyInterface implements KubevirtPhyInterface {

    private final String network;
    private final String intf;
    private final DeviceId physBridge;

    private static final String NOT_NULL_MSG = "% cannot be null";

    /**
     * A default constructor of kubevirt physical interface.
     *
     * @param network network that this physical interface connects with
     * @param intf    name of physical interface
     * @param physBridge device id of the physical bridge
     */
    protected DefaultKubevirtPhyInterface(String network, String intf, DeviceId physBridge) {
        this.network = network;
        this.intf = intf;
        this.physBridge = physBridge;
    }

    @Override
    public String network() {
        return network;
    }

    @Override
    public String intf() {
        return intf;
    }

    @Override
    public DeviceId physBridge() {
        return physBridge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtPhyInterface that = (DefaultKubevirtPhyInterface) o;
        return network.equals(that.network) &&
                intf.equals(that.intf) &&
                physBridge.equals(that.physBridge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(network, intf, physBridge);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("network", network)
                .add("intf", intf)
                .add("physnetBridge", physBridge)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt physical interface builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtPhyInterface.Builder {

        private String network;
        private String intf;
        private DeviceId physBridge;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public KubevirtPhyInterface build() {
            checkArgument(network != null, NOT_NULL_MSG, "network");
            checkArgument(intf != null, NOT_NULL_MSG, "intf");
            checkArgument(physBridge != null, NOT_NULL_MSG, "physBridge");

            return new DefaultKubevirtPhyInterface(network, intf, physBridge);
        }

        @Override
        public KubevirtPhyInterface.Builder network(String network) {
            this.network = network;
            return this;
        }

        @Override
        public KubevirtPhyInterface.Builder intf(String intf) {
            this.intf = intf;
            return this;
        }

        @Override
        public KubevirtPhyInterface.Builder physBridge(DeviceId physBridge) {
            this.physBridge = physBridge;
            return this;
        }

    }
}
