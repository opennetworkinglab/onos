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

package org.onosproject.drivers.p4runtime.mirror;

import com.google.common.annotations.Beta;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient;

import java.util.Collection;

/**
 * Service to keep track of the device state for a given class of PI entities.
 * The need of this service comes from the fact that P4 Runtime makes a
 * distinction between INSERT and MODIFY operations, while ONOS drivers use a
 * more generic "APPLY" behaviour (i.e. ADD or UPDATE). When applying an entry,
 * we need to know if another one with the same handle (e.g. table entry with
 * same match key) is already on the device to decide between INSERT or MODIFY.
 * Moreover, this service maintains a "timed" version of PI entities such that
 * we can compute the life of the entity on the device.
 *
 * @param <H> Handle class
 * @param <E> Entity class
 */
@Beta
public interface P4RuntimeMirror
        <H extends PiHandle, E extends PiEntity> {

    /**
     * Returns all entries for the given device ID.
     *
     * @param deviceId device ID
     * @return collection of table entries
     */
    Collection<TimedEntry<E>> getAll(DeviceId deviceId);

    /**
     * Returns entry associated to the given handle, if present, otherwise
     * null.
     *
     * @param handle handle
     * @return PI table entry
     */
    TimedEntry<E> get(H handle);

    /**
     * Stores the given entry associating it to the given handle.
     *
     * @param handle handle
     * @param entry  entry
     */
    void put(H handle, E entry);

    /**
     * Removes the entry associated to the given handle.
     *
     * @param handle handle
     */
    void remove(H handle);

    /**
     * Stores the given annotations associating it to the given handle.
     *
     * @param handle      handle
     * @param annotations entry
     */
    void putAnnotations(H handle, Annotations annotations);

    /**
     * Returns annotations associated to the given handle, if present, otherwise
     * null.
     *
     * @param handle handle
     * @return PI table annotations
     */
    Annotations annotations(H handle);

    /**
     * Synchronizes the state of the given device ID with the given collection
     * of PI entities.
     *
     * @param deviceId device ID
     * @param entities collection of PI entities
     */
    void sync(DeviceId deviceId, Collection<E> entities);

    /**
     * Uses the given P4Runtime write request to update the state of this
     * mirror by optimistically assuming that all updates in it will succeed.
     *
     * @param request P4Runtime write request
     */
    void applyWriteRequest(P4RuntimeWriteClient.WriteRequest request);

    /**
     * Uses the given P4Runtime write response to update the state of this
     * mirror.
     *
     * @param response P4Runtime write response
     */
    void applyWriteResponse(P4RuntimeWriteClient.WriteResponse response);
}
