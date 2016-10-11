/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.edge;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficTreatment;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Adapter for tests involving the edge port service.
 */
public class EdgePortServiceAdapter implements EdgePortService {
    @Override
    public void addListener(EdgePortListener listener) {

    }

    @Override
    public void removeListener(EdgePortListener listener) {

    }

    @Override
    public boolean isEdgePoint(ConnectPoint point) {
        return false;
    }

    @Override
    public Iterable<ConnectPoint> getEdgePoints() {
        return null;
    }

    @Override
    public Iterable<ConnectPoint> getEdgePoints(DeviceId deviceId) {
        return null;
    }

    @Override
    public void emitPacket(ByteBuffer data, Optional<TrafficTreatment> treatment) {

    }

    @Override
    public void emitPacket(DeviceId deviceId, ByteBuffer data, Optional<TrafficTreatment> treatment) {

    }
}
