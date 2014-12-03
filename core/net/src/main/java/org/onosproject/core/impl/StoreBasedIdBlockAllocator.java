package org.onosproject.core.impl;

import org.onosproject.core.IdBlock;
import org.onosproject.core.IdBlockStore;

public class StoreBasedIdBlockAllocator implements IdBlockAllocator {
    private final IdBlockStore store;
    private final String topic;

    public StoreBasedIdBlockAllocator(String topic, IdBlockStore store) {
        this.topic = topic;
        this.store = store;
    }

    /**
     * Returns a block of IDs which are unique and unused.
     * Range of IDs is fixed size and is assigned incrementally as this method
     * called.
     *
     * @return an IdBlock containing a set of unique IDs
     */
    @Override
    public synchronized IdBlock allocateUniqueIdBlock() {
        return store.getIdBlock(topic);
    }

    @Override
    public IdBlock allocateUniqueIdBlock(long range) {
        throw new UnsupportedOperationException("Not supported yet");
    }
}
