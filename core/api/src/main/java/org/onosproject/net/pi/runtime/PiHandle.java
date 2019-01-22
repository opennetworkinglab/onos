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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI entity applied to a device, unique in the scope of
 * the whole network.
 */
@Beta
public abstract class PiHandle {

    private final DeviceId deviceId;

    protected PiHandle(DeviceId deviceId) {
        this.deviceId = checkNotNull(deviceId);
    }

    /**
     * Returns the device ID of this handle.
     *
     * @return device ID
     */
    public final DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the type of entity identified by this handle.
     *
     * @return PI entity type
     */
    public abstract PiEntityType entityType();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
