package org.onlab.onos.net.intent.impl;

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
