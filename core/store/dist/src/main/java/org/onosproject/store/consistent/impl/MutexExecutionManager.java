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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode.State;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MutexExecutionService;
import org.onosproject.store.service.MutexTask;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implementation of a MutexExecutionService.
 */
@Component(immediate = true)
@Service
public class MutexExecutionManager implements MutexExecutionService {

    private final Logger log = getLogger(getClass());

    protected ConsistentMap<String, MutexState> lockMap;
    protected NodeId localNodeId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final MapEventListener<String, MutexState> mapEventListener = new InternalLockMapEventListener();
    private final ClusterEventListener clusterEventListener = new InternalClusterEventListener();

    private Map<String, CompletableFuture<MutexState>> pending = Maps.newConcurrentMap();
    private Map<String, InnerMutexTask> activeTasks = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        lockMap = storageService.<String, MutexState>consistentMapBuilder()
                    .withName("onos-mutexes")
                    .withSerializer(Serializer.using(Arrays.asList(KryoNamespaces.API), MutexState.class))
                    .withPartitionsDisabled()
                    .build();
        lockMap.addListener(mapEventListener);
        clusterService.addListener(clusterEventListener);
        releaseOldLocks();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        lockMap.removeListener(mapEventListener);
        pending.values().forEach(future -> future.cancel(true));
        activeTasks.forEach((k, v) -> {
            v.stop();
            unlock(k);
        });
        clusterService.removeListener(clusterEventListener);
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<Void> execute(MutexTask task, String exclusionPath, Executor executor) {
        return lock(exclusionPath)
                    .thenApply(state -> activeTasks.computeIfAbsent(exclusionPath,
                                                                    k -> new InnerMutexTask(exclusionPath,
                                                                                            task,
                                                                                            state.term())))
                    .thenAcceptAsync(t -> t.start(), executor)
                    .whenComplete((r, e) -> unlock(exclusionPath));
    }

    protected CompletableFuture<MutexState> lock(String exclusionPath) {
        CompletableFuture<MutexState> future =
                pending.computeIfAbsent(exclusionPath, k -> new CompletableFuture<>());
        tryLock(exclusionPath);
        return future;
    }

    /**
     * Attempts to acquire lock for a path. If lock is held by some other node, adds this node to
     * the wait list.
     * @param exclusionPath exclusion path
     */
    protected void tryLock(String exclusionPath) {
        Tools.retryable(() -> lockMap.asJavaMap()
                                     .compute(exclusionPath,
                                              (k, v) -> MutexState.admit(v, localNodeId)),
                                              ConsistentMapException.ConcurrentModification.class,
                                              Integer.MAX_VALUE,
                                              100).get();
    }

    /**
     * Releases lock for the specific path. This operation is idempotent.
     * @param exclusionPath exclusion path
     */
    protected void unlock(String exclusionPath) {
        Tools.retryable(() -> lockMap.asJavaMap()
                                     .compute(exclusionPath, (k, v) -> MutexState.evict(v, localNodeId)),
                        ConsistentMapException.ConcurrentModification.class,
                        Integer.MAX_VALUE,
                        100).get();
    }

    /**
     * Detects and releases all locks held by this node.
     */
    private void releaseOldLocks() {
        Maps.filterValues(lockMap.asJavaMap(), state -> localNodeId.equals(state.holder()))
            .keySet()
            .forEach(path -> {
                log.info("Detected zombie task still holding lock for {}. Releasing lock.", path);
                unlock(path);
            });
    }

    private class InternalLockMapEventListener implements MapEventListener<String, MutexState> {

        @Override
        public void event(MapEvent<String, MutexState> event) {
            log.debug("Received {}", event);
            if (event.type() == MapEvent.Type.UPDATE || event.type() == MapEvent.Type.INSERT) {
                pending.computeIfPresent(event.key(), (k, future) -> {
                    MutexState state = Versioned.valueOrElse(event.value(), null);
                    if (state != null && localNodeId.equals(state.holder())) {
                        log.debug("Local node is now owner for {}", event.key());
                        future.complete(state);
                        return null;
                    } else {
                        return future;
                    }
                });
                InnerMutexTask task = activeTasks.get(event.key());
                if (task != null && task.term() < Versioned.valueOrElse(event.value(), null).term()) {
                    task.stop();
                }
            }
        }
    }

    private class InternalClusterEventListener implements ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            if (event.type() == ClusterEvent.Type.INSTANCE_DEACTIVATED ||
                    event.type() == ClusterEvent.Type.INSTANCE_REMOVED) {
                NodeId nodeId = event.subject().id();
                log.debug("{} is no longer active. Attemping to clean up its locks.", nodeId);
                lockMap.asJavaMap().forEach((k, v) -> {
                    if (v.contains(nodeId)) {
                        lockMap.compute(k, (path, state) -> MutexState.evict(v, nodeId));
                    }
                });
            }
            long activeNodes = clusterService.getNodes()
                                             .stream()
                                             .map(node -> clusterService.getState(node.id()))
                                             .filter(State.ACTIVE::equals)
                                             .count();
            if (clusterService.getNodes().size() > 1 && activeNodes == 1) {
                log.info("This node is partitioned away from the cluster. Stopping all inflight executions");
                activeTasks.forEach((k, v) -> {
                    v.stop();
                });
            }
        }
    }

    private static final class MutexState {

        private final NodeId holder;
        private final List<NodeId> waitList;
        private final long term;

        public static MutexState admit(MutexState state, NodeId nodeId) {
            if (state == null) {
                return new MutexState(nodeId, 1L, Lists.newArrayList());
            } else if (state.holder() == null) {
                return new MutexState(nodeId, state.term() + 1, Lists.newArrayList());
            } else {
                if (!state.contains(nodeId)) {
                    NodeId newHolder = state.holder();
                    List<NodeId> newWaitList = Lists.newArrayList(state.waitList());
                    newWaitList.add(nodeId);
                    return new MutexState(newHolder, state.term(), newWaitList);
                } else {
                    return state;
                }
            }
        }

        public static MutexState evict(MutexState state, NodeId nodeId) {
            return state.evict(nodeId);
        }

        public MutexState evict(NodeId nodeId) {
            if (nodeId.equals(holder)) {
                if (waitList.isEmpty()) {
                    return new MutexState(null, term, waitList);
                }
                List<NodeId> newWaitList = Lists.newArrayList(waitList);
                NodeId newHolder = newWaitList.remove(0);
                return new MutexState(newHolder, term + 1, newWaitList);
            } else {
                NodeId newHolder = holder;
                List<NodeId> newWaitList = Lists.newArrayList(waitList);
                newWaitList.remove(nodeId);
                return new MutexState(newHolder, term, newWaitList);
            }
        }

        public NodeId holder() {
            return holder;
        }

        public List<NodeId> waitList() {
            return waitList;
        }

        public long term() {
            return term;
        }

        private boolean contains(NodeId nodeId) {
            return (nodeId.equals(holder) || waitList.contains(nodeId));
        }

        private MutexState(NodeId holder, long term, List<NodeId> waitList) {
            this.holder = holder;
            this.term = term;
            this.waitList = Lists.newArrayList(waitList);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("holder", holder)
                    .add("term", term)
                    .add("waitList", waitList)
                    .toString();
        }
    }

    private class InnerMutexTask implements MutexTask {
        private final MutexTask task;
        private final String mutexPath;
        private final long term;

        public InnerMutexTask(String mutexPath, MutexTask task, long term) {
            this.mutexPath = mutexPath;
            this.term = term;
            this.task = task;
        }

        public long term() {
            return term;
        }

        @Override
        public void start() {
            log.debug("Starting execution for mutex task guarded by {}", mutexPath);
            task.start();
            log.debug("Finished execution for mutex task guarded by {}", mutexPath);
        }

        @Override
        public void stop() {
            log.debug("Stopping execution for mutex task guarded by {}", mutexPath);
            task.stop();
        }
    }
}