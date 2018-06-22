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

package org.onosproject.p4runtime.ctl;

import org.onosproject.net.DeviceId;
import org.onosproject.p4runtime.api.P4RuntimeEventSubject;

/**
 * Channel event in P4Runtime.
 */
final class ChannelEvent implements P4RuntimeEventSubject {

    enum Type {
        OPEN,
        CLOSED,
        ERROR
    }

    private DeviceId deviceId;
    private Type type;

    /**
     * Creates channel event with given status and throwable.
     *
     * @param deviceId  the device
     * @param type      error type
     */
    ChannelEvent(DeviceId deviceId, Type type) {
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * Gets the type of this event.
     *
     * @return the error type
     */
    public Type type() {
        return type;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }
}
