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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

/**
 * Model of a meter in a protocol-independent pipeline.
 */
@Beta
public interface PiMeterModel {

    /**
     * Meter rate unit.
     */
    enum Unit {
        /**
         * Measures rate of bytes.
         */
        BYTES,
        /**
         * Measures rate of packets.
         */
        PACKETS
    }

    /**
     * Returns the ID of this meter.
     *
     * @return meter ID
     */
    PiMeterId id();

    /**
     * Returns the type of this meter.
     *
     * @return meter type
     */
    PiMeterType meterType();

    /**
     * Returns the unit of this meter.
     *
     * @return unit
     */
    Unit unit();

    /**
     * Returns the table model associated with this meter. Meaningful only if the meter type is {@link
     * PiMeterType#DIRECT}.
     *
     * @return table model
     */
    PiTableId table();

    /**
     * Returns the number of cells of this meter. Meaningful only if the meter type is {@link PiMeterType#INDIRECT}.
     *
     * @return size
     */
    long size();
}
