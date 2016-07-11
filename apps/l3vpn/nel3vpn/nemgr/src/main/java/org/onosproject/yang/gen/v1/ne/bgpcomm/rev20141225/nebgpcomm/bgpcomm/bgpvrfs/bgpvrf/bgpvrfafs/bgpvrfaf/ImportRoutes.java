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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf;

import java.util.List;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf
            .importroutes.ImportRoute;

/**
 * Abstraction of an entity which represents the functionality of importRoutes.
 */
public interface ImportRoutes {

    /**
     * Returns the attribute importRoute.
     *
     * @return list of importRoute
     */
    List<ImportRoute> importRoute();

    /**
     * Builder for importRoutes.
     */
    interface ImportRoutesBuilder {

        /**
         * Returns the attribute importRoute.
         *
         * @return list of importRoute
         */
        List<ImportRoute> importRoute();

        /**
         * Returns the builder object of importRoute.
         *
         * @param importRoute list of importRoute
         * @return builder object of importRoute
         */
        ImportRoutesBuilder importRoute(List<ImportRoute> importRoute);

        /**
         * Builds object of importRoutes.
         *
         * @return object of importRoutes.
         */
        ImportRoutes build();
    }
}