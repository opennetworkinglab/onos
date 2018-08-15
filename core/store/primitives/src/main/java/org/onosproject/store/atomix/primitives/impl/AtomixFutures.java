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

import com.google.common.base.Throwables;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.StorageException;

/**
 * Utility class for adapting Atomix futures.
 */
final class AtomixFutures {

    /**
     * Adapts the given Atomix future to ONOS.
     *
     * @param future the future to adapt
     * @param <T> the future result type
     * @return the adapted future
     */
    static <T> CompletableFuture<T> adaptFuture(CompletableFuture<T> future) {
        CompletableFuture<T> newFuture = new CompletableFuture<>();
        future.whenComplete((result, error) -> {
            if (error == null) {
                newFuture.complete(result);
            } else {
                Throwable cause = Throwables.getRootCause(error);
                if (cause instanceof io.atomix.primitive.PrimitiveException.ConcurrentModification) {
                    newFuture.completeExceptionally(new StorageException.ConcurrentModification());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Timeout) {
                    newFuture.completeExceptionally(new StorageException.Timeout());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Interrupted) {
                    newFuture.completeExceptionally(new StorageException.Interrupted());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Unavailable) {
                    newFuture.completeExceptionally(new StorageException.Unavailable());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException) {
                    newFuture.completeExceptionally(new StorageException(cause.getMessage()));
                } else {
                    newFuture.completeExceptionally(cause);
                }
            }
        });
        return newFuture;
    }

    /**
     * Adapts the given Atomix future to map exceptions.
     *
     * @param future the future to adapt
     * @param <T> the future result type
     * @return the adapted future
     */
    static <T> CompletableFuture<T> adaptMapFuture(CompletableFuture<T> future) {
        CompletableFuture<T> newFuture = new CompletableFuture<>();
        future.whenComplete((result, error) -> {
            if (error == null) {
                newFuture.complete(result);
            } else {
                Throwable cause = Throwables.getRootCause(error);
                if (cause instanceof io.atomix.primitive.PrimitiveException.ConcurrentModification) {
                    newFuture.completeExceptionally(
                        new ConsistentMapException.ConcurrentModification(cause.getMessage()));
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Timeout) {
                    newFuture.completeExceptionally(new ConsistentMapException.Timeout(cause.getMessage()));
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Interrupted) {
                    newFuture.completeExceptionally(new ConsistentMapException.Interrupted());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Unavailable) {
                    newFuture.completeExceptionally(new ConsistentMapException.Unavailable());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException) {
                    newFuture.completeExceptionally(new ConsistentMapException(cause.getMessage()));
                } else {
                    newFuture.completeExceptionally(cause);
                }
            }
        });
        return newFuture;
    }

    /**
     * Adapts the given Atomix future to document tree exceptions.
     *
     * @param future the future to adapt
     * @param <T> the future result type
     * @return the adapted future
     */
    static <T> CompletableFuture<T> adaptTreeFuture(CompletableFuture<T> future) {
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
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.ConcurrentModification) {
                    newFuture.completeExceptionally(new StorageException.ConcurrentModification());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Timeout) {
                    newFuture.completeExceptionally(new StorageException.Timeout());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Interrupted) {
                    newFuture.completeExceptionally(new StorageException.Interrupted());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException.Unavailable) {
                    newFuture.completeExceptionally(new StorageException.Unavailable());
                } else if (cause instanceof io.atomix.primitive.PrimitiveException) {
                    newFuture.completeExceptionally(new StorageException(cause.getMessage()));
                } else {
                    newFuture.completeExceptionally(cause);
                }
            }
        });
        return newFuture;
    }

    private AtomixFutures() {
    }
}
