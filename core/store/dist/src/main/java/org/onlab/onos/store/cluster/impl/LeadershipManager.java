package org.onlab.onos.store.cluster.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.Leadership;
import org.onlab.onos.cluster.LeadershipEvent;
import org.onlab.onos.cluster.LeadershipEventListener;
import org.onlab.onos.cluster.LeadershipService;
import org.onlab.onos.store.service.Lock;
import org.onlab.onos.store.service.LockService;
import org.onlab.onos.store.service.impl.DistributedLockManager;
import org.slf4j.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Distributed implementation of LeadershipService that is based on the primitives exposed by
 * LockService.
 */
@Component(immediate = true)
@Service
public class LeadershipManager implements LeadershipService {

    private final Logger log = getLogger(getClass());

    // TODO: Remove this dependency
    private static final int TERM_DURATION_MS =
            DistributedLockManager.DEAD_LOCK_TIMEOUT_MS;

    // TODO: Appropriate Thread pool sizing.
    private static final ScheduledExecutorService THREAD_POOL =
            Executors.newScheduledThreadPool(25, namedThreads("leadership-manager-%d"));

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LockService lockService;

    private Map<String, Lock> openContests = Maps.newHashMap();
    private Set<LeadershipEventListener> listeners = Sets.newIdentityHashSet();
    private ControllerNode localNode;

    @Activate
    public void activate() {
        localNode = clusterService.getLocalNode();
        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        THREAD_POOL.shutdown();
        log.info("Stopped.");
    }

    @Override
    public void runForLeadership(String path) {
        checkArgument(path != null);
        if (openContests.containsKey(path)) {
            log.info("Already in the leadership contest for {}", path);
            return;
        } else {
            Lock lock = lockService.create(path);
            openContests.put(path, lock);
            tryAcquireLeadership(path);
        }
    }

    @Override
    public void withdraw(String path) {
        checkArgument(path != null);
        Lock lock = openContests.remove(path);

        if (lock != null && lock.isLocked()) {
            lock.unlock();
            notifyListeners(
                    new LeadershipEvent(
                            LeadershipEvent.Type.LEADER_BOOTED,
                            new Leadership(lock.path(), localNode, 0)));
                            // FIXME: Should set the correct term information.
        }
    }

    @Override
    public void addListener(LeadershipEventListener listener) {
        checkArgument(listener != null);
        listeners.add(listener);
    }

    @Override
    public void removeListener(LeadershipEventListener listener) {
        checkArgument(listener != null);
        listeners.remove(listener);
    }

    private void notifyListeners(LeadershipEvent event) {
        for (LeadershipEventListener listener : listeners) {
            listener.event(event);
        }
    }

    private void tryAcquireLeadership(String path) {
        Lock lock = openContests.get(path);
        verifyNotNull(lock, "Lock should not be null");
        lock.lockAsync(TERM_DURATION_MS).whenComplete((response, error) -> {
            if (error == null) {
                THREAD_POOL.schedule(
                        new RelectionTask(lock),
                        TERM_DURATION_MS / 2,
                        TimeUnit.MILLISECONDS);
                notifyListeners(
                        new LeadershipEvent(
                                LeadershipEvent.Type.LEADER_ELECTED,
                                new Leadership(lock.path(), localNode, 0)));
            } else {
                log.error("Failed to acquire lock for {}", path, error);
                // retry
                tryAcquireLeadership(path);
            }
        });
    }

    private class RelectionTask implements Runnable {

        private final Lock lock;

        public RelectionTask(Lock lock) {
           this.lock = lock;
        }

        @Override
        public void run() {
            if (lock.extendExpiration(TERM_DURATION_MS)) {
                notifyListeners(
                        new LeadershipEvent(
                                LeadershipEvent.Type.LEADER_REELECTED,
                                new Leadership(lock.path(), localNode, 0)));
                THREAD_POOL.schedule(this, TERM_DURATION_MS / 2, TimeUnit.MILLISECONDS);
            } else {
                if (openContests.containsKey(lock.path())) {
                    notifyListeners(
                            new LeadershipEvent(
                                    LeadershipEvent.Type.LEADER_BOOTED,
                                    new Leadership(lock.path(), localNode, 0)));
                    tryAcquireLeadership(lock.path());
                }
            }
        }
    }
}