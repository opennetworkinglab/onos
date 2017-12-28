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
 *
 */

package org.onosproject.drivers.bmv2.mirror;

import org.onosproject.drivers.bmv2.api.runtime.Bmv2Entity;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2Handle;
import org.onosproject.net.DeviceId;

import java.util.Collection;

/**
 * Mirror of entities installed on a BMv2 device.
 *
 * @param <H> Handle class
 * @param <E> Entity class
 */
public interface Bmv2Mirror<H extends Bmv2Handle, E extends Bmv2Entity> {

    /**
     * Returns all entries for the given device ID.
     *
     * @param deviceId device ID
     * @return collection of BMv2 entries
     */
    Collection<E> getAll(DeviceId deviceId);

    /**
     * Returns entry associated to the given handle, if present, otherwise
     * null.
     *
     * @param handle handle
     * @return BMv2 entry
     */
    E get(H handle);

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
}
