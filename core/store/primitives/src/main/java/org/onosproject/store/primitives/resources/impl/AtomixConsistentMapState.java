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

import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.store.service.MapEvent.Type.INSERT;
import static org.onosproject.store.service.MapEvent.Type.REMOVE;
import static org.onosproject.store.service.MapEvent.Type.UPDATE;
import static org.slf4j.LoggerFactory.getLogger;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachineExecutor;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import io.atomix.resource.ResourceStateMachine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.onlab.util.CountDownCompleter;
import org.onlab.util.Match;
import org.onosproject.store.primitives.MapUpdate;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Clear;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.ContainsKey;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.ContainsValue;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.EntrySet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Get;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.GetOrDefault;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.IsEmpty;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.KeySet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Size;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionBegin;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepare;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepareAndCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionRollback;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.UpdateAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Values;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.TransactionLog;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * State Machine for {@link AtomixConsistentMap} resource.
 */
public class AtomixConsistentMapState extends ResourceStateMachine implements SessionListener, Snapshottable {

    private final Logger log = getLogger(getClass());
    private final Map<Long, Commit<? extends Listen>> listeners = new HashMap<>();
    private final Map<String, MapEntryValue> mapEntries = new HashMap<>();
    private final Set<String> preparedKeys = Sets.newHashSet();
    private final Map<TransactionId, TransactionScope> activeTransactions = Maps.newHashMap();
    private long currentVersion;

    public AtomixConsistentMapState(Properties properties) {
        super(properties);
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
    }

    @Override
    public void install(SnapshotReader reader) {
    }

    @Override
    protected void configure(StateMachineExecutor executor) {
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
        // Commands
        executor.register(UpdateAndGet.class, this::updateAndGet);
        executor.register(AtomixConsistentMapCommands.Clear.class, this::clear);
        executor.register(TransactionBegin.class, this::begin);
        executor.register(TransactionPrepare.class, this::prepare);
        executor.register(TransactionCommit.class, this::commit);
        executor.register(TransactionRollback.class, this::rollback);
        executor.register(TransactionPrepareAndCommit.class, this::prepareAndCommit);
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
     * @param commit containsKey commit
     * @return {@code true} if map contains key
     */
    protected boolean containsKey(Commit<? extends ContainsKey> commit) {
        try {
            MapEntryValue value = mapEntries.get(commit.operation().key());
            return value != null && value.type() != MapEntryValue.Type.TOMBSTONE;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a contains value commit.
     *
     * @param commit containsValue commit
     * @return {@code true} if map contains value
     */
    protected boolean containsValue(Commit<? extends ContainsValue> commit) {
        try {
            Match<byte[]> valueMatch = Match.ifValue(commit.operation().value());
            return mapEntries.values().stream()
                    .filter(value -> value.type() != MapEntryValue.Type.TOMBSTONE)
                    .anyMatch(value -> valueMatch.matches(value.value()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a get commit.
     *
     * @param commit get commit
     * @return value mapped to key
     */
    protected Versioned<byte[]> get(Commit<? extends Get> commit) {
        try {
            return toVersioned(mapEntries.get(commit.operation().key()));
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a get or default commit.
     *
     * @param commit get or default commit
     * @return value mapped to key
     */
    protected Versioned<byte[]> getOrDefault(Commit<? extends GetOrDefault> commit) {
        try {
            MapEntryValue value = mapEntries.get(commit.operation().key());
            if (value == null) {
                return new Versioned<>(commit.operation().defaultValue(), 0);
            } else if (value.type() == MapEntryValue.Type.TOMBSTONE) {
                return new Versioned<>(commit.operation().defaultValue(), value.version);
            } else {
                return new Versioned<>(value.value(), value.version);
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a count commit.
     *
     * @param commit size commit
     * @return number of entries in map
     */
    protected int size(Commit<? extends Size> commit) {
        try {
            return (int) mapEntries.values().stream()
                    .filter(value -> value.type() != MapEntryValue.Type.TOMBSTONE)
                    .count();
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an is empty commit.
     *
     * @param commit isEmpty commit
     * @return {@code true} if map is empty
     */
    protected boolean isEmpty(Commit<? extends IsEmpty> commit) {
        try {
            return mapEntries.values().stream()
                    .noneMatch(value -> value.type() != MapEntryValue.Type.TOMBSTONE);
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a keySet commit.
     *
     * @param commit keySet commit
     * @return set of keys in map
     */
    protected Set<String> keySet(Commit<? extends KeySet> commit) {
        try {
            return mapEntries.entrySet().stream()
                    .filter(entry -> entry.getValue().type() != MapEntryValue.Type.TOMBSTONE)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a values commit.
     *
     * @param commit values commit
     * @return collection of values in map
     */
    protected Collection<Versioned<byte[]>> values(Commit<? extends Values> commit) {
        try {
            return mapEntries.entrySet().stream()
                    .filter(entry -> entry.getValue().type() != MapEntryValue.Type.TOMBSTONE)
                    .map(entry -> toVersioned(entry.getValue()))
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
    protected Set<Map.Entry<String, Versioned<byte[]>>> entrySet(Commit<? extends EntrySet> commit) {
        try {
            return mapEntries.entrySet().stream()
                    .filter(entry -> entry.getValue().type() != MapEntryValue.Type.TOMBSTONE)
                    .map(e -> Maps.immutableEntry(e.getKey(), toVersioned(e.getValue())))
                    .collect(Collectors.toSet());
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a update and get commit.
     *
     * @param commit updateAndGet commit
     * @return update result
     */
    protected MapEntryUpdateResult<String, byte[]> updateAndGet(Commit<? extends UpdateAndGet> commit) {
        try {
            MapEntryUpdateResult.Status updateStatus = validate(commit.operation());
            String key = commit.operation().key();
            MapEntryValue oldCommitValue = mapEntries.get(commit.operation().key());
            Versioned<byte[]> oldMapValue = toVersioned(oldCommitValue);

            if (updateStatus != MapEntryUpdateResult.Status.OK) {
                commit.close();
                return new MapEntryUpdateResult<>(updateStatus, "", key, oldMapValue, oldMapValue);
            }

            byte[] newValue = commit.operation().value();
            currentVersion = commit.index();
            Versioned<byte[]> newMapValue = newValue == null ? null
                    : new Versioned<>(newValue, currentVersion);

            MapEvent.Type updateType = newValue == null ? REMOVE
                    : oldCommitValue == null ? INSERT : UPDATE;

            // If a value existed in the map, remove and discard the value to ensure disk can be freed.
            if (updateType == REMOVE || updateType == UPDATE) {
                mapEntries.remove(key);
                oldCommitValue.discard();
            }

            // If this is an insert/update commit, add the commit to the map entries.
            if (updateType == INSERT || updateType == UPDATE) {
                mapEntries.put(key, new NonTransactionalCommit(commit));
            } else if (!activeTransactions.isEmpty()) {
                // If this is a delete but transactions are currently running, ensure tombstones are retained
                // for version checks.
                TombstoneCommit tombstone = new TombstoneCommit(
                        commit.index(),
                        new CountDownCompleter<>(commit, 1, Commit::close));
                mapEntries.put(key, tombstone);
            } else {
                // If no transactions are in progress, we can safely delete the key from memory.
                commit.close();
            }

            publish(Lists.newArrayList(new MapEvent<>("", key, newMapValue, oldMapValue)));
            return new MapEntryUpdateResult<>(updateStatus, "", key, oldMapValue, newMapValue);
        } catch (Exception e) {
            log.error("State machine operation failed", e);
            throw Throwables.propagate(e);
        }
    }

    /**
     * Handles a clear commit.
     *
     * @param commit clear commit
     * @return clear result
     */
    protected MapEntryUpdateResult.Status clear(Commit<? extends Clear> commit) {
        try {
            Iterator<Map.Entry<String, MapEntryValue>> iterator = mapEntries
                    .entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, MapEntryValue> entry = iterator.next();
                String key = entry.getKey();
                MapEntryValue value = entry.getValue();
                Versioned<byte[]> removedValue = new Versioned<>(value.value(),
                        value.version());
                publish(Lists.newArrayList(new MapEvent<>("", key, null, removedValue)));
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
     * @param commit listen commit
     */
    protected void listen(Commit<? extends Listen> commit) {
        Long sessionId = commit.session().id();
        if (listeners.putIfAbsent(sessionId, commit) != null) {
            commit.close();
            return;
        }
        commit.session()
                .onStateChange(
                        state -> {
                            if (state == ServerSession.State.CLOSED
                                    || state == ServerSession.State.EXPIRED) {
                                Commit<? extends Listen> listener = listeners.remove(sessionId);
                                if (listener != null) {
                                    listener.close();
                                }
                            }
                        });
    }

    /**
     * Handles an unlisten commit.
     *
     * @param commit unlisten commit
     */
    protected void unlisten(Commit<? extends Unlisten> commit) {
        try {
            Commit<? extends Listen> listener = listeners.remove(commit.session().id());
            if (listener != null) {
                listener.close();
            }
        } finally {
            commit.close();
        }
    }

    /**
     * Handles a begin commit.
     *
     * @param commit transaction begin commit
     * @return transaction state version
     */
    protected long begin(Commit<? extends TransactionBegin> commit) {
        try {
            long version = commit.index();
            activeTransactions.put(commit.operation().transactionId(), new TransactionScope(version));
            return version;
        } finally {
            commit.close();
        }
    }

    /**
     * Handles an prepare and commit commit.
     *
     * @param commit transaction prepare and commit commit
     * @return prepare result
     */
    protected PrepareResult prepareAndCommit(Commit<? extends TransactionPrepareAndCommit> commit) {
        TransactionId transactionId = commit.operation().transactionLog().transactionId();
        PrepareResult prepareResult = prepare(commit);
        TransactionScope transactionScope = activeTransactions.remove(transactionId);
        if (prepareResult == PrepareResult.OK) {
            this.currentVersion = commit.index();
            transactionScope = transactionScope.prepared(commit);
            commit(transactionScope);
        } else if (transactionScope != null) {
            transactionScope.close();
        }
        discardTombstones();
        return prepareResult;
    }

    /**
     * Handles an prepare commit.
     *
     * @param commit transaction prepare commit
     * @return prepare result
     */
    protected PrepareResult prepare(Commit<? extends TransactionPrepare> commit) {
        boolean ok = false;

        try {
            TransactionLog<MapUpdate<String, byte[]>> transactionLog = commit.operation().transactionLog();

            // Iterate through records in the transaction log and perform isolation checks.
            for (MapUpdate<String, byte[]> record : transactionLog.records()) {
                String key = record.key();

                // If the record is a VERSION_MATCH then check that the record's version matches the current
                // version of the state machine.
                if (record.type() == MapUpdate.Type.VERSION_MATCH && key == null) {
                    if (record.version() > currentVersion) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    } else {
                        continue;
                    }
                }

                // If the prepared keys already contains the key contained within the record, that indicates a
                // conflict with a concurrent transaction.
                if (preparedKeys.contains(key)) {
                    return PrepareResult.CONCURRENT_TRANSACTION;
                }

                // Read the existing value from the map.
                MapEntryValue existingValue = mapEntries.get(key);

                // Note: if the existing value is null, that means the key has not changed during the transaction,
                // otherwise a tombstone would have been retained.
                if (existingValue == null) {
                    // If the value is null, ensure the version is equal to the transaction version.
                    if (record.version() != transactionLog.version()) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    }
                } else {
                    // If the value is non-null, compare the current version with the record version.
                    if (existingValue.version() > record.version()) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    }
                }
            }

            // No violations detected. Mark modified keys locked for transactions.
            transactionLog.records().forEach(record -> {
                if (record.type() != MapUpdate.Type.VERSION_MATCH) {
                    preparedKeys.add(record.key());
                }
            });

            ok = true;

            // Update the transaction scope. If the transaction scope is not set on this node, that indicates the
            // coordinator is communicating with another node. Transactions assume that the client is communicating
            // with a single leader in order to limit the overhead of retaining tombstones.
            TransactionScope transactionScope = activeTransactions.get(transactionLog.transactionId());
            if (transactionScope == null) {
                activeTransactions.put(
                        transactionLog.transactionId(),
                        new TransactionScope(transactionLog.version(), commit));
                return PrepareResult.PARTIAL_FAILURE;
            } else {
                activeTransactions.put(
                        transactionLog.transactionId(),
                        transactionScope.prepared(commit));
                return PrepareResult.OK;
            }
        } catch (Exception e) {
            log.warn("Failure applying {}", commit, e);
            throw Throwables.propagate(e);
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
    protected CommitResult commit(Commit<? extends TransactionCommit> commit) {
        TransactionId transactionId = commit.operation().transactionId();
        TransactionScope transactionScope = activeTransactions.remove(transactionId);
        if (transactionScope == null) {
            return CommitResult.UNKNOWN_TRANSACTION_ID;
        }

        try {
            this.currentVersion = commit.index();
            return commit(transactionScope.committed(commit));
        } catch (Exception e) {
            log.warn("Failure applying {}", commit, e);
            throw Throwables.propagate(e);
        } finally {
            discardTombstones();
        }
    }

    /**
     * Applies committed operations to the state machine.
     */
    private CommitResult commit(TransactionScope transactionScope) {
        TransactionLog<MapUpdate<String, byte[]>> transactionLog = transactionScope.transactionLog();
        boolean retainTombstones = !activeTransactions.isEmpty();

        // Count the total number of keys that will be set by this transaction. This is necessary to do reference
        // counting for garbage collection.
        long totalReferencesToCommit = transactionLog.records().stream()
                // No keys are set for version checks. For deletes, references are only retained of tombstones
                // need to be retained for concurrent transactions.
                .filter(record -> record.type() != MapUpdate.Type.VERSION_MATCH && record.type() != MapUpdate.Type.LOCK
                        && (record.type() != MapUpdate.Type.REMOVE_IF_VERSION_MATCH || retainTombstones))
                .count();

        // Create a count down completer that counts references to the transaction commit for garbage collection.
        CountDownCompleter<TransactionScope> completer = new CountDownCompleter<>(
                transactionScope, totalReferencesToCommit, TransactionScope::close);

        List<MapEvent<String, byte[]>> eventsToPublish = Lists.newArrayList();
        for (MapUpdate<String, byte[]> record : transactionLog.records()) {
            if (record.type() == MapUpdate.Type.VERSION_MATCH) {
                continue;
            }

            String key = record.key();
            checkState(preparedKeys.remove(key), "key is not prepared");

            if (record.type() == MapUpdate.Type.LOCK) {
                continue;
            }

            MapEntryValue previousValue = mapEntries.remove(key);
            MapEntryValue newValue = null;

            // If the record is not a delete, create a transactional commit.
            if (record.type() != MapUpdate.Type.REMOVE_IF_VERSION_MATCH) {
                newValue = new TransactionalCommit(currentVersion, record.value(), completer);
            } else if (retainTombstones) {
                // For deletes, if tombstones need to be retained then create and store a tombstone commit.
                newValue = new TombstoneCommit(currentVersion, completer);
            }

            eventsToPublish.add(new MapEvent<>("", key, toVersioned(newValue), toVersioned(previousValue)));

            if (newValue != null) {
                mapEntries.put(key, newValue);
            }

            if (previousValue != null) {
                previousValue.discard();
            }
        }
        publish(eventsToPublish);
        return CommitResult.OK;
    }

    /**
     * Handles an rollback commit (ha!).
     *
     * @param commit transaction rollback commit
     * @return rollback result
     */
    protected RollbackResult rollback(Commit<? extends TransactionRollback> commit) {
        TransactionId transactionId = commit.operation().transactionId();
        TransactionScope transactionScope = activeTransactions.remove(transactionId);
        if (transactionScope == null) {
            return RollbackResult.UNKNOWN_TRANSACTION_ID;
        } else if (!transactionScope.isPrepared()) {
            discardTombstones();
            transactionScope.close();
            commit.close();
            return RollbackResult.OK;
        } else {
            try {
                transactionScope.transactionLog().records()
                        .forEach(record -> {
                            if (record.type() != MapUpdate.Type.VERSION_MATCH) {
                                preparedKeys.remove(record.key());
                            }
                        });
                return RollbackResult.OK;
            } finally {
                discardTombstones();
                transactionScope.close();
                commit.close();
            }
        }

    }

    /**
     * Discards tombstones no longer needed by active transactions.
     */
    private void discardTombstones() {
        if (activeTransactions.isEmpty()) {
            Iterator<Map.Entry<String, MapEntryValue>> iterator = mapEntries.entrySet().iterator();
            while (iterator.hasNext()) {
                MapEntryValue value = iterator.next().getValue();
                if (value.type() == MapEntryValue.Type.TOMBSTONE) {
                    iterator.remove();
                    value.discard();
                }
            }
        } else {
            long lowWaterMark = activeTransactions.values().stream()
                    .mapToLong(TransactionScope::version)
                    .min().getAsLong();
            Iterator<Map.Entry<String, MapEntryValue>> iterator = mapEntries.entrySet().iterator();
            while (iterator.hasNext()) {
                MapEntryValue value = iterator.next().getValue();
                if (value.type() == MapEntryValue.Type.TOMBSTONE && value.version < lowWaterMark) {
                    iterator.remove();
                    value.discard();
                }
            }
        }
    }

    /**
     * Computes the update status that would result if the specified update were to applied to
     * the state machine.
     *
     * @param update update
     * @return status
     */
    private MapEntryUpdateResult.Status validate(UpdateAndGet update) {
        MapEntryValue existingValue = mapEntries.get(update.key());
        boolean isEmpty = existingValue == null || existingValue.type() == MapEntryValue.Type.TOMBSTONE;
        if (isEmpty && update.value() == null) {
            return MapEntryUpdateResult.Status.NOOP;
        }
        if (preparedKeys.contains(update.key())) {
            return MapEntryUpdateResult.Status.WRITE_LOCK;
        }
        byte[] existingRawValue = isEmpty ? null : existingValue.value();
        Long existingVersion = isEmpty ? null : existingValue.version();
        return update.valueMatch().matches(existingRawValue)
                && update.versionMatch().matches(existingVersion) ? MapEntryUpdateResult.Status.OK
                : MapEntryUpdateResult.Status.PRECONDITION_FAILED;
    }

    /**
     * Utility for turning a {@code MapEntryValue} to {@code Versioned}.
     * @param value map entry value
     * @return versioned instance
     */
    private Versioned<byte[]> toVersioned(MapEntryValue value) {
        return value != null && value.type() != MapEntryValue.Type.TOMBSTONE
                ? new Versioned<>(value.value(), value.version()) : null;
    }

    /**
     * Publishes events to listeners.
     *
     * @param events list of map event to publish
     */
    private void publish(List<MapEvent<String, byte[]>> events) {
        listeners.values().forEach(commit -> commit.session().publish(AtomixConsistentMap.CHANGE_SUBJECT, events));
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

    /**
     * Interface implemented by map values.
     */
    private abstract static class MapEntryValue {
        protected final Type type;
        protected final long version;

        MapEntryValue(Type type, long version) {
            this.type = type;
            this.version = version;
        }

        /**
         * Returns the value type.
         *
         * @return the value type
         */
        Type type() {
            return type;
        }

        /**
         * Returns the version of the value.
         *
         * @return version
         */
        long version() {
            return version;
        }

        /**
         * Returns the raw {@code byte[]}.
         *
         * @return raw value
         */
        abstract byte[] value();

        /**
         * Discards the value by invoke appropriate clean up actions.
         */
        abstract void discard();

        /**
         * Value type.
         */
        enum Type {
            VALUE,
            TOMBSTONE,
        }
    }

    /**
     * A {@code MapEntryValue} that is derived from a non-transactional update
     * i.e. via any standard map update operation.
     */
    private static class NonTransactionalCommit extends MapEntryValue {
        private final Commit<? extends UpdateAndGet> commit;

        NonTransactionalCommit(Commit<? extends UpdateAndGet> commit) {
            super(Type.VALUE, commit.index());
            this.commit = commit;
        }

        @Override
        byte[] value() {
            return commit.operation().value();
        }

        @Override
        void discard() {
            commit.close();
        }
    }

    /**
     * A {@code MapEntryValue} that is derived from updates submitted via a
     * transaction.
     */
    private static class TransactionalCommit extends MapEntryValue {
        private final byte[] value;
        private final CountDownCompleter<?> completer;

        TransactionalCommit(long version, byte[] value, CountDownCompleter<?> completer) {
            super(Type.VALUE, version);
            this.value = value;
            this.completer = completer;
        }

        @Override
        byte[] value() {
            return value;
        }

        @Override
        void discard() {
            completer.countDown();
        }
    }

    /**
     * A {@code MapEntryValue} that represents a deleted entry.
     */
    private static class TombstoneCommit extends MapEntryValue {
        private final CountDownCompleter<?> completer;

        public TombstoneCommit(long version, CountDownCompleter<?> completer) {
            super(Type.TOMBSTONE, version);
            this.completer = completer;
        }

        @Override
        byte[] value() {
            throw new UnsupportedOperationException();
        }

        @Override
        void discard() {
            completer.countDown();
        }
    }

    /**
     * Map transaction scope.
     */
    private static final class TransactionScope {
        private final long version;
        private final Commit<? extends TransactionPrepare> prepareCommit;
        private final Commit<? extends TransactionCommit> commitCommit;

        private TransactionScope(long version) {
            this(version, null, null);
        }

        private TransactionScope(
                long version,
                Commit<? extends TransactionPrepare> prepareCommit) {
            this(version, prepareCommit, null);
        }

        private TransactionScope(
                long version,
                Commit<? extends TransactionPrepare> prepareCommit,
                Commit<? extends TransactionCommit> commitCommit) {
            this.version = version;
            this.prepareCommit = prepareCommit;
            this.commitCommit = commitCommit;
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
            return prepareCommit != null;
        }

        /**
         * Returns the transaction commit log.
         *
         * @return the transaction commit log
         */
        TransactionLog<MapUpdate<String, byte[]>> transactionLog() {
            checkState(isPrepared());
            return prepareCommit.operation().transactionLog();
        }

        /**
         * Returns a new transaction scope with a prepare commit.
         *
         * @param commit the prepare commit
         * @return new transaction scope updated with the prepare commit
         */
        TransactionScope prepared(Commit<? extends TransactionPrepare> commit) {
            return new TransactionScope(version, commit);
        }

        /**
         * Returns a new transaction scope with a commit commit.
         *
         * @param commit the commit commit ;-)
         * @return new transaction scope updated with the commit commit
         */
        TransactionScope committed(Commit<? extends TransactionCommit> commit) {
            checkState(isPrepared());
            return new TransactionScope(version, prepareCommit, commit);
        }

        /**
         * Closes the transaction and all associated commits.
         */
        void close() {
            if (prepareCommit != null) {
                prepareCommit.close();
            }
            if (commitCommit != null) {
                commitCommit.close();
            }
        }
    }
}
