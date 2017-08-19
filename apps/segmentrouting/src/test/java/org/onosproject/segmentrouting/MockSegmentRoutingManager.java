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

package org.onosproject.segmentrouting;

import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock Segment Routing Manager.
 */
public class MockSegmentRoutingManager extends SegmentRoutingManager {
    private Map<Integer, TrafficTreatment> nextTable;
    private AtomicInteger atomicNextId = new AtomicInteger();

    MockSegmentRoutingManager(Map<Integer, TrafficTreatment> nextTable) {
        appId = new DefaultApplicationId(1, SegmentRoutingManager.APP_NAME);
        this.nextTable = nextTable;
    }

    @Override
    public int getPortNextObjectiveId(DeviceId deviceId, PortNumber portNum,
                                      TrafficTreatment treatment,
                                      TrafficSelector meta,
                                      boolean createIfMissing) {
        int nextId = atomicNextId.incrementAndGet();
        nextTable.put(nextId, treatment);
        return nextId;
    }
}
