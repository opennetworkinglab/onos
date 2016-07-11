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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac;

import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.Ipv4Address;

/**
 * Abstraction of an entity which represents the functionality of l3Access.
 */
public interface L3Access {

    /**
     * Returns the attribute address.
     *
     * @return value of address
     */
    Ipv4Address address();

    /**
     * Builder for l3Access.
     */
    interface L3AccessBuilder {

        /**
         * Returns the attribute address.
         *
         * @return value of address
         */
        Ipv4Address address();

        /**
         * Returns the builder object of address.
         *
         * @param address value of address
         * @return builder object of address
         */
        L3AccessBuilder address(Ipv4Address address);

        /**
         * Builds object of l3Access.
         *
         * @return object of l3Access.
         */
        L3Access build();
    }
}