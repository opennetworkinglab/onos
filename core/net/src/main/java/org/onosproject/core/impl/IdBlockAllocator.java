/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.core.impl;

import org.onosproject.core.IdBlock;

/**
 * An interface that gives unique ID spaces.
 */
public interface IdBlockAllocator {
    /**
     * Allocates a unique Id Block.
     *
     * @return Id Block.
     */
    IdBlock allocateUniqueIdBlock();

    /**
     * Allocates next unique id and retrieve a new range of ids if needed.
     *
     * @param range range to use for the identifier
     * @return Id Block.
     */
    IdBlock allocateUniqueIdBlock(long range);
}
