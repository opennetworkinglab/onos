/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cpman.impl.message;

import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoad;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.ControlResource;
import org.onosproject.net.DeviceId;

import java.util.Optional;
import java.util.Set;

/**
 * Test adapter control plane monitoring service.
 */
public class ControlPlaneMonitorServiceAdaptor implements ControlPlaneMonitorService {
    @Override
    public void updateMetric(ControlMetric controlMetric,
                             int updateIntervalInMinutes, Optional<DeviceId> deviceId) {
    }

    @Override
    public void updateMetric(ControlMetric controlMetric,
                             int updateIntervalInMinutes, String resourceName) {
    }

    @Override
    public ControlLoad getLoad(NodeId nodeId,
                               ControlMetricType type, Optional<DeviceId> deviceId) {
        return null;
    }

    @Override
    public ControlLoad getLoad(NodeId nodeId,
                               ControlMetricType type, String resourceName) {
        return null;
    }

    @Override
    public Set<String> availableResources(ControlResource.Type resourceType) {
        return null;
    }
}
