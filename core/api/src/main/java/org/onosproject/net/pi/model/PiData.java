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
 */

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

/**
 * Representation of data that can be used for runtime operations of a protocol-independent pipeline.
 */
@Beta
public interface PiData {
    /**
     * Types of data in a protocol-independent pipeline.
     */
    enum Type {
        /**
         * Bit String.
         */
        BITSTRING,

        /**
         * Bool.
         */
        BOOL,

        /**
         * Tuple.
         */
        TUPLE,

        /**
         * Struct.
         */
        STRUCT,

        /**
         * Header.
         */
        HEADER,

        /**
         * Header Stack.
         */
        HEADERSTACK,

        /**
         * Header Union.
         */
        HEADERUNION,

        /**
         * Header Union Stack.
         */
        HEADERUNIONSTACK,

        /**
         * Enum String.
         */
        ENUMSTRING,

        /**
         * Error String.
         */
        ERRORSTRING
    }

    /**
     * Returns the type of this protocol-independent data.
     * @return the type of this instance
     */
    Type type();
}
