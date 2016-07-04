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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm;

import java.util.List;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.BgpVrf;

/**
 * Abstraction of an entity which represents the functionality of bgpVrfs.
 */
public interface BgpVrfs {

    /**
     * Returns the attribute bgpVrf.
     *
     * @return list of bgpVrf
     */
    List<BgpVrf> bgpVrf();

    /**
     * Builder for bgpVrfs.
     */
    interface BgpVrfsBuilder {

        /**
         * Returns the attribute bgpVrf.
         *
         * @return list of bgpVrf
         */
        List<BgpVrf> bgpVrf();

        /**
         * Returns the builder object of bgpVrf.
         *
         * @param bgpVrf list of bgpVrf
         * @return builder object of bgpVrf
         */
        BgpVrfsBuilder bgpVrf(List<BgpVrf> bgpVrf);

        /**
         * Builds object of bgpVrfs.
         *
         * @return object of bgpVrfs.
         */
        BgpVrfs build();
    }
}