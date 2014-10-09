package org.onlab.onos.net.intent.impl;

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
