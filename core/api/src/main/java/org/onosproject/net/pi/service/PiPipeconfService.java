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

package org.onosproject.net.pi.service;

import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;

import java.util.Optional;

/**
 * A service to manage the configurations of protocol-independent pipelines.
 */
@Beta
public interface PiPipeconfService extends ListenerService<PiPipeconfEvent, PiPipeconfListener> {

    // TODO: we might want to extend ListenerService to support the broadcasting of PipeconfEvent.

    /**
     * Registers the given pipeconf making it available to other subsystems.
     *
     * @param pipeconf a pipeconf
     * @throws IllegalStateException if the same pipeconf identifier is already
     *                               registered.
     */
    void register(PiPipeconf pipeconf) throws IllegalStateException;

    /**
     * Unregisters the given pipeconf. Once unregistered, other subsystems will
     * not be able to access the pipeconf content.
     *
     * @param pipeconfId a pipeconfId
     * @throws IllegalStateException if the same pipeconf identifier is already
     *                               registered.
     */
    void unregister(PiPipeconfId pipeconfId) throws IllegalStateException;

    /**
     * Returns all pipeconfs registered.
     *
     * @return a collection of pipeconfs
     */
    Iterable<PiPipeconf> getPipeconfs();

    /**
     * Returns the pipeconf instance associated with the given identifier, if
     * present. If not present, it means that no pipeconf with such identifier
     * has been registered so far.
     *
     * @param id a pipeconf identifier
     * @return an optional pipeconf
     */
    Optional<PiPipeconf> getPipeconf(PiPipeconfId id);

    /**
     * Returns the pipeconf instance associated with the given device, if
     * present. If not present, it means no pipeconf has been associated with
     * that device so far.
     *
     * @param deviceId a device identifier
     * @return an optional pipeconf
     */
    Optional<PiPipeconf> getPipeconf(DeviceId deviceId);

    /**
     * Signals that the given pipeconf is associated to the given infrastructure
     * device. As a result of this method, the pipeconf for the given device can
     * be later retrieved using {@link #ofDevice(DeviceId)}
     *
     * @param pipeconfId a pipeconf identifier
     * @param deviceId   a device identifier
     */
    void bindToDevice(PiPipeconfId pipeconfId, DeviceId deviceId);

    /**
     * Returns the name of a driver that is equivalent to the base driver of the
     * given device plus all the pipeline-specific behaviors exposed by the
     * given pipeconf (previously registered using {@link
     * #register(PiPipeconf)}). If such driver does not exist, this method
     * creates one and registers is with all necessary ONOS subsystems, such
     * that the returned name can be used to retrieve the driver instance using
     * {@link org.onosproject.net.driver.DriverService#getDriver(String)}.
     * <p>
     * This method needs to be called on all nodes of the cluster that wants to
     * use such merged driver.
     * <p>
     * Returns null if such merged driver cannot be created.
     *
     * @param deviceId   a device identifier
     * @param pipeconfId a pipeconf identifier
     * @return driver name or null.
     */
    String getMergedDriver(DeviceId deviceId, PiPipeconfId pipeconfId);

    /**
     * Returns the pipeconf identifier currently associated with the given
     * device identifier, if present. If not present, it means no pipeconf has
     * been associated with that device so far.
     *
     * @param deviceId device identifier
     * @return an optional pipeconf identifier
     * @deprecated in ONOS 2.1 use {@link #getPipeconf(DeviceId)} instead
     */
    @Deprecated
    Optional<PiPipeconfId> ofDevice(DeviceId deviceId);

}
