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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances;

import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.VpnInstAfs;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.L3VpnIfs;

/**
 * Abstraction of an entity which represents the functionality of l3VpnInstance.
 */
public interface L3VpnInstance {

    /**
     * Returns the attribute vrfName.
     *
     * @return value of vrfName
     */
    String vrfName();

    /**
     * Returns the attribute vrfDescription.
     *
     * @return value of vrfDescription
     */
    String vrfDescription();

    /**
     * Returns the attribute l3VpnIfs.
     *
     * @return value of l3VpnIfs
     */
    L3VpnIfs l3VpnIfs();

    /**
     * Returns the attribute vpnInstAfs.
     *
     * @return value of vpnInstAfs
     */
    VpnInstAfs vpnInstAfs();

    /**
     * Builder for l3VpnInstance.
     */
    interface L3VpnInstanceBuilder {

        /**
         * Returns the attribute vrfName.
         *
         * @return value of vrfName
         */
        String vrfName();

        /**
         * Returns the attribute vrfDescription.
         *
         * @return value of vrfDescription
         */
        String vrfDescription();

        /**
         * Returns the attribute l3VpnIfs.
         *
         * @return value of l3VpnIfs
         */
        L3VpnIfs l3VpnIfs();

        /**
         * Returns the attribute vpnInstAfs.
         *
         * @return value of vpnInstAfs
         */
        VpnInstAfs vpnInstAfs();

        /**
         * Returns the builder object of vrfName.
         *
         * @param vrfName value of vrfName
         * @return builder object of vrfName
         */
        L3VpnInstanceBuilder vrfName(String vrfName);

        /**
         * Returns the builder object of vrfDescription.
         *
         * @param vrfDescription value of vrfDescription
         * @return builder object of vrfDescription
         */
        L3VpnInstanceBuilder vrfDescription(String vrfDescription);

        /**
         * Returns the builder object of l3VpnIfs.
         *
         * @param l3VpnIfs value of l3VpnIfs
         * @return builder object of l3VpnIfs
         */
        L3VpnInstanceBuilder l3VpnIfs(L3VpnIfs l3VpnIfs);

        /**
         * Returns the builder object of vpnInstAfs.
         *
         * @param vpnInstAfs value of vpnInstAfs
         * @return builder object of vpnInstAfs
         */
        L3VpnInstanceBuilder vpnInstAfs(VpnInstAfs vpnInstAfs);

        /**
         * Builds object of l3VpnInstance.
         *
         * @return object of l3VpnInstance.
         */
        L3VpnInstance build();
    }
}