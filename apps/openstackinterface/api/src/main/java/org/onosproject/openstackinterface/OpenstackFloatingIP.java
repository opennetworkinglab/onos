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

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 *  An Openstack Neutron Floating IP Model.
 */
public final class OpenstackFloatingIP {

    public enum FloatingIpStatus {
        UP,
        DOWN,
        ACTIVE,
    }

    private final String tenantId;
    private final String networkId;
    private final Ip4Address fixedIpAddress;
    private String portId;
    private String routerId;
    private final String id;
    private final Ip4Address floatingIpAddress;
    private final FloatingIpStatus status;

    private OpenstackFloatingIP(FloatingIpStatus status, String id, String tenantId,
                                String networkId, Ip4Address fixedIpAddress, String portId,
                                String routerId, Ip4Address floatingIpAddress) {
        this.status = status;
        this.id = id;
        this.tenantId = tenantId;
        this.networkId = networkId;
        this.fixedIpAddress = fixedIpAddress;
        this.portId = portId;
        this.routerId = routerId;
        this.floatingIpAddress = floatingIpAddress;
    }

    /**
     * Returns floating IP status.
     *
     * @return floating IP status
     */
    public FloatingIpStatus status() {
        return status;
    }

    /**
     * Returns floating IP`s ID.
     *
     * @return floating IP`s ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns tenant ID.
     *
     * @return tenant ID
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * Returns network ID.
     *
     * @return network ID
     */
    public String networkId() {
        return networkId;
    }

    /**
     * Returns fixed IP Address.
     *
     * @return fixed IP Address
     */
    public Ip4Address fixedIpAddress() {
        return fixedIpAddress;
    }

    /**
     * Returns port ID.
     *
     * @return port ID
     */
    public String portId() {
        return portId;
    }

    /**
     * Updates port ID.
     *
     * @param portId Updated port ID
     */
    public void updatePortId(String portId) {
        this.portId = portId;
    }

    /**
     * Returns router ID.
     *
     * @return router ID
     */
    public String routerId() {
        return routerId;
    }

    /**
     * Updates router ID.
     *
     * @param routerId Updated router ID
     */
    public void updateRouterId(String routerId) {
        this.routerId = routerId;
    }

    /**
     * Returns floating IP address.
     *
     * @return Floating IP address
     */
    public Ip4Address floatingIpAddress() {
        return floatingIpAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof  OpenstackFloatingIP) {
            OpenstackFloatingIP that = (OpenstackFloatingIP) o;

            return this.status.equals(that.status) &&
                    this.id.equals(that.id) &&
                    this.tenantId.equals(that.tenantId) &&
                    this.networkId.equals(that.networkId) &&
                    this.fixedIpAddress.equals(that.fixedIpAddress) &&
                    this.floatingIpAddress.equals(that.floatingIpAddress) &&
                    this.portId.equals(that.portId) &&
                    this.routerId.equals(that.routerId);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, id, tenantId, networkId, floatingIpAddress, fixedIpAddress, portId, routerId);
    }

    /**
     * An Openstack Floating IP Builder class.
     */
    public static final class Builder {
        private String tenantId;
        private String networkId;
        private Ip4Address fixedIpAddress;
        private String portId;
        private String routerId;
        private String id;
        private Ip4Address floatingIpAddress;
        private FloatingIpStatus status;

        /**
         * Sets tenant ID.
         *
         * @param tenantId tenant ID
         * @return Builder object
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Sets floating IP status.
         *
         * @param status Floating IP status
         * @return Builder object
         */
        public Builder status(FloatingIpStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets Floating IP`s ID.
         *
         * @param id Floating IP`s ID
         * @return Builder object
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets network ID.
         *
         * @param networkId Network ID
         * @return Builder object
         */
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        /**
         * Sets fixed IP address.
         *
         * @param fixedIpAddress Fixed IP address
         * @return Builder object
         */
        public Builder fixedIpAddress(Ip4Address fixedIpAddress) {
            this.fixedIpAddress = fixedIpAddress;
            return this;
        }

        /**
         * Sets port ID.
         *
         * @param portId port ID
         * @return Builder object
         */
        public Builder portId(String portId) {
            this.portId = portId;
            return this;
        }

        /**
         * Sets router ID.
         *
         * @param routerId router ID
         * @return Builder object
         */
        public Builder routerId(String routerId) {
            this.routerId = routerId;
            return this;
        }

        /**
         * Sets floating IP address.
         *
         * @param floatingIpAddress Floating IP address
         * @return Builder object
         */
        public Builder floatingIpAddress(Ip4Address floatingIpAddress) {
            this.floatingIpAddress = floatingIpAddress;
            return this;
        }

        /**
         * Builds an OpenstackFloatingIP object.
         *
         * @return OpenstackFloatingIP object
         */
        public OpenstackFloatingIP build() {
            return new OpenstackFloatingIP(checkNotNull(status), checkNotNull(id), checkNotNull(tenantId),
                    checkNotNull(networkId), fixedIpAddress, portId,
                    routerId, checkNotNull(floatingIpAddress));

        }
    }
}
