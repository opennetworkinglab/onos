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

package org.onosproject.drivers.bmv2.api.runtime;

import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global identifier of an entity applied to a BMv2 device.
 * @param <E> an object extending Bmv2Entity
 */
public abstract class Bmv2Handle<E extends Bmv2Entity> {

    private final DeviceId deviceId;
    private final E bmv2Entity;

    protected Bmv2Handle(DeviceId deviceId, E bmv2Entity) {
        this.deviceId = checkNotNull(deviceId);
        this.bmv2Entity = checkNotNull(bmv2Entity);
    }

    /**
     * Returns the device ID of this handle.
     *
     * @return device ID
     */
    public final DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the type of entity identified by this handle.
     *
     * @return BMv2 entity type
     */
    public final Bmv2EntityType entityType() {
        return bmv2Entity.entityType();
    }

    /**
     * The entity to which this handle is associated.
     *
     * @return Bmv2 entity
     */
    public final E bmv2Entity() {
        return bmv2Entity;
    }

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
