package org.onosproject.core.impl;

import org.onosproject.core.IdBlock;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.UnavailableIdException;

/**
 * Base class of {@link IdGenerator} implementations which use {@link IdBlockAllocator} as
 * backend.
 */
public class BlockAllocatorBasedIdGenerator implements IdGenerator {
    protected final IdBlockAllocator allocator;
    protected IdBlock idBlock;

    /**
     * Constructs an ID generator which use {@link IdBlockAllocator} as backend.
     *
     * @param allocator the ID block allocator to use
     */
    protected BlockAllocatorBasedIdGenerator(IdBlockAllocator allocator) {
        this.allocator = allocator;
        this.idBlock = allocator.allocateUniqueIdBlock();
    }

    @Override
    public long getNewId() {
        try {
            return idBlock.getNextId();
        } catch (UnavailableIdException e) {
            synchronized (allocator) {
                idBlock = allocator.allocateUniqueIdBlock();
                return idBlock.getNextId();
            }
        }
    }
}
