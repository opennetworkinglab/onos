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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;

import java.time.Duration;
import java.util.List;

/**
 * Statistics of a tunnel.
 */
@Beta
public interface TunnelStatistics {

    /**
     * Returns the tunnel id.
     *
     * @return tunnelId id of tunnel
     */
    TunnelId id();

    /**
     * Returns the bandwidth utilization of a tunnel.
     *
     * @return bandwidth utilization
     */
    double bandwidthUtilization();

    /**
     * Returns the packet loss ratio of a tunnel.
     *
     * @return tunnel packet loss ratio
     */
    double packetLossRate();

    /**
     * Returns the end-to-end traffic flow delay of a tunnel.
     *
     * @return tunnel flow delay
     */
    Duration flowDelay();

    /**
     * Returns the alarms on a tunnel.
     *
     * @return tunnel alarms
     */
    List<String> alarms();
}
