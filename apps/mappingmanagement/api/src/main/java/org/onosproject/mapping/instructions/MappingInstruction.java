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
package org.onosproject.mapping.instructions;

/**
 * Presentation of a single mapping instruction.
 */
public interface MappingInstruction {

    /**
     * Represents the type of mapping instruction.
     */
    enum Type {

        /**
         * Signifies that the traffic should be uni-casted with TE parameters.
         */
        UNICAST,

        /**
         * Signifies that the traffic should be multi-casted with TE parameters.
         */
        MULTICAST,

        /**
         * Signifies that an extension instruction will be used.
         */
        EXTENSION
    }

    /**
     * Returns the type of mapping instruction.
     *
     * @return type of mapping instruction
     */
    Type type();
}
