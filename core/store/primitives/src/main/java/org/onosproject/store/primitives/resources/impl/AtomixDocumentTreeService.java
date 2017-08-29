/*
 * Copyright 2016-present Open Networking Foundation
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import io.atomix.protocols.raft.event.EventType;
import io.atomix.protocols.raft.service.AbstractRaftService;
import io.atomix.protocols.raft.service.Commit;
import io.atomix.protocols.raft.service.RaftServiceExecutor;
import io.atomix.protocols.raft.session.RaftSession;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.store.primitives.NodeUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Get;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GetChildren;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Update;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.TransactionBegin;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.TransactionPrepare;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.TransactionPrepareAndCommit;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.TransactionCommit;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.TransactionRollback;

import org.onosproject.store.primitives.resources.impl.DocumentTreeResult.Status;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeEvent.Type;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Versioned;

import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.BEGIN;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.PREPARE;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.PREPARE_AND_COMMIT;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.COMMIT;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.ROLLBACK;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GET_CHILDREN;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.UPDATE;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.REMOVE_LISTENER;

/**
 * State Machine for {@link AtomixDocumentTree} resource.
 */
public class AtomixDocumentTreeService extends AbstractRaftService {
    private final Serializer serializer = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixDocumentTreeOperations.NAMESPACE)
            .register(AtomixDocumentTreeEvents.NAMESPACE)
            .register(new com.esotericsoftware.kryo.Serializer<Listener>() {
                @Override
                public void write(Kryo kryo, Output output, Listener listener) {
                    output.writeLong(listener.session.sessionId().id());
                    kryo.writeObject(output, listener.path);
                }

                @Override
                public Listener read(Kryo kryo, Input input, Class<Listener> type) {
                    return new Listener(sessions().getSession(input.readLong()),
                            kryo.readObjectOrNull(input, DocumentPath.class));
                }
            }, Listener.class)
            .register(Versioned.class)
            .register(DocumentPath.class)
            .register(new LinkedHashMap().keySet().getClass())
            .register(TreeMap.class)
            .register(Ordering.class)
            .register(TransactionScope.class)
            .register(TransactionLog.class)
            .register(TransactionId.class)
            .register(SessionListenCommits.class)
            .register(new com.esotericsoftware.kryo.Serializer<DefaultDocumentTree>() {
                @Override
                public void write(Kryo kryo, Output output, DefaultDocumentTree object) {
                    kryo.writeObject(output, object.root);
                }

                @Override
                @SuppressWarnings("unchecked")
                public DefaultDocumentTree read(Kryo kryo, Input input, Class<DefaultDocumentTree> type) {
                    return new DefaultDocumentTree(versionCounter::incrementAndGet,
                            kryo.readObject(input, DefaultDocumentTreeNode.class));
                }
            }, DefaultDocumentTree.class)
            .register(DefaultDocumentTreeNode.class)
            .build());

    private Map<Long, SessionListenCommits> listeners = new HashMap<>();
    private AtomicLong versionCounter = new AtomicLong(0);
    private DocumentTree<byte[]> docTree;
    private Map<TransactionId, TransactionScope> activeTransactions = Maps.newHashMap();
    private Set<DocumentPath> preparedKeys = Sets.newHashSet();

    public AtomixDocumentTreeService(Ordering ordering) {
        this.docTree = new DefaultDocumentTree<>(versionCounter::incrementAndGet, ordering);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeLong(versionCounter.get());
        writer.writeObject(listeners, serializer::encode);
        writer.writeObject(docTree, serializer::encode);
        writer.writeObject(preparedKeys, serializer::encode);
        writer.writeObject(activeTransactions, serializer::encode);
    }

    @Override
    public void install(SnapshotReader reader) {
        versionCounter = new AtomicLong(reader.readLong());
        listeners = reader.readObject(serializer::decode);
        docTree = reader.readObject(serializer::decode);
        preparedKeys = reader.readObject(serializer::decode);
        activeTransactions = reader.readObject(serializer::decode);
    }

    @Override
    protected void configure(RaftServiceExecutor executor) {
        // Listeners
        executor.register(ADD_LISTENER, serializer::decode, this::listen);
        executor.register(REMOVE_LISTENER, serializer::decode, this::unlisten);
        // queries
        executor.register(GET, serializer::decode, this::get, serializer::encode);
        executor.register(GET_CHILDREN, serializer::decode, this::getChildren, serializer::encode);
        // commands
        executor.register(UPDATE, serializer::decode, this::update, serializer::encode);
        executor.register(CLEAR, this::clear);
        executor.register(BEGIN, serializer::decode, this::begin, serializer::encode);
        executor.register(PREPARE, serializer::decode, this::prepare, serializer::encode);
        executor.register(PREPARE_AND_COMMIT, serializer::decode, this::prepareAndCommit, serializer::encode);
        executor.register(COMMIT, serializer::decode, this::commit, serializer::encode);
        executor.register(ROLLBACK, serializer::decode, this::rollback, serializer::encode);
    }

    /**
     * Returns a boolean indicating whether the given path is currently locked by a transaction.
     *
     * @param path the path to check
     * @return whether the given path is locked by a running transaction
     */
    private boolean isLocked(DocumentPath path) {
        return preparedKeys.contains(path);
    }

    protected void listen(Commit<? extends Listen> commit) {
        Long sessionId = commit.session().sessionId().id();
        listeners.computeIfAbsent(sessionId, k -> new SessionListenCommits())
                .add(new Listener(commit.session(), commit.value().path()));
    }

    protected void unlisten(Commit<? extends Unlisten> commit) {
        Long sessionId = commit.session().sessionId().id();
        SessionListenCommits listenCommits = listeners.get(sessionId);
        if (listenCommits != null) {
            listenCommits.remove(commit);
        }
    }

    protected Versioned<byte[]> get(Commit<? extends Get> commit) {
        try {
            Versioned<byte[]> value = docTree.get(commit.value().path());
            return value == null ? null : value.map(node -> node);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    protected DocumentTreeResult<Map<String, Versioned<byte[]>>> getChildren(Commit<? extends GetChildren> commit) {
        try {
            return DocumentTreeResult.ok(docTree.getChildren(commit.value().path()));
        } catch (NoSuchDocumentPathException e) {
            return DocumentTreeResult.invalidPath();
        }
    }

    protected DocumentTreeResult<Versioned<byte[]>> update(Commit<? extends Update> commit) {
        DocumentTreeResult<Versioned<byte[]>> result = null;
        DocumentPath path = commit.value().path();

        // If the path is locked by a transaction, return a WRITE_LOCK error.
        if (isLocked(path)) {
            return DocumentTreeResult.writeLock();
        }

        Versioned<byte[]> currentValue = docTree.get(path);
        try {
            Match<Long> versionMatch = commit.value().versionMatch();
            Match<byte[]> valueMatch = commit.value().valueMatch();

            if (versionMatch.matches(currentValue == null ? null : currentValue.version())
                    && valueMatch.matches(currentValue == null ? null : currentValue.value())) {
                if (commit.value().value() == null) {
                    Versioned<byte[]> oldValue = docTree.removeNode(path);
                    result = new DocumentTreeResult<>(Status.OK, oldValue);
                    if (oldValue != null) {
                        notifyListeners(new DocumentTreeEvent<>(
                                path,
                                Type.DELETED,
                                Optional.empty(),
                                Optional.of(oldValue)));
                    }
                } else {
                    Versioned<byte[]> oldValue = docTree.set(path, commit.value().value().orElse(null));
                    Versioned<byte[]> newValue = docTree.get(path);
                    result = new DocumentTreeResult<>(Status.OK, newValue);
                    if (oldValue == null) {
                        notifyListeners(new DocumentTreeEvent<>(
                                path,
                                Type.CREATED,
                                Optional.of(newValue),
                                Optional.empty()));
                    } else {
                        notifyListeners(new DocumentTreeEvent<>(
                                path,
                                Type.UPDATED,
                                Optional.of(newValue),
                                Optional.of(oldValue)));
                    }
                }
            } else {
                result = new DocumentTreeResult<>(
                        commit.value().value() == null ? Status.INVALID_PATH : Status.NOOP,
                        currentValue);
            }
        } catch (IllegalDocumentModificationException e) {
            result = DocumentTreeResult.illegalModification();
        } catch (NoSuchDocumentPathException e) {
            result = DocumentTreeResult.invalidPath();
        } catch (Exception e) {
            logger().error("Failed to apply {} to state machine", commit.value(), e);
            throw Throwables.propagate(e);
        }
        return result;
    }

    protected void clear(Commit<Void> commit) {
        Queue<DocumentPath> toClearQueue = Queues.newArrayDeque();
        Map<String, Versioned<byte[]>> topLevelChildren = docTree.getChildren(DocumentPath.from("root"));
        toClearQueue.addAll(topLevelChildren.keySet()
                .stream()
                .map(name -> new DocumentPath(name, DocumentPath.from("root")))
                .collect(Collectors.toList()));
        while (!toClearQueue.isEmpty()) {
            DocumentPath path = toClearQueue.remove();
            Map<String, Versioned<byte[]>> children = docTree.getChildren(path);
            if (children.size() == 0) {
                docTree.removeNode(path);
            } else {
                children.keySet().forEach(name -> toClearQueue.add(new DocumentPath(name, path)));
                toClearQueue.add(path);
            }
        }
    }

    /**
     * Handles a begin commit.
     *
     * @param commit transaction begin commit
     * @return transaction state version
     */
    protected long begin(Commit<? extends TransactionBegin> commit) {
        long version = commit.index();
        activeTransactions.put(commit.value().transactionId(), new TransactionScope(version));
        return version;
    }

    /**
     * Handles an prepare commit.
     *
     * @param commit transaction prepare commit
     * @return prepare result
     */
    protected PrepareResult prepare(Commit<? extends TransactionPrepare> commit) {
        try {
            TransactionLog<NodeUpdate<byte[]>> transactionLog = commit.value().transactionLog();
            // Iterate through records in the transaction log and perform isolation checks.
            for (NodeUpdate<byte[]> record : transactionLog.records()) {
                DocumentPath path = record.path();

                // If the prepared keys already contains the key contained within the record, that indicates a
                // conflict with a concurrent transaction.
                if (preparedKeys.contains(path)) {
                    return PrepareResult.CONCURRENT_TRANSACTION;
                }

                // Read the existing value from the map.
                Versioned<byte[]> existingValue = docTree.get(path);

                // If the update is an UPDATE_NODE or DELETE_NODE, verify that versions match.
                switch (record.type()) {
                    case UPDATE_NODE:
                    case DELETE_NODE:
                        if (existingValue == null || existingValue.version() != record.version()) {
                            return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                        }
                    default:
                        break;
                }
            }

            // No violations detected. Mark modified keys locked for transactions.
            transactionLog.records().forEach(record -> {
                preparedKeys.add(record.path());
            });

            // Update the transaction scope. If the transaction scope is not set on this node, that indicates the
            // coordinator is communicating with another node. Transactions assume that the client is communicating
            // with a single leader in order to limit the overhead of retaining tombstones.
            TransactionScope transactionScope = activeTransactions.get(transactionLog.transactionId());
            if (transactionScope == null) {
                activeTransactions.put(
                        transactionLog.transactionId(),
                        new TransactionScope(transactionLog.version(), commit.value().transactionLog()));
                return PrepareResult.PARTIAL_FAILURE;
            } else {
                activeTransactions.put(
                        transactionLog.transactionId(),
                        transactionScope.prepared(commit));
                return PrepareResult.OK;
            }
        } catch (Exception e) {
            logger().warn("Failure applying {}", commit, e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Handles an prepare and commit commit.
     *
     * @param commit transaction prepare and commit commit
     * @return prepare result
     */
    protected PrepareResult prepareAndCommit(Commit<? extends TransactionPrepareAndCommit> commit) {
        TransactionId transactionId = commit.value().transactionLog().transactionId();
        PrepareResult prepareResult = prepare(commit);
        TransactionScope transactionScope = activeTransactions.remove(transactionId);
        if (prepareResult == PrepareResult.OK) {
            transactionScope = transactionScope.prepared(commit);
            commitTransaction(transactionScope);
        }
        return prepareResult;
    }

    /**
     * Applies committed operations to the state machine.
     */
    private CommitResult commitTransaction(TransactionScope transactionScope) {
        TransactionLog<NodeUpdate<byte[]>> transactionLog = transactionScope.transactionLog();

        List<DocumentTreeEvent<byte[]>> eventsToPublish = Lists.newArrayList();
        DocumentTreeEvent<byte[]> start = new DocumentTreeEvent<>(
                DocumentPath.from(transactionScope.transactionLog().transactionId().toString()),
                Type.TRANSACTION_START,
                Optional.empty(),
                Optional.empty());
        eventsToPublish.add(start);

        for (NodeUpdate<byte[]> record : transactionLog.records()) {
            DocumentPath path = record.path();
            checkState(preparedKeys.remove(path), "path is not prepared");

            Versioned<byte[]> previousValue = null;
            try {
                previousValue = docTree.removeNode(path);
            } catch (NoSuchDocumentPathException e) {
                logger().info("Value is being inserted first time");
            }

            if (record.value() != null) {
                if (docTree.create(path, record.value())) {
                    Versioned<byte[]> newValue = docTree.get(path);
                    eventsToPublish.add(new DocumentTreeEvent<>(
                            path,
                            Optional.ofNullable(newValue),
                            Optional.ofNullable(previousValue)));
                }
            } else if (previousValue != null) {
                eventsToPublish.add(new DocumentTreeEvent<>(
                        path,
                        Optional.empty(),
                        Optional.of(previousValue)));
            }
        }

        DocumentTreeEvent<byte[]> end = new DocumentTreeEvent<byte[]>(
                DocumentPath.from(transactionScope.transactionLog().transactionId().toString()),
                Type.TRANSACTION_END,
                Optional.empty(),
                Optional.empty());
        eventsToPublish.add(end);
        publish(eventsToPublish);

        return CommitResult.OK;
    }

    /**
     * Handles an commit commit (ha!).
     *
     * @param commit transaction commit commit
     * @return commit result
     */
    protected CommitResult commit(Commit<? extends TransactionCommit> commit) {
        TransactionId transactionId = commit.value().transactionId();
        TransactionScope transactionScope = activeTransactions.remove(transactionId);
        if (transactionScope == null) {
            return CommitResult.UNKNOWN_TRANSACTION_ID;
        }
        try {
            return commitTransaction(transactionScope);
        } catch (Exception e) {
            logger().warn("Failure applying {}", commit, e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Handles an rollback commit (ha!).
     *
     * @param commit transaction rollback commit
     * @return rollback result
     */
    protected RollbackResult rollback(Commit<? extends TransactionRollback> commit) {
        TransactionId transactionId = commit.value().transactionId();
        TransactionScope transactionScope = activeTransactions.remove(transactionId);
        if (transactionScope == null) {
            return RollbackResult.UNKNOWN_TRANSACTION_ID;
        } else if (!transactionScope.isPrepared()) {
            return RollbackResult.OK;
        } else {
            transactionScope.transactionLog().records()
                    .forEach(record -> {
                        preparedKeys.remove(record.path());
                    });
            return RollbackResult.OK;
        }

    }

    /**
     * Map transaction scope.
     */
    private static final class TransactionScope {
        private final long version;
        private final TransactionLog<NodeUpdate<byte[]>> transactionLog;

        private TransactionScope(long version) {
            this(version, null);
        }

        private TransactionScope(long version, TransactionLog<NodeUpdate<byte[]>> transactionLog) {
            this.version = version;
            this.transactionLog = transactionLog;
        }

        /**
         * Returns the transaction version.
         *
         * @return the transaction version
         */
        long version() {
            return version;
        }

        /**
         * Returns whether this is a prepared transaction scope.
         *
         * @return whether this is a prepared transaction scope
         */
        boolean isPrepared() {
            return transactionLog != null;
        }

        /**
         * Returns the transaction commit log.
         *
         * @return the transaction commit log
         */
        TransactionLog<NodeUpdate<byte[]>> transactionLog() {
            checkState(isPrepared());
            return transactionLog;
        }

        /**
         * Returns a new transaction scope with a prepare commit.
         *
         * @param commit the prepare commit
         * @return new transaction scope updated with the prepare commit
         */
        TransactionScope prepared(Commit<? extends TransactionPrepare> commit) {
            return new TransactionScope(version, commit.value().transactionLog());
        }
    }

    private void publish(List<DocumentTreeEvent<byte[]>> events) {
        listeners.values().forEach(session -> {
            session.publish(CHANGE, events);
        });
    }

    private void notifyListeners(DocumentTreeEvent<byte[]> event) {
        listeners.values()
                .stream()
                .filter(l -> event.path().isDescendentOf(l.leastCommonAncestorPath()))
                .forEach(listener -> listener.publish(CHANGE, Arrays.asList(event)));
    }

    @Override
    public void onExpire(RaftSession session) {
        closeListener(session.sessionId().id());
    }

    @Override
    public void onClose(RaftSession session) {
        closeListener(session.sessionId().id());
    }

    private void closeListener(Long sessionId) {
        listeners.remove(sessionId);
    }

    private class SessionListenCommits {
        private final List<Listener> listeners = Lists.newArrayList();
        private DocumentPath leastCommonAncestorPath;

        public void add(Listener listener) {
            listeners.add(listener);
            recomputeLeastCommonAncestor();
        }

        public void remove(Commit<? extends Unlisten> commit) {
            // Remove the first listen commit with path matching path in unlisten commit
            Iterator<Listener> iterator = listeners.iterator();
            while (iterator.hasNext()) {
                Listener listener = iterator.next();
                if (listener.path().equals(commit.value().path())) {
                    iterator.remove();
                }
            }
            recomputeLeastCommonAncestor();
        }

        public DocumentPath leastCommonAncestorPath() {
            return leastCommonAncestorPath;
        }

        public <M> void publish(EventType topic, M message) {
            listeners.stream().findAny().ifPresent(listener ->
                    listener.session().publish(topic, serializer::encode, message));
        }

        private void recomputeLeastCommonAncestor() {
            this.leastCommonAncestorPath = DocumentPath.leastCommonAncestor(listeners.stream()
                    .map(Listener::path)
                    .collect(Collectors.toList()));
        }
    }

    private static class Listener {
        private final RaftSession session;
        private final DocumentPath path;

        public Listener(RaftSession session, DocumentPath path) {
            this.session = session;
            this.path = path;
        }

        public DocumentPath path() {
            return path;
        }

        public RaftSession session() {
            return session;
        }
    }
}