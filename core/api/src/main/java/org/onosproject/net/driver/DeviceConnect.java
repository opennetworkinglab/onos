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
 * with a device. A connection is intended as the presence of state (e.g. a
 * transport session) required to carry messages between this node and the
 * device.
 */
@Beta
public interface DeviceConnect extends HandlerBehaviour {

    /**
     * Connects to the device, for example by opening the transport session that
     * will be later used to send control messages. Returns true if the
     * connection was initiated successfully, false otherwise. The
     * implementation might require probing the device over the network to
     * initiate the connection.
     * <p>
     * When calling this method while a connection to the device already exists,
     * the behavior is not defined. For example, some implementations might
     * require to first call {@link #disconnect()}, while other might behave as
     * a no-op.
     *
     * @return CompletableFuture eventually true if a connection was created
     * successfully, false otherwise
     * @throws IllegalStateException if a connection already exists and the
     *                               implementation requires to call {@link
     *                               #disconnect()} first.
     */
    CompletableFuture<Boolean> connect() throws IllegalStateException;

    /**
     * Returns true if a connection to the device exists, false otherwise. This
     * method is NOT expected to send any message over the network to check for
     * device reachability, but rather it should only give an indication if any
     * internal connection state exists for the device. As such, it should NOT
     * block execution.
     * <p>
     * In general, when called after {@link #connect()} it should always return
     * true, while it is expected to always return false after calling {@link
     * #disconnect()} or if {@link #connect()} was never called.
     *
     * @return true if the connection is open, false otherwise
     */
    boolean isConnected();

    /**
     * Disconnects from the device, for example closing the transport session
     * previously opened.
     * <p>
     * Calling multiple times this method while a connection to the device is
     * already closed should result in a no-op.
     */
    void disconnect();
}
