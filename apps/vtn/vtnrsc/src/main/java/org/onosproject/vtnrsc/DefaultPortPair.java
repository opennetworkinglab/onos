/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Implementation of port pair.
 */
public final class DefaultPortPair implements PortPair {

    private final PortPairId portPairId;
    private final TenantId tenantId;
    private final String name;
    private final String description;
    private final String ingress;
    private final String egress;

    /**
     * Default constructor to create Port Pair.
     *
     * @param portPairId port pair id
     * @param tenantId tenant id
     * @param name name of port pair
     * @param description description of port pair
     * @param ingress ingress port
     * @param egress egress port
     */
    private DefaultPortPair(PortPairId portPairId, TenantId tenantId,
                            String name, String description,
                            String ingress, String egress) {

        this.portPairId = portPairId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.ingress = ingress;
        this.egress = egress;
    }

    @Override
    public PortPairId portPairId() {
        return portPairId;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String ingress() {
        return ingress;
    }

    @Override
    public String egress() {
        return egress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(portPairId, tenantId, name, description,
                            ingress, egress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPortPair) {
            DefaultPortPair that = (DefaultPortPair) obj;
            return Objects.equals(portPairId, that.portPairId) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(ingress, that.ingress) &&
                    Objects.equals(egress, that.egress);
        }
        return false;
    }

    @Override
    public boolean exactMatch(PortPair portPair) {
        return this.equals(portPair) &&
                Objects.equals(this.portPairId, portPair.portPairId()) &&
                Objects.equals(this.tenantId, portPair.tenantId());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", portPairId.toString())
                .add("tenantId", tenantId.tenantId())
                .add("name", name)
                .add("description", description)
                .add("ingress", ingress)
                .add("egress", egress)
                .toString();
    }

    /**
     * To create an instance of the builder.
     *
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for Port pair.
     */
    public static final class Builder implements PortPair.Builder {

        private PortPairId portPairId;
        private TenantId tenantId;
        private String name;
        private String description;
        private String ingress;
        private String egress;

        @Override
        public Builder setId(PortPairId portPairId) {
            this.portPairId = portPairId;
            return this;
        }

        @Override
        public Builder setTenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Override
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Builder setIngress(String ingress) {
            this.ingress = ingress;
            return this;
        }

        @Override
        public Builder setEgress(String egress) {
            this.egress = egress;
            return this;
        }

        @Override
        public PortPair build() {

            checkNotNull(portPairId, "Port pair id cannot be null");
            checkNotNull(tenantId, "Tenant id cannot be null");
            checkNotNull(ingress, "Ingress of a port pair cannot be null");
            checkNotNull(egress, "Egress of a port pair cannot be null");

            return new DefaultPortPair(portPairId, tenantId, name, description,
                                       ingress, egress);
        }
    }
}
