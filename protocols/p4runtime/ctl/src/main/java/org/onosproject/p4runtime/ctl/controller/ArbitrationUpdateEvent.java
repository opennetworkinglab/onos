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
 * Default implementation of arbitration in P4Runtime.
 */
public final class ArbitrationUpdateEvent implements P4RuntimeEventSubject {

    private DeviceId deviceId;
    private boolean isMaster;

    /**
     * Creates arbitration with given role and master flag.
     *
     * @param deviceId the device
     * @param isMaster true if arbitration response signals master status
     */
    public ArbitrationUpdateEvent(DeviceId deviceId, boolean isMaster) {
        this.deviceId = deviceId;
        this.isMaster = isMaster;
    }

    /**
     * Returns true if arbitration response signals master status, false
     * otherwise.
     *
     * @return boolean flag
     */
    boolean isMaster() {
        return isMaster;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }
}
