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
import java.util.concurrent.atomic.AtomicLong;
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
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.IsEmpty;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.KeySet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Listen;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Size;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepare;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionPrepareAndCommit;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.TransactionRollback;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Unlisten;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.UpdateAndGet;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapCommands.Values;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapTransaction;
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
    private final Map<TransactionId, Commit<? extends TransactionPrepare>> pendingTransactions = Maps.newHashMap();
    private AtomicLong versionCounter = new AtomicLong(0);

    public AtomixConsistentMapState(Properties properties) {
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
        // Queries
        executor.register(ContainsKey.class, this::containsKey);
        executor.register(ContainsValue.class, this::containsValue);
        executor.register(EntrySet.class, this::entrySet);
        executor.register(Get.class, this::get);
        executor.register(IsEmpty.class, this::isEmpty);
        executor.register(KeySet.class, this::keySet);
        executor.register(Size.class, this::size);
        executor.register(Values.class, this::values);
        // Commands
        executor.register(UpdateAndGet.class, this::updateAndGet);
        executor.register(AtomixConsistentMapCommands.Clear.class, this::clear);
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
            return toVersioned(mapEntries.get(commit.operation().key())) != null;
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
    protected Versioned<byte[]> get(Commit<? extends Get> commit) {
        try {
            return toVersioned(mapEntries.get(commit.operation().key()));
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
            return mapEntries.size();
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
            return mapEntries.isEmpty();
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
            return mapEntries.keySet().stream().collect(Collectors.toSet());
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
            return mapEntries.values().stream().map(this::toVersioned).collect(Collectors.toList());
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
            } else {
                commit.close();
            }
            publish(Lists.newArrayList(new MapEvent<>("", key, newMapValue, oldMapValue)));
            return new MapEntryUpdateResult<>(updateStatus, "", key, oldMapValue,
                    newMapValue);
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
     * Handles an prepare and commit commit.
     *
     * @param commit transaction prepare and commit commit
     * @return prepare result
     */
    protected PrepareResult prepareAndCommit(Commit<? extends TransactionPrepareAndCommit> commit) {
        PrepareResult prepareResult = prepare(commit);
        if (prepareResult == PrepareResult.OK) {
            commitInternal(commit.operation().transaction().transactionId());
        }
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
            MapTransaction<String, byte[]> transaction = commit.operation().transaction();
            for (MapUpdate<String, byte[]> update : transaction.updates()) {
                String key = update.key();
                if (preparedKeys.contains(key)) {
                    return PrepareResult.CONCURRENT_TRANSACTION;
                }
                MapEntryValue existingValue = mapEntries.get(key);
                if (existingValue == null) {
                    if (update.type() != MapUpdate.Type.PUT_IF_ABSENT) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    }
                } else {
                    if (existingValue.version() != update.currentVersion()) {
                        return PrepareResult.OPTIMISTIC_LOCK_FAILURE;
                    }
                }
            }
            // No violations detected. Add to pendingTranctions and mark
            // modified keys as locked for updates.
            pendingTransactions.put(transaction.transactionId(), commit);
            transaction.updates().forEach(u -> preparedKeys.add(u.key()));
            ok = true;
            return PrepareResult.OK;
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
        try {
            return commitInternal(transactionId);
        } catch (Exception e) {
            log.warn("Failure applying {}", commit, e);
            throw Throwables.propagate(e);
        } finally {
            commit.close();
        }
    }

    private CommitResult commitInternal(TransactionId transactionId) {
        Commit<? extends TransactionPrepare> prepareCommit = pendingTransactions
                .remove(transactionId);
        if (prepareCommit == null) {
            return CommitResult.UNKNOWN_TRANSACTION_ID;
        }
        MapTransaction<String, byte[]> transaction = prepareCommit.operation().transaction();
        long totalReferencesToCommit = transaction
                .updates()
                .stream()
                .filter(update -> update.type() != MapUpdate.Type.REMOVE_IF_VERSION_MATCH)
                .count();
        CountDownCompleter<Commit<? extends TransactionPrepare>> completer =
                new CountDownCompleter<>(prepareCommit, totalReferencesToCommit, Commit::close);
        List<MapEvent<String, byte[]>> eventsToPublish = Lists.newArrayList();
        for (MapUpdate<String, byte[]> update : transaction.updates()) {
            String key = update.key();
            checkState(preparedKeys.remove(key), "key is not prepared");
            MapEntryValue previousValue = mapEntries.remove(key);
            MapEntryValue newValue = null;
            if (update.type() != MapUpdate.Type.REMOVE_IF_VERSION_MATCH) {
                newValue = new TransactionalCommit(key, versionCounter.incrementAndGet(), completer);
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
        try {
            Commit<? extends TransactionPrepare> prepareCommit = pendingTransactions.remove(transactionId);
            if (prepareCommit == null) {
                return RollbackResult.UNKNOWN_TRANSACTION_ID;
            } else {
                prepareCommit.operation()
                             .transaction()
                             .updates()
                             .forEach(u -> preparedKeys.remove(u.key()));
                prepareCommit.close();
                return RollbackResult.OK;
            }
        } finally {
            commit.close();
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

    /**
     * Utility for turning a {@code MapEntryValue} to {@code Versioned}.
     * @param value map entry value
     * @return versioned instance
     */
    private Versioned<byte[]> toVersioned(MapEntryValue value) {
        return value == null ? null : new Versioned<>(value.value(), value.version());
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
        private final Commit<? extends UpdateAndGet> commit;

        public NonTransactionalCommit(long version, Commit<? extends UpdateAndGet> commit) {
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
        private final CountDownCompleter<Commit<? extends TransactionPrepare>> completer;

        public TransactionalCommit(
                String key,
                long version,
                CountDownCompleter<Commit<? extends TransactionPrepare>> commit) {
            this.key = key;
            this.version = version;
            this.completer = commit;
        }

        @Override
        public byte[] value() {
            MapTransaction<String, byte[]> transaction = completer.object().operation().transaction();
            return valueForKey(key, transaction);
        }

        @Override
        public long version() {
            return version;
        }

        @Override
        public void discard() {
            completer.countDown();
        }

        private byte[] valueForKey(String key, MapTransaction<String, byte[]> transaction) {
            MapUpdate<String, byte[]>  update = transaction.updates()
                                                           .stream()
                                                           .filter(u -> u.key().equals(key))
                                                           .findFirst()
                                                           .orElse(null);
            return update == null ? null : update.value();
        }
    }
}
