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

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of a PI clone session entry applied to the packet
 * replication engine (PRE) of a device, uniquely defined by a device ID, and
 * session ID.
 */
@Beta
public final class PiCloneSessionEntryHandle extends PiPreEntryHandle {

    private final int sessionId;

    private PiCloneSessionEntryHandle(DeviceId deviceId, int sessionId) {
        super(deviceId);
        this.sessionId = sessionId;
    }

    /**
     * Creates a new handle for the given device ID and PI clone session ID.
     *
     * @param deviceId  device ID
     * @param sessionId clone session ID
     * @return PI clone session entry handle
     */
    public static PiCloneSessionEntryHandle of(DeviceId deviceId,
                                               int sessionId) {
        return new PiCloneSessionEntryHandle(deviceId, sessionId);
    }

    /**
     * Creates a new handle for the given device ID and PI clone session entry.
     *
     * @param deviceId device ID
     * @param entry    PI clone session entry
     * @return PI clone session entry handle
     */
    public static PiCloneSessionEntryHandle of(DeviceId deviceId,
                                               PiCloneSessionEntry entry) {
        checkNotNull(entry);
        return new PiCloneSessionEntryHandle(deviceId, entry.sessionId());
    }

    /**
     * Returns the clone session ID associated with this handle.
     *
     * @return session ID
     */
    public int sessionId() {
        return sessionId;
    }

    @Override
    public PiPreEntryType preEntryType() {
        return PiPreEntryType.CLONE_SESSION;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId(), sessionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiCloneSessionEntryHandle that = (PiCloneSessionEntryHandle) o;
        return Objects.equal(deviceId(), that.deviceId()) &&
                Objects.equal(sessionId, that.sessionId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId())
                .add("sessionId", sessionId)
                .toString();
    }
}
