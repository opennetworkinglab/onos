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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.net.DeviceId;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Implementation of port chain.
 */
public final class DefaultPortChain implements PortChain {

    private final PortChainId portChainId;
    private final TenantId tenantId;
    private final String name;
    private final String description;
    private final List<PortPairGroupId> portPairGroupList;
    private final List<FlowClassifierId> flowClassifierList;
    private final PortChain oldPortChain;

    private final Map<FiveTuple, LoadBalanceId> sfcLoadBalanceIdMap = new ConcurrentHashMap<>();
    private final Map<LoadBalanceId, List<PortPairId>> sfcLoadBalancePathMap = new ConcurrentHashMap<>();
    private final Map<LoadBalanceId, List<DeviceId>> sfcClassifiersMap = new ConcurrentHashMap<>();
    private final Map<LoadBalanceId, List<DeviceId>> sfcForwardersMap = new ConcurrentHashMap<>();

    /**
     * Default constructor to create port chain.
     *
     * @param portChainId port chain id
     * @param tenantId tenant id
     * @param name name of port chain
     * @param description description of port chain
     * @param portPairGroupList port pair group list
     * @param flowClassifierList flow classifier list
     */
    private DefaultPortChain(PortChainId portChainId, TenantId tenantId,
            String name, String description,
            List<PortPairGroupId> portPairGroupList,
            List<FlowClassifierId> flowClassifierList,
            PortChain portChain) {

        this.portChainId = portChainId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.portPairGroupList = portPairGroupList;
        this.flowClassifierList = flowClassifierList;
        this.oldPortChain = portChain;
    }

    /**
     * To create port chain for update with old port chain.
     *
     * @param newPortChain updated port chain
     * @param oldPortChain old port chain
     * @return port chain
     */
    public static PortChain create(PortChain newPortChain, PortChain oldPortChain) {
        return new DefaultPortChain(newPortChain.portChainId(), newPortChain.tenantId(),
                                    newPortChain.name(), newPortChain.description(),
                                    newPortChain.portPairGroups(), newPortChain.flowClassifiers(), oldPortChain);
    }

    /**
     * Match for two given paths.
     *
     * @param path1 path of sfc
     * @param path2 path of sfc
     * @return true if the given path are same false otherwise
     */
    private boolean comparePath(List<PortPairId> path1, List<PortPairId> path2) {
        Iterator it = path1.iterator();
        for (PortPairId portPairId: path2) {
            if (!portPairId.equals(it.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public PortChainId portChainId() {
        return portChainId;
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
    public List<PortPairGroupId> portPairGroups() {
        return ImmutableList.copyOf(portPairGroupList);
    }

    @Override
    public List<FlowClassifierId> flowClassifiers() {
        return ImmutableList.copyOf(flowClassifierList);
    }

    @Override
    public PortChain oldPortChain() {
        return oldPortChain;
    }

    @Override
    public void addLoadBalancePath(FiveTuple fiveTuple, LoadBalanceId id,
                                   List<PortPairId> path) {
        this.sfcLoadBalanceIdMap.put(fiveTuple, id);
        this.sfcLoadBalancePathMap.put(id, path);
    }

    @Override
    public void addSfcClassifiers(LoadBalanceId id, List<DeviceId> classifierList) {
        this.sfcClassifiersMap.put(id, classifierList);
    }

    @Override
    public void addSfcForwarders(LoadBalanceId id, List<DeviceId> forwarderList) {
        this.sfcForwardersMap.put(id, forwarderList);
    }

    @Override
    public void removeSfcClassifiers(LoadBalanceId id, List<DeviceId> classifierList) {
        List<DeviceId> list = sfcClassifiersMap.get(id);
        list.removeAll(classifierList);
        this.sfcForwardersMap.put(id, list);
    }

    @Override
    public void removeSfcForwarders(LoadBalanceId id, List<DeviceId> forwarderList) {
        List<DeviceId> list = sfcForwardersMap.get(id);
        list.removeAll(forwarderList);
        this.sfcForwardersMap.put(id, list);
    }

    @Override
    public List<DeviceId> getSfcClassifiers(LoadBalanceId id) {
        return ImmutableList.copyOf(this.sfcClassifiersMap.get(id));
    }

    @Override
    public List<DeviceId> getSfcForwarders(LoadBalanceId id) {
        return ImmutableList.copyOf(this.sfcForwardersMap.get(id));
    }

    @Override
    public LoadBalanceId getLoadBalanceId(FiveTuple fiveTuple) {
        return this.sfcLoadBalanceIdMap.get(fiveTuple);
    }

    @Override
    public Set<FiveTuple> getLoadBalanceIdMapKeys() {
        return ImmutableSet.copyOf(sfcLoadBalanceIdMap.keySet());
    }

    @Override
    public Set<LoadBalanceId> getLoadBalancePathMapKeys() {
        return ImmutableSet.copyOf(sfcLoadBalancePathMap.keySet());
    }

    @Override
    public List<PortPairId> getLoadBalancePath(LoadBalanceId id) {
        return ImmutableList.copyOf(this.sfcLoadBalancePathMap.get(id));
    }

    @Override
    public List<PortPairId> getLoadBalancePath(FiveTuple fiveTuple) {
        return ImmutableList.copyOf(this.sfcLoadBalancePathMap.get(this.sfcLoadBalanceIdMap.get(fiveTuple)));
    }

    @Override
    public int getLoadBalancePathSize() {
        if (sfcLoadBalanceIdMap.isEmpty()) {
            return 0;
        }
        return sfcLoadBalanceIdMap.size();
    }

    @Override
    public LoadBalanceId matchPath(List<PortPairId> path) {

        LoadBalanceId id = null;
        for (Map.Entry<LoadBalanceId, List<PortPairId>> entry : sfcLoadBalancePathMap.entrySet()) {
            List<PortPairId> tempPath = entry.getValue();
            if (comparePath(path, tempPath)) {
                id = entry.getKey();
                break;
            }
        }
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(portChainId, tenantId, name, description,
                            portPairGroupList, flowClassifierList);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPortChain) {
            DefaultPortChain that = (DefaultPortChain) obj;
            return Objects.equals(portChainId, that.portChainId) &&
                    Objects.equals(tenantId, that.tenantId) &&
                    Objects.equals(name, that.name) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(portPairGroupList, that.portPairGroupList) &&
                    Objects.equals(flowClassifierList, that.flowClassifierList);
        }
        return false;
    }

    @Override
    public boolean exactMatch(PortChain portChain) {
        return this.equals(portChain) &&
                Objects.equals(this.portChainId, portChain.portChainId()) &&
                Objects.equals(this.tenantId, portChain.tenantId());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", portChainId.toString())
                .add("tenantId", tenantId.toString())
                .add("name", name)
                .add("description", description)
                .add("portPairGroupList", portPairGroupList)
                .add("flowClassifier", flowClassifierList)
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
     * Builder class for Port chain.
     */
    public static final class Builder implements PortChain.Builder {

        private PortChainId portChainId;
        private TenantId tenantId;
        private String name;
        private String description;
        private List<PortPairGroupId> portPairGroupList;
        private List<FlowClassifierId> flowClassifierList;
        private PortChain portChain;

        @Override
        public Builder setId(PortChainId portChainId) {
            this.portChainId = portChainId;
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
        public Builder setPortPairGroups(List<PortPairGroupId> portPairGroups) {
            this.portPairGroupList = portPairGroups;
            return this;
        }

        @Override
        public Builder setFlowClassifiers(List<FlowClassifierId> flowClassifiers) {
            this.flowClassifierList = flowClassifiers;
            return this;
        }

        @Override
        public PortChain build() {

            checkNotNull(portChainId, "Port chain id cannot be null");
            checkNotNull(tenantId, "Tenant id cannot be null");
            checkNotNull(portPairGroupList, "Port pair groups cannot be null");

            return new DefaultPortChain(portChainId, tenantId, name, description,
                                        portPairGroupList, flowClassifierList, portChain);
        }
    }
}
