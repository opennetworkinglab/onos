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

package org.onosproject.net.behaviour;

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.concurrent.CompletableFuture;

/**
 * Handler behaviour capable of device reboot execution and getting system time since UNIX epoch.
 */
public interface BasicSystemOperations extends HandlerBehaviour {

    /**
     * Causes the target to reboot immediately.
     *
     * @return true if the operation was successful, false otherwise
     */
    CompletableFuture<Boolean> reboot();

    /**
     * Returns the current time on the target.
     *
     * @return Current time in nanoseconds since UNIX epoch.
     */
    CompletableFuture<Long> time();
}
