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
package org.onosproject.openstacknode.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation class of openstack physical interface.
 */
public class DefaultOpenstackPhyInterface implements OpenstackPhyInterface {

    private final String network;
    private final String intf;

    private static final String NOT_NULL_MSG = "% cannot be null";

    /**
     * A default constructor of Openstack physical interface.
     *
     * @param network   network that this physical interface connects with
     * @param intf      name of physical interface
     */
    protected DefaultOpenstackPhyInterface(String network, String intf) {
        this.network = network;
        this.intf = intf;
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOpenstackPhyInterface) {
            DefaultOpenstackPhyInterface that = (DefaultOpenstackPhyInterface) obj;
            return Objects.equals(network, that.network) &&
                    Objects.equals(intf, that.intf);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(network, intf);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("network", network)
                .add("intf", intf)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return openstack physical interface builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for openstack physical interface.
     */
    public static final class Builder implements OpenstackPhyInterface.Builder {

        private String network;
        private String intf;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public OpenstackPhyInterface build() {
            checkArgument(network != null, NOT_NULL_MSG, "network");
            checkArgument(intf != null, NOT_NULL_MSG, "intf");

            return new DefaultOpenstackPhyInterface(network, intf);
        }

        @Override
        public OpenstackPhyInterface.Builder network(String network) {
            this.network = network;
            return this;
        }

        @Override
        public OpenstackPhyInterface.Builder intf(String intf) {
            this.intf = intf;
            return this;
        }
    }
}
