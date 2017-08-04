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
package org.onosproject.mapping;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * Abstraction of mapping.
 */
public interface Mapping {

    /**
     * Obtains the identifier of this mapping.
     *
     * @return mapping identifier
     */
    MappingId id();

    /**
     * Obtains the application identifier of this mapping.
     *
     * @return an application identifier
     */
    short appId();

    /**
     * Obtains the identity of the device where this mapping applies.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Obtains the mapping key that is used for query the mapping entry.
     *
     * @return mapping key
     */
    MappingKey key();

    /**
     * Obtains the mapping value that is queried using the mapping key.
     *
     * @return mapping value
     */
    MappingValue value();

    /**
     * {@inheritDoc}
     * <p>
     * Equality for mappings only considers 'match equality'. This means that
     * two mappings with the same match conditions will be equal.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     */
    boolean equals(Object obj);

    /**
     * A mapping builder.
     */
    interface Builder {

        /**
         * Assigns an id value to this mapping.
         *
         * @param id a long value
         * @return this builder object
         */
        Builder withId(long id);

        /**
         * Assigns the application that built this mapping to this object.
         * The short value of the appId will be used as a basis for the
         * cookie value computation. It is expected that application use this
         * call to set their application id.
         *
         * @param appId an application identifier
         * @return this builder object
         */
        Builder fromApp(ApplicationId appId);

        /**
         * Sets the deviceId for this mapping.
         *
         * @param deviceId a device identifier
         * @return this builder object
         */
        Builder forDevice(DeviceId deviceId);

        /**
         * Sets the mapping key for this mapping.
         *
         * @param key mapping key
         * @return this builder object
         */
        Builder withKey(MappingKey key);

        /**
         * Sets the mapping value for this mapping.
         *
         * @param value mapping value
         * @return this builder object
         */
        Builder withValue(MappingValue value);

        /**
         * Builds a mapping object.
         *
         * @return a mapping object
         */
        Mapping build();
    }
}
