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

package org.onosproject.grpc.api;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Key that uniquely identifies a gRPC client.
 */
@Beta
public class GrpcClientKey {
    private final String serviceName;
    private final DeviceId deviceId;
    private final String serverAddr;
    private final int serverPort;

    /**
     * Creates a new client key.
     *
     * @param serviceName gRPC service name of the client
     * @param deviceId ONOS device ID
     * @param serverAddr gRPC server address
     * @param serverPort gRPC server port
     */
    public GrpcClientKey(String serviceName, DeviceId deviceId, String serverAddr, int serverPort) {
        checkNotNull(serviceName);
        checkNotNull(deviceId);
        checkNotNull(serverAddr);
        checkArgument(!serviceName.isEmpty(),
                "Service name can not be null");
        checkArgument(!serverAddr.isEmpty(),
                "Server address should not be empty");
        checkArgument(serverPort > 0 && serverPort <= 65535, "Invalid server port");
        this.serviceName = serviceName;
        this.deviceId = deviceId;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    /**
     * Gets the gRPC service name of the client.
     *
     * @return the service name
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * Gets the device ID.
     *
     * @return the device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Gets the gRPC server address.
     *
     * @return the gRPC server address.
     */
    public String serverAddr() {
        return serverAddr;
    }

    /**
     * Gets the gRPC server port.
     *
     * @return the gRPC server port.
     */
    public int serverPort() {
        return serverPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GrpcClientKey that = (GrpcClientKey) o;
        return serverPort == that.serverPort &&
                Objects.equal(serviceName, that.serviceName) &&
                Objects.equal(deviceId, that.deviceId) &&
                Objects.equal(serverAddr, that.serverAddr);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serviceName, deviceId, serverAddr, serverPort);
    }

    @Override
    public String toString() {
        return format("%s/%s@%s:%s", deviceId, serviceName, serverAddr, serverPort);
    }
}
