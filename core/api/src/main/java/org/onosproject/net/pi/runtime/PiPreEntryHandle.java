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

package org.onosproject.net.pi.runtime;

import org.onosproject.net.DeviceId;

/**
 * Abstract implementation of a PI handle for PRE entries.
 */
public abstract class PiPreEntryHandle extends PiHandle {

    PiPreEntryHandle(DeviceId deviceId) {
        super(deviceId);
    }

    /**
     * Returns the type of PRE entry associated with this handle.
     *
     * @return PRE entry type
     */
    public abstract PiPreEntryType preEntryType();

    @Override
    public PiEntityType entityType() {
        return PiEntityType.PRE_ENTRY;
    }
}
