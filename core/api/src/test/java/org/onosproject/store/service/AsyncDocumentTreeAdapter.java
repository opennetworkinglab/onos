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
package org.onosproject.store.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.onosproject.store.primitives.NodeUpdate;
import org.onosproject.store.primitives.TransactionId;

/**
 * Async document tree adapter.
 */
public class AsyncDocumentTreeAdapter<V> implements AsyncDocumentTree<V> {
    @Override
    public String name() {
        return null;
    }

    @Override
    public DocumentPath root() {
        return null;
    }

    @Override
    public CompletableFuture<Map<String, Versioned<V>>> getChildren(DocumentPath path) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> get(DocumentPath path) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> set(DocumentPath path, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> create(DocumentPath path, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> createRecursive(DocumentPath path, V value) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, long version) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, V currentValue) {
        return null;
    }

    @Override
    public CompletableFuture<Versioned<V>> removeNode(DocumentPath path) {
        return null;
    }

    @Override
    public CompletableFuture<Void> addListener(DocumentPath path, DocumentTreeListener<V> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Void> removeListener(DocumentTreeListener<V> listener) {
        return null;
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<NodeUpdate<V>> transactionLog) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<NodeUpdate<V>> transactionLog) {
        return null;
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        return null;
    }
}
