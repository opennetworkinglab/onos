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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf;

import java.util.List;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAf;

/**
 * Abstraction of an entity which represents the functionality of bgpVrfAfs.
 */
public interface BgpVrfAfs {

    /**
     * Returns the attribute bgpVrfAf.
     *
     * @return list of bgpVrfAf
     */
    List<BgpVrfAf> bgpVrfAf();

    /**
     * Builder for bgpVrfAfs.
     */
    interface BgpVrfAfsBuilder {

        /**
         * Returns the attribute bgpVrfAf.
         *
         * @return list of bgpVrfAf
         */
        List<BgpVrfAf> bgpVrfAf();

        /**
         * Returns the builder object of bgpVrfAf.
         *
         * @param bgpVrfAf list of bgpVrfAf
         * @return builder object of bgpVrfAf
         */
        BgpVrfAfsBuilder bgpVrfAf(List<BgpVrfAf> bgpVrfAf);

        /**
         * Builds object of bgpVrfAfs.
         *
         * @return object of bgpVrfAfs.
         */
        BgpVrfAfs build();
    }
}