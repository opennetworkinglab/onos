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

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Key the uniquely identifies a P4Runtime client.
 */
final class ClientKey {

    private final DeviceId deviceId;
    private final String serverAddr;
    private final int serverPort;
    private final long p4DeviceId;

    /**
     * Creates a new client key.
     *
     * @param deviceId   ONOS device ID
     * @param serverAddr P4Runtime server address
     * @param serverPort P4Runtime server port
     * @param p4DeviceId P4Runtime server-internal device ID
     */
    ClientKey(DeviceId deviceId, String serverAddr, int serverPort, long p4DeviceId) {
        this.deviceId = deviceId;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.p4DeviceId = p4DeviceId;
    }

    /**
     * Returns the device ID.
     *
     * @return device ID.
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the P4Runtime server address.
     *
     * @return P4Runtime server address
     */
    public String serverAddr() {
        return serverAddr;
    }

    /**
     * Returns the P4Runtime server port.
     *
     * @return P4Runtime server port
     */
    public int serverPort() {
        return serverPort;
    }

    /**
     * Returns the P4Runtime server-internal device ID.
     *
     * @return P4Runtime server-internal device ID
     */
    public long p4DeviceId() {
        return p4DeviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverAddr, serverPort, p4DeviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ClientKey other = (ClientKey) obj;
        return Objects.equals(this.serverAddr, other.serverAddr)
                && Objects.equals(this.serverPort, other.serverPort)
                && Objects.equals(this.p4DeviceId, other.p4DeviceId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceId", deviceId)
                .add("serverAddr", serverAddr)
                .add("serverPort", serverPort)
                .add("p4DeviceId", p4DeviceId)
                .toString();
    }
}
