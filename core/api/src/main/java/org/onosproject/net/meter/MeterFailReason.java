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

/**
 * Enum used to represent a meter failure condition.
 */
public enum MeterFailReason {
    /**
     * A meter with the same identifier already exists.
     * Essentially a duplicate meter exists.
     */
    EXISTING_METER,

    /**
     * The device does not support any more meters.
     */
    OUT_OF_METERS,

    /**
     * The device does not support any more bands for this meter.
     */
    OUT_OF_BANDS,

    /**
     * The meter that was attempted to be modified is unknown.
     */
    UNKNOWN,

    /**
     * The operation for this meter installation timed out.
     */
    TIMEOUT,

    /**
     * Invalid meter definition.
     */
    INVALID_METER,

    /**
     * The target device is unknown.
     */
    UNKNOWN_DEVICE,

    /**
     * Unknown command.
     */
    UNKNOWN_COMMAND,

    /**
     * Unknown flags.
     */
    UNKNOWN_FLAGS,

    /**
     * Bad rate value.
     */
    BAD_RATE,

    /**
     * Bad burst size value.
     */
    BAD_BURST,

    /**
     * Bad band.
     */
    BAD_BAND,

    /**
     * Bad value value.
     */
    BAD_BAND_VALUE


}
