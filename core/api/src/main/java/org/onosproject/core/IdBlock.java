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
package org.onosproject.core;

import com.google.common.base.MoreObjects;

import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A class representing an ID space.
 */
public final class IdBlock {
    private final long start;
    private final long size;

    private final AtomicLong currentId;

    /**
     * Constructs a new ID block with the specified size and initial value.
     *
     * @param start initial value of the block
     * @param size size of the block
     * @throws IllegalArgumentException if the size is less than or equal to 0
     */
    public IdBlock(long start, long size) {
        checkArgument(size > 0, "size should be more than 0, but %s", size);

        this.start = start;
        this.size = size;

        this.currentId = new AtomicLong(start);
    }

    /**
     * Returns the initial value.
     *
     * @return initial value
     */
    private long getStart() {
        return start;
    }

    /**
     * Returns the last value.
     *
     * @return last value
     */
    private long getEnd() {
        return start + size - 1;
    }

    /**
     * Returns the block size.
     *
     * @return block size
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the next ID in the block.
     *
     * @return next ID
     * @throws UnavailableIdException if there is no available ID in the block.
     */
    public long getNextId() {
        final long id = currentId.getAndIncrement();
        if (id > getEnd()) {
            throw new UnavailableIdException(String.format(
                    "used all IDs in allocated space (size: %d, end: %d, current: %d)",
                    size, getEnd(), id
            ));
        }

        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("start", start)
                .add("size", size)
                .add("currentId", currentId)
                .toString();
    }
}
