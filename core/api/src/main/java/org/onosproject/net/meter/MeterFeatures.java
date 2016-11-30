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

import org.onosproject.net.DeviceId;

import java.util.Set;

/**
 * Meter Features of a device.
 */
public interface MeterFeatures {

    /**
     * Return the device id to which this meter features apply.
     *
     * @return the device id
     */
    DeviceId deviceId();

    /**
     * Returns the maximum number of meters accepted by the device.
     *
     * @return the maximum meter value.
     */
    long maxMeter();

    /**
     * Returns band types supported.
     *
     * @return the band types supported.
     */
    Set<Band.Type> bandTypes();

    /**
     * Returns unit types available for meters.
     *
     * @return the unit types available.
     */
    Set<Meter.Unit> unitTypes();

    /**
     * Returns if burst size is available.
     *
     * @return burst availability
     */
    boolean isBurstSupported();

    /**
     * Returns if statistics collection is available.
     *
     * @return statistics availability
     */
    boolean isStatsSupported();

    /**
     * Returns the maximum bands per meter.
     *
     * @return the max bands value
     */
    short maxBands();

    /**
     * Returns the maximum colors value for DiffServ operation.
     *
     * @return the maximum colors value.
     */
    short maxColor();

    /**
     * A meter features builder.
     */
    interface Builder {
        /**
         * Assigns the target device for this meter features.
         *
         * @param deviceId a device id
         * @return this builder
         */
        Builder forDevice(DeviceId deviceId);

        /**
         * Assigns the max meters value for this meter features.
         *
         * @param maxMeter the maximum meters available
         * @return this builder
         */
        Builder withMaxMeters(long maxMeter);

        /**
         * Assigns the max bands value for this meter features.
         *
         * @param maxBands the maximum bands available.
         * @return this builder
         */
        Builder withMaxBands(short maxBands);

        /**
         * Assigns the max colors value for this meter features.
         *
         * @param maxColors the maximum colors available.
         * @return this builder
         */
        Builder withMaxColors(short maxColors);

        /**
         * Assigns the band types for this meter features.
         *
         * @param types the band types available.
         * @return this builder
         */
        Builder withBandTypes(Set<Band.Type> types);

        /**
         * Assigns the capabilities for this meter features.
         *
         * @param units the units available
         * @return this
         */
        Builder withUnits(Set<Meter.Unit> units);

        /**
         * Assigns the burst capabilities.
         *
         * @param hasBurst if the burst is supported
         * @return this builder
         */
        Builder hasBurst(boolean hasBurst);

        /**
         * Assigns the stats capabilities.
         *
         * @param hasStats if the statistics are supported
         * @return this builder
         */
        Builder hasStats(boolean hasStats);

        /**
         * Builds the Meter Features based on the specified parameters.
         *
         * @return the meter features
         */
        MeterFeatures build();
    }
}
