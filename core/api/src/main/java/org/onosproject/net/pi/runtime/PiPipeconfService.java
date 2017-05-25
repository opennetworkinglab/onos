/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;

import java.util.Optional;

/**
 * A service to manage the configurations of protocol-independent pipelines.
 */
@Beta
public interface PiPipeconfService {

    // TODO: we might want to extend ListenerService to support the broadcasting of PipeconfEvent.

    /**
     * Registers the given pipeconf.
     *
     * @param pipeconf a pipeconf
     * @throws IllegalStateException if the same pipeconf identifier is already registered.
     */
    void register(PiPipeconf pipeconf) throws IllegalStateException;

    /**
     * Returns all pipeconfs registered.
     *
     * @return a collection of pipeconfs
     */
    Iterable<PiPipeconf> getPipeconfs();

    /**
     * Returns the pipeconf instance associated with the given identifier, if present.
     * If not present, it means that no pipeconf with such identifier has been registered so far.
     *
     * @param id a pipeconf identifier
     * @return an optional pipeconf
     */
    Optional<PiPipeconf> getPipeconf(PiPipeconfId id);

    /**
     * Binds the given pipeconf to the given infrastructure device. As a result of this method call,
     * if the given pipeconf exposes any pipeline-specific behaviours, those will be merged to the
     * device's driver.
     *
     * @param deviceId a device identifier
     * @param pipeconf a pipeconf identifier
     */
    // TODO: This service doesn't make any effort in deploying the configuration to the device.
    // Someone else should do that.
    void bindToDevice(PiPipeconfId pipeconf, DeviceId deviceId);

    /**
     * Returns the pipeconf identifier currently associated with the given device identifier, if
     * present. If not present, it means no pipeconf has been associated with that device so far.
     *
     * @param deviceId device identifier
     * @return an optional pipeconf identifier
     */
    Optional<PiPipeconfId> ofDevice(DeviceId deviceId);
}
