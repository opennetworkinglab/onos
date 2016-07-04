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

package org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf
            .importroutes;

import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.BgpcommImRouteProtocol;

/**
 * Abstraction of an entity which represents the functionality of importRoute.
 */
public interface ImportRoute {

    /**
     * Returns the attribute importProtocol.
     *
     * @return value of importProtocol
     */
    BgpcommImRouteProtocol importProtocol();

    /**
     * Returns the attribute importProcessId.
     *
     * @return value of importProcessId
     */
    String importProcessId();

    /**
     * Builder for importRoute.
     */
    interface ImportRouteBuilder {

        /**
         * Returns the attribute importProtocol.
         *
         * @return value of importProtocol
         */
        BgpcommImRouteProtocol importProtocol();

        /**
         * Returns the attribute importProcessId.
         *
         * @return value of importProcessId
         */
        String importProcessId();

        /**
         * Returns the builder object of importProtocol.
         *
         * @param importProtocol value of importProtocol
         * @return builder object of importProtocol
         */
        ImportRouteBuilder importProtocol(BgpcommImRouteProtocol importProtocol);

        /**
         * Returns the builder object of importProcessId.
         *
         * @param importProcessId value of importProcessId
         * @return builder object of importProcessId
         */
        ImportRouteBuilder importProcessId(String importProcessId);

        /**
         * Builds object of importRoute.
         *
         * @return object of importRoute.
         */
        ImportRoute build();
    }
}