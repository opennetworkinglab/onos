/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;
import java.util.function.Consumer;

import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.DistributedQueue;
import org.onosproject.store.service.DistributedQueueBuilder;
import org.onosproject.store.service.Serializer;

import com.google.common.base.Charsets;

/**
 * Default implementation of a {@code DistributedQueueBuilder}.
 *
 * @param <E> queue entry type
 */
public class DefaultDistributedQueueBuilder<E> implements DistributedQueueBuilder<E> {

    private Serializer serializer;
    private String name;
    private boolean persistenceEnabled = true;
    private final DatabaseManager databaseManager;

    public DefaultDistributedQueueBuilder(
            DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public DistributedQueueBuilder<E> withName(String name) {
        checkArgument(name != null && !name.isEmpty());
        this.name = name;
        return this;
    }

    @Override
    public DistributedQueueBuilder<E> withSerializer(Serializer serializer) {
        checkArgument(serializer != null);
        this.serializer = serializer;
        return this;
    }

    @Override
    public DistributedQueueBuilder<E> withPersistenceDisabled() {
        persistenceEnabled = false;
        return this;
    }

    private boolean validInputs() {
        return name != null && serializer != null;
    }

    @Override
    public DistributedQueue<E> build() {
        checkState(validInputs());
        Consumer<Set<NodeId>> notifyOthers = nodes -> databaseManager.clusterCommunicator.multicast(name,
                        DatabaseManager.QUEUE_UPDATED_TOPIC,
                        s -> s.getBytes(Charsets.UTF_8),
                        nodes);
        DefaultDistributedQueue<E> queue = new DefaultDistributedQueue<>(
                name,
                persistenceEnabled ? databaseManager.partitionedDatabase : databaseManager.inMemoryDatabase,
                serializer,
                databaseManager.localNodeId,
                notifyOthers);
        databaseManager.registerQueue(queue);
        return queue;
    }
}
