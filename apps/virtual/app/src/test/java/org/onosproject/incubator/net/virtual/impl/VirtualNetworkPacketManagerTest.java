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

package org.onosproject.incubator.net.virtual.impl;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.TestApplicationId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowObjectiveStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkFlowRuleStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkPacketStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualFlowRuleProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.net.virtual.store.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualFlowObjectiveStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualFlowRuleStore;
import org.onosproject.incubator.net.virtual.store.impl.SimpleVirtualPacketStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.FlowObjectiveServiceAdapter;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestStorageService;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.onosproject.net.flowobjective.Objective.Operation.ADD;
import static org.onosproject.net.flowobjective.Objective.Operation.REMOVE;
import static org.onosproject.net.packet.PacketPriority.CONTROL;
import static org.onosproject.net.packet.PacketPriority.REACTIVE;

/**
 * Junit tests for VirtualNetworkPacketManager using SimpleVirtualPacketStore.
 */
public class VirtualNetworkPacketManagerTest extends VirtualNetworkTestUtil {

    private static final int PROCESSOR_PRIORITY = 1;

    protected VirtualNetworkManager manager;
    protected DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService = new TestCoreService();
    protected TestServiceDirectory testDirectory;
    private EventDeliveryService eventDeliveryService;
    private VirtualProviderManager providerRegistryService;

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    private VirtualPacketProvider provider = new TestPacketProvider();
    protected VirtualNetworkPacketStore packetStore = new SimpleVirtualPacketStore();

    protected VirtualNetworkPacketManager packetManager1;
    private VirtualNetworkPacketManager packetManager2;

    private ApplicationId appId = new TestApplicationId("VirtualPacketManagerTest");

    private VirtualFlowRuleProvider flowRuleProvider = new TestFlowRuleProvider();
    private SimpleVirtualFlowRuleStore flowRuleStore;
    private SimpleVirtualFlowObjectiveStore flowObjectiveStore;
    protected StorageService storageService = new TestStorageService();

    @Before
    public void setUp() throws TestUtils.TestUtilsException {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", storageService);
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.coreService = coreService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());

        flowObjectiveStore = new SimpleVirtualFlowObjectiveStore();
        TestUtils.setField(flowObjectiveStore, "storageService", storageService);
        flowObjectiveStore.activate();
        flowRuleStore = new SimpleVirtualFlowRuleStore();
        flowRuleStore.activate();

        providerRegistryService = new VirtualProviderManager();
        providerRegistryService.registerProvider(provider);
        providerRegistryService.registerProvider(flowRuleProvider);

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(VirtualProviderRegistryService.class, providerRegistryService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(ClusterService.class, new ClusterServiceAdapter())
                .add(VirtualNetworkFlowRuleStore.class, flowRuleStore)
                .add(VirtualNetworkFlowObjectiveStore.class, flowObjectiveStore)
                .add(VirtualNetworkPacketStore.class, packetStore);
        TestUtils.setField(manager, "serviceDirectory", testDirectory);

        eventDeliveryService = new TestEventDispatcher();
        NetTestTools.injectEventDispatcher(manager, eventDeliveryService);

        manager.activate();

        vnet1 = VirtualNetworkTestUtil.setupVirtualNetworkTopology(manager, TID1);
        vnet2 = VirtualNetworkTestUtil.setupVirtualNetworkTopology(manager, TID2);

        packetManager1 = new VirtualNetworkPacketManager(manager, vnet1.id());
        packetManager2 = new VirtualNetworkPacketManager(manager, vnet2.id());
    }

    /**
     * Tests the correct usage of addProcessor() for a outbound packet.
     */
    @Test
    public void addProcessorTest() {
        PacketProcessor testProcessor = new TestProcessor();
        packetManager1.addProcessor(testProcessor, PROCESSOR_PRIORITY);

        assertEquals("1 processor expected", 1,
                    packetManager1.getProcessors().size());
        assertEquals("0 processor expected", 0,
                     packetManager2.getProcessors().size());

        assertEquals("not equal packet processor", testProcessor,
                     packetManager1.getProcessors().get(0).processor());
        assertEquals("not equal packet processor priority", PROCESSOR_PRIORITY,
                     packetManager1.getProcessors().get(0).priority());
    }

    /**
     * Tests the correct usage of addProcessor() for a outbound packet.
     */
    @Test
    public void removeProcessorTest() {
        PacketProcessor testProcessor = new TestProcessor();
        packetManager1.addProcessor(testProcessor, PROCESSOR_PRIORITY);

        assertEquals("1 processor expected", 1,
                     packetManager1.getProcessors().size());
        assertEquals("0 processor expected", 0,
                     packetManager2.getProcessors().size());

        packetManager1.removeProcessor(testProcessor);

        assertEquals("0 processor expected", 0,
                     packetManager1.getProcessors().size());
        assertEquals("0 processor expected", 0,
                     packetManager2.getProcessors().size());
    }

    /**
     * Tests the correct usage of emit() for a outbound packet.
     */
    @Test
    public void emitTest() {
        OutboundPacket packet =
                new DefaultOutboundPacket(VDID1, DefaultTrafficTreatment.emptyTreatment(), ByteBuffer.allocate(5));
        packetManager1.emit(packet);
        assertEquals("Packet not emitted correctly", packet, emittedPacket);
    }

    /**
     * Tests the addition and removal of packet requests for a device.
     *
     * @throws TestUtils.TestUtilsException
     */
    @Test
    public void requestAndCancelPacketsForDeviceTest() throws TestUtils.TestUtilsException {
        TestFlowObjectiveService testFlowObjectiveService = new TestFlowObjectiveService();
        TestUtils.setField(packetManager1, "objectiveService", testFlowObjectiveService);
        TrafficSelector ts = DefaultTrafficSelector.emptySelector();
        Optional<DeviceId> optionalDeviceId = Optional.of(VDID3);

        // add first request
        packetManager1.requestPackets(ts, CONTROL, appId, optionalDeviceId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectiveForDevice(VDID3, ts, CONTROL, ADD);

        // add same request as first
        packetManager1.requestPackets(ts, CONTROL, appId, optionalDeviceId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectiveForDevice(VDID3, ts, CONTROL, ADD);

        // add second request
        packetManager1.requestPackets(ts, REACTIVE, appId, optionalDeviceId);
        assertEquals("2 packets expected", 2, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectiveForDevice(VDID3, ts, REACTIVE, ADD);

        // cancel second request
        packetManager1.cancelPackets(ts, REACTIVE, appId, optionalDeviceId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectiveForDevice(VDID3, ts, REACTIVE, REMOVE);

        // cancel second request again
        packetManager1.cancelPackets(ts, REACTIVE, appId, optionalDeviceId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectiveForDevice(VDID3, ts, REACTIVE, REMOVE);

        // cancel first request
        packetManager1.cancelPackets(ts, CONTROL, appId, optionalDeviceId);
        assertEquals("0 packet expected", 0, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectiveForDevice(VDID3, ts, CONTROL, REMOVE);
    }

    /**
     * Tests the addition and removal of packet requests for all devices in a virtual
     * network.
     *
     * @throws TestUtils.TestUtilsException
     */
    @Test
    public void requestAndCancelPacketsForVnetTest() throws TestUtils.TestUtilsException {
        TestFlowObjectiveService testFlowObjectiveService = new TestFlowObjectiveService();
        TestUtils.setField(packetManager1, "objectiveService", testFlowObjectiveService);
        TrafficSelector ts = DefaultTrafficSelector.emptySelector();
        Set<VirtualDevice> vnet1Devices = manager.getVirtualDevices(vnet1.id());

        // add first request
        packetManager1.requestPackets(ts, CONTROL, appId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectives(vnet1Devices, ts, CONTROL, ADD);

        // add same request as first
        packetManager1.requestPackets(ts, CONTROL, appId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectives(vnet1Devices, ts, CONTROL, ADD);

        // add second request
        packetManager1.requestPackets(ts, REACTIVE, appId);
        assertEquals("2 packets expected", 2, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectives(vnet1Devices, ts, REACTIVE, ADD);

        // cancel second request
        packetManager1.cancelPackets(ts, REACTIVE, appId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectives(vnet1Devices, ts, REACTIVE, REMOVE);

        // cancel second request again
        packetManager1.cancelPackets(ts, REACTIVE, appId);
        assertEquals("1 packet expected", 1, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectives(vnet1Devices, ts, REACTIVE, REMOVE);

        // cancel first request
        packetManager1.cancelPackets(ts, CONTROL, appId);
        assertEquals("0 packet expected", 0, packetManager1.getRequests().size());
        testFlowObjectiveService.validateObjectives(vnet1Devices, ts, CONTROL, REMOVE);
    }

    protected OutboundPacket emittedPacket = null;

    /**
     * Core service test class.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }

        @Override
        public ApplicationId registerApplication(String name) {
            return appId;
        }
    }

    private class TestPacketProvider extends AbstractVirtualProvider
            implements VirtualPacketProvider {

        /**
         * Creates a provider with the supplied identifier.
         */
        protected TestPacketProvider() {
            super(new ProviderId("test-packet",
                                 "org.onosproject.virtual.test-packet"));
        }

        @Override
        public void emit(NetworkId networkId, OutboundPacket packet) {
            emittedPacket = packet;
        }

    }

    private class TestProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

        }
    }

    private class TestFlowObjectiveService extends FlowObjectiveServiceAdapter {
        // track objectives received for each device
        private final Map<DeviceId, Set<ForwardingObjective>> deviceFwdObjs = new HashMap<>();

        @Override
        public void forward(DeviceId deviceId, ForwardingObjective forwardingObjective) {
            deviceFwdObjs.compute(deviceId, (deviceId1, forwardingObjectives) -> {
                        if (forwardingObjectives == null) {
                            return Sets.newHashSet(forwardingObjective);
                        }
                        forwardingObjectives.add(forwardingObjective);
                        return forwardingObjectives;
                    }
            );
        }

        private void validateObjectives(Set<VirtualDevice> vdevs, TrafficSelector ts,
                                    PacketPriority pp, Objective.Operation op) {
            assertNotNull("set of devices must not be null", vdevs);
            for (VirtualDevice vdev: vdevs) {
                assertTrue("Forwarding objective must exist for device " + vdev.id(),
                           deviceHasObjective(vdev.id(), ts, pp, op));
            }
        }

        private void validateObjectiveForDevice(DeviceId deviceId, TrafficSelector ts,
                                    PacketPriority pp, Objective.Operation op) {
            assertNotNull("deviceId must not be null", deviceId);
            assertTrue("Forwarding objective must exist for device " + deviceId,
                           deviceHasObjective(deviceId, ts, pp, op));
        }

        private boolean deviceHasObjective(DeviceId deviceId, TrafficSelector ts,
                                   PacketPriority pp, Objective.Operation op) {
            Set<ForwardingObjective> fos = deviceFwdObjs.get(deviceId);
            if (fos != null) {
                for (ForwardingObjective fo: fos) {
                    if (fo.selector().equals(ts)
                            && fo.priority() == pp.priorityValue()
                            && fo.op().equals(op)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private class TestFlowRuleProvider extends AbstractVirtualProvider
            implements VirtualFlowRuleProvider {

        protected TestFlowRuleProvider() {
            super(new ProviderId("test", "org.onosproject.virtual.testprovider"));
        }

        @Override
        public void applyFlowRule(NetworkId networkId, FlowRule... flowRules) {

        }

        @Override
        public void removeFlowRule(NetworkId networkId, FlowRule... flowRules) {

        }

        @Override
        public void executeBatch(NetworkId networkId, FlowRuleBatchOperation batch) {

        }
    }
}
