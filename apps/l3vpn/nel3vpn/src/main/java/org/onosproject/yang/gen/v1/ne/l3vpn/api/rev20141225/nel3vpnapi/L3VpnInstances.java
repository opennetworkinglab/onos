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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi;

import java.util.List;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.L3VpnInstance;

/**
 * Abstraction of an entity which represents the functionality of l3VpnInstances.
 */
public interface L3VpnInstances {

    /**
     * Returns the attribute l3VpnInstance.
     *
     * @return list of l3VpnInstance
     */
    List<L3VpnInstance> l3VpnInstance();

    /**
     * Builder for l3VpnInstances.
     */
    interface L3VpnInstancesBuilder {

        /**
         * Returns the attribute l3VpnInstance.
         *
         * @return list of l3VpnInstance
         */
        List<L3VpnInstance> l3VpnInstance();

        /**
         * Returns the builder object of l3VpnInstance.
         *
         * @param l3VpnInstance list of l3VpnInstance
         * @return builder object of l3VpnInstance
         */
        L3VpnInstancesBuilder l3VpnInstance(List<L3VpnInstance> l3VpnInstance);

        /**
         * Builds object of l3VpnInstances.
         *
         * @return object of l3VpnInstances.
         */
        L3VpnInstances build();
    }
}