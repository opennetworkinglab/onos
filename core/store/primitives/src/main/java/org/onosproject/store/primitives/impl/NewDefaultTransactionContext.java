/*
 * Copyright 2016 Open Networking Laboratory
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.store.primitives.DistributedPrimitiveCreator;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;

import com.google.common.collect.Sets;

/**
 * Default implementation of transaction context.
 */
public class NewDefaultTransactionContext implements TransactionContext {

    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final DistributedPrimitiveCreator creator;
    private final TransactionId transactionId;
    private final TransactionCoordinator transactionCoordinator;
    private final Set<TransactionParticipant> txParticipants = Sets.newConcurrentHashSet();

    public NewDefaultTransactionContext(TransactionId transactionId,
            DistributedPrimitiveCreator creator,
            TransactionCoordinator transactionCoordinator) {
        this.transactionId = transactionId;
        this.creator = creator;
        this.transactionCoordinator = transactionCoordinator;
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
    public boolean commit() {
        transactionCoordinator.commit(transactionId, txParticipants).getNow(null);
        return true;
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
                creator.<K, V>newAsyncConsistentMap(mapName, serializer),
                this,
                serializer);
        txParticipants.add(txMap);
        return txMap;
    }
}