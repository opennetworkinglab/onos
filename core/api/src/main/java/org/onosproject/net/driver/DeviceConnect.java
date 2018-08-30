/*
 * Copyright 2015-present Open Networking Foundation
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
 * Abstraction of handler behaviour used to set-up and tear-down connections
 * with a device.
 */
@Beta
public interface DeviceConnect extends HandlerBehaviour {

    /**
     * Connects to the device, for example by opening the transport session that
     * will be later used to send control messages. Returns true if the
     * connection was initiated successfully, false otherwise.
     * <p>
     * Calling multiple times this method while a connection to the device is
     * open should result in a no-op.
     *
     * @return CompletableFuture with true if the operation was successful
     */
    CompletableFuture<Boolean> connect();

    /**
     * Returns true if a connection to the device is open, false otherwise.
     *
     * @return true if the connection is open, false otherwise
     */
    boolean isConnected();

    /**
     * Disconnects from the device, for example closing the transport session
     * previously opened. Returns true if the disconnection procedure was
     * successful, false otherwise.
     * <p>
     * Calling multiple times this method while a connection to the device is
     * closed should result in a no-op.
     *
     * @return CompletableFuture with true if the operation was successful
     */
    CompletableFuture<Boolean> disconnect();

}
