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

public final class VoltTenant {

    private final String humanReadableName;
    private final long id;
    private final long providerService;
    private final String serviceSpecificId;
    private final String vlanId;

    private VoltTenant(String humanReadableName, long id, long providerService,
                       String serviceSpecificId, String vlanId) {
        this.humanReadableName = humanReadableName;
        this.id = id;
        this.providerService = providerService;
        this.serviceSpecificId = serviceSpecificId;
        this.vlanId = vlanId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String humanReadableName() {
        return humanReadableName;
    }

    public long id() {
        return id;
    }

    public long providerService() {
        return providerService;
    }

    public String serviceSpecificId() {
        return serviceSpecificId;
    }

    public String vlanId() {
        return vlanId;
    }

    public static final class Builder {
        private String humanReadableName = "unknown";
        private long id = 0;
        private long providerService = 0;
        private String serviceSpecificId = "unknown";
        private String vlanId = "unknown";

        public Builder withHumanReadableName(String humanReadableName) {
            this.humanReadableName = humanReadableName;
            return this;
        }

        public Builder withId(long id) {
            this.id = id;
            return this;
        }

        public Builder withProviderService(long providerService) {
            this.providerService = providerService;
            return this;
        }

        public Builder withServiceSpecificId(String serviceSpecificId) {
            this.serviceSpecificId = serviceSpecificId;
            return this;
        }

        public Builder withVlanId(String vlanId) {
            this.vlanId = vlanId;
            return this;
        }

        public VoltTenant build() {
            return new VoltTenant(humanReadableName, id, providerService,
                    serviceSpecificId, vlanId);
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
                .toString();
    }

}
