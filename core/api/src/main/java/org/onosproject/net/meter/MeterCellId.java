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

package org.onosproject.net.meter;

import com.google.common.annotations.Beta;

/**
 * A representation of a meter cell identifier.
 * Uniquely identifies a meter cell in the scope of a single device.
 */
@Beta
public interface MeterCellId {

    /**
     * Types of meter cell identifier.
     */
    enum MeterCellType {
        /**
         * Signifies that the meter cell can be identified with an integer index.
         * Valid for pipelines that defines one global meter table, e.g. as with
         * OpenFlow meters.
         */
        INDEX,

        /**
         * Signifies that the meter cell identifier is pipeline-independent.
         */
        PIPELINE_INDEPENDENT
    }

    /**
     * Return the type of this meter cell identifier.
     *
     * @return type
     */
    MeterCellType type();
}
