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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.concurrent.CompletableFuture;

/**
 * Means to administratively enable,disable and query the state
 * of a port on a device.
 */
@Beta
public interface PortAdmin extends HandlerBehaviour {

    /**
     * Enable administratively a port.
     *
     * @param number the port to be enabled
     * @return CompletableFuture with true if the operation was successful
     */
    CompletableFuture<Boolean> enable(PortNumber number);

    /**
     * Disable administratively a port.
     *
     * @param number the port to be disabled
     * @return CompletableFuture with true if the operation was successful
     */
    CompletableFuture<Boolean> disable(PortNumber number);

    /**
     * Retrieves the information about the administrative state of a port.
     *
     * @param number identifier of the port to be queried about its state
     * @return CompletableFuture containing, when completed, true if the port isEnabled.
     */
    CompletableFuture<Boolean> isEnabled(PortNumber number);

    //TODO this behaviour can be augmented or others can be created for
    // LED and Speed configuration

}
