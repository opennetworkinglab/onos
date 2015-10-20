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

import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
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
            .register(DefaultOvsdbNode.class);
    private static final String DEFAULT_BRIDGE_NAME = "br-int";
    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final Map<String, String> DEFAULT_TUNNEL_OPTIONS = new HashMap<String, String>() {
        {
            put("key", "flow");
            put("local_ip", "flow");
            put("remote_ip", "flow");
        }
    };
    private static final int DPID_BEGIN = 3;
    private static final int OFPORT = 6653;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private final ExecutorService eventExecutor = Executors
            .newFixedThreadPool(NUM_THREADS, groupedThreads("onos/cordvtn", "event-handler"));

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();
    private final VmHandler vmHandler = new VmHandler();

    private ConsistentMap<DeviceId, OvsdbNode> nodeStore;

    @Activate
    protected void activate() {
        ApplicationId appId = coreService.registerApplication("org.onosproject.cordvtn");
        nodeStore = storageService.<DeviceId, OvsdbNode>consistentMapBuilder()
                .withSerializer(Serializer.using(NODE_SERIALIZER.build()))
                .withName("cordvtn-nodestore")
                .withApplicationId(appId)
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
        nodeStore.clear();

        log.info("Stopped");
    }

    @Override
    public void addNode(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        nodeStore.putIfAbsent(ovsdb.deviceId(), ovsdb);

        if (isNodeConnected(ovsdb)) {
            init(ovsdb);
        } else {
            connect(ovsdb);
        }
    }

    @Override
    public void deleteNode(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        if (deviceService.getDevice(ovsdb.deviceId()) != null) {
            if (deviceService.isAvailable(ovsdb.deviceId())) {
                log.warn("Cannot delete connected node {}", ovsdb.host());
                return;
            }
        }
        nodeStore.remove(ovsdb.deviceId());
    }

    @Override
    public void connect(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        if (!nodeStore.containsKey(ovsdb.deviceId())) {
            log.warn("Node {} does not exist", ovsdb.host());
            return;
        }

        if (!isNodeConnected(ovsdb)) {
            controller.connect(ovsdb.ip(), ovsdb.port());
        }
    }

    @Override
    public void disconnect(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        if (!nodeStore.containsKey(ovsdb.deviceId())) {
            log.warn("Node {} does not exist", ovsdb.host());
            return;
        }

        if (isNodeConnected(ovsdb)) {
            OvsdbClientService ovsdbClient = getOvsdbClient(ovsdb);
            ovsdbClient.disconnect();
        }
    }

    private void init(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        if (!nodeStore.containsKey(ovsdb.deviceId())) {
            log.warn("Node {} does not exist", ovsdb.host());
            return;
        }

        if (!isNodeConnected(ovsdb)) {
            log.warn("Node {} is not connected", ovsdb.host());
            return;
        }

        if (deviceService.getDevice(ovsdb.intBrId()) == null ||
                !deviceService.isAvailable(ovsdb.intBrId())) {
            createIntegrationBridge(ovsdb);
        } else if (!checkVxlanPort(ovsdb)) {
            createVxlanPort(ovsdb);
        }
    }

    @Override
    public int getNodeCount() {
        return nodeStore.size();
    }

    @Override
    public OvsdbNode getNode(DeviceId deviceId) {
        Versioned<OvsdbNode> ovsdb = nodeStore.get(deviceId);
        if (ovsdb != null) {
            return ovsdb.value();
        } else {
            return null;
        }
    }

    @Override
    public List<OvsdbNode> getNodes() {
        List<OvsdbNode> ovsdbs = new ArrayList<>();
        ovsdbs.addAll(Collections2.transform(nodeStore.values(), Versioned::value));
        return ovsdbs;
    }

    @Override
    public boolean isNodeConnected(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        OvsdbClientService ovsdbClient = getOvsdbClient(ovsdb);
        if (ovsdbClient == null) {
            return false;
        } else {
            return ovsdbClient.isConnected();
        }
    }

    private OvsdbClientService getOvsdbClient(OvsdbNode ovsdb) {
        checkNotNull(ovsdb);

        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
                new OvsdbNodeId(ovsdb.ip(), ovsdb.port().toInt()));
        if (ovsdbClient == null) {
            log.debug("Couldn't find ovsdb client for {}", ovsdb.host());
        }
        return ovsdbClient;
    }

    private void createIntegrationBridge(OvsdbNode ovsdb) {
        List<ControllerInfo> controllers = new ArrayList<>();
        Sets.newHashSet(clusterService.getNodes())
                .forEach(controller -> {
                    ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "tcp");
                    controllers.add(ctrlInfo);
                });
        String dpid = ovsdb.intBrId().toString().substring(DPID_BEGIN);

        // TODO change to use bridge config
        OvsdbClientService ovsdbClient = getOvsdbClient(ovsdb);
        ovsdbClient.createBridge(DEFAULT_BRIDGE_NAME, dpid, controllers);
    }

    private void createVxlanPort(OvsdbNode ovsdb) {
        // TODO change to use tunnel config and tunnel description
        OvsdbClientService ovsdbClient = getOvsdbClient(ovsdb);
        ovsdbClient.createTunnel(DEFAULT_BRIDGE_NAME, DEFAULT_TUNNEL,
                                 DEFAULT_TUNNEL, DEFAULT_TUNNEL_OPTIONS);
    }

    private boolean checkVxlanPort(OvsdbNode ovsdb) {
        // TODO change to use tunnel config
        OvsdbClientService ovsdbClient = getOvsdbClient(ovsdb);
        try {
            ovsdbClient.getPorts().stream()
                    .filter(p -> p.portName().value().equals(DEFAULT_TUNNEL))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
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
                    if (deviceService.isAvailable(device.id())) {
                        eventExecutor.submit(() -> handler.connected(device));
                    } else {
                        eventExecutor.submit(() -> handler.disconnected(device));
                    }
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
            log.info("Ovsdb {} is connected", device.id());

            OvsdbNode ovsdb = getNode(device.id());
            if (ovsdb != null) {
                init(ovsdb);
            }
        }

        @Override
        public void disconnected(Device device) {
            log.warn("Ovsdb {} is disconnected", device.id());
        }
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            log.info("Integration Bridge {} is detected", device.id());

            OvsdbNode ovsdb;
            try {
                ovsdb = getNodes().stream()
                        .filter(node -> node.intBrId().equals(device.id()))
                        .findFirst().get();
            } catch (NoSuchElementException e) {
                log.warn("Couldn't find OVSDB associated with {}", device.id());
                return;
            }

            if (!checkVxlanPort(ovsdb)) {
                createVxlanPort(ovsdb);
            }
        }

        @Override
        public void disconnected(Device device) {
            log.info("Integration Bridge {} is vanished", device.id());
        }
    }

    private class VmHandler implements ConnectionHandler<Host> {

        @Override
        public void connected(Host host) {
            log.info("VM {} is detected", host.id());
        }

        @Override
        public void disconnected(Host host) {
            log.info("VM {} is vanished", host.id());
        }
    }
}
