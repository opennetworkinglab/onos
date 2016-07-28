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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An OpenStack Neutron router interface model.
 */
public final class OpenstackRouterInterface {
    private final String id;
    private final String tenantId;
    private final String subnetId;
    private final String portId;

    private OpenstackRouterInterface(String id, String tenantId,
                                     String subnetId, String portId) {
        this.id = checkNotNull(id);
        this.tenantId = checkNotNull(tenantId);
        this.subnetId = checkNotNull(subnetId);
        this.portId = checkNotNull(portId);
    }

    /**
     * Returns router interface ID.
     *
     * @return router interface id
     */
    public String id() {
        return id;
    }

    /**
     * Returns tenant ID.
     *
     * @return tenant id
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * Returns subnet ID.
     *
     * @return subnet id
     */
    public String subnetId() {
        return subnetId;
    }

    /**
     * Returns port ID.
     *
     * @return port id
     */
    public String portId() {
        return portId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof OpenstackRouterInterface) {
            OpenstackRouterInterface that = (OpenstackRouterInterface)  o;

            return this.id.equals(that.id) &&
                    this.portId.equals(that.portId) &&
                    this.subnetId.equals(that.subnetId) &&
                    this.tenantId.equals(that.tenantId);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, portId, subnetId, tenantId);
    }

    /**
     * Returns OpenStack router interface builder.
     *
     * @return openstack router interface builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * An OpenStack Router interface builder class.
     */
    public static final class Builder {
        private String id;
        private String tenantId;
        private String subnetId;
        private String portId;

        /**
         * Sets router interface ID.
         *
         * @param id router interface id
         * @return builder object
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets tenant ID.
         *
         * @param tenantId tenant ID
         * @return builder object
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Sets subnet ID.
         *
         * @param subnetId subnet ID
         * @return builder object
         */
        public Builder subnetId(String subnetId) {
            this.subnetId = subnetId;
            return this;
        }

        /**
         * Sets port ID.
         *
         * @param portId port ID
         * @return builder object
         */
        public Builder portId(String portId) {
            this.portId = portId;
            return this;
        }

        /**
         * Builds an OpenStack router interface object.
         *
         * @return openstack router interface object
         */
        public OpenstackRouterInterface build() {
            return new OpenstackRouterInterface(checkNotNull(id), checkNotNull(tenantId),
                    checkNotNull(subnetId), checkNotNull(portId));
        }
    }
}
