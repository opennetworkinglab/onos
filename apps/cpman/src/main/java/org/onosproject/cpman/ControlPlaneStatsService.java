/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cpman;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Control Plane Statistics Service Interface.
 */
public interface ControlPlaneStatsService {

    /**
     * Add a new control plane metric value with a certain update interval.
     *
     * @param cpm            control plane metric (e.g., control message rate, cpu, memory, etc.)
     * @param updateInterval value update interval (time unit will be in minute)
     */
    void updateMetric(ControlPlaneMetric cpm, int updateInterval);

    /**
     * Obtain the control plane load of a specific device.
     *
     * @param nodeId   node id {@link org.onosproject.cluster.NodeId}
     * @param type     control metric type
     * @param deviceId device id {@link org.onosproject.net.DeviceId}
     * @return control plane load
     */
    ControlPlaneLoad getLoad(NodeId nodeId, ControlMetricType type, Optional<DeviceId> deviceId);

    /**
     * Obtain the control plane load of a specific device with a specific time duration.
     *
     * @param nodeId   node id {@link org.onosproject.cluster.NodeId}
     * @param type     control metric type
     * @param duration time duration
     * @param unit     time unit
     * @param deviceId device id {@link org.onosproject.net.Device}
     * @return control plane load
     */
    ControlPlaneLoad getLoad(NodeId nodeId, ControlMetricType type, Optional<DeviceId> deviceId,
                             int duration, TimeUnit unit);
}