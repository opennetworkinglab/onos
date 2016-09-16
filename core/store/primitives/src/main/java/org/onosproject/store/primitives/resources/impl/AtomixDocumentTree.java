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

package org.onosproject.store.primitives.resources.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.primitives.resources.impl.DocumentTreeUpdateResult.Status.ILLEGAL_MODIFICATION;
import static org.onosproject.store.primitives.resources.impl.DocumentTreeUpdateResult.Status.INVALID_PATH;
import static org.onosproject.store.primitives.resources.impl.DocumentTreeUpdateResult.Status.OK;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.resource.AbstractResource;
import io.atomix.resource.ResourceTypeInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.onlab.util.Match;
import org.onlab.util.Tools;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Get;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.GetChildren;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Update;
import org.onosproject.store.service.AsyncDocumentTree;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeListener;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Versioned;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * Distributed resource providing the {@link AsyncDocumentTree} primitive.
 */
@ResourceTypeInfo(id = -156, factory = AtomixDocumentTreeFactory.class)
public class AtomixDocumentTree extends AbstractResource<AtomixDocumentTree>
    implements AsyncDocumentTree<byte[]> {

    private final Map<DocumentTreeListener<byte[]>, InternalListener> eventListeners = new HashMap<>();
    public static final String CHANGE_SUBJECT = "changeEvents";

    protected AtomixDocumentTree(CopycatClient client, Properties options) {
        super(client, options);
    }

    @Override
    public CompletableFuture<AtomixDocumentTree> open() {
        return super.open().thenApply(result -> {
            client.onStateChange(state -> {
                if (state == CopycatClient.State.CONNECTED && isListening()) {
                    client.submit(new Listen());
                }
            });
            client.onEvent(CHANGE_SUBJECT, this::processTreeUpdates);
            return result;
        });
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public Type primitiveType() {
        return Type.DOCUMENT_TREE;
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return client.submit(new Clear());
    }

    @Override
    public DocumentPath root() {
        return DocumentPath.from("root");
    }

    @Override
    public CompletableFuture<Map<String, Versioned<byte[]>>> getChildren(DocumentPath path) {
        return client.submit(new GetChildren(checkNotNull(path)));
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> get(DocumentPath path) {
        return client.submit(new Get(checkNotNull(path)));
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> set(DocumentPath path, byte[] value) {
        return client.submit(new Update(checkNotNull(path), Optional.ofNullable(value), Match.any(), Match.any()))
                .thenCompose(result -> {
                    if (result.status() == INVALID_PATH) {
                        return Tools.exceptionalFuture(new NoSuchDocumentPathException());
                    } else if (result.status() == ILLEGAL_MODIFICATION) {
                        return Tools.exceptionalFuture(new IllegalDocumentModificationException());
                    } else {
                        return CompletableFuture.completedFuture(result);
                    }
                }).thenApply(result -> result.oldValue());
    }

    @Override
    public CompletableFuture<Boolean> create(DocumentPath path, byte[] value) {
        return createInternal(path, value)
                .thenCompose(status -> {
                    if (status == ILLEGAL_MODIFICATION) {
                        return Tools.exceptionalFuture(new IllegalDocumentModificationException());
                    }
                    return CompletableFuture.completedFuture(true);
                });
    }

    @Override
    public CompletableFuture<Boolean> createRecursive(DocumentPath path, byte[] value) {
        return createInternal(path, value)
                .thenCompose(status -> {
                    if (status == ILLEGAL_MODIFICATION) {
                        return createRecursive(path.parent(), null)
                                    .thenCompose(r -> createInternal(path, value).thenApply(v -> true));
                    }
                    return CompletableFuture.completedFuture(status == OK);
                });
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, byte[] newValue, long version) {
        return client.submit(new Update(checkNotNull(path),
                                        Optional.ofNullable(newValue),
                                        Match.any(),
                                        Match.ifValue(version)))
                .thenApply(result -> result.updated());
    }

    @Override
    public CompletableFuture<Boolean> replace(DocumentPath path, byte[] newValue, byte[] currentValue) {
        return client.submit(new Update(checkNotNull(path),
                                        Optional.ofNullable(newValue),
                                        Match.ifValue(currentValue),
                                        Match.any()))
                .thenCompose(result -> {
                    if (result.status() == INVALID_PATH) {
                        return Tools.exceptionalFuture(new NoSuchDocumentPathException());
                    } else if (result.status() == ILLEGAL_MODIFICATION) {
                        return Tools.exceptionalFuture(new IllegalDocumentModificationException());
                    } else {
                        return CompletableFuture.completedFuture(result);
                    }
                }).thenApply(result -> result.updated());
    }

    @Override
    public CompletableFuture<Versioned<byte[]>> removeNode(DocumentPath path) {
        if (path.equals(DocumentPath.from("root"))) {
            return Tools.exceptionalFuture(new IllegalDocumentModificationException());
        }
        return client.submit(new Update(checkNotNull(path), null, Match.any(), Match.ifNotNull()))
                .thenCompose(result -> {
                    if (result.status() == INVALID_PATH) {
                        return Tools.exceptionalFuture(new NoSuchDocumentPathException());
                    } else if (result.status() == ILLEGAL_MODIFICATION) {
                        return Tools.exceptionalFuture(new IllegalDocumentModificationException());
                    } else {
                        return CompletableFuture.completedFuture(result);
                    }
                }).thenApply(result -> result.oldValue());
    }

    @Override
    public CompletableFuture<Void> addListener(DocumentPath path, DocumentTreeListener<byte[]> listener) {
        checkNotNull(path);
        checkNotNull(listener);
        InternalListener internalListener = new InternalListener(path, listener, MoreExecutors.directExecutor());
        // TODO: Support API that takes an executor
        if (!eventListeners.containsKey(listener)) {
            return client.submit(new Listen(path))
                         .thenRun(() -> eventListeners.put(listener, internalListener));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> removeListener(DocumentTreeListener<byte[]> listener) {
        checkNotNull(listener);
        InternalListener internalListener = eventListeners.remove(listener);
        if  (internalListener != null && eventListeners.isEmpty()) {
            return client.submit(new Unlisten(internalListener.path)).thenApply(v -> null);
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<DocumentTreeUpdateResult.Status> createInternal(DocumentPath path, byte[] value) {
        return client.submit(new Update(checkNotNull(path), Optional.ofNullable(value), Match.any(), Match.ifNull()))
                     .thenApply(result -> result.status());
    }

    private boolean isListening() {
        return !eventListeners.isEmpty();
    }

    private void processTreeUpdates(List<DocumentTreeEvent<byte[]>> events) {
        events.forEach(event -> eventListeners.values().forEach(listener -> listener.event(event)));
    }

    private class InternalListener implements DocumentTreeListener<byte[]> {

        private final DocumentPath path;
        private final DocumentTreeListener<byte[]> listener;
        private final Executor executor;

        public InternalListener(DocumentPath path, DocumentTreeListener<byte[]> listener, Executor executor) {
            this.path = path;
            this.listener = listener;
            this.executor = executor;
        }

        @Override
        public void event(DocumentTreeEvent<byte[]> event) {
            if (event.path().isDescendentOf(path)) {
                executor.execute(() -> listener.event(event));
            }
        }
    }
}
