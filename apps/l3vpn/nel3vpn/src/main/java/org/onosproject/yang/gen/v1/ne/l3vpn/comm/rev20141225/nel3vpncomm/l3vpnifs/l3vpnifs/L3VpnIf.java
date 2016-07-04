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

package org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs;

import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.Ipv4Address;

/**
 * Abstraction of an entity which represents the functionality of l3VpnIf.
 */
public interface L3VpnIf {

    /**
     * Returns the attribute ifName.
     *
     * @return value of ifName
     */
    String ifName();

    /**
     * Returns the attribute ipv4Addr.
     *
     * @return value of ipv4Addr
     */
    Ipv4Address ipv4Addr();

    /**
     * Returns the attribute subnetMask.
     *
     * @return value of subnetMask
     */
    Ipv4Address subnetMask();

    /**
     * Builder for l3VpnIf.
     */
    interface L3VpnIfBuilder {

        /**
         * Returns the attribute ifName.
         *
         * @return value of ifName
         */
        String ifName();

        /**
         * Returns the attribute ipv4Addr.
         *
         * @return value of ipv4Addr
         */
        Ipv4Address ipv4Addr();

        /**
         * Returns the attribute subnetMask.
         *
         * @return value of subnetMask
         */
        Ipv4Address subnetMask();

        /**
         * Returns the builder object of ifName.
         *
         * @param ifName value of ifName
         * @return builder object of ifName
         */
        L3VpnIfBuilder ifName(String ifName);

        /**
         * Returns the builder object of ipv4Addr.
         *
         * @param ipv4Addr value of ipv4Addr
         * @return builder object of ipv4Addr
         */
        L3VpnIfBuilder ipv4Addr(Ipv4Address ipv4Addr);

        /**
         * Returns the builder object of subnetMask.
         *
         * @param subnetMask value of subnetMask
         * @return builder object of subnetMask
         */
        L3VpnIfBuilder subnetMask(Ipv4Address subnetMask);

        /**
         * Builds object of l3VpnIf.
         *
         * @return object of l3VpnIf.
         */
        L3VpnIf build();
    }
}