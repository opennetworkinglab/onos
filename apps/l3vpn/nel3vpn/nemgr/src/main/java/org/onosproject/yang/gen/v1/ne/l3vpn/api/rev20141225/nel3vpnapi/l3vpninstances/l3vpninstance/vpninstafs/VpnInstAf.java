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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs;

import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonL3VpnPrefixType;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs
            .vpninstaf.VpnTargets;

/**
 * Abstraction of an entity which represents the functionality of vpnInstAf.
 */
public interface VpnInstAf {

    /**
     * Returns the attribute afType.
     *
     * @return value of afType
     */
    L3VpncommonL3VpnPrefixType afType();

    /**
     * Returns the attribute vrfRd.
     *
     * @return value of vrfRd
     */
    String vrfRd();

    /**
     * Returns the attribute vpnTargets.
     *
     * @return value of vpnTargets
     */
    VpnTargets vpnTargets();

    /**
     * Builder for vpnInstAf.
     */
    interface VpnInstAfBuilder {

        /**
         * Returns the attribute afType.
         *
         * @return value of afType
         */
        L3VpncommonL3VpnPrefixType afType();

        /**
         * Returns the attribute vrfRd.
         *
         * @return value of vrfRd
         */
        String vrfRd();

        /**
         * Returns the attribute vpnTargets.
         *
         * @return value of vpnTargets
         */
        VpnTargets vpnTargets();

        /**
         * Returns the builder object of afType.
         *
         * @param afType value of afType
         * @return builder object of afType
         */
        VpnInstAfBuilder afType(L3VpncommonL3VpnPrefixType afType);

        /**
         * Returns the builder object of vrfRd.
         *
         * @param vrfRd value of vrfRd
         * @return builder object of vrfRd
         */
        VpnInstAfBuilder vrfRd(String vrfRd);

        /**
         * Returns the builder object of vpnTargets.
         *
         * @param vpnTargets value of vpnTargets
         * @return builder object of vpnTargets
         */
        VpnInstAfBuilder vpnTargets(VpnTargets vpnTargets);

        /**
         * Builds object of vpnInstAf.
         *
         * @return object of vpnInstAf.
         */
        VpnInstAf build();
    }
}