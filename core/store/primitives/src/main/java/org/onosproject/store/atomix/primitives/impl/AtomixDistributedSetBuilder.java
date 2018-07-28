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
import org.onosproject.store.service.AsyncDistributedSet;
import org.onosproject.store.service.DistributedSetBuilder;

/**
 * Default {@link org.onosproject.store.service.AsyncDistributedSet} builder.
 *
 * @param <E> type for set value
 */
public class AtomixDistributedSetBuilder<E> extends DistributedSetBuilder<E> {
    private static final int MAX_RETRIES = 5;
    private final Atomix atomix;
    private final String group;

    public AtomixDistributedSetBuilder(Atomix atomix, String group) {
        this.atomix = atomix;
        this.group = group;
    }

    @Override
    public AsyncDistributedSet<E> build() {
        return new AtomixDistributedSet<E>(atomix.<E>setBuilder(name())
            .withRegistrationRequired()
            .withProtocol(MultiRaftProtocol.builder(group)
                .withRecoveryStrategy(Recovery.RECOVER)
                .withMaxRetries(MAX_RETRIES)
                .build())
            .withReadOnly(readOnly())
            // TODO: Enable caching for DistributedSet in Atomix
            .withSerializer(new AtomixSerializerAdapter(serializer()))
            .build()
            .async());
    }
}