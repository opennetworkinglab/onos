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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.atomix.core.transaction.AsyncTransaction;
import io.atomix.primitive.Recovery;
import io.atomix.protocols.raft.MultiRaftProtocol;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.CommitStatus;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionalMap;

/**
 * Atomix transaction context.
 */
public class AtomixTransactionContext implements TransactionContext {
    private static final int MAX_RETRIES = 5;

    private final AsyncTransaction atomixTransaction;
    private final String group;

    public AtomixTransactionContext(AsyncTransaction atomixTransaction, String group) {
        this.atomixTransaction = atomixTransaction;
        this.group = group;
    }

    @Override
    public String name() {
        return atomixTransaction.name();
    }

    @Override
    public TransactionId transactionId() {
        return TransactionId.from(atomixTransaction.transactionId().id());
    }

    @Override
    public boolean isOpen() {
        return atomixTransaction.isOpen();
    }

    @Override
    public void begin() {
        try {
            atomixTransaction.begin().get(DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new StorageException.Interrupted();
        } catch (TimeoutException e) {
            throw new StorageException.Timeout();
        } catch (ExecutionException e) {
            throw new StorageException.Unavailable();
        }
    }

    @Override
    public CompletableFuture<CommitStatus> commit() {
        return atomixTransaction.commit().thenApply(status -> CommitStatus.valueOf(status.name()));
    }

    @Override
    public void abort() {
        try {
            atomixTransaction.abort().get(DistributedPrimitive.DEFAULT_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new StorageException.Interrupted();
        } catch (TimeoutException e) {
            throw new StorageException.Timeout();
        } catch (ExecutionException e) {
            throw new StorageException.Unavailable();
        }
    }

    @Override
    public <K, V> TransactionalMap<K, V> getTransactionalMap(String mapName, Serializer serializer) {
        return new AtomixTransactionalMap<>(atomixTransaction.<K, V>mapBuilder(mapName)
            .withProtocol(MultiRaftProtocol.builder(group)
                .withRecoveryStrategy(Recovery.RECOVER)
                .withMaxRetries(MAX_RETRIES)
                .build())
            .withSerializer(new AtomixSerializerAdapter(serializer))
            .get());
    }
}
