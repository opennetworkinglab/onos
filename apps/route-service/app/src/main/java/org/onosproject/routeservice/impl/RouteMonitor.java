/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routeservice.impl;

import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncDistributedLock;
import org.onosproject.store.service.DistributedPrimitive;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WorkQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Monitors cluster nodes and removes routes if a cluster node becomes unavailable.
 */
public class RouteMonitor {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String TOPIC = "route-reaper";
    private static final String LOCK_NAME = "route-monitor-lock";
    private static final int NUM_PARALLEL_JOBS = 10;

    private RouteAdminService routeService;
    private final ClusterService clusterService;
    private StorageService storageService;

    private final AsyncDistributedLock asyncLock;

    private WorkQueue<NodeId> queue;

    private final InternalClusterListener clusterListener = new InternalClusterListener();

    private final ScheduledExecutorService reaperExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("route/reaper", "", log));

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(groupedThreads(
            "onos/routemonitor", "events-%d", log));

    /**
     * Creates a new route monitor.
     *
     * @param routeService route service
     * @param clusterService cluster service
     * @param storageService storage service
     */
    public RouteMonitor(RouteAdminService routeService,
                        ClusterService clusterService, StorageService storageService) {
        this.routeService = routeService;
        this.clusterService = clusterService;
        this.storageService = storageService;

        asyncLock = storageService.lockBuilder().withName(LOCK_NAME).build();

        clusterService.addListener(clusterListener);

        queue = storageService.getWorkQueue(TOPIC, Serializer.using(KryoNamespaces.API));
        queue.addStatusChangeListener(this::statusChange);

        startProcessing();
    }

    /**
     * Shuts down the route monitor.
     */
    public void shutdown() {
        stopProcessing();
        clusterService.removeListener(clusterListener);
        eventExecutor.shutdownNow();
        reaperExecutor.shutdownNow();
        asyncLock.unlock();
    }

    private void statusChange(DistributedPrimitive.Status status) {
        switch (status) {
        case ACTIVE:
            startProcessing();
            break;
        case SUSPENDED:
            stopProcessing();
            break;
        case INACTIVE:
        default:
            break;
        }
    }

    private void startProcessing() {
        queue.registerTaskProcessor(this::cleanRoutes, NUM_PARALLEL_JOBS, reaperExecutor);
    }

    private void stopProcessing() {
        queue.stopProcessing();
    }

    private void cleanRoutes(NodeId node) {
        log.info("Cleaning routes from unavailable node {}", node);
        Collection<Route> routes = routeService.getRouteTables().stream()
                .flatMap(id -> routeService.getRoutes(id).stream())
                .flatMap(route -> route.allRoutes().stream())
                .map(ResolvedRoute::route)
                .filter(r -> r.sourceNode().equals(node))
                .collect(Collectors.toList());
        if (node.equals(clusterService.getLocalNode().id())) {
            log.debug("Do not remove routes from local nodes {}", node);
            return;
        }

        if (clusterService.getState(node) == ControllerNode.State.READY) {
            log.debug("Do not remove routes from active nodes {}", node);
            return;
        }

        log.debug("Withdrawing routes: {}", routes);
        routeService.withdraw(routes);
    }

    private class InternalClusterListener implements ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            eventExecutor.execute(() -> {
                if (event.instanceType() == ClusterEvent.InstanceType.STORAGE) {
                    log.debug("Skipping cluster event for {}", event.subject().id().id());
                    return;
                }

                switch (event.type()) {
                    case INSTANCE_DEACTIVATED:
                        NodeId id = event.subject().id();
                        log.info("Node {} deactivated", id);

                        // DistributedLock is introduced to guarantee that minority nodes won't try to remove
                        // routes that originated from majority nodes.
                        // Adding 15 seconds retry for the leadership election to be completed.
                        asyncLock.tryLock(Duration.ofSeconds(15)).whenComplete((result, error) -> {
                            if (result != null && result.isPresent()) {
                                log.debug("Lock obtained. Put {} into removal queue", id);
                                queue.addOne(id);
                                asyncLock.unlock();
                            } else {
                                log.debug("Fail to obtain lock. Do not remove routes from {}", id);
                            }
                        });
                        break;
                    case INSTANCE_ADDED:
                    case INSTANCE_REMOVED:
                    case INSTANCE_ACTIVATED:
                    case INSTANCE_READY:
                    default:
                        break;
                }
            });
        }
    }
}
