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

import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.onlab.util.Match;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Get;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.GetChildren;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeCommands.Update;
import org.onosproject.store.primitives.resources.impl.DocumentTreeUpdateResult.Status;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeEvent.Type;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

/**
 * State Machine for {@link AtomixDocumentTree} resource.
 */
public class AtomixDocumentTreeState
    extends ResourceStateMachine
    implements SessionListener, Snapshottable {

    private final Logger log = getLogger(getClass());
    private final Map<Long, SessionListenCommits> listeners = new HashMap<>();
    private AtomicLong versionCounter = new AtomicLong(0);
    private final DocumentTree<TreeNodeValue> docTree = new DefaultDocumentTree<>(versionCounter::incrementAndGet);

    public AtomixDocumentTreeState(Properties properties) {
        super(properties);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeLong(versionCounter.get());
    }

    @Override
    public void install(SnapshotReader reader) {
        versionCounter = new AtomicLong(reader.readLong());
    }

    @Override
    protected void configure(StateMachineExecutor executor) {
        // Listeners
        executor.register(Listen.class, this::listen);
        executor.register(Unlisten.class, this::unlisten);
        // queries
        executor.register(Get.class, this::get);
        executor.register(GetChildren.class, this::getChildren);
        // commands
        executor.register(Update.class, this::update);
        executor.register(Clear.class, this::clear);
    }

    protected void listen(Commit<? extends Listen> commit) {
        Long sessionId = commit.session().id();
        listeners.computeIfAbsent(sessionId, k -> new SessionListenCommits()).add(commit);
        commit.session().onStateChange(
                        state -> {
                            if (state == ServerSession.State.CLOSED
                                    || state == ServerSession.State.EXPIRED) {
                                closeListener(commit.session().id());
                            }
                        });
    }

    protected void unlisten(Commit<? extends Unlisten> commit) {
        Long sessionId = commit.session().id();
        try {
            SessionListenCommits listenCommits = listeners.get(sessionId);
            if (listenCommits != null) {
                listenCommits.remove(commit);
            }
        } finally {
            commit.close();
        }
    }

    protected Versioned<byte[]> get(Commit<? extends Get> commit) {
        try {
            Versioned<TreeNodeValue> value = docTree.get(commit.operation().path());
            return value == null ? null : value.map(node -> node == null ? null : node.value());
        } catch (IllegalStateException e) {
            return null;
        } finally {
            commit.close();
        }
    }

    protected Map<String, Versioned<byte[]>> getChildren(Commit<? extends GetChildren> commit) {
        try {
            Map<String, Versioned<TreeNodeValue>> children = docTree.getChildren(commit.operation().path());
            return children == null
                    ? null : Maps.newHashMap(Maps.transformValues(children,
                                                                  value -> value.map(TreeNodeValue::value)));
        } finally {
            commit.close();
        }
    }

    protected DocumentTreeUpdateResult<byte[]> update(Commit<? extends Update> commit) {
        DocumentTreeUpdateResult<byte[]> result = null;
        DocumentPath path = commit.operation().path();
        boolean updated = false;
        Versioned<TreeNodeValue> currentValue = docTree.get(path);
        try {
            Match<Long> versionMatch = commit.operation().versionMatch();
            Match<byte[]> valueMatch = commit.operation().valueMatch();

            if (versionMatch.matches(currentValue == null ? null : currentValue.version())
                    && valueMatch.matches(currentValue == null ? null : currentValue.value().value())) {
                if (commit.operation().value() == null) {
                    docTree.removeNode(path);
                } else {
                    docTree.set(path, new NonTransactionalCommit(commit));
                }
                updated = true;
            }
            Versioned<TreeNodeValue> newValue = updated ? docTree.get(path) : currentValue;
            Status updateStatus = updated
                    ? Status.OK : commit.operation().value() == null ? Status.INVALID_PATH : Status.NOOP;
            result = new DocumentTreeUpdateResult<>(path,
                    updateStatus,
                    newValue == null
                        ? null : newValue.map(TreeNodeValue::value),
                    currentValue == null
                        ? null : currentValue.map(TreeNodeValue::value));
        } catch (IllegalDocumentModificationException e) {
            result = DocumentTreeUpdateResult.illegalModification(path);
        } catch (NoSuchDocumentPathException e) {
            result = DocumentTreeUpdateResult.invalidPath(path);
        } catch (Exception e) {
            log.error("Failed to apply {} to state machine", commit.operation(), e);
            throw Throwables.propagate(e);
        } finally {
            if (updated) {
                if (currentValue != null) {
                    currentValue.value().discard();
                }
            } else {
                commit.close();
            }
        }
        notifyListeners(path, result);
        return result;
    }

    protected void clear(Commit<? extends Clear> commit) {
        try {
            Queue<DocumentPath> toClearQueue = Queues.newArrayDeque();
            Map<String, Versioned<TreeNodeValue>> topLevelChildren = docTree.getChildren(DocumentPath.from("root"));
            toClearQueue.addAll(topLevelChildren.keySet()
                                                .stream()
                                                .map(name -> new DocumentPath(name, DocumentPath.from("root")))
                                                .collect(Collectors.toList()));
            while (!toClearQueue.isEmpty()) {
                DocumentPath path = toClearQueue.remove();
                Map<String, Versioned<TreeNodeValue>> children = docTree.getChildren(path);
                if (children.size() == 0) {
                    docTree.removeNode(path).value().discard();
                } else {
                    children.keySet()
                            .stream()
                            .forEach(name -> toClearQueue.add(new DocumentPath(name, path)));
                    toClearQueue.add(path);
                }
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Interface implemented by tree node values.
     */
    private interface TreeNodeValue {
        /**
         * Returns the raw {@code byte[]}.
         *
         * @return raw value
         */
        byte[] value();

        /**
         * Discards the value by invoke appropriate clean up actions.
         */
        void discard();
    }

    /**
     * A {@code TreeNodeValue} that is derived from a non-transactional update
     * i.e. via any standard tree update operation.
     */
    private class NonTransactionalCommit implements TreeNodeValue {
        private final Commit<? extends Update> commit;

        public NonTransactionalCommit(Commit<? extends Update> commit) {
            this.commit = commit;
        }

        @Override
        public byte[] value() {
            return commit.operation().value().orElse(null);
        }

        @Override
        public void discard() {
            commit.close();
        }
    }

    private void notifyListeners(DocumentPath path, DocumentTreeUpdateResult<byte[]> result) {
        if (result.status() != Status.OK) {
            return;
        }
        DocumentTreeEvent<byte[]> event =
                new DocumentTreeEvent<>(path,
                        result.created() ? Type.CREATED : result.newValue() == null ? Type.DELETED : Type.UPDATED,
                        Optional.ofNullable(result.newValue()),
                        Optional.ofNullable(result.oldValue()));

        listeners.values()
                 .stream()
                 .filter(l -> event.path().isDescendentOf(l.leastCommonAncestorPath()))
                 .forEach(listener -> listener.publish(AtomixDocumentTree.CHANGE_SUBJECT, Arrays.asList(event)));
    }

    @Override
    public void register(ServerSession session) {
    }

    @Override
    public void unregister(ServerSession session) {
        closeListener(session.id());
    }

    @Override
    public void expire(ServerSession session) {
        closeListener(session.id());
    }

    @Override
    public void close(ServerSession session) {
        closeListener(session.id());
    }

    private void closeListener(Long sessionId) {
        SessionListenCommits listenCommits = listeners.remove(sessionId);
        if (listenCommits != null) {
            listenCommits.close();
        }
    }

    private class SessionListenCommits {
        private final List<Commit<? extends Listen>> commits = Lists.newArrayList();
        private DocumentPath leastCommonAncestorPath;

        public void add(Commit<? extends Listen> commit) {
            commits.add(commit);
            recomputeLeastCommonAncestor();
        }

        public void remove(Commit<? extends Unlisten> commit) {
            // Remove the first listen commit with path matching path in unlisten commit
            Iterator<Commit<? extends Listen>> iterator = commits.iterator();
            while (iterator.hasNext()) {
                Commit<? extends Listen> listenCommit = iterator.next();
                if (listenCommit.operation().path().equals(commit.operation().path())) {
                    iterator.remove();
                    listenCommit.close();
                }
            }
            recomputeLeastCommonAncestor();
        }

        public DocumentPath leastCommonAncestorPath() {
            return leastCommonAncestorPath;
        }

        public <M> void publish(String topic, M message) {
            commits.stream().findAny().ifPresent(commit -> commit.session().publish(topic, message));
        }

        public void close() {
            commits.forEach(Commit::close);
            commits.clear();
            leastCommonAncestorPath = null;
        }

        private void recomputeLeastCommonAncestor() {
            this.leastCommonAncestorPath = DocumentPath.leastCommonAncestor(commits.stream()
                    .map(c -> c.operation().path())
                    .collect(Collectors.toList()));
        }
    }
}
