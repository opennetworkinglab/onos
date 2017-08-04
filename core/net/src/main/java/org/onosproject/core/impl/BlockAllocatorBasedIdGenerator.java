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

import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.core.IdBlock;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.UnavailableIdException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class of {@link IdGenerator} implementations which use {@link IdBlockAllocator} as
 * backend.
 */
public class BlockAllocatorBasedIdGenerator implements IdGenerator {
    protected final IdBlockAllocator allocator;
    protected IdBlock idBlock;
    protected AtomicBoolean initialized;


    /**
     * Constructs an ID generator which use {@link IdBlockAllocator} as backend.
     *
     * @param allocator the ID block allocator to use
     */
    protected BlockAllocatorBasedIdGenerator(IdBlockAllocator allocator) {
        this.allocator = checkNotNull(allocator, "allocator cannot be null");
        this.initialized = new AtomicBoolean(false);
    }

    @Override
    public long getNewId() {
        try {
            if (!initialized.get()) {
                synchronized (allocator) {
                    if (!initialized.get()) {
                        idBlock = allocator.allocateUniqueIdBlock();
                        initialized.set(true);
                    }
                }
            }
            return idBlock.getNextId();
        } catch (UnavailableIdException e) {
            synchronized (allocator) {
                idBlock = allocator.allocateUniqueIdBlock();
            }
            return idBlock.getNextId();
        }
    }
}
