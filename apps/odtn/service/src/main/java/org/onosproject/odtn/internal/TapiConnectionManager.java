/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.internal;

import org.onosproject.odtn.utils.tapi.TapiNepPair;
import org.onosproject.odtn.utils.tapi.TapiConnectionHandler;

/**
 * ODTN Tapi connectivity-service:connection manager.
 */
public interface TapiConnectionManager {

    /**
     * Queue Tapi connection object to add to DCS.
     * If target connection has lower-connections, they are also queued recursively
     * using other TapiConnectionManager instance.
     * <p>
     * Operation will not be conducted until apply method called.
     *
     * @param neps Both NodeEdgePoints of Nodes(devices or domains) in the calculated route.
     * @return connectionHandler of connection to be created
     */
    TapiConnectionHandler createConnection(TapiNepPair neps);

    /**
     * Queue Tapi connection object to remove from DCS
     * If target connection has lower-connections, they are also queued recursively
     * using other TapiConnectionManager instance.
     * <p>
     * Operation will not be conducted until apply method called.
     *
     * @param connectionHandler handler of TAPI connection object to be deleted
     */
    void deleteConnection(TapiConnectionHandler connectionHandler);

    /**
     * Apply add/remove operation for queued connection instances.
     * If target connection has lower-connections, they are also operated recursively
     * by other TapiConnectionManager instance.
     */
    void apply();

}
