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

import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

/**
 * Key that uniquely identifies a gRPC client.
 */
@Beta
public class GrpcClientKey {

    private static final String GRPC = "grpc";
    private static final String GRPCS = "grpcs";

    private final String serviceName;
    private final DeviceId deviceId;
    private final URI serverUri;

    /**
     * Creates a new client key.
     *
     * @param serviceName gRPC service name of the client
     * @param deviceId    ONOS device ID
     * @param serverUri   gRPC server URI
     */
    public GrpcClientKey(String serviceName, DeviceId deviceId, URI serverUri) {
        checkNotNull(serviceName);
        checkNotNull(deviceId);
        checkNotNull(serverUri);
        checkArgument(!serviceName.isEmpty(),
                      "Service name can not be null");
        checkArgument(serverUri.getScheme().equals(GRPC)
                              || serverUri.getScheme().equals(GRPCS),
                      format("Server URI scheme must be %s or %s", GRPC, GRPCS));
        checkArgument(!isNullOrEmpty(serverUri.getHost()),
                      "Server host address should not be empty");
        checkArgument(serverUri.getPort() > 0 && serverUri.getPort() <= 65535, "Invalid server port");
        this.serviceName = serviceName;
        this.deviceId = deviceId;
        this.serverUri = serverUri;
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
     * Returns the gRPC server URI.
     *
     * @return the gRPC server URI.
     */
    public URI serveUri() {
        return serverUri;
    }

    /**
     * Returns true if the client requires TLS/SSL, false otherwise.
     *
     * @return boolean
     */
    public boolean requiresSecureChannel() {
        return serverUri.getScheme().equals(GRPCS);
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
        return Objects.equal(serviceName, that.serviceName) &&
                Objects.equal(deviceId, that.deviceId) &&
                Objects.equal(serverUri, that.serverUri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serviceName, deviceId, serverUri);
    }

    @Override
    public String toString() {
        return format("%s/%s@%s", deviceId, serviceName, serverUri);
    }
}
