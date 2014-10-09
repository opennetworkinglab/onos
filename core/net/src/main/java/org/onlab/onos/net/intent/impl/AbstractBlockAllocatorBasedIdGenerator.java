package org.onlab.onos.net.intent.impl;

import org.onlab.onos.net.intent.IdGenerator;

/**
 * Base class of {@link IdGenerator} implementations which use {@link IdBlockAllocator} as
 * backend.
 *
 * @param <T> the type of ID
 */
public abstract class AbstractBlockAllocatorBasedIdGenerator<T> implements IdGenerator<T> {
    protected final IdBlockAllocator allocator;
    protected IdBlock idBlock;

    /**
     * Constructs an ID generator which use {@link IdBlockAllocator} as backend.
     *
     * @param allocator
     */
    protected AbstractBlockAllocatorBasedIdGenerator(IdBlockAllocator allocator) {
        this.allocator = allocator;
        this.idBlock = allocator.allocateUniqueIdBlock();
    }

    @Override
    public synchronized T getNewId() {
        try {
            return convertFrom(idBlock.getNextId());
        } catch (UnavailableIdException e) {
            idBlock = allocator.allocateUniqueIdBlock();
            return convertFrom(idBlock.getNextId());
        }
    }

    /**
     * Returns an ID instance of {@code T} type from the long value.
     *
     * @param value original long value
     * @return ID instance
     */
    protected abstract T convertFrom(long value);
}
