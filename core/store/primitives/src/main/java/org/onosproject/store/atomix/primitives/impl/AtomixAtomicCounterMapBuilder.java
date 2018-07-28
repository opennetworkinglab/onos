/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.atomix.primitives.impl;

import io.atomix.core.Atomix;
import io.atomix.primitive.Recovery;
import io.atomix.protocols.raft.MultiRaftProtocol;
import org.onosproject.store.service.AsyncAtomicCounterMap;
import org.onosproject.store.service.AtomicCounterMap;
import org.onosproject.store.service.AtomicCounterMapBuilder;

/**
 * Default {@link org.onosproject.store.service.AsyncAtomicCounterMap} builder.
 *
 * @param <K> type for tree value
 */
public class AtomixAtomicCounterMapBuilder<K> extends AtomicCounterMapBuilder<K> {
    private static final int MAX_RETRIES = 5;
    private final Atomix atomix;
    private final String group;

    public AtomixAtomicCounterMapBuilder(Atomix atomix, String group) {
        this.atomix = atomix;
        this.group = group;
    }

    @Override
    public AtomicCounterMap<K> build() {
        return buildAsyncMap().asAtomicCounterMap();
    }

    @Override
    public AsyncAtomicCounterMap<K> buildAsyncMap() {
        return new AtomixAtomicCounterMap<K>(atomix.<K>atomicCounterMapBuilder(name())
            .withProtocol(MultiRaftProtocol.builder(group)
                .withRecoveryStrategy(Recovery.RECOVER)
                .withMaxRetries(MAX_RETRIES)
                .build())
            .withReadOnly(readOnly())
            .withSerializer(new AtomixSerializerAdapter(serializer()))
            .build()
            .async());
    }
}