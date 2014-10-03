package org.onlab.onos.net.intent.impl;

import org.onlab.onos.net.intent.IntentId;

/**
 * An implementation of {@link net.onrc.onos.core.util.IdGenerator} of intent ID,
 * which uses {@link IdBlockAllocator}.
 */
public class IdBlockAllocatorBasedIntentIdGenerator extends AbstractBlockAllocatorBasedIdGenerator<IntentId> {

    /**
     * Constructs an intent ID generator, which uses the specified ID block allocator
     * to generate a global unique intent ID.
     *
     * @param allocator the ID block allocator to use for generating intent IDs
     */
    public IdBlockAllocatorBasedIntentIdGenerator(IdBlockAllocator allocator) {
        super(allocator);
    }

    @Override
    protected IntentId convertFrom(long value) {
        return new IntentId(value);
    }
}
