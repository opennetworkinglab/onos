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

package org.onosproject.protobuf.api;

import com.google.common.annotations.Beta;
import io.grpc.BindableService;

/**
 * A service that allows for de/registration of gRPC services, and determining
 * whether a service is present.
 */
@Beta
public interface GrpcServiceRegistry {
    /**
     * Register a gRPC service with this registry.
     * @param service the service to be registered
     * @return true if the service was added and server successfully started,
     * false otherwise
     */
    boolean register(BindableService service);

    /**
     * Unregister a gRPC service with this registry.
     * @param service the service to be unregistered
     * @return true if the service was removed and the server successfully
     * started, false otherwise
     */
    boolean unregister(BindableService service);

    /**
     * Checks if an instance of the  provided serviceClass is currently
     * registered with this registry.
     * @param serviceClass the class being queries
     * @return true if an instance of this specified class has been registered,
     * false otherwise
     */
    boolean containsService(Class<BindableService> serviceClass);
}
