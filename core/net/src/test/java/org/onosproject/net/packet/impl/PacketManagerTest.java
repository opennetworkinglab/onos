/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.packet.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.TestListener;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DriverRegistry;
import org.onosproject.net.driver.impl.DriverManager;
import org.onosproject.net.driver.impl.DriverRegistryManager;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.provider.TestProvider;
import org.onosproject.store.trivial.SimplePacketStore;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;

/**
 * Test packet manager activity.
 */
public class PacketManagerTest {

    private static final ProviderId FOO_PID = new ProviderId("foo", "foo");

    private static final DeviceId FOO_DID = DeviceId.deviceId("foo:002");

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "foo").build();

    private static final Device FOO_DEV =
            new DefaultDevice(FOO_PID, FOO_DID, Device.Type.SWITCH, "", "", "", "", null, ANNOTATIONS);

    private PacketManager mgr;

    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    private PacketProviderRegistry providerRegistry;

    private TestDriverManager driverService;

    @Before
    public void setUp() {
        mgr = new PacketManager();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        mgr.store = new SimplePacketStore();
        mgr.clusterService = new ClusterServiceAdapter();
        mgr.deviceService = new TestDeviceService();
        mgr.deviceService = new TestDeviceService();
        mgr.coreService = new TestCoreService();
        providerRegistry = mgr;
        mgr.activate();

        DriverRegistryManager driverRegistry = new DriverRegistryManager();
        driverService = new TestDriverManager(driverRegistry);
        driverRegistry.addDriver(new DefaultDriver("foo", ImmutableList.of(), "", "", "",
                                                   ImmutableMap.of(PacketProgrammable.class,
                                                                   TestPacketProgrammable.class),
                                                   ImmutableMap.of()));
    }

    /**
     * Tests the correct usage of fallback driver provider for packets.
     */
    @Test
    public void packetProviderfallbackBasics() {
        OutboundPacket packet =
                new DefaultOutboundPacket(FOO_DID, DefaultTrafficTreatment.emptyTreatment(), ByteBuffer.allocate(5));
        mgr.emit(packet);
        assertEquals("Packet not emitted correctly", packet, emittedPacket);
    }

    private static class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public int getDeviceCount() {
            return 1;
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.of(FOO_DEV);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return getDevices();
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return FOO_DEV;
        }
    }

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

    private class TestDriverManager extends DriverManager {
        TestDriverManager(DriverRegistry registry) {
            this.registry = registry;
            this.deviceService = mgr.deviceService;
            activate();
        }
    }

    private static OutboundPacket emittedPacket = null;

    public static class TestPacketProgrammable extends AbstractHandlerBehaviour implements PacketProgrammable {

        @Override
        public void emit(OutboundPacket packet) {
            emittedPacket = packet;
        }
    }
}
