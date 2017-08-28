/*
 * Copyright 2017-present Open Networking Foundation
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.MoreObjects;
import org.onosproject.store.primitives.NodeUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

/**
 * Document tree that delegates to an underlying instance.
 */
public class DelegatingAsyncDocumentTree<V> extends DelegatingDistributedPrimitive implements AsyncDocumentTree<V> {
    private final AsyncDocumentTree<V> delegateTree;

    public DelegatingAsyncDocumentTree(AsyncDocumentTree<V> delegateTree) {
        super(delegateTree);
        this.delegateTree = delegateTree;
    }

    @Override
    public DocumentPath root() {
        return delegateTree.root();
    }

    @Override
    public CompletableFuture<Map<String, Versioned<V>>> getChildren(DocumentPath path) {
        return delegateTree.getChildren(path);
    }

    @Override
    public CompletableFuture<Versioned<V>> get(DocumentPath path) {
        return delegateTree.get(path);
    }

    @Override
    public CompletableFuture<Versioned<V>> set(DocumentPath path, V value) {
        return delegateTree.set(path, value);
    }

    @Override
    public CompletableFuture<Boolean> create(DocumentPath path, V value) {
        return delegateTree.create(path, value);
    }

    @Override
    public CompletableFuture<Boolean> createRecursive(DocumentPath path, V value) {
        return delegateTree.createRecursive(path, value);
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, long version) {
        return delegateTree.replace(path, newValue, version);
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, V currentValue) {
        return delegateTree.replace(path, newValue, currentValue);
    }

    @Override
    public CompletableFuture<Versioned<V>> removeNode(DocumentPath path) {
        return delegateTree.removeNode(path);
    }

    @Override
    public CompletableFuture<Void> addListener(DocumentPath path, DocumentTreeListener<V> listener) {
        return delegateTree.addListener(path, listener);
    }

    @Override
    public CompletableFuture<Void> removeListener(DocumentTreeListener<V> listener) {
        return delegateTree.removeListener(listener);
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return delegateTree.begin(transactionId);
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<NodeUpdate<V>> transactionLog) {
        return delegateTree.prepare(transactionLog);
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<NodeUpdate<V>> transactionLog) {
        return delegateTree.prepareAndCommit(transactionLog);
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return delegateTree.commit(transactionId);
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return delegateTree.rollback(transactionId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("delegateTree", delegateTree)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegateTree);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DelegatingAsyncDocumentTree) {
            DelegatingAsyncDocumentTree<V> that = (DelegatingAsyncDocumentTree) other;
            return this.delegateTree.equals(that.delegateTree);
        }
        return false;
    }
}
