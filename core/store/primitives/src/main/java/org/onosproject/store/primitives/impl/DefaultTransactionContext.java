/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.CommitStatus;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;
import org.onosproject.utils.MeteringAgent;

import com.google.common.collect.Sets;

/**
 * Default implementation of transaction context.
 */
public class DefaultTransactionContext implements TransactionContext {

    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final DistributedPrimitiveCreator creator;
    private final TransactionId transactionId;
    private final TransactionCoordinator transactionCoordinator;
    private final Set<TransactionParticipant> txParticipants = Sets.newConcurrentHashSet();
    private final MeteringAgent monitor;

    public DefaultTransactionContext(TransactionId transactionId,
            DistributedPrimitiveCreator creator,
            TransactionCoordinator transactionCoordinator) {
        this.transactionId = transactionId;
        this.creator = creator;
        this.transactionCoordinator = transactionCoordinator;
        this.monitor = new MeteringAgent("transactionContext", "*", true);
    }

    @Override
    public String name() {
        return transactionId.toString();
    }

    @Override
    public TransactionId transactionId() {
        return transactionId;
    }

    @Override
    public boolean isOpen() {
        return isOpen.get();
    }

    @Override
    public void begin() {
        if (!isOpen.compareAndSet(false, true)) {
            throw new IllegalStateException("TransactionContext is already open");
        }
    }

    @Override
    public CompletableFuture<CommitStatus> commit() {
        final MeteringAgent.Context timer = monitor.startTimer("commit");
        return transactionCoordinator.commit(transactionId, txParticipants)
                                     .whenComplete((r, e) -> timer.stop(e));
    }

    @Override
    public void abort() {
        isOpen.set(false);
    }

    @Override
    public <K, V> TransactionalMap<K, V> getTransactionalMap(String mapName,
            Serializer serializer) {
        // FIXME: Do not create duplicates.
        DefaultTransactionalMap<K, V> txMap = new DefaultTransactionalMap<K, V>(mapName,
                DistributedPrimitives.newMeteredMap(creator.<K, V>newAsyncConsistentMap(mapName, serializer)),
                this,
                serializer);
        txParticipants.add(txMap);
        return txMap;
    }
}