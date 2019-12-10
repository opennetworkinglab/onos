/*
 * Copyright 2019-present Open Networking Foundation
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
 * limitations under the License.%
 */

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import java.util.NoSuchElementException;
import java.util.Queue;

import static org.onosproject.net.behaviour.BngProgrammable.Attachment;

/**
 * Trivial, thread-safe, non-distributed implementation of {@link
 * FabricBngLineIdAllocator}.
 */
final class SimpleBngLineIdAllocator implements FabricBngLineIdAllocator {

    private final long size;
    private final BiMap<Handle, Long> allocated = HashBiMap.create();
    private final Queue<Long> released = Lists.newLinkedList();

    /**
     * Creates a new allocator that can allocate and hold up to the given number
     * of IDs.
     *
     * @param size maximum number of IDs to allocate.
     */
    SimpleBngLineIdAllocator(long size) {
        this.size = size;
        for (long i = 0; i < size; i++) {
            released.add(i);
        }
    }

    @Override
    public long allocate(Attachment attachment) throws IdExhaustedException {
        final Handle handle = new Handle(attachment);
        synchronized (this) {
            if (allocated.containsKey(handle)) {
                return allocated.get(handle);
            }
            try {
                final long id = released.remove();
                allocated.put(handle, id);
                return id;
            } catch (NoSuchElementException e) {
                // All IDs are taken;
                throw new IdExhaustedException();
            }
        }
    }

    @Override
    public void release(Attachment attachment) {
        final Handle handle = new Handle(attachment);
        synchronized (this) {
            Long id = allocated.remove(handle);
            if (id != null) {
                released.add(id);
            }
        }
    }

    @Override
    public void release(long id) {
        synchronized (this) {
            if (allocated.inverse().containsKey(id)) {
                allocated.inverse().remove(id);
                released.add(id);
            }
        }
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public long freeCount() {
        return released.size();
    }

    @Override
    public long allocatedCount() {
        return allocated.size();
    }
}
