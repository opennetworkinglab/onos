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

package org.onosproject.net.pi.service;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

/**
 * Entity that represents pipeconf to device binding events.
 */
@Beta
public final class PiPipeconfDeviceMappingEvent extends AbstractEvent<PiPipeconfDeviceMappingEvent.Type, DeviceId> {

    /**
     * Type of pipeconf to device mapping event.
     */
    public enum Type {

        /**
         * Individual mapping pipeconf to device added.
         */
        CREATED,

        /**
         * Individual mapping pipeconf to device removed.
         */
        REMOVED,
    }

    /**
     * Creates an event due to one Pipeconf being mapped to a device.
     *
     * @param type     event type
     * @param deviceId the deviceId for which the pipeconf was bound or updated.
     */
    public PiPipeconfDeviceMappingEvent(PiPipeconfDeviceMappingEvent.Type type, DeviceId deviceId) {
        super(type, deviceId);
    }

}
