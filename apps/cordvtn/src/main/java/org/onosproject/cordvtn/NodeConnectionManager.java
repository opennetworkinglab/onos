/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Node connection manager.
 */
public class NodeConnectionManager {
    protected final Logger log = getLogger(getClass());

    private final ApplicationId appId;
    private final NodeId localId;
    private final EventuallyConsistentMap<DeviceId, OvsdbNode> nodeStore;
    private final MastershipService mastershipService;
    private final LeadershipService leadershipService;

    private static final int DELAY_SEC = 5;
    private ScheduledExecutorService connectionExecutor;

    /**
     * Creates a new NodeConnectionManager.
     *
     * @param appId             app id
     * @param localId           local id
     * @param nodeStore         node store
     * @param mastershipService mastership service
     * @param leadershipService leadership service
     */
    public NodeConnectionManager(ApplicationId appId, NodeId localId,
                                 EventuallyConsistentMap<DeviceId, OvsdbNode> nodeStore,
                                 MastershipService mastershipService,
                                 LeadershipService leadershipService) {
        this.appId = appId;
        this.localId = localId;
        this.nodeStore = nodeStore;
        this.mastershipService = mastershipService;
        this.leadershipService = leadershipService;
    }

    /**
     * Starts the node connection manager.
     */
    public void start() {
        connectionExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/cordvtn", "connection-executor"));
        connectionExecutor.scheduleWithFixedDelay(() -> nodeStore.values()
                .stream()
                .filter(node -> localId.equals(getMaster(node)))
                .forEach(this::connectNode), 0, DELAY_SEC, TimeUnit.SECONDS);
    }

    /**
     * Stops the node connection manager.
     */
    public void stop() {
        connectionExecutor.shutdown();
    }

    /**
     * Adds a new node to the system.
     *
     * @param ovsdbNode ovsdb node
     */
    public void connectNode(OvsdbNode ovsdbNode) {
        switch (ovsdbNode.state()) {
            case INIT:
            case DISCONNECTED:
                // TODO: set the node to passive mode
            case READY:
                // TODO: initiate connection
                break;
            case CONNECTED:
                break;
            default:
        }
    }

    /**
     * Deletes the ovsdb node.
     *
     * @param ovsdbNode ovsdb node
     */
    public void disconnectNode(OvsdbNode ovsdbNode) {
        switch (ovsdbNode.state()) {
            case CONNECTED:
                // TODO: disconnect
                break;
            case INIT:
            case READY:
            case DISCONNECTED:
                break;
            default:
        }
    }

    private NodeId getMaster(OvsdbNode ovsdbNode) {
        // Return the master of the bridge(switch) if it exist or
        // return the current leader
        if (ovsdbNode.bridgeId() == DeviceId.NONE) {
            return leadershipService.getLeader(this.appId.name());
        } else {
            return mastershipService.getMasterFor(ovsdbNode.bridgeId());
        }
    }

    private void setPassiveMode(OvsdbNode ovsdbNode) {
        // TODO: need ovsdb client implementation first
        // TODO: set the remove ovsdb server passive mode
        // TODO: set the node state READY if it succeed
    }

    private void connect(OvsdbNode ovsdbNode) {
        // TODO: need ovsdb client implementation first
    }

    private void disconnect(OvsdbNode ovsdbNode) {
        // TODO: need ovsdb client implementation first
    }
}
