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

package org.onosproject.store.primitives.resources.impl;

import com.google.common.base.Throwables;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.DocumentException;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.Synchronous;
import org.onosproject.store.service.Versioned;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronous wrapper for a {@link AsyncDocumentTree}.  All operations are
 * made by making the equivalent calls to a backing {@link AsyncDocumentTree}
 * then blocking until the operations complete or timeout.
 *
 * @param <V> the type of the values
 */
public class DefaultConsistentDocumentTree<V> extends Synchronous<AsyncDocumentTree<V>> implements DocumentTree<V> {

    private final AsyncDocumentTree<V> backingMap;
    private static final int MAX_DELAY_BETWEEN_RETRY_MILLS = 50;
    private final long operationTimeoutMillis;

    public DefaultConsistentDocumentTree(AsyncDocumentTree<V> backingMap,
                                         long operationTimeoutMillis) {
        super(backingMap);
        this.backingMap = backingMap;
        this.operationTimeoutMillis = operationTimeoutMillis;
    }

    @Override
    public DocumentPath root() {
        return backingMap.root();
    }

    @Override
    public Map<String, Versioned<V>> getChildren(DocumentPath path) {
        return complete(backingMap.getChildren(path));
    }

    @Override
    public Versioned<V> get(DocumentPath path) {
        return complete(backingMap.get(path));
    }

    @Override
    public Versioned<V> set(DocumentPath path, V value) {
        return complete(backingMap.set(path, value));
    }

    @Override
    public boolean create(DocumentPath path, V value) {
        return complete(backingMap.create(path, value));
    }

    @Override
    public boolean createRecursive(DocumentPath path, V value) {
        return complete(backingMap.createRecursive(path, value));
    }

    @Override
    public boolean replace(DocumentPath path, V newValue, long version) {
        return complete(backingMap.replace(path, newValue, version));
    }

    @Override
    public boolean replace(DocumentPath path, V newValue, V currentValue) {
        return complete(backingMap.replace(path, newValue, currentValue));
    }

    @Override
    public Versioned<V> removeNode(DocumentPath path) {
        return complete(backingMap.removeNode(path));
    }

    @Override
    public void addListener(DocumentPath path, DocumentTreeListener<V> listener) {
        complete(backingMap.addListener(path, listener));
    }

    @Override
    public void removeListener(DocumentTreeListener<V> listener) {
        complete(backingMap.removeListener(listener));
    }

    @Override
    public void addListener(DocumentTreeListener<V> listener) {
        complete(backingMap.addListener(listener));
    }

    private <T> T complete(CompletableFuture<T> future) {
        try {
            return future.get(operationTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentException.Interrupted();
        } catch (TimeoutException e) {
            throw new DocumentException.Timeout(name());
        } catch (ExecutionException e) {
            Throwables.propagateIfPossible(e.getCause());
            throw new ConsistentMapException(e.getCause());
        }
    }
}
