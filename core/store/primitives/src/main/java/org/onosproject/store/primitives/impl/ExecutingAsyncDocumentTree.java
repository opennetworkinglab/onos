/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.common.collect.Maps;
import org.onlab.util.Tools;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.Versioned;

/**
 * {@link AsyncDocumentTree} that executes asynchronous callbacks on a user provided
 * {@link Executor}.
 */
public class ExecutingAsyncDocumentTree<V> extends ExecutingDistributedPrimitive implements AsyncDocumentTree<V> {
    private final AsyncDocumentTree<V> delegateTree;
    private final Executor executor;
    private final Map<DocumentTreeListener<V>, DocumentTreeListener<V>> listenerMap = Maps.newConcurrentMap();

    public ExecutingAsyncDocumentTree(AsyncDocumentTree<V> delegateTree, Executor executor) {
        super(delegateTree, executor);
        this.delegateTree = delegateTree;
        this.executor = executor;
    }

    @Override
    public DocumentPath root() {
        return delegateTree.root();
    }

    @Override
    public CompletableFuture<Map<String, Versioned<V>>> getChildren(DocumentPath path) {
        return Tools.asyncFuture(delegateTree.getChildren(path), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(DocumentPath path) {
        return Tools.asyncFuture(delegateTree.get(path), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> set(DocumentPath path, V value) {
        return Tools.asyncFuture(delegateTree.set(path, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> create(DocumentPath path, V value) {
        return Tools.asyncFuture(delegateTree.create(path, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> createRecursive(DocumentPath path, V value) {
        return Tools.asyncFuture(delegateTree.createRecursive(path, value), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, long version) {
        return Tools.asyncFuture(delegateTree.replace(path, newValue, version), executor);
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, V currentValue) {
        return Tools.asyncFuture(delegateTree.replace(path, newValue, currentValue), executor);
    }

    @Override
    public CompletableFuture<Versioned<V>> removeNode(DocumentPath path) {
        return Tools.asyncFuture(delegateTree.removeNode(path), executor);
    }

    @Override
    public CompletableFuture<Void> addListener(DocumentPath path, DocumentTreeListener<V> listener) {
        DocumentTreeListener<V> wrappedListener = e -> executor.execute(() -> listener.event(e));
        listenerMap.put(listener, wrappedListener);
        return Tools.asyncFuture(delegateTree.addListener(path, wrappedListener), executor);
    }

    @Override
    public CompletableFuture<Void> removeListener(DocumentTreeListener<V> listener) {
        DocumentTreeListener<V> wrappedListener = listenerMap.remove(listener);
        if (wrappedListener != null) {
            return Tools.asyncFuture(delegateTree.removeListener(wrappedListener), executor);
        }
        return CompletableFuture.completedFuture(null);
    }
}
