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

import org.onosproject.yang.gen.v1.net.l3vpn.type.rev20160701.netl3vpntype.l2access.Port;

/**
 * Abstraction of an entity which represents the functionality of l2Access.
 */
public interface L2Access {

    /**
     * Returns the attribute accessType.
     *
     * @return value of accessType
     */
    String accessType();

    /**
     * Returns the attribute port.
     *
     * @return value of port
     */
    Port port();

    /**
     * Builder for l2Access.
     */
    interface L2AccessBuilder {

        /**
         * Returns the attribute accessType.
         *
         * @return value of accessType
         */
        String accessType();

        /**
         * Returns the attribute port.
         *
         * @return value of port
         */
        Port port();

        /**
         * Returns the builder object of accessType.
         *
         * @param accessType value of accessType
         * @return builder object of accessType
         */
        L2AccessBuilder accessType(String accessType);

        /**
         * Returns the builder object of port.
         *
         * @param port value of port
         * @return builder object of port
         */
        L2AccessBuilder port(Port port);

        /**
         * Builds object of l2Access.
         *
         * @return object of l2Access.
         */
        L2Access build();
    }
}