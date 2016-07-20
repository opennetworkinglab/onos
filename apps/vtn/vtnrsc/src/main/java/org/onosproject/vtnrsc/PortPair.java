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


/**
 * Abstraction of an entity providing Port Pair information.
 * A port pair represents a service function instance.
 */
public interface PortPair {

    /**
     * Returns the ID of this port Pair.
     *
     * @return the port pair id
     */
    PortPairId portPairId();

    /**
     * Returns the tenant id of this port pair.
     *
     * @return an tenant id
     */
    TenantId tenantId();

    /**
     * Returns the description of this port pair.
     *
     * @return description of port pair
     */
    String name();

    /**
     * Returns the description of this port pair.
     *
     * @return description of port pair
     */
    String description();

    /**
     * Returns the ingress port of this port pair.
     *
     * @return ingress of port pair
     */
    String ingress();

    /**
     * Returns the egress port of this port pair.
     *
     * @return egress of port pair
     */
    String egress();

    /**
     * Returns whether this port pair is an exact match to the port pair given
     * in the argument.
     * <p>
     * Exact match means the Port port pairs match with the given port pair.
     * It does not consider the port pair id, name and description.
     * </p>
     * @param portPair other port pair to match against
     * @return true if the port pairs are an exact match, otherwise false
     */
    boolean exactMatch(PortPair portPair);

    /**
     * A port pair builder..
     */
    interface Builder {

        /**
         * Assigns the port pair id to this object.
         *
         * @param portPairId the port pair id
         * @return this the builder object
         */
        Builder setId(PortPairId portPairId);

        /**
         * Assigns tenant id to this object.
         *
         * @param tenantId tenant id of the port pair
         * @return this the builder object
         */
        Builder setTenantId(TenantId tenantId);

        /**
         * Assigns the name to this object.
         *
         * @param name name of the port pair
         * @return this the builder object
         */
        Builder setName(String name);

        /**
         * Assigns the description to this object.
         *
         * @param description description of the port pair
         * @return this the builder object
         */
        Builder setDescription(String description);

        /**
         * Assigns the ingress port to this object.
         *
         * @param port ingress port of the port pair
         * @return this the builder object
         */
        Builder setIngress(String port);

        /**
         * Assigns the egress port to this object.
         *
         * @param port egress port of the port pair
         * @return this the builder object
         */
        Builder setEgress(String port);

        /**
         * Builds a port pair object.
         *
         * @return a port pair.
         */
        PortPair build();
    }
}
