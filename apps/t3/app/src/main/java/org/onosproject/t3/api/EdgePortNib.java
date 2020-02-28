/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Map;
import java.util.Set;

/**
 * Represents Network Information Base (NIB) for edge ports
 * and supports alternative functions to
 * {@link org.onosproject.net.edge.EdgePortService} for offline data.
 */
public class EdgePortNib extends AbstractNib {

    private Map<DeviceId, Set<ConnectPoint>> edgePorts;

    // use the singleton helper to create the instance
    protected EdgePortNib() {
    }

    /**
     * Sets a map of device id : edge ports of the device.
     *
     * @param edgePorts device-edge ports map
     */
    public void setEdgePorts(Map<DeviceId, Set<ConnectPoint>> edgePorts) {
        this.edgePorts = edgePorts;
    }

    /**
     * Returns the device-edge ports map.
     * @return device-edge ports map
     */
    public Map<DeviceId, Set<ConnectPoint>> getEdgePorts() {
        return ImmutableMap.copyOf(edgePorts);
    }

    /**
     * Indicates whether or not the specified connection point is an edge point.
     *
     * @param point connection point
     * @return true if edge point
     */
    public boolean isEdgePoint(ConnectPoint point) {
        Set<ConnectPoint> connectPoints = edgePorts.get(point.deviceId());
        return connectPoints != null && connectPoints.contains(point);
    }

    /**
     * Returns the singleton instance of edge ports NIB.
     *
     * @return instance of edge ports NIB
     */
    public static EdgePortNib getInstance() {
        return EdgePortNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final EdgePortNib INSTANCE = new EdgePortNib();
    }

}
