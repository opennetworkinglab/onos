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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.store.primitives.NodeUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Version;
import org.onosproject.store.service.Versioned;

/**
 * Atomix document tree.
 */
public class AtomixDocumentTree<V> implements AsyncDocumentTree<V> {
    private final io.atomix.core.tree.AsyncAtomicDocumentTree<V> atomixTree;
    private final Map<DocumentTreeListener<V>, io.atomix.core.tree.DocumentTreeEventListener<V>> listenerMap =
        Maps.newIdentityHashMap();

    public AtomixDocumentTree(io.atomix.core.tree.AsyncAtomicDocumentTree<V> atomixTree) {
        this.atomixTree = atomixTree;
    }

    @Override
    public String name() {
        return atomixTree.name();
    }

    @Override
    public DocumentPath root() {
        return toOnosPath(atomixTree.root());
    }

    @Override
    public CompletableFuture<Map<String, Versioned<V>>> getChildren(DocumentPath path) {
        return atomixTree.getChildren(toAtomixPath(path))
            .thenApply(map -> map.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(),
                    e -> toVersioned(e.getValue()))));
    }

    @Override
    public CompletableFuture<Versioned<V>> get(DocumentPath path) {
        return atomixTree.get(toAtomixPath(path)).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Versioned<V>> set(DocumentPath path, V value) {
        return atomixTree.set(toAtomixPath(path), value).thenApply(this::toVersioned);
    }

    @Override
    public CompletableFuture<Boolean> create(DocumentPath path, V value) {
        return convertException(atomixTree.create(toAtomixPath(path), value));
    }

    @Override
    public CompletableFuture<Boolean> createRecursive(DocumentPath path, V value) {
        return atomixTree.createRecursive(toAtomixPath(path), value);
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, long version) {
        return atomixTree.replace(toAtomixPath(path), newValue, version);
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, V newValue, V currentValue) {
        return atomixTree.replace(toAtomixPath(path), newValue, currentValue);
    }

    @Override
    public CompletableFuture<Versioned<V>> removeNode(DocumentPath path) {
        return atomixTree.remove(toAtomixPath(path)).thenApply(this::toVersioned);
    }

    @Override
    public synchronized CompletableFuture<Void> addListener(DocumentPath path, DocumentTreeListener<V> listener) {
        io.atomix.core.tree.DocumentTreeEventListener<V> atomixListener = event ->
            listener.event(new DocumentTreeEvent<V>(
                DocumentPath.from(event.path().pathElements()),
                DocumentTreeEvent.Type.valueOf(event.type().name()),
                event.newValue().map(this::toVersioned),
                event.oldValue().map(this::toVersioned)));
        listenerMap.put(listener, atomixListener);
        return atomixTree.addListener(toAtomixPath(path), atomixListener);
    }

    @Override
    public CompletableFuture<Void> removeListener(DocumentTreeListener<V> listener) {
        io.atomix.core.tree.DocumentTreeEventListener<V> atomixListener = listenerMap.remove(listener);
        if (atomixListener != null) {
            return atomixTree.removeListener(atomixListener);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Version> begin(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> prepare(TransactionLog<NodeUpdate<V>> transactionLog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Boolean> prepareAndCommit(TransactionLog<NodeUpdate<V>> transactionLog) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> commit(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> rollback(TransactionId transactionId) {
        throw new UnsupportedOperationException();
    }

    private <T> CompletableFuture<T> convertException(CompletableFuture<T> future) {
        CompletableFuture<T> newFuture = new CompletableFuture<>();
        future.whenComplete((result, error) -> {
            if (error == null) {
                newFuture.complete(result);
            } else {
                Throwable cause = Throwables.getRootCause(error);
                if (cause instanceof io.atomix.core.tree.NoSuchDocumentPathException) {
                    newFuture.completeExceptionally(new NoSuchDocumentPathException());
                } else if (cause instanceof io.atomix.core.tree.IllegalDocumentModificationException) {
                    newFuture.completeExceptionally(new IllegalDocumentModificationException());
                } else {
                    newFuture.completeExceptionally(cause);
                }
            }
        });
        return newFuture;
    }

    private DocumentPath toOnosPath(io.atomix.core.tree.DocumentPath path) {
        List<String> pathElements = Lists.newArrayList(path.pathElements());
        pathElements.set(0, DocumentPath.ROOT.pathElements().get(0));
        return DocumentPath.from(pathElements);
    }

    private io.atomix.core.tree.DocumentPath toAtomixPath(DocumentPath path) {
        List<String> pathElements = Lists.newArrayList(path.pathElements());
        pathElements.set(0, "");
        return io.atomix.core.tree.DocumentPath.from(pathElements);
    }

    private Versioned<V> toVersioned(io.atomix.utils.time.Versioned<V> versioned) {
        return versioned != null
            ? new Versioned<>(versioned.value(), versioned.version(), versioned.creationTime())
            : null;
    }
}
