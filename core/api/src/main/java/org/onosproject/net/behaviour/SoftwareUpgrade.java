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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;

import org.onosproject.net.driver.HandlerBehaviour;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Behaviour that upgrades the software on a device.
 *
 */
@Beta
public interface SoftwareUpgrade extends HandlerBehaviour {

    /**
     * Completion status of upgrade.
     */
    public enum Status {
        /**
         * Indicates a successfully completed upgrade.
         */
        SUCCESS,

        /**
         * Indicates an aborted upgrade.
         */
        FAILURE
    }

    /**
     * Configures the uri from where the upgrade will be pulled.
     *
     * @param uri uri of the software upgrade location
     * @return boolean true if the uri was properly configured
     */
    boolean configureUri(URI uri);

    /**
     * Performs an upgrade.
     *
     * @return A future that will be completed when the upgrade completes
     */
    CompletableFuture<Status> upgrade();
}
