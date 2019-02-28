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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Behavior to program the pipeline of a device that supports
 * protocol-independence.
 */
@Beta
public interface PiPipelineProgrammable extends HandlerBehaviour {
    /**
     * Writes the given pipeconf to the device, returns a completable future
     * with true is the operations was successful, false otherwise.
     * <p>
     * After the future has been completed, the device is expected to process
     * data plane packets according to the written pipeconf.
     *
     * @param pipeconf pipeconf
     * @return completable future set to true if the operation was successful,
     * false otherwise
     */
    // TODO: return an explanation of why things went wrong, and the status of the device.
    CompletableFuture<Boolean> setPipeconf(PiPipeconf pipeconf);

    /**
     * Probes the device to verify that the given pipeconf is the one currently
     * configured.
     * <p>
     * This method is expected to always return true after successfully calling
     * {@link #setPipeconf(PiPipeconf)} with the given pipeconf.
     *
     * @param pipeconf pipeconf
     * @return completable future eventually true if the device has the given
     * pipeconf set, false otherwise
     */
    CompletableFuture<Boolean> isPipeconfSet(PiPipeconf pipeconf);

    /**
     * Returns the default pipeconf for this device, to be used when any other
     * pipeconf is not available.
     *
     * @return optional pipeconf
     */
    Optional<PiPipeconf> getDefaultPipeconf();
}
