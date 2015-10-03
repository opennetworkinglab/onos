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
package org.onosproject.vtnrsc;

/**
 * Representation of the tenantNetwork.
 */
public interface TenantNetwork {

    /**
     * Coarse classification of the state of the tenantNetwork.
     */
    enum State {
        /**
         * Signifies that a tenantNetwork is currently active.This state means
         * that this network is available.
         */
        ACTIVE,
        /**
         * Signifies that a tenantNetwork is currently built.
         */
        BUILD,
        /**
         * Signifies that a tenantNetwork is currently unavailable.
         */
        DOWN,
        /**
         * Signifies that a tenantNetwork is currently error.
         */
        ERROR
    }

    /**
     * Coarse classification of the type of the tenantNetwork.
     */
    enum Type {
        /**
         * Signifies that a tenantNetwork is local.
         */
        LOCAL
    }

    /**
     * Returns the tenantNetwork identifier.
     *
     * @return tenantNetwork identifier
     */
    TenantNetworkId id();

    /**
     * Returns the tenantNetwork name.
     *
     * @return tenantNetwork name
     */
    String name();

    /**
     * Returns the administrative state of the tenantNetwork,which is up(true)
     * or down(false).
     *
     * @return true or false
     */
    boolean adminStateUp();

    /**
     * Returns the tenantNetwork state.
     *
     * @return tenant network state
     */
    State state();

    /**
     * Indicates whether this tenantNetwork is shared across all tenants. By
     * default,only administrative user can change this value.
     *
     * @return true or false
     */
    boolean shared();

    /**
     * Returns the UUID of the tenant that will own the tenantNetwork. This
     * tenant can be different from the tenant that makes the create
     * tenantNetwork request.
     *
     * @return the tenant identifier
     */
    TenantId tenantId();

    /**
     * Returns the routerExternal.Indicates whether this network is externally
     * accessible.
     *
     * @return true or false
     */
    boolean routerExternal();

    /**
     * Returns the tenantNetwork Type.
     *
     * @return tenantNetwork Type
     */
    Type type();

    /**
     * Returns the tenantNetwork physical network.
     *
     * @return tenantNetwork physical network
     */
    PhysicalNetwork physicalNetwork();

    /**
     * Returns the tenantNetwork segmentation id.
     *
     * @return tenantNetwork segmentation id
     */
    SegmentationId segmentationId();
}
