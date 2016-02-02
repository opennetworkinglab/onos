/*
 * Copyright 2016 Open Networking Laboratory
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

import static org.onosproject.store.service.MapEvent.Type.INSERT;
import static org.onosproject.store.service.MapEvent.Type.REMOVE;
import static org.onosproject.store.service.MapEvent.Type.UPDATE;
import io.atomix.copycat.client.session.Session;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.onlab.util.CountDownCompleter;
import org.onlab.util.Match;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepare;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Versioned;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkState;

/**
 * State Machine for {@link AtomixConsistentMap} resource.
 */
public class AtomixConsistentMapState extends ResourceStateMachine implements
        SessionListener, Snapshottable {
    private final Map<Long, Commit<? extends AtomixConsistentMapCommands.Listen>> listeners = new HashMap<>();
    private final Map<String, MapEntryValue> mapEntries = new HashMap<>();
    private final Set<String> preparedKeys = Sets.newHashSet();
    private final Map<TransactionId, Commit<? extends TransactionPrepare>> pendingTransactions = Maps
            .newHashMap();
    private AtomicLong versionCounter = new AtomicLong(0);

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
        executor.register(AtomixConsistentMapCommands.Listen.class,
                this::listen);
        executor.register(AtomixConsistentMapCommands.Unlisten.class,
                this::unlisten);
        // Queries
        executor.register(AtomixConsistentMapCommands.ContainsKey.class,
                this::containsKey);
        executor.register(AtomixConsistentMapCommands.ContainsValue.class,
                this::containsValue);
        executor.register(AtomixConsistentMapCommands.EntrySet.class,
                this::entrySet);
        executor.register(AtomixConsistentMapCommands.Get.class, this::get);
        executor.register(AtomixConsistentMapCommands.IsEmpty.class,
                this::isEmpty);
        executor.register(AtomixConsistentMapCommands.KeySet.class,
                this::keySet);
        executor.register(AtomixConsistentMapCommands.Size.class, this::size);
        executor.register(AtomixConsistentMapCommands.Values.class,
                this::values);
        // Commands
        executor.register(AtomixConsistentMapCommands.UpdateAndGet.class,
                this::updateAndGet);
        executor.register(AtomixConsistentMapCommands.Clear.class, this::clear);
        executor.register(AtomixConsistentMapCommands.TransactionPrepare.class,
                this::prepare);
        executor.register(AtomixConsistentMapCommands.TransactionCommit.class,
                this::commit);
        executor.register(
                AtomixConsistentMapCommands.TransactionRollback.class,
                this::rollback);
    }

    @Override
    public void delete() {
        // Delete Listeners
        listeners.values().forEach(Commit::close);
        listeners.clear();

        // Delete Map entries
        mapEntries.values().forEach(MapEntryValue::discard);
        mapEntries.clear();
    }

    /**
     * Handles a contains key commit.
     *
     * @param commit
     *            containsKey commit
     * @return {@code true} if map contains key
     */
    protected boolean containsKey(
            Commit<? extends AtomixConsistentMapCommands.ContainsKey> commit) {
        try {
            return toVersioned(mapEntries.get(commit.operation().key())) != null;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a contains value commit.
     *
     * @param commit
     *            containsValue commit
     * @return {@code true} if map contains value
     */
    protected boolean containsValue(
            Commit<? extends AtomixConsistentMapCommands.ContainsValue> commit) {
        try {
            Match<byte[]> valueMatch = Match
                    .ifValue(commit.operation().value());
            return mapEntries.values().stream()
                    .anyMatch(value -> valueMatch.matches(value.value()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a get commit.
     *
     * @param commit
     *            get commit
     * @return value mapped to key
     */
    protected Versioned<byte[]> get(
            Commit<? extends AtomixConsistentMapCommands.Get> commit) {
        try {
            return toVersioned(mapEntries.get(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a count commit.
     *
     * @param commit
     *            size commit
     * @return number of entries in map
     */
    protected int size(Commit<? extends AtomixConsistentMapCommands.Size> commit) {
        try {
            return mapEntries.size();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an is empty commit.
     *
     * @param commit
     *            isEmpty commit
     * @return {@code true} if map is empty
     */
    protected boolean isEmpty(
            Commit<? extends AtomixConsistentMapCommands.IsEmpty> commit) {
        try {
            return mapEntries.isEmpty();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a keySet commit.
     *
     * @param commit
     *            keySet commit
     * @return set of keys in map
     */
    protected Set<String> keySet(
            Commit<? extends AtomixConsistentMapCommands.KeySet> commit) {
        try {
            return mapEntries.keySet();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a values commit.
     *
     * @param commit
     *            values commit
     * @return collection of values in map
     */
    protected Collection<Versioned<byte[]>> values(
            Commit<? extends AtomixConsistentMapCommands.Values> commit) {
        try {
            return mapEntries.values().stream().map(this::toVersioned)
                    .collect(Collectors.toList());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a entry set commit.
     *
     * @param commit
     *            entrySet commit
     * @return set of map entries
     */
    protected Set<Map.Entry<String, Versioned<byte[]>>> entrySet(
            Commit<? extends AtomixConsistentMapCommands.EntrySet> commit) {
        try {
            return mapEntries
                    .entrySet()
                    .stream()
                    .map(e -> Maps.immutableEntry(e.getKey(),
                            toVersioned(e.getValue())))
                    .collect(Collectors.toSet());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a update and get commit.
     *
     * @param commit
     *            updateAndGet commit
     * @return update result
     */
    protected MapEntryUpdateResult<String, byte[]> updateAndGet(
            Commit<? extends AtomixConsistentMapCommands.UpdateAndGet> commit) {
        MapEntryUpdateResult.Status updateStatus = validate(commit.operation());
        String key = commit.operation().key();
        MapEntryValue oldCommitValue = mapEntries.get(commit.operation().key());
        Versioned<byte[]> oldMapValue = toVersioned(oldCommitValue);

        if (updateStatus != MapEntryUpdateResult.Status.OK) {
            commit.close();
            return new MapEntryUpdateResult<>(updateStatus, "", key,
                    oldMapValue, oldMapValue);
        }

        byte[] newValue = commit.operation().value();
        long newVersion = versionCounter.incrementAndGet();
        Versioned<byte[]> newMapValue = newValue == null ? null
                : new Versioned<>(newValue, newVersion);

        MapEvent.Type updateType = newValue == null ? REMOVE
                : oldCommitValue == null ? INSERT : UPDATE;
        if (updateType == REMOVE || updateType == UPDATE) {
            mapEntries.remove(key);
            oldCommitValue.discard();
        }
        if (updateType == INSERT || updateType == UPDATE) {
            mapEntries.put(key, new NonTransactionalCommit(newVersion, commit));
        }
        notify(new MapEvent<>("", key, newMapValue, oldMapValue));
        return new MapEntryUpdateResult<>(updateStatus, "", key, oldMapValue,
                newMapValue);
    }

    /**
     * Handles a clear commit.
     *
     * @param commit
     *            clear commit
     * @return clear result
     */
    protected MapEntryUpdateResult.Status clear(
            Commit<? extends AtomixConsistentMapCommands.Clear> commit) {
        try {
            Iterator<Map.Entry<String, MapEntryValue>> iterator = mapEntries
                    .entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, MapEntryValue> entry = iterator.next();
                String key = entry.getKey();
                MapEntryValue value = entry.getValue();
                Versioned<byte[]> removedValue = new Versioned<>(value.value(),
                        value.version());
                notify(new MapEvent<>("", key, null, removedValue));
                value.discard();
                iterator.remove();
            }
            return MapEntryUpdateResult.Status.OK;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a listen commit.
     *
     * @param commit
     *            listen commit
     */
    protected void listen(
            Commit<? extends AtomixConsistentMapCommands.Listen> commit) {
        Long sessionId = commit.session().id();
        listeners.put(sessionId, commit);
        commit.session()
                .onStateChange(
                        state -> {
                            if (state == Session.State.CLOSED
                                    || state == Session.State.EXPIRED) {
                                Commit<? extends AtomixConsistentMapCommands.Listen> listener = listeners
                                        .remove(sessionId);
                                if (listener != null) {
                                    listener.close();
                                }
                            }
                        });
    }

    /**
     * Handles an unlisten commit.
     *
     * @param commit
     *            unlisten commit
     */
    protected void unlisten(
            Commit<? extends AtomixConsistentMapCommands.Unlisten> commit) {
        try {
            Commit<? extends AtomixConsistentMapCommands.Listen> listener = listeners
                    .remove(commit.session());
            if (listener != null) {
                listener.close();
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Triggers a change event.
     *
     * @param value
     *            map event
     */
    private void notify(MapEvent<String, byte[]> value) {
        listeners.values().forEach(
                commit -> commit.session().publish("change", value));
    }

    /**
     * Handles an prepare commit.
     *
     * @param commit
     *            transaction prepare commit
     * @return prepare result
     */
    protected PrepareResult prepare(
            Commit<? extends AtomixConsistentMapCommands.TransactionPrepare> commit) {
        boolean ok = false;
        try {
            TransactionalMapUpdate<String, byte[]> transactionUpdate = commit
                    .operation().transactionUpdate();
            for (MapUpdate<String, byte[]> update : transactionUpdate.batch()) {
                String key = update.key();
                if (preparedKeys.contains(key)) {
                    return PrepareResult.CONCURRENT_TRANSACTION;
                }
                MapEntryValue existingValue = mapEntries.get(key);
                if (existingValue == null) {
                    if (update.currentValue() != null) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    }
                } else {
                    if (existingValue.version() != update.currentVersion()) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    }
                }
            }
            // No violations detected. Add to pendingTranctions and mark
            // modified keys as
            // currently locked to updates.
            pendingTransactions.put(transactionUpdate.transactionId(), commit);
            transactionUpdate.batch().forEach(u -> preparedKeys.add(u.key()));
            ok = true;
            return PrepareResult.OK;
        } finally {
            if (!ok) {
                commit.close();
            }
        }
    }

    /**
     * Handles an commit commit (ha!).
     *
     * @param commit transaction commit commit
     * @return commit result
     */
    protected CommitResult commit(
            Commit<? extends AtomixConsistentMapCommands.TransactionCommit> commit) {
        TransactionId transactionId = commit.operation().transactionId();
        try {
            Commit<? extends AtomixConsistentMapCommands.TransactionPrepare> prepareCommit = pendingTransactions
                    .remove(transactionId);
            if (prepareCommit == null) {
                return CommitResult.UNKNOWN_TRANSACTION_ID;
            }
            TransactionalMapUpdate<String, byte[]> transactionalUpdate = prepareCommit
                    .operation().transactionUpdate();
            long totalReferencesToCommit = transactionalUpdate
                    .batch()
                    .stream()
                    .filter(update -> update.type() != MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                    .count();
            CountDownCompleter<Commit<? extends AtomixConsistentMapCommands.TransactionPrepare>> completer =
                    new CountDownCompleter<>(prepareCommit, totalReferencesToCommit, Commit::close);
            for (MapUpdate<String, byte[]> update : transactionalUpdate.batch()) {
                String key = update.key();
                MapEntryValue previousValue = mapEntries.remove(key);
                MapEntryValue newValue = null;
                checkState(preparedKeys.remove(key), "key is not prepared");
                if (update.type() != MapUpdate.Type.REMOVE_IF_VERSION_MATCH) {
                    newValue = new TransactionalCommit(key,
                            versionCounter.incrementAndGet(), completer);
                }
                mapEntries.put(key, newValue);
                // Notify map listeners
                notify(new MapEvent<>("", key, toVersioned(newValue),
                        toVersioned(previousValue)));
            }
            return CommitResult.OK;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an rollback commit (ha!).
     *
     * @param commit transaction rollback commit
     * @return rollback result
     */
    protected RollbackResult rollback(
            Commit<? extends AtomixConsistentMapCommands.TransactionRollback> commit) {
        TransactionId transactionId = commit.operation().transactionId();
        try {
            Commit<? extends AtomixConsistentMapCommands.TransactionPrepare> prepareCommit = pendingTransactions
                    .remove(transactionId);
            if (prepareCommit == null) {
                return RollbackResult.UNKNOWN_TRANSACTION_ID;
            } else {
                prepareCommit.operation().transactionUpdate().batch()
                        .forEach(u -> preparedKeys.remove(u.key()));
                prepareCommit.close();
                return RollbackResult.OK;
            }
        } finally {
            commit.close();
        }
    }

    private MapEntryUpdateResult.Status validate(
            AtomixConsistentMapCommands.UpdateAndGet update) {
        MapEntryValue existingValue = mapEntries.get(update.key());
        if (existingValue == null && update.value() == null) {
            return MapEntryUpdateResult.Status.NOOP;
        }
        if (preparedKeys.contains(update.key())) {
            return MapEntryUpdateResult.Status.WRITE_LOCK;
        }
        byte[] existingRawValue = existingValue == null ? null : existingValue
                .value();
        Long existingVersion = existingValue == null ? null : existingValue
                .version();
        return update.valueMatch().matches(existingRawValue)
                && update.versionMatch().matches(existingVersion) ? MapEntryUpdateResult.Status.OK
                : MapEntryUpdateResult.Status.PRECONDITION_FAILED;
    }

    private Versioned<byte[]> toVersioned(MapEntryValue value) {
        return value == null ? null : new Versioned<>(value.value(),
                value.version());
    }

    @Override
    public void register(Session session) {
    }

    @Override
    public void unregister(Session session) {
        closeListener(session.id());
    }

    @Override
    public void expire(Session session) {
        closeListener(session.id());
    }

    @Override
    public void close(Session session) {
        closeListener(session.id());
    }

    private void closeListener(Long sessionId) {
        Commit<? extends AtomixConsistentMapCommands.Listen> commit = listeners
                .remove(sessionId);
        if (commit != null) {
            commit.close();
        }
    }

    /**
     * Interface implemented by map values.
     */
    private interface MapEntryValue {
        /**
         * Returns the raw {@code byte[]}.
         *
         * @return raw value
         */
        byte[] value();

        /**
         * Returns the version of the value.
         *
         * @return version
         */
        long version();

        /**
         * Discards the value by invoke appropriate clean up actions.
         */
        void discard();
    }

    /**
     * A {@code MapEntryValue} that is derived from a non-transactional update
     * i.e. via any standard map update operation.
     */
    private class NonTransactionalCommit implements MapEntryValue {
        private final long version;
        private final Commit<? extends AtomixConsistentMapCommands.UpdateAndGet> commit;

        public NonTransactionalCommit(
                long version,
                Commit<? extends AtomixConsistentMapCommands.UpdateAndGet> commit) {
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

    /**
     * A {@code MapEntryValue} that is derived from updates submitted via a
     * transaction.
     */
    private class TransactionalCommit implements MapEntryValue {
        private final String key;
        private final long version;
        private final CountDownCompleter<Commit<? extends AtomixConsistentMapCommands.TransactionPrepare>> completer;

        public TransactionalCommit(
                String key,
                long version,
                CountDownCompleter<Commit<? extends AtomixConsistentMapCommands.TransactionPrepare>> commit) {
            this.key = key;
            this.version = version;
            this.completer = commit;
        }

        @Override
        public byte[] value() {
            TransactionalMapUpdate<String, byte[]> update = completer.object()
                    .operation().transactionUpdate();
            return update.valueForKey(key);
        }

        @Override
        public long version() {
            return version;
        }

        @Override
        public void discard() {
            completer.countDown();
        }
    }
}
