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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onosproject.cluster.ClusterService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.ClusterMessage;
import org.onosproject.store.cluster.messaging.ClusterMessageHandler;
import org.onosproject.store.service.DatabaseAdminService;
import org.onosproject.store.service.DatabaseException;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.Lock;
import org.onosproject.store.service.LockEventListener;
import org.onosproject.store.service.LockService;
import org.slf4j.Logger;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

@Component(immediate = false)
@Service
public class DistributedLockManager implements LockService {

    private static final ExecutorService THREAD_POOL =
            Executors.newCachedThreadPool(namedThreads("lock-manager-%d"));

    private final Logger log = getLogger(getClass());

    public static final String ONOS_LOCK_TABLE_NAME = "onos-locks";

    public static final int DEAD_LOCK_TIMEOUT_MS = 5000;

    private final ListMultimap<String, LockRequest> locksToAcquire =
                Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, LockRequest>create());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DatabaseAdminService databaseAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DatabaseService databaseService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Activate
    public void activate() {
        try {
            Set<String> tables = databaseAdminService.listTables();

            if (!tables.contains(ONOS_LOCK_TABLE_NAME)) {
                if (databaseAdminService.createTable(ONOS_LOCK_TABLE_NAME, DEAD_LOCK_TIMEOUT_MS)) {
                    log.info("Created {} table.", ONOS_LOCK_TABLE_NAME);
                }
            }
        } catch (DatabaseException e) {
            log.error("DistributedLockManager#activate failed.", e);
        }

         clusterCommunicator.addSubscriber(
                 DatabaseStateMachine.DATABASE_UPDATE_EVENTS,
                 new LockEventMessageListener());

         log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clusterCommunicator.removeSubscriber(DatabaseStateMachine.DATABASE_UPDATE_EVENTS);
        locksToAcquire.clear();
        log.info("Stopped.");
    }

    @Override
    public Lock create(String path) {
        return new DistributedLock(path, databaseService, clusterService, this);
    }

    @Override
    public void addListener(LockEventListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener(LockEventListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire the lock as soon as it becomes available.
     * @param lock lock to acquire.
     * @param waitTimeMillis maximum time to wait before giving up.
     * @param leaseDurationMillis the duration for which to acquire the lock initially.
     * @return Future that can be blocked on until lock becomes available.
     */
    protected CompletableFuture<Void> lockIfAvailable(
            Lock lock,
            int waitTimeMillis,
            int leaseDurationMillis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        LockRequest request = new LockRequest(
                lock,
                leaseDurationMillis,
                DateTime.now().plusMillis(waitTimeMillis),
                future);
        locksToAcquire.put(lock.path(), request);
        return future;
    }

    /**
     * Attempts to acquire the lock as soon as it becomes available.
     * @param lock lock to acquire.
     * @param leaseDurationMillis the duration for which to acquire the lock initially.
     * @return Future lease expiration date.
     */
    protected CompletableFuture<Void> lockIfAvailable(
            Lock lock,
            int leaseDurationMillis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        LockRequest request = new LockRequest(
                lock,
                leaseDurationMillis,
                DateTime.now().plusYears(100),
                future);
        locksToAcquire.put(lock.path(), request);
        return future;
    }

    private class LockEventMessageListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            TableModificationEvent event = ClusterMessagingProtocol.DB_SERIALIZER
                    .decode(message.payload());
            if (event.tableName().equals(ONOS_LOCK_TABLE_NAME) &&
                    event.type().equals(TableModificationEvent.Type.ROW_DELETED)) {
                THREAD_POOL.submit(new RetryLockTask(event.key()));
            }
        }
    }

    private class RetryLockTask implements Runnable {

        private final String path;

        public RetryLockTask(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            if (!locksToAcquire.containsKey(path)) {
                return;
            }

            List<LockRequest> existingRequests = locksToAcquire.get(path);
            if (existingRequests == null || existingRequests.isEmpty()) {
                return;
            }
            log.info("Path {} is now available for locking. There are {} outstanding "
                    + "requests for it.",
                    path, existingRequests.size());

            synchronized (existingRequests) {
                Iterator<LockRequest> existingRequestIterator = existingRequests.iterator();
                while (existingRequestIterator.hasNext()) {
                    LockRequest request = existingRequestIterator.next();
                    if (DateTime.now().isAfter(request.requestExpirationTime())) {
                        // request expired.
                        existingRequestIterator.remove();
                    } else {
                        if (request.lock().tryLock(request.leaseDurationMillis())) {
                            request.future().complete(null);
                            existingRequestIterator.remove();
                        }
                    }
                }
            }
        }
    }

    private class LockRequest {

        private final Lock lock;
        private final DateTime requestExpirationTime;
        private final int leaseDurationMillis;
        private final CompletableFuture<Void> future;

        public LockRequest(
                Lock lock,
                int leaseDurationMillis,
                DateTime requestExpirationTime,
                CompletableFuture<Void> future) {

            this.lock = lock;
            this.requestExpirationTime = requestExpirationTime;
            this.leaseDurationMillis = leaseDurationMillis;
            this.future = future;
        }

        public Lock lock() {
            return lock;
        }

        public DateTime requestExpirationTime() {
            return requestExpirationTime;
        }

        public int leaseDurationMillis() {
            return leaseDurationMillis;
        }

        public CompletableFuture<Void> future() {
            return future;
        }
    }
}
