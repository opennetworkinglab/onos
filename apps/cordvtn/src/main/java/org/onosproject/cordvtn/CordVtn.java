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
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.OvsdbNode.State.INIT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * CORD VTN Application that provisions overlay virtual tenant networks.
 */
@Component(immediate = true)
@Service
public class CordVtn implements CordVtnService {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private static final int DEFAULT_NUM_THREADS = 1;
    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OvsdbNode.class);

    private final ExecutorService eventExecutor = Executors.newFixedThreadPool(
            DEFAULT_NUM_THREADS, groupedThreads("onos/cordvtn", "event-handler"));

    private final LeadershipEventListener leadershipListener = new InternalLeadershipListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();
    private final NodeHandler nodeHandler = new NodeHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();
    private final VirtualMachineHandler vmHandler = new VirtualMachineHandler();

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, CordVtnConfig.class, "cordvtn") {
                @Override
                public CordVtnConfig createConfig() {
                    return new CordVtnConfig();
                }
            };

    private ApplicationId appId;
    private NodeId local;
    private EventuallyConsistentMap<DeviceId, OvsdbNode> nodeStore;
    private NodeConnectionManager nodeConnectionManager;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.cordvtn");

        local = clusterService.getLocalNode().id();
        nodeStore = storageService.<DeviceId, OvsdbNode>eventuallyConsistentMapBuilder()
                .withName("cordvtn-nodestore")
                .withSerializer(NODE_SERIALIZER)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();
        configRegistry.registerConfigFactory(configFactory);

        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        leadershipService.addListener(leadershipListener);
        leadershipService.runForLeadership(appId.name());
        nodeConnectionManager = new NodeConnectionManager(appId, local, nodeStore,
                                            mastershipService, leadershipService);
        nodeConnectionManager.start();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        nodeConnectionManager.stop();
        leadershipService.removeListener(leadershipListener);
        leadershipService.withdraw(appId.name());
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        eventExecutor.shutdown();
        nodeStore.destroy();
        configRegistry.unregisterConfigFactory(configFactory);
        log.info("Stopped");
    }

    @Override
    public void addNode(String hostname, IpAddress ip, TpPort port) {
        DefaultOvsdbNode node = new DefaultOvsdbNode(hostname, ip, port, DeviceId.NONE, INIT);

        if (nodeStore.containsKey(node.deviceId())) {
            log.warn("Node {} with ovsdb-server {}:{} already exists", hostname, ip, port);
            return;
        }
        nodeStore.put(node.deviceId(), node);
        log.info("New node {} with ovsdb-server {}:{} has been added", hostname, ip, port);
    }

    @Override
    public void deleteNode(IpAddress ip, TpPort port) {
        DeviceId deviceId = DeviceId.deviceId("ovsdb:" + ip + ":" + port);
        OvsdbNode node = nodeStore.get(deviceId);

        if (node == null) {
            log.warn("Node with ovsdb-server on {}:{} does not exist", ip, port);
            return;
        }
        nodeConnectionManager.disconnectNode(node);
        nodeStore.remove(node.deviceId());
    }

    @Override
    public int getNodeCount() {
        return nodeStore.size();
    }

    @Override
    public List<OvsdbNode> getNodes() {
        return nodeStore.values()
                .stream()
                .collect(Collectors.toList());
    }

    private void initialSetup() {
        // Read ovsdb nodes from network config
        CordVtnConfig config = configService.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.warn("No configuration found");
            return;
        }
        config.ovsdbNodes().forEach(
                node -> addNode(node.hostname(), node.ip(), node.port()));
    }

    private synchronized void processLeadershipChange(NodeId leader) {
        // Only the leader performs the initial setup
        if (leader == null || !leader.equals(local)) {
            return;
        }
        initialSetup();
    }

    private class InternalLeadershipListener implements LeadershipEventListener {

        @Override
        public void event(LeadershipEvent event) {
            if (event.subject().topic().equals(appId.name())) {
                processLeadershipChange(event.subject().leader());
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            ConnectionHandler handler =
                    (device.type() == Device.Type.CONTROLLER ? nodeHandler : bridgeHandler);

            switch (event.type()) {
                    case DEVICE_ADDED:
                        eventExecutor.submit(() -> handler.connected(device));
                        break;
                    case DEVICE_AVAILABILITY_CHANGED:
                        eventExecutor.submit(() -> handler.disconnected(device));
                        break;
                    default:
                        break;
            }
        }
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host vm = event.subject();

            switch (event.type()) {
                case HOST_ADDED:
                    eventExecutor.submit(() -> vmHandler.connected(vm));
                    break;
                case HOST_REMOVED:
                    eventExecutor.submit(() -> vmHandler.disconnected(vm));
                    break;
                default:
                    break;
            }
        }
    }

    private class NodeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            // create bridge and set bridgeId
            // set node state connected
        }

        @Override
        public void disconnected(Device device) {
            // set node state disconnected if the node exists
            // which means that the node is not deleted explicitly
        }
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            // create vxlan port
        }

        @Override
        public void disconnected(Device device) {

        }
    }

    private class VirtualMachineHandler implements ConnectionHandler<Host> {

        @Override
        public void connected(Host host) {
            // install flow rules for this vm
        }

        @Override
        public void disconnected(Host host) {
            // uninstall flow rules associated with this vm
        }
    }
}
