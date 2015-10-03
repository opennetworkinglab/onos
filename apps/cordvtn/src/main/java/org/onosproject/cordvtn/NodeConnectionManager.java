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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.OvsdbNode.State.CONNECTED;
import static org.onosproject.cordvtn.OvsdbNode.State.DISCONNECTED;
import static org.onosproject.cordvtn.OvsdbNode.State.READY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides the connection state management of all nodes registered to the service
 * so that the nodes keep connected unless it is requested to be deleted.
 */
@Component(immediate = true)
public class NodeConnectionManager {
    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    CordVtnService cordVtnService;

    private static final int DELAY_SEC = 5;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final ScheduledExecutorService connectionExecutor = Executors
            .newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtn", "connection-manager"));

    private NodeId localId;

    @Activate
    protected void activate() {
        localId = clusterService.getLocalNode().id();
        deviceService.addListener(deviceListener);

        connectionExecutor.scheduleWithFixedDelay(() -> cordVtnService.getNodes()
                .stream()
                .filter(node -> localId.equals(getMaster(node)))
                .forEach(node -> {
                    connect(node);
                    disconnect(node);
                }), 0, DELAY_SEC, TimeUnit.SECONDS);
    }

    @Deactivate
    public void stop() {
        connectionExecutor.shutdown();
        deviceService.removeListener(deviceListener);
    }

    public void connect(OvsdbNode ovsdbNode) {
        switch (ovsdbNode.state()) {
            case INIT:
            case DISCONNECTED:
                setPassiveMode(ovsdbNode);
            case READY:
                setupConnection(ovsdbNode);
                break;
            default:
                break;
        }
    }

    public void disconnect(OvsdbNode ovsdbNode) {
        switch (ovsdbNode.state()) {
            case DISCONNECT:
                // TODO: disconnect
                break;
            default:
                break;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            if (device.type() != Device.Type.CONTROLLER) {
                return;
            }

            DefaultOvsdbNode node;
            switch (event.type()) {
                case DEVICE_ADDED:
                    node = (DefaultOvsdbNode) cordVtnService.getNode(device.id());
                    if (node != null) {
                        cordVtnService.updateNode(node, CONNECTED);
                    }
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    node = (DefaultOvsdbNode) cordVtnService.getNode(device.id());
                    if (node != null) {
                        cordVtnService.updateNode(node, DISCONNECTED);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private NodeId getMaster(OvsdbNode ovsdbNode) {
        NodeId master = mastershipService.getMasterFor(ovsdbNode.intBrId());

        // master is null if there's no such device
        if (master == null) {
            master = leadershipService.getLeader(CordVtnService.CORDVTN_APP_ID);
        }
        return master;
    }

    private void setPassiveMode(OvsdbNode ovsdbNode) {
        // TODO: need ovsdb client implementation first
        // TODO: set the remove ovsdb server passive mode
        cordVtnService.updateNode(ovsdbNode, READY);
    }

    private void setupConnection(OvsdbNode ovsdbNode) {
        // TODO initiate connection
    }
}
