package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

import com.google.common.collect.ArrayListMultimap;

@Component(immediate = true)
@Service
public class DistributedLockManager implements LockService {

    private final Logger log = getLogger(getClass());

    public static final String ONOS_LOCK_TABLE_NAME = "onos-locks";

    private final ArrayListMultimap<String, LockRequest> locksToAcquire = ArrayListMultimap
            .create();

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
        locksToAcquire.clear();
        log.info("Started.");
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

    protected CompletableFuture<Void> lockIfAvailable(Lock lock,
            long waitTimeMillis, int leaseDurationMillis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        locksToAcquire.put(lock.path(), new LockRequest(lock, waitTimeMillis,
                leaseDurationMillis, future));
        return future;
    }

    private class LockEventMessageListener implements ClusterMessageHandler {
        @Override
        public void handle(ClusterMessage message) {
            TableModificationEvent event = DatabaseStateMachine.SERIALIZER
                    .decode(message.payload());
            if (!event.tableName().equals(ONOS_LOCK_TABLE_NAME)) {
                return;
            }

            String path = event.key();
            if (!locksToAcquire.containsKey(path)) {
                return;
            }

            if (event.type() == TableModificationEvent.Type.ROW_DELETED) {
                List<LockRequest> existingRequests = locksToAcquire.get(path);
                if (existingRequests == null) {
                    return;
                }

                synchronized (existingRequests) {

                    Iterator<LockRequest> existingRequestIterator = existingRequests
                            .iterator();
                    while (existingRequestIterator.hasNext()) {
                        LockRequest request = existingRequestIterator.next();
                        if (request.expirationTime().isAfter(DateTime.now())) {
                            existingRequestIterator.remove();
                        } else {
                            if (request.lock().tryLock(
                                    request.leaseDurationMillis())) {
                                request.future().complete(null);
                                existingRequestIterator.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    private class LockRequest {

        private final Lock lock;
        private final DateTime expirationTime;
        private final int leaseDurationMillis;
        private final CompletableFuture<Void> future;

        public LockRequest(Lock lock, long waitTimeMillis,
                int leaseDurationMillis, CompletableFuture<Void> future) {

            this.lock = lock;
            this.expirationTime = DateTime.now().plusMillis(
                    (int) waitTimeMillis);
            this.leaseDurationMillis = leaseDurationMillis;
            this.future = future;
        }

        public Lock lock() {
            return lock;
        }

        public DateTime expirationTime() {
            return expirationTime;
        }

        public int leaseDurationMillis() {
            return leaseDurationMillis;
        }

        public CompletableFuture<Void> future() {
            return future;
        }
    }
}