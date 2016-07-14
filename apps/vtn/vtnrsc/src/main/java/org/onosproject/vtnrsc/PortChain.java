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

import java.util.List;
import java.util.Set;

import org.onosproject.net.DeviceId;

/**
 * Abstraction of an entity providing Port Chain information.
 * A Port Chain (Service Function Path) consists of
 * a set of Neutron ports, to define the sequence of service functions
 * a set of flow classifiers, to specify the classified traffic flows to enter the chain
 */
public interface PortChain {

    /**
     * Returns the ID of this port chain.
     *
     * @return the port chain id
     */
    PortChainId portChainId();

    /**
     * Returns the tenant id of this port chain.
     *
     * @return the tenant id
     */
    TenantId tenantId();

    /**
     * Returns the name of this port chain.
     *
     * @return name of port chain
     */
    String name();

    /**
     * Returns the description of this port chain.
     *
     * @return description of port chain
     */
    String description();

    /**
     * Returns the list of port pair groups associated with
     * this port chain.
     *
     * @return list of port pair groups
     */
    List<PortPairGroupId> portPairGroups();

    /**
     * Returns the list of flow classifiers associated with
     * this port chain.
     *
     * @return list of flow classifiers
     */
    List<FlowClassifierId> flowClassifiers();

    /**
     * Returns the old port chain.
     *
     * @return old port chain
     */
    PortChain oldPortChain();

    /**
     * Adds a new load balanced path.
     *
     * @param fiveTuple five tuple from the packet
     * @param id load balance path identifier
     * @param path load balanced path of list of port pairs
     */
    void addLoadBalancePath(FiveTuple fiveTuple, LoadBalanceId id,
                            List<PortPairId> path);

    /**
     * Adds sfc classifiers to the given load balance id for a port chain.
     *
     * @param id load balance path identifier
     * @param classifierList list of classifier devices
     */
    void addSfcClassifiers(LoadBalanceId id, List<DeviceId> classifierList);

    /**
     * Adds sfc forwarders to the given load balance id for a port chain.
     *
     * @param id load balance path identifier
     * @param forwarderList list of forwarder devices
     */
    void addSfcForwarders(LoadBalanceId id, List<DeviceId> forwarderList);

    /**
     * Removes sfc classifiers to the given load balance id for a port chain.
     *
     * @param id load balance path identifier
     * @param classifierList list of classifier devices
     */
    void removeSfcClassifiers(LoadBalanceId id, List<DeviceId> classifierList);

    /**
     * Removes sfc forwarders to the given load balance id for a port chain.
     *
     * @param id load balance path identifier
     * @param forwarderList list of forwarder devices
     */
    void removeSfcForwarders(LoadBalanceId id, List<DeviceId> forwarderList);

    /**
     * Returns sfc classifiers to the given load balance id for a port chain.
     *
     * @param id load balance path identifier
     * @return list of classifier devices
     */
    List<DeviceId> getSfcClassifiers(LoadBalanceId id);

    /**
     * Returns sfc forwarders to the given load balance id for a port chain.
     *
     * @param id load balance path identifier
     * @return list of forwarder devices
     */
    List<DeviceId> getSfcForwarders(LoadBalanceId id);

    /**
     * Returns the load balance id from five tuple.
     *
     * @param fiveTuple five tuple from the packet
     * @return load balance identifier for the given packet
     */
    LoadBalanceId getLoadBalanceId(FiveTuple fiveTuple);

    /**
     * Returns the keys set from load balance id map.
     *
     * @return set of five tuple info
     */
    Set<FiveTuple> getLoadBalanceIdMapKeys();

    /**
     * Returns the keys set from load balance path map.
     *
     * @return set of load balance id's
     */
    Set<LoadBalanceId> getLoadBalancePathMapKeys();

    /**
     * Returns the load balanced path from load balance Id.
     *
     * @param id load balance id.
     * @return path containing list of port pairs
     */
    List<PortPairId> getLoadBalancePath(LoadBalanceId id);

    /**
     * Returns the load balanced path from five tuple.
     *
     * @param fiveTuple five tuple from the packet
     * @return path containing list of port pairs
     */
    List<PortPairId> getLoadBalancePath(FiveTuple fiveTuple);

    /**
     * Returns the no of load balance paths created.
     *
     * @return size of load balanced paths
     */
    int getLoadBalancePathSize();

    /**
     * Match the given path with existing load balanced paths.
     *
     * @param path load balanced path
     * @return load balance id if the path matches, null otherwise.
     */
    LoadBalanceId matchPath(List<PortPairId> path);

    /**
     * Returns whether this port chain is an exact match to the port chain given
     * in the argument.
     * <p>
     * Exact match means the port pair groups and flow classifiers match
     * with the given port chain. It does not consider the port chain id, name
     * and description.
     * </p>
     *
     * @param portChain other port chain to match against
     * @return true if the port chains are an exact match, otherwise false
     */
    boolean exactMatch(PortChain portChain);

    /**
     * A port chain builder..
     */
    interface Builder {

        /**
         * Assigns the port chain id to this object.
         *
         * @param portChainId the port chain id
         * @return this the builder object
         */
        Builder setId(PortChainId portChainId);

        /**
         * Assigns tenant id to this object.
         *
         * @param tenantId tenant id of the port chain
         * @return this the builder object
         */
        Builder setTenantId(TenantId tenantId);

        /**
         * Assigns the name to this object.
         *
         * @param name name of the port chain
         * @return this the builder object
         */
        Builder setName(String name);

        /**
         * Assigns the description to this object.
         *
         * @param description description of the port chain
         * @return this the builder object
         */
        Builder setDescription(String description);

        /**
         * Assigns the port pair groups associated with the port chain
         * to this object.
         *
         * @param portPairGroups list of port pair groups
         * @return this the builder object
         */
        Builder setPortPairGroups(List<PortPairGroupId> portPairGroups);

        /**
         * Assigns the flow classifiers associated with the port chain
         * to this object.
         *
         * @param flowClassifiers list of flow classifiers
         * @return this the builder object
         */
        Builder setFlowClassifiers(List<FlowClassifierId> flowClassifiers);

        /**
         * Builds a port chain object.
         *
         * @return a port chain.
         */
        PortChain build();
    }
}
