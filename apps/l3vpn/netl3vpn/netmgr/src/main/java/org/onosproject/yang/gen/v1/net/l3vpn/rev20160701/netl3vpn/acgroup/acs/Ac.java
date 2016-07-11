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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs;

import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac.L2Access;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac.L3Access;

/**
 * Abstraction of an entity which represents the functionality of ac.
 */
public interface Ac {

    /**
     * Returns the attribute id.
     *
     * @return value of id
     */
    String id();

    /**
     * Returns the attribute neId.
     *
     * @return value of neId
     */
    String neId();

    /**
     * Returns the attribute l2Access.
     *
     * @return value of l2Access
     */
    L2Access l2Access();

    /**
     * Returns the attribute l3Access.
     *
     * @return value of l3Access
     */
    L3Access l3Access();

    /**
     * Builder for ac.
     */
    interface AcBuilder {

        /**
         * Returns the attribute id.
         *
         * @return value of id
         */
        String id();

        /**
         * Returns the attribute neId.
         *
         * @return value of neId
         */
        String neId();

        /**
         * Returns the attribute l2Access.
         *
         * @return value of l2Access
         */
        L2Access l2Access();

        /**
         * Returns the attribute l3Access.
         *
         * @return value of l3Access
         */
        L3Access l3Access();

        /**
         * Returns the builder object of id.
         *
         * @param id value of id
         * @return builder object of id
         */
        AcBuilder id(String id);

        /**
         * Returns the builder object of neId.
         *
         * @param neId value of neId
         * @return builder object of neId
         */
        AcBuilder neId(String neId);

        /**
         * Returns the builder object of l2Access.
         *
         * @param l2Access value of l2Access
         * @return builder object of l2Access
         */
        AcBuilder l2Access(L2Access l2Access);

        /**
         * Returns the builder object of l3Access.
         *
         * @param l3Access value of l3Access
         * @return builder object of l3Access
         */
        AcBuilder l3Access(L3Access l3Access);

        /**
         * Builds object of ac.
         *
         * @return object of ac.
         */
        Ac build();
    }
}