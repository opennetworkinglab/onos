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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Implementation of port pair group.
 */
public final class DefaultPortPairGroup implements PortPairGroup {

    private final PortPairGroupId portPairGroupId;
    private final TenantId tenantId;
    private final String name;
    private final String description;
    private final List<PortPairId> portPairList;
    private final Map<PortPairId, Integer> portPairLoadMap;

    /**
     * Default constructor to create Port Pair Group.
     *
     * @param portPairGroupId port pair group id
     * @param tenantId tenant id
     * @param name name of port pair group
     * @param description description of port pair group
     * @param portPairList list of port pairs
     */
    private DefaultPortPairGroup(PortPairGroupId portPairGroupId, TenantId tenantId,
                                 String name, String description,
                                 List<PortPairId> portPairList) {

        this.portPairGroupId = portPairGroupId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.portPairList = portPairList;
        portPairLoadMap = new ConcurrentHashMap<>();
        for (PortPairId portPairId : portPairList) {
            portPairLoadMap.put(portPairId, new Integer(0));
        }
    }

    @Override
    public PortPairGroupId portPairGroupId() {
        return portPairGroupId;
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
    public List<PortPairId> portPairs() {
        return ImmutableList.copyOf(portPairList);
    }

    @Override
    public void addLoad(PortPairId portPairId) {
        int load = portPairLoadMap.get(portPairId);
        load = load + 1;
        portPairLoadMap.put(portPairId, new Integer(load));
    }

    @Override
    public void resetLoad() {
        for (PortPairId portPairId : portPairList) {
            portPairLoadMap.put(portPairId, new Integer(0));
        }
    }

    @Override
    public int getLoad(PortPairId portPairId) {
        return portPairLoadMap.get(portPairId);
    }

    @Override
    public Map<PortPairId, Integer> portPairLoadMap() {
        return ImmutableMap.copyOf(portPairLoadMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portPairGroupId, tenantId, name, description,
                            portPairList);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPortPairGroup) {
            DefaultPortPairGroup that = (DefaultPortPairGroup) obj;
            return Objects.equals(portPairGroupId, that.portPairGroupId) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(portPairList, that.portPairList);
        }
        return false;
    }

    @Override
    public boolean exactMatch(PortPairGroup portPairGroup) {
        return this.equals(portPairGroup) &&
                Objects.equals(this.portPairGroupId, portPairGroup.portPairGroupId()) &&
                Objects.equals(this.tenantId, portPairGroup.tenantId());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", portPairGroupId.toString())
                .add("tenantId", tenantId.toString())
                .add("name", name)
                .add("description", description)
                .add("portPairGroupList", portPairList)
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
     * Builder class for Port pair group.
     */
    public static final class Builder implements PortPairGroup.Builder {

        private PortPairGroupId portPairGroupId;
        private TenantId tenantId;
        private String name;
        private String description;
        private List<PortPairId> portPairList;

        @Override
        public Builder setId(PortPairGroupId portPairGroupId) {
            this.portPairGroupId = portPairGroupId;
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
        public Builder setPortPairs(List<PortPairId> portPairs) {
            this.portPairList = portPairs;
            return this;
        }

        @Override
        public PortPairGroup build() {

            checkNotNull(portPairGroupId, "Port pair group id cannot be null");
            checkNotNull(tenantId, "Tenant id cannot be null");
            checkNotNull(portPairList, "Port pairs cannot be null");

            return new DefaultPortPairGroup(portPairGroupId, tenantId, name, description,
                                            portPairList);
        }
    }
}
