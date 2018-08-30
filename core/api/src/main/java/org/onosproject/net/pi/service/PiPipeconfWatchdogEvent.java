/*
 * Copyright 2018-present Open Networking Foundation
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
 * Event representing changes in the status of a device pipeline.
 */
@Beta
public class PiPipeconfWatchdogEvent
        extends AbstractEvent<PiPipeconfWatchdogEvent.Type, DeviceId> {

    /**
     * Type of event.
     */
    public enum Type {
        PIPELINE_READY,
        PIPELINE_UNKNOWN
    }

    /**
     * Creates a new event for the given device.
     *
     * @param type type
     * @param subject device ID
     */
    public PiPipeconfWatchdogEvent(PiPipeconfWatchdogEvent.Type type, DeviceId subject) {
        super(type, subject);
    }
}
