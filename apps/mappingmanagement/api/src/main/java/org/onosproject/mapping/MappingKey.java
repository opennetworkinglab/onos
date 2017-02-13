/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onosproject.mapping.addresses.MappingAddress;

/**
 * Abstraction of network mapping key.
 */
public interface MappingKey {

    /**
     * Returns a mapping address.
     *
     * @return a mapping address
     */
    MappingAddress address();

    interface Builder {

        /**
         * Specifies a mapping address.
         *
         * @param address mapping address
         * @return a mapping key builder
         */
        Builder withAddress(MappingAddress address);

        /**
         * Builds an immutable mapping key.
         *
         * @return a mapping key
         */
        MappingKey build();
    }
}
