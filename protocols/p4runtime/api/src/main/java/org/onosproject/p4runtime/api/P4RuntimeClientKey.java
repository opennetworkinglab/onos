/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.grpc.api.GrpcClientKey;
import org.onosproject.net.DeviceId;

import java.util.Objects;

/**
 * Key that uniquely identifies a P4Runtime client.
 */
@Beta
public final class P4RuntimeClientKey extends GrpcClientKey {
    private static final String P4RUNTIME = "P4Runtime";
    private final long p4DeviceId;

    /**
     * Creates a new client key.
     *
     * @param deviceId   ONOS device ID
     * @param serverAddr P4Runtime server address
     * @param serverPort P4Runtime server port
     * @param p4DeviceId P4Runtime server-internal device ID
     */
    public P4RuntimeClientKey(DeviceId deviceId, String serverAddr,
                              int serverPort, long p4DeviceId) {
        super(P4RUNTIME, deviceId, serverAddr, serverPort);
        this.p4DeviceId = p4DeviceId;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        P4RuntimeClientKey that = (P4RuntimeClientKey) o;
        return p4DeviceId == that.p4DeviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), p4DeviceId);
    }

    @Override
    public String toString() {
        return super.toString() + "/" + p4DeviceId;
    }
}
