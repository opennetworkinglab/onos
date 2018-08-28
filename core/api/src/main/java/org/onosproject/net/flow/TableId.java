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

package org.onosproject.net.flow;

/**
 * Match+action table identifier.
 */
public interface TableId {

    /**
     * Types of table identifier.
     */
    enum Type {
        /**
         * Signifies that the table identifier corresponds to the position of the table in the pipeline.
         */
        INDEX,

        /**
         * Signifies that the table identifier is pipeline-independent.
         */
        PIPELINE_INDEPENDENT
    }

    /**
     * Gets type of this table ID.
     *
     * @return type
     */
    Type type();

    /**
     * Compares table ID.
     *
     * @param other table ID to be compared
     * @return zero if the table IDs are the same. Otherwise, return a non-zero integer
     */
    int compareTo(TableId other);
}
