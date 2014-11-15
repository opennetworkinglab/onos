package org.onlab.onos.store.service.impl;

import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
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
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.store.cluster.messaging.ClusterCommunicationService;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.ClusterMessageHandler;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.Lock;
import org.onlab.onos.store.service.LockEventListener;
import org.onlab.onos.store.service.LockService;
import org.slf4j.Logger;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

@Component(immediate = true)
@Service
public class DistributedLockManager implements LockService {

    private static final ExecutorService THREAD_POOL =
            Executors.newCachedThreadPool(namedThreads("lock-manager-%d"));

    private final Logger log = getLogger(getClass());

    public static final String ONOS_LOCK_TABLE_NAME = "onos-locks";

    private final ListMultimap<String, LockRequest> locksToAcquire =
                Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, LockRequest>create());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DatabaseService databaseService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Activate
    public void activate() {
        clusterCommunicator.addSubscriber(
                DatabaseStateMachine.DATABASE_UPDATE_EVENTS,
                new LockEventMessageListener());
        log.info("Started.");

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
        // FIXME:
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeListener(LockEventListener listener) {
        // FIXME:
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to acquire the lock as soon as it becomes available.
     * @param lock lock to acquire.
     * @param waitTimeMillis maximum time to wait before giving up.
     * @param leaseDurationMillis the duration for which to acquire the lock initially.
     * @return Future lease expiration date.
     */
    protected CompletableFuture<DateTime> lockIfAvailable(
            Lock lock,
            long waitTimeMillis,
            int leaseDurationMillis) {
        CompletableFuture<DateTime> future = new CompletableFuture<>();
        locksToAcquire.put(lock.path(), new LockRequest(lock, waitTimeMillis, leaseDurationMillis, future));
        return future;
    }

    private class LockEventMessageListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            TableModificationEvent event = DatabaseStateMachine.SERIALIZER
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
                            request.future().complete(DateTime.now().plusMillis(request.leaseDurationMillis()));
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
        private final CompletableFuture<DateTime> future;

        public LockRequest(Lock lock, long waitTimeMillis,
                int leaseDurationMillis, CompletableFuture<DateTime> future) {

            this.lock = lock;
            this.requestExpirationTime = DateTime.now().plusMillis((int) waitTimeMillis);
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

        public CompletableFuture<DateTime> future() {
            return future;
        }
    }
}
