/*
 * Copyright 2014 Open Networking Laboratory
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

package org.onosproject.store.service.impl;

import static org.onlab.util.Tools.namedThreads;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.event.EventHandler;
import net.kuujo.copycat.event.LeaderElectEvent;

import org.onosproject.cluster.ControllerNode;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.VersionedValue;
import org.onosproject.store.service.impl.DatabaseStateMachine.State;
import org.onosproject.store.service.impl.DatabaseStateMachine.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Plugs into the database update stream and track the TTL of entries added to
 * the database. For tables with pre-configured finite TTL, this class has
 * mechanisms for expiring (deleting) old, expired entries from the database.
 */
public class DatabaseEntryExpirationTracker implements
        DatabaseUpdateEventListener, EventHandler<LeaderElectEvent> {

    private static final ExecutorService THREAD_POOL =
            Executors.newCachedThreadPool(namedThreads("database-stale-entry-expirer-%d"));

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DatabaseService databaseService;
    private final ClusterCommunicationService clusterCommunicator;

    private final Member localMember;
    private final ControllerNode localNode;
    private final AtomicBoolean isLocalMemberLeader = new AtomicBoolean(false);

    private final Map<String, Map<DatabaseRow, Long>> tableEntryExpirationMap = new HashMap<>();

    private final ExpirationListener<DatabaseRow, Long> expirationObserver = new ExpirationObserver();

    DatabaseEntryExpirationTracker(
            Member localMember,
            ControllerNode localNode,
            ClusterCommunicationService clusterCommunicator,
            DatabaseService databaseService) {
        this.localMember = localMember;
        this.localNode = localNode;
        this.clusterCommunicator = clusterCommunicator;
        this.databaseService = databaseService;
    }

    @Override
    public void tableModified(TableModificationEvent event) {
        log.debug("{}: Received {}", localNode.id(), event);

        if (!tableEntryExpirationMap.containsKey(event.tableName())) {
            return;
        }

        Map<DatabaseRow, Long> map = tableEntryExpirationMap.get(event.tableName());
        DatabaseRow row = new DatabaseRow(event.tableName(), event.key());
        Long eventVersion = event.value().version();

        switch (event.type()) {
        case ROW_DELETED:
            map.remove(row, eventVersion);
            if (isLocalMemberLeader.get()) {
                try {
                    log.debug("Broadcasting {} to the entire cluster", event);
                    clusterCommunicator.broadcastIncludeSelf(new ClusterMessage(
                            localNode.id(), DatabaseStateMachine.DATABASE_UPDATE_EVENTS,
                            ClusterMessagingProtocol.DB_SERIALIZER.encode(event)));
                } catch (IOException e) {
                    log.error("Failed to broadcast a database row deleted event.", e);
                }
            }
            break;
        case ROW_ADDED:
        case ROW_UPDATED:
            // To account for potential reordering of notifications,
            // check to make sure we are replacing an old version with a new version
            Long currentVersion = map.get(row);
            if (currentVersion == null || currentVersion < eventVersion) {
                map.put(row, eventVersion);
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void tableCreated(TableMetadata metadata) {
        log.debug("Received a table created event {}", metadata);
        if (metadata.expireOldEntries()) {
            tableEntryExpirationMap.put(metadata.tableName(), ExpiringMap.builder()
                    .expiration(metadata.ttlMillis(), TimeUnit.MILLISECONDS)
                    .expirationListener(expirationObserver)
                    .expirationPolicy(ExpirationPolicy.CREATED).build());
        }
    }

    @Override
    public void tableDeleted(String tableName) {
        log.debug("Received a table deleted event for table ({})", tableName);
        tableEntryExpirationMap.remove(tableName);
    }

    private class ExpirationObserver implements
            ExpirationListener<DatabaseRow, Long> {
        @Override
        public void expired(DatabaseRow row, Long version) {
            THREAD_POOL.submit(new ExpirationTask(row, version));
        }
    }

    private class ExpirationTask implements Runnable {

        private final DatabaseRow row;
        private final Long version;

        public ExpirationTask(DatabaseRow row, Long version) {
            this.row = row;
            this.version = version;
        }

        @Override
        public void run() {
            log.trace("Received an expiration event for {}, version: {}", row, version);
            Map<DatabaseRow, Long> map = tableEntryExpirationMap.get(row.tableName);
            try {
                if (isLocalMemberLeader.get()) {
                    if (!databaseService.removeIfVersionMatches(row.tableName,
                            row.key, version)) {
                        log.info("Entry in database was updated right before its expiration.");
                    } else {
                        log.debug("Successfully expired old entry with key ({}) from table ({})",
                                row.key, row.tableName);
                    }
                } else {
                    // Only the current leader will expire keys from database.
                    // Everyone else function as standby just in case they need to take over
                    if (map != null) {
                        map.putIfAbsent(row, version);
                    }
                }

            } catch (Exception e) {
                log.warn("Failed to delete entry from the database after ttl "
                        + "expiration. Operation will be retried.", e);
                map.putIfAbsent(row, version);
            }
        }
    }

    @Override
    public void handle(LeaderElectEvent event) {
        isLocalMemberLeader.set(localMember.equals(event.leader()));
        if (isLocalMemberLeader.get()) {
            log.info("{} is now the leader of Raft cluster", localNode.id());
        }
    }

    /**
     * Wrapper class for a database row identifier.
     */
    private class DatabaseRow {

        String tableName;
        String key;

        public DatabaseRow(String tableName, String key) {
            this.tableName = tableName;
            this.key = key;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("tableName", tableName)
                .add("key", key)
                .toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DatabaseRow)) {
                return false;
            }
            DatabaseRow that = (DatabaseRow) obj;

            return Objects.equals(this.tableName, that.tableName)
                    && Objects.equals(this.key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName, key);
        }
    }

    @Override
    public void snapshotInstalled(State state) {
        if (!tableEntryExpirationMap.isEmpty()) {
            return;
        }
        log.debug("Received a snapshot installed notification");
        for (String tableName : state.getTableNames()) {

            TableMetadata metadata = state.getTableMetadata(tableName);
            if (!metadata.expireOldEntries()) {
                continue;
            }

            Map<DatabaseRow, Long> tableExpirationMap = ExpiringMap.builder()
                    .expiration(metadata.ttlMillis(), TimeUnit.MILLISECONDS)
                    .expirationListener(expirationObserver)
                    .expirationPolicy(ExpirationPolicy.CREATED).build();
            for (Map.Entry<String, VersionedValue> entry : state.getTable(tableName).entrySet()) {
                tableExpirationMap.put(new DatabaseRow(tableName, entry.getKey()), entry.getValue().version());
            }

            tableEntryExpirationMap.put(tableName, tableExpirationMap);
        }
    }
}
