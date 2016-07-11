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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup;

import java.util.List;
import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.Ac;

/**
 * Abstraction of an entity which represents the functionality of acs.
 */
public interface Acs {

    /**
     * Returns the attribute ac.
     *
     * @return list of ac
     */
    List<Ac> ac();

    /**
     * Builder for acs.
     */
    interface AcsBuilder {

        /**
         * Returns the attribute ac.
         *
         * @return list of ac
         */
        List<Ac> ac();

        /**
         * Returns the builder object of ac.
         *
         * @param ac list of ac
         * @return builder object of ac
         */
        AcsBuilder ac(List<Ac> ac);

        /**
         * Builds object of acs.
         *
         * @return object of acs.
         */
        Acs build();
    }
}