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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.resource.ResourceStateMachine;
import org.onlab.util.Match;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Versioned;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.CeilingEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.CeilingKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Clear;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.ContainsKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.ContainsValue;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.EntrySet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FirstEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FirstKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FloorEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.FloorKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Get;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.GetOrDefault;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.HigherEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.HigherKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.IsEmpty;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.KeySet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LastEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LastKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Listen;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LowerEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.LowerKey;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.PollFirstEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.PollLastEntry;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Size;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.SubMap;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Unlisten;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.UpdateAndGet;
import static org.onosproject.store.primitives.resources.impl.AtomixConsistentTreeMapCommands.Values;
import static org.onosproject.store.primitives.resources.impl.MapEntryUpdateResult.*;

/**
 * State machine corresponding to {@link AtomixConsistentTreeMap} backed by a
 * {@link TreeMap}.
 */
public class AtomixConsistentTreeMapState extends ResourceStateMachine implements SessionListener {

    private final Map<Long, Commit<? extends Listen>> listeners =
            Maps.newHashMap();
    private TreeMap<String, TreeMapEntryValue> tree = Maps.newTreeMap();
    private final Set<String> preparedKeys = Sets.newHashSet();

    private Function<Commit<SubMap>, NavigableMap<String, TreeMapEntryValue>> subMapFunction = this::subMap;
    private Function<Commit<FirstKey>, String> firstKeyFunction = this::firstKey;
    private Function<Commit<LastKey>, String> lastKeyFunction = this::lastKey;
    private Function<Commit<HigherEntry>, Map.Entry<String, Versioned<byte[]>>> higherEntryFunction =
            this::higherEntry;
    private Function<Commit<FirstEntry>, Map.Entry<String, Versioned<byte[]>>> firstEntryFunction =
            this::firstEntry;
    private Function<Commit<LastEntry>, Map.Entry<String, Versioned<byte[]>>> lastEntryFunction =
            this::lastEntry;
    private Function<Commit<PollFirstEntry>, Map.Entry<String, Versioned<byte[]>>> pollFirstEntryFunction =
            this::pollFirstEntry;
    private Function<Commit<PollLastEntry>, Map.Entry<String, Versioned<byte[]>>> pollLastEntryFunction =
            this::pollLastEntry;
    private Function<Commit<LowerEntry>, Map.Entry<String, Versioned<byte[]>>> lowerEntryFunction =
            this::lowerEntry;
    private Function<Commit<LowerKey>, String> lowerKeyFunction = this::lowerKey;
    private Function<Commit<FloorEntry>, Map.Entry<String, Versioned<byte[]>>> floorEntryFunction =
            this::floorEntry;
    private Function<Commit<CeilingEntry>, Map.Entry<String, Versioned<byte[]>>> ceilingEntryFunction =
            this::ceilingEntry;
    private Function<Commit<FloorKey>, String> floorKeyFunction = this::floorKey;
    private Function<Commit<CeilingKey>, String> ceilingKeyFunction = this::ceilingKey;
    private Function<Commit<HigherKey>, String> higherKeyFunction = this::higherKey;

    public AtomixConsistentTreeMapState(Properties properties) {
        super(properties);
    }

    @Override
    public void configure(StateMachineExecutor executor) {
        // Listeners
        executor.register(Listen.class, this::listen);
        executor.register(Unlisten.class, this::unlisten);
        // Queries
        executor.register(ContainsKey.class, this::containsKey);
        executor.register(ContainsValue.class, this::containsValue);
        executor.register(EntrySet.class, this::entrySet);
        executor.register(Get.class, this::get);
        executor.register(GetOrDefault.class, this::getOrDefault);
        executor.register(IsEmpty.class, this::isEmpty);
        executor.register(KeySet.class, this::keySet);
        executor.register(Size.class, this::size);
        executor.register(Values.class, this::values);
        executor.register(SubMap.class, subMapFunction);
        executor.register(FirstKey.class, firstKeyFunction);
        executor.register(LastKey.class, lastKeyFunction);
        executor.register(FirstEntry.class, firstEntryFunction);
        executor.register(LastEntry.class, lastEntryFunction);
        executor.register(PollFirstEntry.class, pollFirstEntryFunction);
        executor.register(PollLastEntry.class, pollLastEntryFunction);
        executor.register(LowerEntry.class, lowerEntryFunction);
        executor.register(LowerKey.class, lowerKeyFunction);
        executor.register(FloorEntry.class, floorEntryFunction);
        executor.register(FloorKey.class, floorKeyFunction);
        executor.register(CeilingEntry.class, ceilingEntryFunction);
        executor.register(CeilingKey.class, ceilingKeyFunction);
        executor.register(HigherEntry.class, higherEntryFunction);
        executor.register(HigherKey.class, higherKeyFunction);

        // Commands
        executor.register(UpdateAndGet.class, this::updateAndGet);
        executor.register(Clear.class, this::clear);
    }

    @Override
    public void delete() {
        listeners.values().forEach(Commit::close);
        listeners.clear();
        tree.values().forEach(TreeMapEntryValue::discard);
        tree.clear();
    }

    protected boolean containsKey(Commit<? extends ContainsKey> commit) {
        try {
            return toVersioned(tree.get((commit.operation().key()))) != null;
        } finally {
            commit.close();
        }
    }

    protected boolean containsValue(Commit<? extends ContainsValue> commit) {
        try {
            Match<byte[]> valueMatch = Match
                    .ifValue(commit.operation().value());
            return tree.values().stream().anyMatch(
                    value -> valueMatch.matches(value.value()));
        } finally {
            commit.close();
        }
    }

    protected Versioned<byte[]> get(Commit<? extends Get> commit) {
        try {
            return toVersioned(tree.get(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    protected Versioned<byte[]> getOrDefault(Commit<? extends GetOrDefault> commit) {
        try {
            Versioned<byte[]> value = toVersioned(tree.get(commit.operation().key()));
            return value != null ? value : new Versioned<>(commit.operation().defaultValue(), 0);
        } finally {
            commit.close();
        }
    }

    protected int size(Commit<? extends Size> commit) {
        try {
            return tree.size();
        } finally {
            commit.close();
        }
    }

    protected boolean isEmpty(Commit<? extends IsEmpty> commit) {
        try {
            return tree.isEmpty();
        } finally {
            commit.close();
        }
    }

    protected Set<String> keySet(Commit<? extends KeySet> commit) {
        try {
            return tree.keySet().stream().collect(Collectors.toSet());
        } finally {
            commit.close();
        }
    }

    protected Collection<Versioned<byte[]>> values(
            Commit<? extends Values> commit) {
        try {
            return tree.values().stream().map(this::toVersioned)
                    .collect(Collectors.toList());
        } finally {
            commit.close();
        }
    }

    protected Set<Map.Entry<String, Versioned<byte[]>>> entrySet(
            Commit<? extends EntrySet> commit) {
        try {
            return tree
                    .entrySet()
                    .stream()
                    .map(e -> Maps.immutableEntry(e.getKey(),
                                                  toVersioned(e.getValue())))
                    .collect(Collectors.toSet());
        } finally {
            commit.close();
        }
    }

    protected MapEntryUpdateResult<String, byte[]> updateAndGet(
            Commit<? extends UpdateAndGet> commit) {
        Status updateStatus = validate(commit.operation());
        String key = commit.operation().key();
        TreeMapEntryValue oldCommitValue = tree.get(commit.operation().key());
        Versioned<byte[]> oldTreeValue = toVersioned(oldCommitValue);

        if (updateStatus != Status.OK) {
            commit.close();
            return new MapEntryUpdateResult<>(updateStatus, "", key,
                                                  oldTreeValue, oldTreeValue);
        }

        byte[] newValue = commit.operation().value();
        long newVersion = commit.index();
        Versioned<byte[]> newTreeValue = newValue == null ? null
                : new Versioned<byte[]>(newValue, newVersion);

        MapEvent.Type updateType = newValue == null ? MapEvent.Type.REMOVE
                : oldCommitValue == null ? MapEvent.Type.INSERT :
                MapEvent.Type.UPDATE;
        if (updateType == MapEvent.Type.REMOVE ||
                updateType == MapEvent.Type.UPDATE) {
            tree.remove(key);
            oldCommitValue.discard();
        }
        if (updateType == MapEvent.Type.INSERT ||
                updateType == MapEvent.Type.UPDATE) {
            tree.put(key, new NonTransactionalCommit(newVersion, commit));
        } else {
            commit.close();
        }
        publish(Lists.newArrayList(new MapEvent<>("", key, newTreeValue,
                                                  oldTreeValue)));
        return new MapEntryUpdateResult<>(updateStatus, "", key, oldTreeValue,
                                          newTreeValue);
    }

    protected Status clear(
            Commit<? extends Clear> commit) {
        try {
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
                value.discard();
                iterator.remove();
            }
            return Status.OK;
        } finally {
            commit.close();
        }
    }

    protected void listen(
            Commit<? extends Listen> commit) {
        Long sessionId = commit.session().id();
        listeners.put(sessionId, commit);
        commit.session()
                .onStateChange(
                        state -> {
                            if (state == ServerSession.State.CLOSED
                                    || state == ServerSession.State.EXPIRED) {
                                Commit<? extends Listen> listener =
                                        listeners.remove(sessionId);
                                if (listener != null) {
                                    listener.close();
                                }
                            }
                        });
    }

    protected void unlisten(
            Commit<? extends Unlisten> commit) {
        try {
            Commit<? extends AtomixConsistentTreeMapCommands.Listen> listener =
                    listeners.remove(commit.session().id());
            if (listener != null) {
                listener.close();
            }
        } finally {
            commit.close();
        }
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
        //Do not support this until lazy communication is possible.  At present
        // it transmits up to the entire map.
        try {
            SubMap<String, TreeMapEntryValue> subMap = commit.operation();
            return tree.subMap(subMap.fromKey(), subMap.isInclusiveFrom(),
                               subMap.toKey(), subMap.isInclusiveTo());
        } finally {
            commit.close();
        }
    }

    protected String firstKey(Commit<? extends FirstKey> commit) {
        try {
            if (tree.isEmpty()) {
                return null;
            }
            return tree.firstKey();
        } finally {
            commit.close();
        }
    }

    protected String lastKey(Commit<? extends LastKey> commit) {
        try {
            return tree.isEmpty() ? null : tree.lastKey();
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> higherEntry(
            Commit<? extends HigherEntry> commit) {
        try {
            if (tree.isEmpty()) {
                return null;
            }
            return toVersionedEntry(
                    tree.higherEntry(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> firstEntry(
            Commit<? extends FirstEntry> commit) {
        try {
            if (tree.isEmpty()) {
                return null;
            }
            return toVersionedEntry(tree.firstEntry());
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> lastEntry(
            Commit<? extends LastEntry> commit) {
        try {
            if (tree.isEmpty()) {
                return null;
            }
            return toVersionedEntry(tree.lastEntry());
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> pollFirstEntry(
            Commit<? extends PollFirstEntry> commit) {
        try {
            return toVersionedEntry(tree.pollFirstEntry());
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> pollLastEntry(
            Commit<? extends PollLastEntry> commit) {
        try {
            return toVersionedEntry(tree.pollLastEntry());
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> lowerEntry(
            Commit<? extends LowerEntry> commit) {
        try {
            return toVersionedEntry(tree.lowerEntry(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    protected String lowerKey(Commit<? extends LowerKey> commit) {
        try {
            return tree.lowerKey(commit.operation().key());
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> floorEntry(
            Commit<? extends FloorEntry> commit) {
        try {
            return toVersionedEntry(tree.floorEntry(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    protected String floorKey(Commit<? extends FloorKey> commit) {
        try {
            return tree.floorKey(commit.operation().key());
        } finally {
            commit.close();
        }
    }

    protected Map.Entry<String, Versioned<byte[]>> ceilingEntry(
            Commit<CeilingEntry> commit) {
        try {
            return toVersionedEntry(
                    tree.ceilingEntry(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    protected String ceilingKey(Commit<CeilingKey> commit) {
        try {
            return tree.ceilingKey(commit.operation().key());
        } finally {
            commit.close();
        }
    }

    protected String higherKey(Commit<HigherKey> commit) {
        try {
            return tree.higherKey(commit.operation().key());
        } finally {
            commit.close();
        }
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
        listeners.values().forEach(commit -> commit.session()
                .publish(AtomixConsistentTreeMap.CHANGE_SUBJECT, events));
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
        Commit<? extends Listen> commit = listeners.remove(sessionId);
        if (commit != null) {
            commit.close();
        }
    }

    private interface TreeMapEntryValue {

        byte[] value();

        long version();

        void discard();
    }

    private class NonTransactionalCommit implements TreeMapEntryValue {
        private final long version;
        private final Commit<? extends UpdateAndGet> commit;

        public NonTransactionalCommit(long version,
                                      Commit<? extends UpdateAndGet> commit) {
            this.version = version;
            this.commit = commit;
        }

        @Override
        public byte[] value() {
            return commit.operation().value();
        }

        @Override
        public long version() {
            return version;
        }

        @Override
        public void discard() {
            commit.close();
        }
    }
}
