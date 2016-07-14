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
import java.util.Map;

/**
 * Abstraction of an entity providing Port Pair Group information.
 * A port pair group consists of one or more port pairs.
 */
public interface PortPairGroup {

    /**
     * Returns the ID of this port pair group.
     *
     * @return the port pair group id
     */
    PortPairGroupId portPairGroupId();

    /**
     * Returns the tenant id of this port pair group.
     *
     * @return the tenant id
     */
    TenantId tenantId();

    /**
     * Returns the name of this port pair group.
     *
     * @return name of port pair group
     */
    String name();

    /**
     * Returns the description of this port pair group.
     *
     * @return description of port pair group
     */
    String description();

    /**
     * Returns the list of port pairs associated with this port pair group.
     *
     * @return list of port pairs
     */
    List<PortPairId> portPairs();

    /**
     * Adds the load on the given port pair id.
     *
     * @param portPairId port pair id.
     */
    void addLoad(PortPairId portPairId);

    /**
     * Reset the load for all the port pairs in the group.
     */
    void resetLoad();

    /**
     * Get the load on the given port pair id.
     *
     * @param portPairId port pair id
     * @return load on the given port pair id.
     */
    int getLoad(PortPairId portPairId);

    /**
     * Get the map of port pair id and its load.
     *
     * @return port pair and load map
     */
    Map<PortPairId, Integer> portPairLoadMap();

    /**
     * Returns whether this port pair group is an exact match to the
     * port pair group given in the argument.
     * <p>
     * Exact match means the Port pairs match with the given port pair group.
     * It does not consider the port pair group id, name and description.
     * </p>
     * @param portPairGroup other port pair group to match against
     * @return true if the port pairs are an exact match, otherwise false
     */
    boolean exactMatch(PortPairGroup portPairGroup);

    /**
     * A port pair group builder..
     */
    interface Builder {

        /**
         * Assigns the port pair group id to this object.
         *
         * @param portPairGroupId the port pair group id
         * @return this the builder object
         */
        Builder setId(PortPairGroupId portPairGroupId);

        /**
         * Assigns tenant id to this object.
         *
         * @param tenantId tenant id of port pair group
         * @return this the builder object
         */
        Builder setTenantId(TenantId tenantId);

        /**
         * Assigns the name to this object.
         *
         * @param name name of the port pair group
         * @return this the builder object
         */
        Builder setName(String name);

        /**
         * Assigns the description to this object.
         *
         * @param description description of the port pair group
         * @return this the builder object
         */
        Builder setDescription(String description);

        /**
         * Assigns the port pairs associated with the port pair group
         * to this object.
         *
         * @param portPairs list of port pairs
         * @return this the builder object
         */
        Builder setPortPairs(List<PortPairId> portPairs);

        /**
         * Builds a port pair group object.
         *
         * @return a port pair group object.
         */
        PortPairGroup build();
    }
}
