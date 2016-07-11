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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs
            .vpninstaf.vpntargets;

import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonVrfRtType;

/**
 * Abstraction of an entity which represents the functionality of vpnTarget.
 */
public interface VpnTarget {

    /**
     * Returns the attribute vrfRtvalue.
     *
     * @return value of vrfRtvalue
     */
    String vrfRtvalue();

    /**
     * Returns the attribute vrfRttype.
     *
     * @return value of vrfRttype
     */
    L3VpncommonVrfRtType vrfRttype();

    /**
     * Builder for vpnTarget.
     */
    interface VpnTargetBuilder {

        /**
         * Returns the attribute vrfRtvalue.
         *
         * @return value of vrfRtvalue
         */
        String vrfRtvalue();

        /**
         * Returns the attribute vrfRttype.
         *
         * @return value of vrfRttype
         */
        L3VpncommonVrfRtType vrfRttype();

        /**
         * Returns the builder object of vrfRtvalue.
         *
         * @param vrfRtvalue value of vrfRtvalue
         * @return builder object of vrfRtvalue
         */
        VpnTargetBuilder vrfRtvalue(String vrfRtvalue);

        /**
         * Returns the builder object of vrfRttype.
         *
         * @param vrfRttype value of vrfRttype
         * @return builder object of vrfRttype
         */
        VpnTargetBuilder vrfRttype(L3VpncommonVrfRtType vrfRttype);

        /**
         * Builds object of vpnTarget.
         *
         * @return object of vpnTarget.
         */
        VpnTarget build();
    }
}