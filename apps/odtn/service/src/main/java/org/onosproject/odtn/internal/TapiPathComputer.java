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

import org.onosproject.odtn.utils.tapi.TapiConnection;
import org.onosproject.odtn.utils.tapi.TapiNepPair;

/**
 * ODTN Tapi path computation service.
 */
public interface TapiPathComputer {

    /**
     * Compute and decide multi-hop route/path from e2e intent.
     *
     * @param neps Both NodeEdgePoints associated with ServiceInterfacePoint of NBI request
     * @return List of both CEPs of devices or domains in the calculated route
     */
    TapiConnection pathCompute(TapiNepPair neps);
}
