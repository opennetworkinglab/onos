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
package org.onosproject.net.host;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;

public interface HostLocationProbingService {
    /**
     * Mode of host location probing.
     */
    enum ProbeMode {
        /**
         * Append probed host location if reply is received before timeout. Otherwise, do nothing.
         * Typically used to discover secondary locations.
         */
        DISCOVER,

        /**
         * Remove probed host location if reply is received after timeout. Otherwise, do nothing.
         * Typically used to verify previous locations.
         */
        VERIFY
    }

    /**
     * Probes given host on given location.
     *
     * @param host the host to be probed
     * @param connectPoint the location of host to be probed
     * @param probeMode probe mode
     */
    void probeHostLocation(Host host, ConnectPoint connectPoint, ProbeMode probeMode);

}
