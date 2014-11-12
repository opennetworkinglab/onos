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
import java.util.concurrent.atomic.AtomicBoolean;

//import net.jodah.expiringmap.ExpiringMap;
//import net.jodah.expiringmap.ExpiringMap.ExpirationListener;
//import net.jodah.expiringmap.ExpiringMap.ExpirationPolicy;
import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.event.EventHandler;
import net.kuujo.copycat.event.LeaderElectEvent;

import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database update event handler.
 */
public class DatabaseUpdateEventHandler implements
    DatabaseUpdateEventListener, EventHandler<LeaderElectEvent> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final MessageSubject DATABASE_UPDATES =
            new MessageSubject("database-update-event");

    private DatabaseService databaseService;
    private ClusterService cluster;
    private ClusterCommunicationService clusterCommunicator;

    private final Member localMember;
    private final AtomicBoolean isLocalMemberLeader = new AtomicBoolean(false);
    private final Map<String, Map<DatabaseRow, Void>> tableEntryExpirationMap = new HashMap<>();
    //private final ExpirationListener<DatabaseRow, Void> expirationObserver = new ExpirationObserver();

    DatabaseUpdateEventHandler(Member localMember) {
        this.localMember = localMember;
    }

    @Override
    public void tableModified(TableModificationEvent event) {
        DatabaseRow row = new DatabaseRow(event.tableName(), event.key());
        Map<DatabaseRow, Void> map = tableEntryExpirationMap.get(event.tableName());

        switch (event.type()) {
        case ROW_DELETED:
            if (isLocalMemberLeader.get()) {
                try {
                    clusterCommunicator.broadcast(
                            new ClusterMessage(
                                    cluster.getLocalNode().id(),
                                    DATABASE_UPDATES,
                                    DatabaseStateMachine.SERIALIZER.encode(event)));
                } catch (IOException e) {
                    log.error("Failed to broadcast a database table modification event.", e);
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
    public void tableCreated(String tableName, int expirationTimeMillis) {
        // make this explicit instead of relying on a negative value
        // to indicate no expiration.
        if (expirationTimeMillis > 0) {
            tableEntryExpirationMap.put(tableName, null);
            /*
            ExpiringMap.builder()
                    .expiration(expirationTimeMillis, TimeUnit.SECONDS)
                    .expirationListener(expirationObserver)
                    // FIXME: make the expiration policy configurable.
                    .expirationPolicy(ExpirationPolicy.CREATED)
                    .build());
                    */
        }
    }

    @Override
    public void tableDeleted(String tableName) {
        tableEntryExpirationMap.remove(tableName);
    }

    /*
    private class ExpirationObserver implements ExpirationListener<DatabaseRow, Void> {
        @Override
        public void expired(DatabaseRow key, Void value) {
            try {
                // TODO: The safety of this check needs to be verified.
                // Couple of issues:
                // 1. It is very likely that only one member should attempt deletion of the entry from database.
                // 2. A potential race condition exists where the entry expires, but before its can be deleted
                // from the database, a new entry is added or existing entry is updated.
                // That means ttl and expiration should be for a given version.
                if (isLocalMemberLeader.get()) {
                    databaseService.remove(key.tableName, key.key);
                }
            } catch (Exception e) {
                log.warn("Failed to delete entry from the database after ttl expiration. Will retry eviction", e);
                tableEntryExpirationMap.get(key.tableName).put(new DatabaseRow(key.tableName, key.key), null);
            }
        }
    }
    */

    @Override
    public void handle(LeaderElectEvent event) {
        if (localMember.equals(event.leader())) {
            isLocalMemberLeader.set(true);
        }
    }

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

            return Objects.equals(this.tableName, that.tableName) &&
                   Objects.equals(this.key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tableName, key);
        }
    }
}