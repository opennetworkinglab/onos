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
package org.onosproject.net.host;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.provider.Provider;

/**
 * Provider of host probing.
 */
public interface HostProbingProvider extends Provider {

    /**
     * Probe given host on the given connectPoint with given probeMode.
     *
     * @param host host to be probed
     * @param connectPoint connect point on which the probe is sent
     * @param probeMode probe mode
     */
    void probeHost(Host host, ConnectPoint connectPoint, ProbeMode probeMode);

    /**
     * Process host probing events.
     *
     * @param event host probing event
     */
    void processEvent(HostProbingEvent event);
}
