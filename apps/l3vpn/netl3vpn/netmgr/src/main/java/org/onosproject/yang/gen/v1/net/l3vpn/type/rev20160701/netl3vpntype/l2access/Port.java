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

package org.onosproject.yang.gen.v1.net.l3vpn.type.rev20160701.netl3vpntype.l2access;

/**
 * Abstraction of an entity which represents the functionality of port.
 */
public interface Port {

    /**
     * Returns the attribute ltpId.
     *
     * @return value of ltpId
     */
    String ltpId();

    /**
     * Builder for port.
     */
    interface PortBuilder {

        /**
         * Returns the attribute ltpId.
         *
         * @return value of ltpId
         */
        String ltpId();

        /**
         * Returns the builder object of ltpId.
         *
         * @param ltpId value of ltpId
         * @return builder object of ltpId
         */
        PortBuilder ltpId(String ltpId);

        /**
         * Builds object of port.
         *
         * @return object of port.
         */
        Port build();
    }
}