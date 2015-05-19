/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.xosintegration;

import com.google.common.base.MoreObjects;
import org.onosproject.net.ConnectPoint;

public final class VoltTenant {

    private final String humanReadableName;
    private final long id;
    private final long providerService;
    private final String serviceSpecificId;
    private final String vlanId;
    private final ConnectPoint port;

    /**
     * Constructs a vOLT tenant object.
     *
     * @param humanReadableName name string
     * @param id identifier for the tenant
     * @param providerService provider service ID
     * @param serviceSpecificId id for the user
     * @param vlanId vlan id for the user
     */
    private VoltTenant(String humanReadableName, long id, long providerService,
                       String serviceSpecificId, String vlanId, ConnectPoint port) {
        this.humanReadableName = humanReadableName;
        this.id = id;
        this.providerService = providerService;
        this.serviceSpecificId = serviceSpecificId;
        this.vlanId = vlanId;
        this.port = port;
    }

    /**
     * Fetches a builder to make a tenant.
     *
     * @return tenant builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fetches the name of the tenant.
     *
     * @return human readable name
     */
    public String humanReadableName() {
        return humanReadableName;
    }

    /**
     * Fetches the ID of the tenant object.
     *
     * @return ID of tenant object.
     */
    public long id() {
        return id;
    }

    /**
     * Fetches the identifier for the provider service.
     *
     * @return provider service ID
     */
    public long providerService() {
        return providerService;
    }

    /**
     * Fetches the server specific ID (user id).
     *
     * @return server specific ID
     */
    public String serviceSpecificId() {
        return serviceSpecificId;
    }

    /**
     * Fetches the vlan id for this tenant.
     *
     * @return VLAN ID
     */
    public String vlanId() {
        return vlanId;
    }

    public ConnectPoint port() {
        return port;
    }

    /**
     * Builder class to allow callers to assemble tenants.
     */

    public static final class Builder {
        private String humanReadableName = "unknown";
        private long id = 0;
        private long providerService = -1;
        private String serviceSpecificId = "unknown";
        private String vlanId = "unknown";
        private ConnectPoint port;

        /**
         * Sets the name string for the tenant.
         *
         * @param humanReadableName name
         * @return self
         */
        public Builder withHumanReadableName(String humanReadableName) {
            this.humanReadableName = humanReadableName;
            return this;
        }

        /**
         * Sets the identifier for the tenant.
         *
         * @param id identifier for the tenant
         * @return self
         */
        public Builder withId(long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the server specific id (user id) for the tenant.
         *
         * @param serviceSpecificId server specific (user) id
         * @return self
         */
        public Builder withServiceSpecificId(String serviceSpecificId) {
            this.serviceSpecificId = serviceSpecificId;
            return this;
        }

        /**
         * Sets the VLAN ID for the tenant.
         *
         * @param vlanId VLAN ID
         * @return self
         */
        public Builder withVlanId(String vlanId) {
            this.vlanId = vlanId;
            return this;
        }

        /**
         * Sets the provider service ID.
         *
         * @param providerService provider service ID
         * @return self
         */
        public Builder withProviderService(long providerService) {
            this.providerService = providerService;
            return this;
        }

        public Builder withPort(ConnectPoint port) {
            this.port = port;
            return this;
        }

        /**
         * Constructs a VoltTenant from the assembled data.
         *
         * @return constructed tenant object
         */
        public VoltTenant build() {
            return new VoltTenant(humanReadableName, id, providerService,
                    serviceSpecificId, vlanId, port);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("humanReadableName", humanReadableName())
                .add("id", id())
                .add("providerService", providerService())
                .add("serviceSpecificId", serviceSpecificId())
                .add("vlanId", vlanId())
                .add("port", port())
                .toString();
    }

}
