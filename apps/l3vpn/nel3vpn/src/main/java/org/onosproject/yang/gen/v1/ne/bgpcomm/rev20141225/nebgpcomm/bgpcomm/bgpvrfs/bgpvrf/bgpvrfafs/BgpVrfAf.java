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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs;

import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf
            .ImportRoutes;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.BgpcommPrefixType;

/**
 * Abstraction of an entity which represents the functionality of bgpVrfAf.
 */
public interface BgpVrfAf {

    /**
     * Returns the attribute afType.
     *
     * @return value of afType
     */
    BgpcommPrefixType afType();

    /**
     * Returns the attribute importRoutes.
     *
     * @return value of importRoutes
     */
    ImportRoutes importRoutes();

    /**
     * Builder for bgpVrfAf.
     */
    interface BgpVrfAfBuilder {

        /**
         * Returns the attribute afType.
         *
         * @return value of afType
         */
        BgpcommPrefixType afType();

        /**
         * Returns the attribute importRoutes.
         *
         * @return value of importRoutes
         */
        ImportRoutes importRoutes();

        /**
         * Returns the builder object of afType.
         *
         * @param afType value of afType
         * @return builder object of afType
         */
        BgpVrfAfBuilder afType(BgpcommPrefixType afType);

        /**
         * Returns the builder object of importRoutes.
         *
         * @param importRoutes value of importRoutes
         * @return builder object of importRoutes
         */
        BgpVrfAfBuilder importRoutes(ImportRoutes importRoutes);

        /**
         * Builds object of bgpVrfAf.
         *
         * @return object of bgpVrfAf.
         */
        BgpVrfAf build();
    }
}