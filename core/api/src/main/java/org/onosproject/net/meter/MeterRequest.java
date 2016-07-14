/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.meter;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents a generalized meter request to be deployed on a device.
 */
public interface MeterRequest {

    enum Type {
        ADD,
        MODIFY,
        REMOVE
    }

    /**
     * The target device for this meter.
     *
     * @return a device id
     */
    DeviceId deviceId();

    /**
     * The id of the application which created this meter.
     *
     * @return an application id
     */
    ApplicationId appId();

    /**
     * The unit used within this meter.
     *
     * @return the unit
     */
    Meter.Unit unit();

    /**
     * Signals whether this meter applies to bursts only.
     *
     * @return a boolean
     */
    boolean isBurst();

    /**
     * The collection of bands to apply on the dataplane.
     *
     * @return a collection of bands.
     */
    Collection<Band> bands();

    /**
     * Returns the callback context for this meter.
     *
     * @return an optional meter context
     */
    Optional<MeterContext> context();

    /**
     * A meter builder.
     */
    interface Builder {

        /**
         * Assigns the target device for this meter.
         *
         * @param deviceId a device id
         * @return this
         */
        Builder forDevice(DeviceId deviceId);

        /**
         * Assigns the application that built this meter.
         *
         * @param appId an application id
         * @return this
         */
        Builder fromApp(ApplicationId appId);

        /**
         * Assigns the @See Unit to use for this meter.
         * Defaults to kb/s
         *
         * @param unit a unit
         * @return this
         */
        Builder withUnit(Meter.Unit unit);

        /**
         * Sets this meter as applicable to burst traffic only.
         * Defaults to false.
         *
         * @return this
         */
        Builder burst();

        /**
         * Assigns bands to this meter. There must be at least one band.
         *
         * @param bands a collection of bands
         * @return this
         */
        Builder withBands(Collection<Band> bands);

        /**
         * Assigns an execution context for this meter request.
         *
         * @param context a meter context
         * @return this
         */
        Builder withContext(MeterContext context);

        /**
         * Requests the addition of a meter.
         *
         * @return a meter request
         */
        MeterRequest add();

        /**
         * Requests the removal of a meter.
         *
         * @return a meter request
         */
        MeterRequest remove();

    }

}
