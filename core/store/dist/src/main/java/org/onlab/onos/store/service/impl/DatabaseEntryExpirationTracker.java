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

package org.onlab.onos.store.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.jodah.expiringmap.ExpiringMap;
import net.jodah.expiringmap.ExpiringMap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.event.EventHandler;
import net.kuujo.copycat.event.LeaderElectEvent;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.VersionedValue;
import org.onlab.onos.store.service.impl.DatabaseStateMachine.State;
import org.onlab.onos.store.service.impl.DatabaseStateMachine.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugs into the database update stream and track the TTL of entries added to
 * the database. For tables with pre-configured finite TTL, this class has
 * mechanisms for expiring (deleting) old, expired entries from the database.
 */
public class DatabaseEntryExpirationTracker implements
        DatabaseUpdateEventListener, EventHandler<LeaderElectEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final MessageSubject DATABASE_UPDATES = new MessageSubject(
            "database-update-event");

    private final DatabaseService databaseService;
    private final ClusterCommunicationService clusterCommunicator;

    private final Member localMember;
    private final ControllerNode localNode;
    private final AtomicBoolean isLocalMemberLeader = new AtomicBoolean(false);

    private final Map<String, Map<DatabaseRow, VersionedValue>> tableEntryExpirationMap = new HashMap<>();

    private final ExpirationListener<DatabaseRow, VersionedValue> expirationObserver = new ExpirationObserver();

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
        if (!tableEntryExpirationMap.containsKey(event.tableName())) {
            return;
        }

        DatabaseRow row = new DatabaseRow(event.tableName(), event.key());
        Map<DatabaseRow, VersionedValue> map = tableEntryExpirationMap
                .get(event.tableName());

        switch (event.type()) {
        case ROW_DELETED:
            if (isLocalMemberLeader.get()) {
                try {
                    clusterCommunicator.broadcast(new ClusterMessage(
                            localNode.id(), DATABASE_UPDATES,
                            DatabaseStateMachine.SERIALIZER.encode(event)));
                } catch (IOException e) {
                    log.error(
                            "Failed to broadcast a database table modification event.",
                            e);
                }
            }
            break;
        case ROW_ADDED:
        case ROW_UPDATED:
            map.put(row, null);
            break;
        default:
            break;
        }
    }

    @Override
    public void tableCreated(TableMetadata metadata) {
        if (metadata.expireOldEntries()) {
            tableEntryExpirationMap.put(metadata.tableName(), ExpiringMap.builder()
                    .expiration(metadata.ttlMillis(), TimeUnit.SECONDS)
                    .expirationListener(expirationObserver)
                    // FIXME: make the expiration policy configurable.
                    .expirationPolicy(ExpirationPolicy.CREATED).build());
        }
    }

    @Override
    public void tableDeleted(String tableName) {
        tableEntryExpirationMap.remove(tableName);
    }

    private class ExpirationObserver implements
            ExpirationListener<DatabaseRow, VersionedValue> {
        @Override
        public void expired(DatabaseRow key, VersionedValue value) {
            try {
                if (isLocalMemberLeader.get()) {
                    if (!databaseService.removeIfVersionMatches(key.tableName,
                            key.key, value.version())) {
                        log.info("Entry in the database changed before right its TTL expiration.");
                    }
                } else {
                    // If this node is not the current leader, we should never
                    // let the expiring entries drop off
                    // Under stable conditions (i.e no leadership switch) the
                    // current leader will initiate
                    // a database remove and this instance will get notified
                    // of a tableModification event causing it to remove from
                    // the map.
                    Map<DatabaseRow, VersionedValue> map = tableEntryExpirationMap
                            .get(key.tableName);
                    if (map != null) {
                        map.put(key, value);
                    }
                }

            } catch (Exception e) {
                log.warn(
                        "Failed to delete entry from the database after ttl expiration. Will retry eviction",
                        e);
                tableEntryExpirationMap.get(key.tableName).put(
                        new DatabaseRow(key.tableName, key.key), value);
            }
        }
    }

    @Override
    public void handle(LeaderElectEvent event) {
        if (localMember.equals(event.leader())) {
            isLocalMemberLeader.set(true);
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
        for (String tableName : state.getTableNames()) {

            TableMetadata metadata = state.getTableMetadata(tableName);
            if (!metadata.expireOldEntries()) {
                continue;
            }

            Map<DatabaseRow, VersionedValue> tableExpirationMap = ExpiringMap.builder()
                    .expiration(metadata.ttlMillis(), TimeUnit.MILLISECONDS)
                    .expirationListener(expirationObserver)
                    .expirationPolicy(ExpirationPolicy.CREATED).build();
            for (Map.Entry<String, VersionedValue> entry : state.getTable(tableName).entrySet()) {
                tableExpirationMap.put(new DatabaseRow(tableName, entry.getKey()), entry.getValue());
            }

            tableEntryExpirationMap.put(tableName, tableExpirationMap);
        }
    }
}
