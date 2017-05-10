/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.driver;

import com.google.common.annotations.Beta;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction of handler behaviour used to set-up and tear-down
 * connection with a device.
 */
@Beta
public interface DeviceConnect extends HandlerBehaviour {

    /**
     * Connects to the device.
     * It's supposed to initiate the transport sessions, channel and also,
     * if applicable, store them in the proper protocol specific
     * controller (e.g. GrpcController).
     *
     * @return CompletableFuture with true if the operation was successful
     */
    CompletableFuture<Boolean> connect();

    /**
     * Disconnects from the device.
     * It's supposed to destroy the transport sessions and channel and also,
     * if applicable, remove them in the proper protocol specific
     * controller (e.g. GrpcController).
     *
     * @return CompletableFuture with true if the operation was successful
     */
    CompletableFuture<Boolean> disconnect();

}
