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
package org.onosproject.segmentrouting;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Segment Routing Flow Objective Context.
 */
public class SRObjectiveContext implements ObjectiveContext {
    enum ObjectiveType {
        FILTER,
        FORWARDING
    }
    private final DeviceId deviceId;
    private final ObjectiveType type;

    private static final Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);

    SRObjectiveContext(DeviceId deviceId, ObjectiveType type) {
        this.deviceId = deviceId;
        this.type = type;
    }
    @Override
    public void onSuccess(Objective objective) {
        log.debug("{} objective operation successful in device {}",
                type.name(), deviceId);
    }

    @Override
    public void onError(Objective objective, ObjectiveError error) {
        log.warn("{} objective {} operation failed with error: {} in device {}",
                type.name(), objective, error, deviceId);
    }
}

