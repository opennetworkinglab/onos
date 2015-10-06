/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.store.consistent.impl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.kuujo.copycat.state.Initializer;
import net.kuujo.copycat.state.StateContext;
import org.onosproject.store.service.DatabaseUpdate;
import org.onosproject.store.service.Transaction;
import org.onosproject.store.service.Versioned;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Default database state.
 */
public class DefaultDatabaseState implements DatabaseState<String, byte[]> {
    private Long nextVersion;
    private Map<String, AtomicLong> counters;
    private Map<String, Map<String, Versioned<byte[]>>> maps;
    private Map<String, Queue<byte[]>> queues;

    /**
     * This locks map has a structure similar to the "tables" map above and
     * holds all the provisional updates made during a transaction's prepare phase.
     * The entry value is represented as the tuple: (transactionId, newValue)
     * If newValue == null that signifies this update is attempting to
     * delete the existing value.
     * This map also serves as a lock on the entries that are being updated.
     * The presence of a entry in this map indicates that element is
     * participating in a transaction and is currently locked for updates.
     */
    private Map<String, Map<String, Update>> locks;

    @Initializer
    @Override
    public void init(StateContext<DatabaseState<String, byte[]>> context) {
        counters = context.get("counters");
        if (counters == null) {
            counters = Maps.newConcurrentMap();
            context.put("counters", counters);
        }
        maps = context.get("maps");
        if (maps == null) {
            maps = Maps.newConcurrentMap();
            context.put("maps", maps);
        }
        locks = context.get("locks");
        if (locks == null) {
            locks = Maps.newConcurrentMap();
            context.put("locks", locks);
        }
        queues = context.get("queues");
        if (queues == null) {
            queues = Maps.newConcurrentMap();
            context.put("queues", queues);
        }
        nextVersion = context.get("nextVersion");
        if (nextVersion == null) {
            nextVersion = 0L;
            context.put("nextVersion", nextVersion);
        }
    }

    @Override
    public Set<String> maps() {
        return ImmutableSet.copyOf(maps.keySet());
    }

    @Override
    public Map<String, Long> counters() {
        Map<String, Long> counterMap = Maps.newHashMap();
        counters.forEach((k, v) -> counterMap.put(k, v.get()));
        return counterMap;
    }

    @Override
    public int mapSize(String mapName) {
      return getMap(mapName).size();
    }

    @Override
    public boolean mapIsEmpty(String mapName) {
        return getMap(mapName).isEmpty();
    }

    @Override
    public boolean mapContainsKey(String mapName, String key) {
        return getMap(mapName).containsKey(key);
    }

    @Override
    public boolean mapContainsValue(String mapName, byte[] value) {
        return getMap(mapName).values().stream().anyMatch(v -> Arrays.equals(v.value(), value));
    }

    @Override
    public Versioned<byte[]> mapGet(String mapName, String key) {
        return getMap(mapName).get(key);
    }


    @Override
    public Result<UpdateResult<String, byte[]>> mapUpdate(
            String mapName,
            String key,
            Match<byte[]> valueMatch,
            Match<Long> versionMatch,
            byte[] value) {
        if (isLockedForUpdates(mapName, key)) {
            return Result.locked();
        }
        Versioned<byte[]> currentValue = getMap(mapName).get(key);
        if (!valueMatch.matches(currentValue == null ? null : currentValue.value()) ||
                !versionMatch.matches(currentValue == null ? null : currentValue.version())) {
            return Result.ok(new UpdateResult<>(false, mapName, key, currentValue, currentValue));
        } else {
            if (value == null) {
                if (currentValue == null) {
                    return Result.ok(new UpdateResult<>(false, mapName, key, null, null));
                } else {
                    getMap(mapName).remove(key);
                    return Result.ok(new UpdateResult<>(true, mapName, key, currentValue, null));
                }
            }
            Versioned<byte[]> newValue = new Versioned<>(value, ++nextVersion);
            getMap(mapName).put(key, newValue);
            return Result.ok(new UpdateResult<>(true, mapName, key, currentValue, newValue));
        }
    }

    @Override
    public Result<Void> mapClear(String mapName) {
        if (areTransactionsInProgress(mapName)) {
            return Result.locked();
        }
        getMap(mapName).clear();
        return Result.ok(null);
    }

    @Override
    public Set<String> mapKeySet(String mapName) {
        return ImmutableSet.copyOf(getMap(mapName).keySet());
    }

    @Override
    public Collection<Versioned<byte[]>> mapValues(String mapName) {
        return ImmutableList.copyOf(getMap(mapName).values());
    }

    @Override
    public Set<Entry<String, Versioned<byte[]>>> mapEntrySet(String mapName) {
        return ImmutableSet.copyOf(getMap(mapName)
                .entrySet()
                .stream()
                .map(entry -> Maps.immutableEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet()));
    }

    @Override
    public Long counterAddAndGet(String counterName, long delta) {
        return getCounter(counterName).addAndGet(delta);
    }

    @Override
    public Long counterGetAndAdd(String counterName, long delta) {
        return getCounter(counterName).getAndAdd(delta);
    }

    @Override
    public Boolean counterCompareAndSet(String counterName, long expectedValue, long updateValue) {
        return getCounter(counterName).compareAndSet(expectedValue, updateValue);
    }

    @Override
    public Long counterGet(String counterName) {
        return getCounter(counterName).get();
    }

    @Override
    public Long queueSize(String queueName) {
        return Long.valueOf(getQueue(queueName).size());
    }

    @Override
    public byte[] queuePeek(String queueName) {
        return getQueue(queueName).peek();
    }

    @Override
    public byte[] queuePop(String queueName) {
        return getQueue(queueName).poll();
    }

    @Override
    public void queuePush(String queueName, byte[] entry) {
        getQueue(queueName).offer(entry);
    }

    @Override
    public CommitResponse prepareAndCommit(Transaction transaction) {
        if (prepare(transaction)) {
            return commit(transaction);
        }
        return CommitResponse.failure();
    }

    @Override
    public boolean prepare(Transaction transaction) {
        if (transaction.updates().stream().anyMatch(update ->
                    isLockedByAnotherTransaction(update.mapName(),
                                                 update.key(),
                                                 transaction.id()))) {
            return false;
        }

        if (transaction.updates().stream().allMatch(this::isUpdatePossible)) {
            transaction.updates().forEach(update -> doProvisionalUpdate(update, transaction.id()));
            return true;
        }
        return false;
    }

    @Override
    public CommitResponse commit(Transaction transaction) {
        return CommitResponse.success(Lists.transform(transaction.updates(),
                                                      update -> commitProvisionalUpdate(update, transaction.id())));
    }

    @Override
    public boolean rollback(Transaction transaction) {
        transaction.updates().forEach(update -> undoProvisionalUpdate(update, transaction.id()));
        return true;
    }

    private Map<String, Versioned<byte[]>> getMap(String mapName) {
        return maps.computeIfAbsent(mapName, name -> Maps.newConcurrentMap());
    }

    private Map<String, Update> getLockMap(String mapName) {
        return locks.computeIfAbsent(mapName, name -> Maps.newConcurrentMap());
    }

    private AtomicLong getCounter(String counterName) {
        return counters.computeIfAbsent(counterName, name -> new AtomicLong(0));
    }

    private Queue<byte[]> getQueue(String queueName) {
        return queues.computeIfAbsent(queueName, name -> new LinkedList<>());
    }

    private boolean isUpdatePossible(DatabaseUpdate update) {
        Versioned<byte[]> existingEntry = mapGet(update.mapName(), update.key());
        switch (update.type()) {
        case PUT:
        case REMOVE:
            return true;
        case PUT_IF_ABSENT:
            return existingEntry == null;
        case PUT_IF_VERSION_MATCH:
            return existingEntry != null && existingEntry.version() == update.currentVersion();
        case PUT_IF_VALUE_MATCH:
            return existingEntry != null && Arrays.equals(existingEntry.value(), update.currentValue());
        case REMOVE_IF_VERSION_MATCH:
            return existingEntry == null || existingEntry.version() == update.currentVersion();
        case REMOVE_IF_VALUE_MATCH:
            return existingEntry == null || Arrays.equals(existingEntry.value(), update.currentValue());
        default:
            throw new IllegalStateException("Unsupported type: " + update.type());
        }
    }

    private void doProvisionalUpdate(DatabaseUpdate update, long transactionId) {
        Map<String, Update> lockMap = getLockMap(update.mapName());
        switch (update.type()) {
        case PUT:
        case PUT_IF_ABSENT:
        case PUT_IF_VERSION_MATCH:
        case PUT_IF_VALUE_MATCH:
            lockMap.put(update.key(), new Update(transactionId, update.value()));
            break;
        case REMOVE:
        case REMOVE_IF_VERSION_MATCH:
        case REMOVE_IF_VALUE_MATCH:
            lockMap.put(update.key(), new Update(transactionId, null));
            break;
        default:
            throw new IllegalStateException("Unsupported type: " + update.type());
        }
    }

    private UpdateResult<String, byte[]> commitProvisionalUpdate(DatabaseUpdate update, long transactionId) {
        String mapName = update.mapName();
        String key = update.key();
        Update provisionalUpdate = getLockMap(mapName).get(key);
        if (Objects.equal(transactionId, provisionalUpdate.transactionId()))  {
            getLockMap(mapName).remove(key);
        } else {
            throw new IllegalStateException("Invalid transaction Id");
        }
        return mapUpdate(mapName, key, Match.any(), Match.any(), provisionalUpdate.value()).value();
    }

    private void undoProvisionalUpdate(DatabaseUpdate update, long transactionId) {
        String mapName = update.mapName();
        String key = update.key();
        Update provisionalUpdate = getLockMap(mapName).get(key);
        if (provisionalUpdate == null) {
            return;
        }
        if (Objects.equal(transactionId, provisionalUpdate.transactionId()))  {
            getLockMap(mapName).remove(key);
        }
    }

    private boolean isLockedByAnotherTransaction(String mapName, String key, long transactionId) {
        Update update = getLockMap(mapName).get(key);
        return update != null && !Objects.equal(transactionId, update.transactionId());
    }

    private boolean isLockedForUpdates(String mapName, String key) {
        return getLockMap(mapName).containsKey(key);
    }

    private boolean areTransactionsInProgress(String mapName) {
        return !getLockMap(mapName).isEmpty();
    }

    private class Update {
        private final long transactionId;
        private final byte[] value;

        public Update(long txId, byte[] value) {
            this.transactionId = txId;
            this.value = value;
        }

        public long transactionId() {
            return this.transactionId;
        }

        public byte[] value() {
            return this.value;
        }
    }
}
