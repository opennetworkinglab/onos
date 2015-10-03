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
import org.onlab.util.KryoNamespace;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
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
import static org.onosproject.cordvtn.OvsdbNode.State;
import static org.onosproject.cordvtn.OvsdbNode.State.INIT;
import static org.onosproject.cordvtn.OvsdbNode.State.DISCONNECT;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides initial setup or cleanup for provisioning virtual tenant networks
 * on ovsdb, integration bridge and vm when they are added or deleted.
 */
@Component(immediate = true)
@Service
public class CordVtn implements CordVtnService {

    protected final Logger log = getLogger(getClass());

    private static final int NUM_THREADS = 1;
    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OvsdbNode.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private final ExecutorService eventExecutor = Executors
            .newFixedThreadPool(NUM_THREADS, groupedThreads("onos/cordvtn", "event-handler"));

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();
    private final VmHandler vmHandler = new VmHandler();

    private EventuallyConsistentMap<DeviceId, OvsdbNode> nodeStore;

    @Activate
    protected void activate() {
        coreService.registerApplication("org.onosproject.cordvtn");
        nodeStore = storageService.<DeviceId, OvsdbNode>eventuallyConsistentMapBuilder()
                .withName("cordvtn-nodestore")
                .withSerializer(NODE_SERIALIZER)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();

        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);

        eventExecutor.shutdown();
        nodeStore.destroy();

        log.info("Stopped");
    }

    @Override
    public void addNode(OvsdbNode ovsdbNode) {
        if (nodeStore.containsKey(ovsdbNode.deviceId())) {
            log.warn("Node {} already exists", ovsdbNode.host());
            return;
        }
        nodeStore.put(ovsdbNode.deviceId(), ovsdbNode);
        if (ovsdbNode.state() != INIT) {
            updateNode(ovsdbNode, INIT);
        }
    }

    @Override
    public void deleteNode(OvsdbNode ovsdbNode) {
        if (!nodeStore.containsKey(ovsdbNode.deviceId())) {
            log.warn("Node {} does not exist", ovsdbNode.host());
            return;
        }
        updateNode(ovsdbNode, DISCONNECT);
    }

    @Override
    public void updateNode(OvsdbNode ovsdbNode, State state) {
        if (!nodeStore.containsKey(ovsdbNode.deviceId())) {
            log.warn("Node {} does not exist", ovsdbNode.host());
            return;
        }
        DefaultOvsdbNode updatedNode = new DefaultOvsdbNode(ovsdbNode.host(),
                                                            ovsdbNode.ip(),
                                                            ovsdbNode.port(),
                                                            state);
        nodeStore.put(ovsdbNode.deviceId(), updatedNode);
    }

    @Override
    public int getNodeCount() {
        return nodeStore.size();
    }

    @Override
    public OvsdbNode getNode(DeviceId deviceId) {
        return nodeStore.get(deviceId);
    }

    @Override
    public List<OvsdbNode> getNodes() {
        return nodeStore.values()
                .stream()
                .collect(Collectors.toList());
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            ConnectionHandler handler = (device.type() == SWITCH ? bridgeHandler : ovsdbHandler);

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

    private class OvsdbHandler implements ConnectionHandler<Device> {

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

    private class VmHandler implements ConnectionHandler<Host> {

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
