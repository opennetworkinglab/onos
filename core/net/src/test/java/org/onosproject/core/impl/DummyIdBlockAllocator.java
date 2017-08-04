/*
 * Copyright 2014-present Open Networking Foundation
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

public class DummyIdBlockAllocator implements IdBlockAllocator {
    private long blockTop;
    private static final long BLOCK_SIZE = 0x1000000L;

    /**
     * Returns a block of IDs which are unique and unused.
     * Range of IDs is fixed size and is assigned incrementally as this method
     * called.
     *
     * @return an IdBlock containing a set of unique IDs
     */
    @Override
    public IdBlock allocateUniqueIdBlock() {
        synchronized (this)  {
            long blockHead = blockTop;
            long blockTail = blockTop + BLOCK_SIZE;

            IdBlock block = new IdBlock(blockHead, BLOCK_SIZE);
            blockTop = blockTail;

            return block;
        }
    }

    @Override
    public IdBlock allocateUniqueIdBlock(long range) {
        throw new UnsupportedOperationException("Not supported yet");
    }
}
