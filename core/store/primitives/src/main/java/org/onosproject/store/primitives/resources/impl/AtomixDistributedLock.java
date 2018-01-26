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
package org.onosproject.store.primitives.resources.impl;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.atomix.protocols.raft.proxy.RaftProxy;
import io.atomix.utils.concurrent.Futures;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDistributedLock;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.Version;

import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.LOCK;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.Lock;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.UNLOCK;
import static org.onosproject.store.primitives.resources.impl.AtomixDistributedLockOperations.Unlock;

/**
 * Atomix lock implementation.
 */
public class AtomixDistributedLock extends AbstractRaftPrimitive implements AsyncDistributedLock {
    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
        .register(KryoNamespaces.BASIC)
        .register(AtomixDistributedLockOperations.NAMESPACE)
        .register(AtomixDistributedLockEvents.NAMESPACE)
        .build());

    private final Map<Integer, CompletableFuture<Version>> futures = new ConcurrentHashMap<>();
    private final AtomicInteger id = new AtomicInteger();
    private int lock;

    public AtomixDistributedLock(RaftProxy proxy) {
        super(proxy);
        proxy.addStateChangeListener(this::handleStateChange);
        proxy.addEventListener(AtomixDistributedLockEvents.LOCK, SERIALIZER::decode, this::handleLocked);
        proxy.addEventListener(AtomixDistributedLockEvents.FAIL, SERIALIZER::decode, this::handleFailed);
    }

    private void handleLocked(LockEvent event) {
        CompletableFuture<Version> future = futures.remove(event.id());
        if (future != null) {
            this.lock = event.id();
            future.complete(new Version(event.version()));
        }
    }

    private void handleFailed(LockEvent event) {
        CompletableFuture<Version> future = futures.remove(event.id());
        if (future != null) {
            future.complete(null);
        }
    }

    private void handleStateChange(RaftProxy.State state) {
        if (state != RaftProxy.State.CONNECTED) {
            Iterator<Map.Entry<Integer, CompletableFuture<Version>>> iterator = futures.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, CompletableFuture<Version>> entry = iterator.next();
                entry.getValue().completeExceptionally(new StorageException.Unavailable());
                proxy.invoke(UNLOCK, SERIALIZER::encode, new Unlock(entry.getKey()));
                iterator.remove();
            }
            lock = 0;
        }
    }

    @Override
    public CompletableFuture<Version> lock() {
        RaftProxy.State state = proxy.getState();
        if (state != RaftProxy.State.CONNECTED) {
            return Futures.exceptionalFuture(new StorageException.Unavailable());
        }

        CompletableFuture<Version> future = new CompletableFuture<>();
        int id = this.id.incrementAndGet();
        futures.put(id, future);
        proxy.invoke(LOCK, SERIALIZER::encode, new Lock(id, -1)).whenComplete((result, error) -> {
            if (error != null) {
                futures.remove(id);
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Optional<Version>> tryLock() {
        RaftProxy.State state = proxy.getState();
        if (state != RaftProxy.State.CONNECTED) {
            return Futures.exceptionalFuture(new StorageException.Unavailable());
        }

        CompletableFuture<Version> future = new CompletableFuture<>();
        int id = this.id.incrementAndGet();
        futures.put(id, future);
        proxy.invoke(LOCK, SERIALIZER::encode, new Lock(id, 0)).whenComplete((result, error) -> {
            if (error != null) {
                futures.remove(id);
                future.completeExceptionally(error);
            }
        });
        return future.thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Optional<Version>> tryLock(Duration timeout) {
        RaftProxy.State state = proxy.getState();
        if (state != RaftProxy.State.CONNECTED) {
            return Futures.exceptionalFuture(new StorageException.Unavailable());
        }

        CompletableFuture<Version> future = new CompletableFuture<>();
        int id = this.id.incrementAndGet();
        futures.put(id, future);
        proxy.invoke(LOCK, SERIALIZER::encode, new Lock(id, timeout.toMillis())).whenComplete((result, error) -> {
            if (error != null) {
                futures.remove(id);
                future.completeExceptionally(error);
            }
        });
        return future.thenApply(Optional::ofNullable);
    }

    @Override
    public CompletableFuture<Void> unlock() {
        int lock = this.lock;
        this.lock = 0;
        if (lock != 0) {
            return proxy.invoke(UNLOCK, SERIALIZER::encode, new Unlock(lock));
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Closes the lock.
     *
     * @return a future to be completed once the lock has been closed
     */
    public CompletableFuture<Void> close() {
        return proxy.close();
    }
}