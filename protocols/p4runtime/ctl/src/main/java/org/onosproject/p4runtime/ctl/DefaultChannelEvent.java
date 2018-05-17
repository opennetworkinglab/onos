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
import org.onosproject.net.device.ChannelEvent.Type;
import org.onosproject.p4runtime.api.P4RuntimeEventSubject;

/**
 * Default implementation of channel event in P4Runtime. It allows passing any type of event.
 * If the event is an error a throwable can be directly passed.
 * Any other type of event cause can be passed as string.
 */
public class DefaultChannelEvent implements P4RuntimeEventSubject {
    private DeviceId deviceId;
    private Type type;
    private Throwable throwable;
    private String message;

    /**
     * Creates channel event with given status and throwable.
     *
     * @param deviceId  the device
     * @param type      error type
     * @param throwable the cause
     */
    public DefaultChannelEvent(DeviceId deviceId, Type type, Throwable throwable) {
        this.deviceId = deviceId;
        this.type = type;
        this.message = throwable.getMessage();
        this.throwable = throwable;
    }

    /**
     * Creates channel event with given status and string cause.
     *
     * @param deviceId the device
     * @param type     error type
     * @param message    the message
     */
    public DefaultChannelEvent(DeviceId deviceId, Type type, String message) {
        this.deviceId = deviceId;
        this.type = type;
        this.message = message;
        this.throwable = null;
    }

    /**
     * Creates channel event with given status, cause and throwable.
     *
     * @param deviceId the device
     * @param type     error type
     * @param message the message
     * @param throwable the cause
     */
    public DefaultChannelEvent(DeviceId deviceId, Type type, String message, Throwable throwable) {
        this.deviceId = deviceId;
        this.type = type;
        this.message = message;
        this.throwable = throwable;
    }

    /**
     * Gets the type of this event.
     *
     * @return the error type
     */
    public Type type() {
        return type;
    }

    /**
     * Gets the message related to this event.
     *
     * @return the message
     */
    public String message() {
        return message;
    }


    /**
     * Gets throwable of this event.
     * If no throwable is present returns null.
     *
     * @return the throwable
     */
    public Throwable throwable() {
        return throwable;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }
}
