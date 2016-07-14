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
 * An Openstack Neutron Router Model.
 */
public final class OpenstackRouter {

    public enum RouterStatus {
        UP,
        DOWN,
        ACTIVE,
    }

    private final String tenantId;
    private final String id;
    private final String name;
    private RouterStatus status;
    private boolean adminStateUp;
    private OpenstackExternalGateway gatewayExternalInfo;

    private OpenstackRouter(String id, String tenantId, String name, RouterStatus status,
                           boolean adminStateUp, OpenstackExternalGateway gatewayExternalInfo) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.adminStateUp = adminStateUp;
        this.gatewayExternalInfo = gatewayExternalInfo;

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
     * Returns router ID.
     *
     * @return router ID
     */
    public String id() {
        return id;
    }

    /**
     * Returns router name.
     *
     * @return router name
     */
    public String name() {
        return name;
    }

    /**
     * Returns router status.
     *
     * @return router stauts
     */
    public RouterStatus status() {
        return status;
    }

    /**
     * Returns whether admin state up or not.
     *
     * @return true if admin state up, false otherwise
     */
    public boolean adminStateUp() {
        return adminStateUp;
    }

    /**
     * Returns external gateway information.
     *
     * @return external gateway information
     */
    public OpenstackExternalGateway gatewayExternalInfo() {
        return gatewayExternalInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof OpenstackRouter) {
            OpenstackRouter that = (OpenstackRouter) o;

            return this.adminStateUp == that.adminStateUp &&
                    this.gatewayExternalInfo.equals(that.gatewayExternalInfo) &&
                    this.id.equals(that.id) &&
                    this.name.equals(that.name) &&
                    this.status.equals(that.status) &&
                    this.tenantId.equals(that.tenantId);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(adminStateUp, gatewayExternalInfo, id, name, status, tenantId);
    }

    /**
     * An Openstack Router Builder class.
     */
    public static final class Builder {

        private String tenantId;
        private String id;
        private String name;
        private RouterStatus status;
        private Boolean adminStateUp;
        private OpenstackExternalGateway gatewayExternalInfo;

        /**
         * Sets router ID.
         *
         * @param id router ID
         * @return Builder object
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets router name.
         *
         * @param name router name
         * @return Builder object
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets router status.
         *
         * @param status router status
         * @return Builder object
         */
        public Builder status(RouterStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets tenant ID.
         *
         * @param tenantId Tenant ID
         * @return Builder object
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * Sets whether admin state up or not.
         *
         * @param adminStateUp true if admin state is up, false otherwise
         * @return Builder object
         */
        public Builder adminStateUp(boolean adminStateUp) {
            this.adminStateUp = adminStateUp;
            return this;
        }

        /**
         * Sets external gateway information.
         *
         * @param gatewayExternalInfo external gateway information
         * @return Builder object
         */
        public Builder gatewayExternalInfo(OpenstackExternalGateway gatewayExternalInfo) {
            this.gatewayExternalInfo = gatewayExternalInfo;
            return this;
        }

        /**
         * Builds an OpenstackRouter object.
         *
         * @return OpenstasckRouter object
         */
        public OpenstackRouter build() {
            return new OpenstackRouter(checkNotNull(id), checkNotNull(tenantId), name, checkNotNull(status),
                    checkNotNull(adminStateUp), gatewayExternalInfo);
        }
    }


}
