/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.pcep.api;

import java.time.Duration;
import java.util.List;

/**
 * Statistics of a PCEP tunnel.
 */
public interface PcepTunnelStatistics {


    /**
     * Returns the id of PCEP tunnel.
     *
     * @return PCEP tunnel id
     */
    long id();


    /**
     * Returns the bandwidth utilization of a PCEP tunnel.
     *
     * @return PCEP bandwidth utilization
     */
    double bandwidthUtilization();

    /**
     * Returns the flow loss rate of a tunnel.
     *
     * @return tunnel flow loss rate
     */
    double packetLossRate();

    /**
     * Returns the end-to-end traffic flow delay of a tunnel.
     *
     * @return tunnel traffic flow delay
     */
    Duration flowDelay();

    /**
     * Returns the alarms on a tunnel.
     *
     * @return tunnel alarms
     */
    List<String> alarms();
}
