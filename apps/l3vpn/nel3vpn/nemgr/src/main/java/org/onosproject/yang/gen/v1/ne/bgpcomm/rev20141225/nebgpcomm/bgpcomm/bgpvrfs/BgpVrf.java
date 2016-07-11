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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs;

import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.BgpVrfAfs;

/**
 * Abstraction of an entity which represents the functionality of bgpVrf.
 */
public interface BgpVrf {

    /**
     * Returns the attribute vrfName.
     *
     * @return value of vrfName
     */
    String vrfName();

    /**
     * Returns the attribute bgpVrfAfs.
     *
     * @return value of bgpVrfAfs
     */
    BgpVrfAfs bgpVrfAfs();

    /**
     * Builder for bgpVrf.
     */
    interface BgpVrfBuilder {

        /**
         * Returns the attribute vrfName.
         *
         * @return value of vrfName
         */
        String vrfName();

        /**
         * Returns the attribute bgpVrfAfs.
         *
         * @return value of bgpVrfAfs
         */
        BgpVrfAfs bgpVrfAfs();

        /**
         * Returns the builder object of vrfName.
         *
         * @param vrfName value of vrfName
         * @return builder object of vrfName
         */
        BgpVrfBuilder vrfName(String vrfName);

        /**
         * Returns the builder object of bgpVrfAfs.
         *
         * @param bgpVrfAfs value of bgpVrfAfs
         * @return builder object of bgpVrfAfs
         */
        BgpVrfBuilder bgpVrfAfs(BgpVrfAfs bgpVrfAfs);

        /**
         * Builds object of bgpVrf.
         *
         * @return object of bgpVrf.
         */
        BgpVrf build();
    }
}