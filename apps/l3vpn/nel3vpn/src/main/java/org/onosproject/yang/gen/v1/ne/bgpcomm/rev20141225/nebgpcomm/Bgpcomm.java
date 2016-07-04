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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm;

import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.BgpVrfs;

/**
 * Abstraction of an entity which represents the functionality of bgpcomm.
 */
public interface Bgpcomm {

    /**
     * Returns the attribute bgpVrfs.
     *
     * @return value of bgpVrfs
     */
    BgpVrfs bgpVrfs();

    /**
     * Builder for bgpcomm.
     */
    interface BgpcommBuilder {

        /**
         * Returns the attribute bgpVrfs.
         *
         * @return value of bgpVrfs
         */
        BgpVrfs bgpVrfs();

        /**
         * Returns the builder object of bgpVrfs.
         *
         * @param bgpVrfs value of bgpVrfs
         * @return builder object of bgpVrfs
         */
        BgpcommBuilder bgpVrfs(BgpVrfs bgpVrfs);

        /**
         * Builds object of bgpcomm.
         *
         * @return object of bgpcomm.
         */
        Bgpcomm build();
    }
}