/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.provider.ProviderService;

/**
 * Means of conveying host probing information to the core.
 */
public interface HostProbingProviderService extends ProviderService<HostProbingProvider> {

    /**
     * Notifies HostProbeStore the beginning of pending host location verification and
     * retrieves the unique MAC address for the probe.
     *
     * @param host host to be probed
     * @param connectPoint the connect point that is under verification
     * @param probeMode probe mode
     * @param probeMac probeMac if this is a retry.
     *                 Null if this is the very first probe and the probeMac is to-be-generated
     * @param retry max retry times
     * @return probeMac, the source MAC address ONOS uses to probe the host
     */
     MacAddress addProbingHost(Host host, ConnectPoint connectPoint, ProbeMode probeMode,
                                      MacAddress probeMac, int retry);

    /**
     * Notifies HostProbeStore the end of pending host location verification.
     *
     * @param probeMac the source MAC address ONOS uses to probe the host
     */
     void removeProbingHost(MacAddress probeMac);
}
