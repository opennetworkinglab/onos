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

package org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs;

import java.util.List;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIf;

/**
 * Abstraction of an entity which represents the functionality of l3VpnIfs.
 */
public interface L3VpnIfs {

    /**
     * Returns the attribute l3VpnIf.
     *
     * @return list of l3VpnIf
     */
    List<L3VpnIf> l3VpnIf();

    /**
     * Builder for l3VpnIfs.
     */
    interface L3VpnIfsBuilder {

        /**
         * Returns the attribute l3VpnIf.
         *
         * @return list of l3VpnIf
         */
        List<L3VpnIf> l3VpnIf();

        /**
         * Returns the builder object of l3VpnIf.
         *
         * @param l3VpnIf list of l3VpnIf
         * @return builder object of l3VpnIf
         */
        L3VpnIfsBuilder l3VpnIf(List<L3VpnIf> l3VpnIf);

        /**
         * Builds object of l3VpnIfs.
         *
         * @return object of l3VpnIfs.
         */
        L3VpnIfs build();
    }
}