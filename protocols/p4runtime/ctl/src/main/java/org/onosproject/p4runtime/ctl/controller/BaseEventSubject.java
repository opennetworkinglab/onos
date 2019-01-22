/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.controller;

import org.onosproject.net.DeviceId;
import org.onosproject.p4runtime.api.P4RuntimeEventSubject;

/**
 * Base P4Runtime event subject that carries just the device ID that originated
 * the event.
 */
public final class BaseEventSubject implements P4RuntimeEventSubject {

    private DeviceId deviceId;

    /**
     * Creates an event subject.
     *
     * @param deviceId the device
     */
    public BaseEventSubject(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }
}
