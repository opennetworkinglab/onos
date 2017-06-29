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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.atomix.protocols.raft.service.AbstractRaftService;
import io.atomix.protocols.raft.service.Commit;
import io.atomix.protocols.raft.service.RaftServiceExecutor;
import io.atomix.protocols.raft.session.RaftSession;
import io.atomix.protocols.raft.storage.snapshot.SnapshotReader;
import io.atomix.protocols.raft.storage.snapshot.SnapshotWriter;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Match;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapEvents.CHANGE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ADD_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CEILING_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CEILING_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CLEAR;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CONTAINS_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CONTAINS_VALUE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CeilingEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.CeilingKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.ENTRY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FIRST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FIRST_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FLOOR_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FLOOR_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FloorEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.FloorKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.GET_OR_DEFAULT;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.GetOrDefault;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HIGHER_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HIGHER_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HigherEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.HigherKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.IS_EMPTY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.KEY_SET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LAST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LAST_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LOWER_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LOWER_KEY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LowerEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.LowerKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.POLL_FIRST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.POLL_LAST_ENTRY;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.REMOVE_LISTENER;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.SIZE;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.SUB_MAP;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.SubMap;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.UPDATE_AND_GET;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.UpdateAndGet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapOperations.VALUES;
import static org.onosproject.store.primitives.resources.impl.MapEntryUpdateResult.Status;

/**
 * State machine corresponding to {@link AtomixConsistentTreeMap} backed by a
 * {@link TreeMap}.
 */
public class AtomixConsistentTreeMapService extends AbstractRaftService {

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespace.newBuilder()
            .register(KryoNamespaces.BASIC)
            .register(AtomixConsistentTreeMapOperations.NAMESPACE)
            .register(AtomixConsistentTreeMapEvents.NAMESPACE)
            .register(TreeMapEntryValue.class)
            .register(new HashMap<>().keySet().getClass())
            .register(TreeMap.class)
            .build());

    private final Map<Long, RaftSession> listeners = Maps.newHashMap();
    private TreeMap<String, TreeMapEntryValue> tree = Maps.newTreeMap();
    private final Set<String> preparedKeys = Sets.newHashSet();

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeObject(Sets.newHashSet(listeners.keySet()), SERIALIZER::encode);
        writer.writeObject(preparedKeys, SERIALIZER::encode);
        writer.writeObject(tree, SERIALIZER::encode);
    }

    @Override
    public void install(SnapshotReader reader) {
        listeners.clear();
        for (long sessionId : reader.<Set<Long>>readObject(SERIALIZER::decode)) {
            listeners.put(sessionId, getSessions().getSession(sessionId));
        }

        preparedKeys.clear();
        preparedKeys.addAll(reader.readObject(SERIALIZER::decode));

        tree.clear();
        tree.putAll(reader.readObject(SERIALIZER::decode));
    }

    @Override
    public void configure(RaftServiceExecutor executor) {
        // Listeners
        executor.register(ADD_LISTENER, this::listen);
        executor.register(REMOVE_LISTENER, this::unlisten);
        // Queries
        executor.register(CONTAINS_KEY, SERIALIZER::decode, this::containsKey, SERIALIZER::encode);
        executor.register(CONTAINS_VALUE, SERIALIZER::decode, this::containsValue, SERIALIZER::encode);
        executor.register(ENTRY_SET, this::entrySet, SERIALIZER::encode);
        executor.register(GET, SERIALIZER::decode, this::get, SERIALIZER::encode);
        executor.register(GET_OR_DEFAULT, SERIALIZER::decode, this::getOrDefault, SERIALIZER::encode);
        executor.register(IS_EMPTY, this::isEmpty, SERIALIZER::encode);
        executor.register(KEY_SET, this::keySet, SERIALIZER::encode);
        executor.register(SIZE, this::size, SERIALIZER::encode);
        executor.register(VALUES, this::values, SERIALIZER::encode);
        executor.register(SUB_MAP, SERIALIZER::decode, this::subMap, SERIALIZER::encode);
        executor.register(FIRST_KEY, this::firstKey, SERIALIZER::encode);
        executor.register(LAST_KEY, this::lastKey, SERIALIZER::encode);
        executor.register(FIRST_ENTRY, this::firstEntry, SERIALIZER::encode);
        executor.register(LAST_ENTRY, this::lastEntry, SERIALIZER::encode);
        executor.register(POLL_FIRST_ENTRY, this::pollFirstEntry, SERIALIZER::encode);
        executor.register(POLL_LAST_ENTRY, this::pollLastEntry, SERIALIZER::encode);
        executor.register(LOWER_ENTRY, SERIALIZER::decode, this::lowerEntry, SERIALIZER::encode);
        executor.register(LOWER_KEY, SERIALIZER::decode, this::lowerKey, SERIALIZER::encode);
        executor.register(FLOOR_ENTRY, SERIALIZER::decode, this::floorEntry, SERIALIZER::encode);
        executor.register(FLOOR_KEY, SERIALIZER::decode, this::floorKey, SERIALIZER::encode);
        executor.register(CEILING_ENTRY, SERIALIZER::decode, this::ceilingEntry, SERIALIZER::encode);
        executor.register(CEILING_KEY, SERIALIZER::decode, this::ceilingKey, SERIALIZER::encode);
        executor.register(HIGHER_ENTRY, SERIALIZER::decode, this::higherEntry, SERIALIZER::encode);
        executor.register(HIGHER_KEY, SERIALIZER::decode, this::higherKey, SERIALIZER::encode);

        // Commands
        executor.register(UPDATE_AND_GET, SERIALIZER::decode, this::updateAndGet, SERIALIZER::encode);
        executor.register(CLEAR, this::clear, SERIALIZER::encode);
    }

    protected boolean containsKey(Commit<? extends ContainsKey> commit) {
        return toVersioned(tree.get((commit.value().key()))) != null;
    }

    protected boolean containsValue(Commit<? extends ContainsValue> commit) {
        Match<byte[]> valueMatch = Match
                .ifValue(commit.value().value());
        return tree.values().stream().anyMatch(
                value -> valueMatch.matches(value.value()));
    }

    protected Versioned<byte[]> get(Commit<? extends Get> commit) {
        return toVersioned(tree.get(commit.value().key()));
    }

    protected Versioned<byte[]> getOrDefault(Commit<? extends GetOrDefault> commit) {
        Versioned<byte[]> value = toVersioned(tree.get(commit.value().key()));
        return value != null ? value : new Versioned<>(commit.value().defaultValue(), 0);
    }

    protected int size(Commit<Void> commit) {
        return tree.size();
    }

    protected boolean isEmpty(Commit<Void> commit) {
        return tree.isEmpty();
    }

    protected Set<String> keySet(Commit<Void> commit) {
        return tree.keySet().stream().collect(Collectors.toSet());
    }

    protected Collection<Versioned<byte[]>> values(Commit<Void> commit) {
        return tree.values().stream().map(this::toVersioned)
                .collect(Collectors.toList());
    }

    protected Set<Map.Entry<String, Versioned<byte[]>>> entrySet(Commit<Void> commit) {
        return tree
                .entrySet()
                .stream()
                .map(e -> Maps.immutableEntry(e.getKey(),
                        toVersioned(e.getValue())))
                .collect(Collectors.toSet());
    }

    protected MapEntryUpdateResult<String, byte[]> updateAndGet(Commit<? extends UpdateAndGet> commit) {
        Status updateStatus = validate(commit.value());
        String key = commit.value().key();
        TreeMapEntryValue oldCommitValue = tree.get(commit.value().key());
        Versioned<byte[]> oldTreeValue = toVersioned(oldCommitValue);

        if (updateStatus != Status.OK) {
            return new MapEntryUpdateResult<>(updateStatus, "", key,
                    oldTreeValue, oldTreeValue);
        }

        byte[] newValue = commit.value().value();
        long newVersion = commit.index();
        Versioned<byte[]> newTreeValue = newValue == null ? null
                : new Versioned<byte[]>(newValue, newVersion);

        MapEvent.Type updateType = newValue == null ? MapEvent.Type.REMOVE
                : oldCommitValue == null ? MapEvent.Type.INSERT :
                MapEvent.Type.UPDATE;
        if (updateType == MapEvent.Type.REMOVE ||
                updateType == MapEvent.Type.UPDATE) {
            tree.remove(key);
        }
        if (updateType == MapEvent.Type.INSERT ||
                updateType == MapEvent.Type.UPDATE) {
            tree.put(key, new TreeMapEntryValue(newVersion, commit.value().value()));
        }
        publish(Lists.newArrayList(new MapEvent<>("", key, newTreeValue,
                oldTreeValue)));
        return new MapEntryUpdateResult<>(updateStatus, "", key, oldTreeValue,
                newTreeValue);
    }

    protected Status clear(Commit<Void> commit) {
        Iterator<Map.Entry<String, TreeMapEntryValue>> iterator = tree
                .entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TreeMapEntryValue> entry = iterator.next();
            String key = entry.getKey();
            TreeMapEntryValue value = entry.getValue();
            Versioned<byte[]> removedValue =
                    new Versioned<byte[]>(value.value(),
                            value.version());
            publish(Lists.newArrayList(new MapEvent<>("", key, null,
                    removedValue)));
            iterator.remove();
        }
        return Status.OK;
    }

    protected void listen(Commit<Void> commit) {
        listeners.put(commit.session().sessionId().id(), commit.session());
    }

    protected void unlisten(Commit<Void> commit) {
        closeListener(commit.session().sessionId().id());
    }

    private Status validate(UpdateAndGet update) {
        TreeMapEntryValue existingValue = tree.get(update.key());
        if (existingValue == null && update.value() == null) {
            return Status.NOOP;
        }
        if (preparedKeys.contains(update.key())) {
            return Status.WRITE_LOCK;
        }
        byte[] existingRawValue = existingValue == null ? null :
                existingValue.value();
        Long existingVersion = existingValue == null ? null :
                existingValue.version();
        return update.valueMatch().matches(existingRawValue)
                && update.versionMatch().matches(existingVersion) ?
                Status.OK
                : Status.PRECONDITION_FAILED;
    }

    protected NavigableMap<String, TreeMapEntryValue> subMap(
            Commit<? extends SubMap> commit) {
        // Do not support this until lazy communication is possible.  At present
        // it transmits up to the entire map.
        SubMap<String, TreeMapEntryValue> subMap = commit.value();
        return tree.subMap(subMap.fromKey(), subMap.isInclusiveFrom(),
                subMap.toKey(), subMap.isInclusiveTo());
    }

    protected String firstKey(Commit<Void> commit) {
        if (tree.isEmpty()) {
            return null;
        }
        return tree.firstKey();
    }

    protected String lastKey(Commit<Void> commit) {
        return tree.isEmpty() ? null : tree.lastKey();
    }

    protected Map.Entry<String, Versioned<byte[]>> higherEntry(Commit<? extends HigherEntry> commit) {
        if (tree.isEmpty()) {
            return null;
        }
        return toVersionedEntry(
                tree.higherEntry(commit.value().key()));
    }

    protected Map.Entry<String, Versioned<byte[]>> firstEntry(Commit<Void> commit) {
        if (tree.isEmpty()) {
            return null;
        }
        return toVersionedEntry(tree.firstEntry());
    }

    protected Map.Entry<String, Versioned<byte[]>> lastEntry(Commit<Void> commit) {
        if (tree.isEmpty()) {
            return null;
        }
        return toVersionedEntry(tree.lastEntry());
    }

    protected Map.Entry<String, Versioned<byte[]>> pollFirstEntry(Commit<Void> commit) {
        return toVersionedEntry(tree.pollFirstEntry());
    }

    protected Map.Entry<String, Versioned<byte[]>> pollLastEntry(Commit<Void> commit) {
        return toVersionedEntry(tree.pollLastEntry());
    }

    protected Map.Entry<String, Versioned<byte[]>> lowerEntry(Commit<? extends LowerEntry> commit) {
        return toVersionedEntry(tree.lowerEntry(commit.value().key()));
    }

    protected String lowerKey(Commit<? extends LowerKey> commit) {
        return tree.lowerKey(commit.value().key());
    }

    protected Map.Entry<String, Versioned<byte[]>> floorEntry(Commit<? extends FloorEntry> commit) {
        return toVersionedEntry(tree.floorEntry(commit.value().key()));
    }

    protected String floorKey(Commit<? extends FloorKey> commit) {
        return tree.floorKey(commit.value().key());
    }

    protected Map.Entry<String, Versioned<byte[]>> ceilingEntry(Commit<CeilingEntry> commit) {
        return toVersionedEntry(
                tree.ceilingEntry(commit.value().key()));
    }

    protected String ceilingKey(Commit<CeilingKey> commit) {
        return tree.ceilingKey(commit.value().key());
    }

    protected String higherKey(Commit<HigherKey> commit) {
        return tree.higherKey(commit.value().key());
    }

    private Versioned<byte[]> toVersioned(TreeMapEntryValue value) {
        return value == null ? null :
                new Versioned<byte[]>(value.value(), value.version());
    }

    private Map.Entry<String, Versioned<byte[]>> toVersionedEntry(
            Map.Entry<String, TreeMapEntryValue> entry) {
        //FIXME is this the best type of entry to return?
        return entry == null ? null : new SimpleImmutableEntry<>(
                entry.getKey(), toVersioned(entry.getValue()));
    }

    private void publish(List<MapEvent<String, byte[]>> events) {
        listeners.values().forEach(session -> session.publish(CHANGE, SERIALIZER::encode, events));
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

    private static class TreeMapEntryValue {
        private final long version;
        private final byte[] value;

        public TreeMapEntryValue(long version, byte[] value) {
            this.version = version;
            this.value = value;
        }

        public byte[] value() {
            return value;
        }

        public long version() {
            return version;
        }
    }
}