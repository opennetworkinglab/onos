/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.fpm;

import java.util.Collection;
import java.util.Map;


/**
 * Provides information about the FPM route receiver module.
 */
public interface FpmInfoService {

    /**
     * Returns the FPM peers that are currently connected.
     *
     * @return a map of FPM peer with related information
     */
    Map<FpmPeer, FpmPeerInfo> peers();

    /**
     * Returns true if pushing routes to Quagga is emabled.
     *
     * @return true or false
     */
    boolean isPdPushEnabled();

    /**
     * Pushes all local FPM routes to route store.
     */
    void pushFpmRoutes();

    /**
     * Updates the acceptRoute flag to either accept or discard routes for input peers address.
     *
     * @param peers peers for which flag is updated
     */
    void updateAcceptRouteFlag(Collection<FpmPeerAcceptRoutes> peers);

}
