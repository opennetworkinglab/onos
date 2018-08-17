/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.trivial;

import org.onosproject.core.IdBlock;
import org.onosproject.core.IdBlockStore;
import org.osgi.service.component.annotations.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple implementation of id block store.
 */
@Component(immediate = true, service = IdBlockStore.class)
public class SimpleIdBlockStore implements IdBlockStore {

    private static final long DEFAULT_BLOCK_SIZE = 0x1000L;

    private final Map<String, AtomicLong> topicBlocks = new ConcurrentHashMap<>();

    @Override
    public synchronized IdBlock getIdBlock(String topic) {
        AtomicLong blockGenerator = topicBlocks.get(topic);
        if (blockGenerator == null) {
            blockGenerator = new AtomicLong(0);
            topicBlocks.put(topic, blockGenerator);
        }
        Long blockBase = blockGenerator.getAndAdd(DEFAULT_BLOCK_SIZE);
        return new IdBlock(blockBase, DEFAULT_BLOCK_SIZE);
    }
}
