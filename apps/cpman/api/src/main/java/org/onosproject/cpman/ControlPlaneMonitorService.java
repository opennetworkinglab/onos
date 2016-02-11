/*
 * Copyright 2015-2016 Open Networking Laboratory
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
import java.util.Set;

import static org.onosproject.cpman.ControlResource.*;

/**
 * Control Plane Statistics Service Interface.
 */
public interface ControlPlaneMonitorService {

    /**
     * Adds a new control metric value with a certain update interval.
     *
     * @param controlMetric             control plane metric (e.g., control
     *                                  message rate, cpu, memory, etc.)
     * @param updateIntervalInMinutes   value update interval (in minute)
     * @param deviceId                  device identifier
     */
    void updateMetric(ControlMetric controlMetric, int updateIntervalInMinutes,
                      Optional<DeviceId> deviceId);

    /**
     * Adds a new control metric value with a certain update interval.
     *
     * @param controlMetric              control plane metric (e.g., disk and
     *                                   network metrics)
     * @param updateIntervalInMinutes    value update interval (in minute)
     * @param resourceName               resource name
     */
    void updateMetric(ControlMetric controlMetric, int updateIntervalInMinutes,
                      String resourceName);

    /**
     * Obtains the control plane load of a specific device.
     * The metrics range from control messages and system metrics
     * (e.g., CPU and memory info)
     *
     * @param nodeId                     node identifier
     * @param type                       control metric type
     * @param deviceId                   device identifier
     * @return control plane load
     */
    ControlLoad getLoad(NodeId nodeId, ControlMetricType type,
                        Optional<DeviceId> deviceId);

    /**
     * Obtains the control plane load of a specific device.
     * The metrics range from I/O device metrics
     * (e.g., disk and network interface)
     *
     * @param nodeId                     node identifier
     * @param type                       control metric type
     * @param resourceName               resource name
     * @return control plane load
     */
    ControlLoad getLoad(NodeId nodeId, ControlMetricType type, String resourceName);

    /**
     * Obtains a list of names of available resources.
     *
     * @param resourceType resource type
     * @return a collection of names of available resources
     */
    Set<String> availableResources(Type resourceType);
}