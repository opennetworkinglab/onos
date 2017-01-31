/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.TestServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkPacketStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkStore;
import org.onosproject.incubator.net.virtual.impl.provider.VirtualProviderManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualPacketProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
import org.onosproject.incubator.store.virtual.impl.DistributedVirtualNetworkStore;
import org.onosproject.incubator.store.virtual.impl.SimpleVirtualPacketStore;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.TestableIntentService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.TestStorageService;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class VirtualNetworkPacketManagerTest extends VirtualNetworkTestUtil {

    private static final int PROCESSOR_PRIORITY = 1;

    private VirtualNetworkManager manager;
    private DistributedVirtualNetworkStore virtualNetworkManagerStore;
    private CoreService coreService = new TestCoreService();
    private TestableIntentService intentService = new FakeIntentManager();
    private TestServiceDirectory testDirectory;
    private EventDeliveryService eventDeliveryService;
    private VirtualProviderManager providerRegistryService;

    private VirtualNetwork vnet1;
    private VirtualNetwork vnet2;

    private VirtualPacketProvider provider = new TestPacketProvider();
    private VirtualNetworkPacketStore packetStore = new SimpleVirtualPacketStore();

    private VirtualNetworkPacketManager packetManager1;
    private VirtualNetworkPacketManager packetManager2;

    @Before
    public void setUp() throws TestUtils.TestUtilsException {
        virtualNetworkManagerStore = new DistributedVirtualNetworkStore();

        TestUtils.setField(virtualNetworkManagerStore, "coreService", coreService);
        TestUtils.setField(virtualNetworkManagerStore, "storageService", new TestStorageService());
        virtualNetworkManagerStore.activate();

        manager = new VirtualNetworkManager();
        manager.store = virtualNetworkManagerStore;
        manager.coreService = coreService;
        manager.intentService = intentService;
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());

        providerRegistryService = new VirtualProviderManager();
        providerRegistryService.registerProvider(provider);

        testDirectory = new TestServiceDirectory()
                .add(VirtualNetworkStore.class, virtualNetworkManagerStore)
                .add(CoreService.class, coreService)
                .add(VirtualProviderRegistryService.class, providerRegistryService)
                .add(EventDeliveryService.class, eventDeliveryService)
                .add(ClusterService.class, new ClusterServiceAdapter())
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

    private static OutboundPacket emittedPacket = null;

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

        @Override
        public void startPacketHandling() {

        }
    }

    private class TestProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

        }
    }
}