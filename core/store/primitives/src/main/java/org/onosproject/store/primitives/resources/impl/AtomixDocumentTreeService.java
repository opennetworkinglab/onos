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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import io.atomix.protocols.raft.event.EventType;
import io.atomix.protocols.raft.service.AbstractRaftService;
import io.atomix.protocols.raft.service.Commit;
import io.atomix.protocols.raft.service.RaftServiceExecutor;
import io.atomix.protocols.raft.session.RaftSession;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Get;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GetChildren;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.Update;
import org.onosproject.store.primitives.resources.impl.DocumentTreeUpdateResult.Status;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.DocumentTreeEvent;
import org.onosproject.store.service.DocumentTreeEvent.Type;
import org.onosproject.store.service.IllegalDocumentModificationException;
import org.onosproject.store.service.NoSuchDocumentPathException;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.GET_CHILDREN;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeOperations.UPDATE;

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
                    return new Listener(getSessions().getSession(input.readLong()),
                            kryo.readObjectOrNull(input, DocumentPath.class));
                }
            }, Listener.class)
            .register(Versioned.class)
            .register(DocumentPath.class)
            .register(new HashMap().keySet().getClass())
            .register(TreeMap.class)
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
    private DocumentTree<byte[]> docTree = new DefaultDocumentTree<>(versionCounter::incrementAndGet);

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeLong(versionCounter.get());
        writer.writeObject(listeners, serializer::encode);
        writer.writeObject(docTree, serializer::encode);
    }

    @Override
    public void install(SnapshotReader reader) {
        versionCounter = new AtomicLong(reader.readLong());
        listeners = reader.readObject(serializer::decode);
        docTree = reader.readObject(serializer::decode);
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
            return value == null ? null : value.map(node -> node == null ? null : node);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    protected Map<String, Versioned<byte[]>> getChildren(Commit<? extends GetChildren> commit) {
        return docTree.getChildren(commit.value().path());
    }

    protected DocumentTreeUpdateResult<byte[]> update(Commit<? extends Update> commit) {
        DocumentTreeUpdateResult<byte[]> result = null;
        DocumentPath path = commit.value().path();
        boolean updated = false;
        Versioned<byte[]> currentValue = docTree.get(path);
        try {
            Match<Long> versionMatch = commit.value().versionMatch();
            Match<byte[]> valueMatch = commit.value().valueMatch();

            if (versionMatch.matches(currentValue == null ? null : currentValue.version())
                    && valueMatch.matches(currentValue == null ? null : currentValue.value())) {
                if (commit.value().value() == null) {
                    docTree.removeNode(path);
                } else {
                    docTree.set(path, commit.value().value().orElse(null));
                }
                updated = true;
            }
            Versioned<byte[]> newValue = updated ? docTree.get(path) : currentValue;
            Status updateStatus = updated
                    ? Status.OK : commit.value().value() == null ? Status.INVALID_PATH : Status.NOOP;
            result = new DocumentTreeUpdateResult<>(path, updateStatus, newValue, currentValue);
        } catch (IllegalDocumentModificationException e) {
            result = DocumentTreeUpdateResult.illegalModification(path);
        } catch (NoSuchDocumentPathException e) {
            result = DocumentTreeUpdateResult.invalidPath(path);
        } catch (Exception e) {
            getLogger().error("Failed to apply {} to state machine", commit.value(), e);
            throw Throwables.propagate(e);
        }
        notifyListeners(path, result);
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