/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.lldp.impl;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.ONOSLLDP;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cluster.ClusterMetadataServiceAdapter;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigEvent.Type;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkProviderRegistryAdapter;
import org.onosproject.net.link.LinkProviderServiceAdapter;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.provider.lldpcommon.LinkDiscovery;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onosproject.provider.lldp.impl.LldpLinkProvider.DEFAULT_RULES;


public class LldpLinkProviderTest {

    private static final DeviceId DID1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DID2 = DeviceId.deviceId("of:0000000000000002");
    private static final DeviceId DID3 = DeviceId.deviceId("of:0000000000000003");
    private static final int EVENT_MS = 500;

    private static Port pd1;
    private static Port pd2;
    private static Port pd3;
    private static Port pd4;

    private final LldpLinkProvider provider = new LldpLinkProvider();
    private final LinkProviderRegistryAdapter linkRegistry = new LinkProviderRegistryAdapter();
    private final TestLinkService linkService = new TestLinkService();
    private final TestPacketService packetService = new TestPacketService();
    private final TestDeviceService deviceService = new TestDeviceService();
    private final TestMasterShipService masterService = new TestMasterShipService();
    private final TestNetworkConfigRegistry configRegistry = new TestNetworkConfigRegistry();

    private CoreService coreService;
    private LinkProviderServiceAdapter providerService;

    private PacketProcessor testProcessor;
    private DeviceListener deviceListener;
    private NetworkConfigListener configListener;

    private ApplicationId appId =
            new DefaultApplicationId(100, "org.onosproject.provider.lldp");

    private TestSuppressionConfig cfg;

    private Set<DeviceId> deviceBlacklist;

    private Set<ConnectPoint> portBlacklist;

    @Before
    public void setUp() {
        deviceBlacklist = new HashSet<>();
        portBlacklist = new HashSet<>();
        cfg = new TestSuppressionConfig();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication(appId.name()))
                .andReturn(appId).anyTimes();
        replay(coreService);

        provider.cfgService = new ComponentConfigAdapter();
        provider.coreService = coreService;
        provider.cfgRegistry = configRegistry;

        provider.deviceService = deviceService;
        provider.linkService = linkService;
        provider.packetService = packetService;
        provider.providerRegistry = linkRegistry;
        provider.masterService = masterService;
        provider.clusterMetadataService = new ClusterMetadataServiceAdapter();

        provider.activate(null);

        provider.eventExecutor = MoreExecutors.newDirectExecutorService();

        providerService = linkRegistry.registeredProvider();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void switchAdd() {
        DeviceEvent de = deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1);
        deviceListener.event(de);

        assertFalse("Device not added", provider.discoverers.isEmpty());
    }

    @Test
    public void switchRemove() {
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_REMOVED, DID1));

        final LinkDiscovery linkDiscovery = provider.discoverers.get(DID1);
        if (linkDiscovery != null) {
            // If LinkDiscovery helper is there after DEVICE_REMOVED,
            // it should be stopped
            assertTrue("Discoverer is not stopped", linkDiscovery.isStopped());
        }
        assertTrue("Device is not gone.", vanishedDpid(DID1));
    }

    /**
     * Checks that links on a reconfigured switch are properly removed.
     */
    @Test
    public void switchSuppressedByAnnotation() {

        // add device to stub DeviceService
        deviceService.putDevice(device(DID3));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID3));

        assertFalse("Device not added", provider.discoverers.isEmpty());

        // update device in stub DeviceService with suppression config
        deviceService.putDevice(device(DID3, DefaultAnnotations.builder()
                .set(LldpLinkProvider.NO_LLDP, "true")
                .build()));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_UPDATED, DID3));

        // discovery on device is expected to be gone or stopped
        LinkDiscovery linkDiscovery = provider.discoverers.get(DID3);
        if (linkDiscovery != null) {
            assertTrue("Discovery expected to be stopped", linkDiscovery.isStopped());
        }
    }

    @Test
    public void switchSuppressByBlacklist() {
        // add device in stub DeviceService
        deviceService.putDevice(device(DID3));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID3));

        // add deviveId to device blacklist
        deviceBlacklist.add(DID3);
        configListener.event(new NetworkConfigEvent(Type.CONFIG_ADDED,
                                                    DID3,
                                                    LinkDiscoveryFromDevice.class));

        assertAfter(EVENT_MS, () -> {
            // discovery helper for device is expected to be gone or stopped
            LinkDiscovery linkDiscovery = provider.discoverers.get(DID3);
            if (linkDiscovery != null) {
                assertTrue("Discovery expected to be stopped", linkDiscovery.isStopped());
            }
        });
    }

    @Test
    public void portUp() {
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID1, port(DID1, 3, true)));

        assertTrue("Port not added to discoverer",
                   provider.discoverers.get(DID1).containsPort(3L));
    }

    @Test
    public void portDown() {

        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID1, port(DID1, 1, false)));

        assertFalse("Port added to discoverer",
                    provider.discoverers.get(DID1).containsPort(1L));
        assertTrue("Port is not gone.", vanishedPort(1L));
    }

    @Test
    public void portRemoved() {
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID1, port(DID1, 3, true)));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_REMOVED, DID1, port(DID1, 3, true)));

        assertTrue("Port is not gone.", vanishedPort(3L));
        assertFalse("Port was not removed from discoverer",
                    provider.discoverers.get(DID1).containsPort(3L));
    }

    /**
     * Checks that discovery on reconfigured switch are properly restarted.
     */
    @Test
    public void portSuppressedByDeviceAnnotationConfig() {

        /// When Device is configured with suppression:ON, Port also is same

        // add device in stub DeviceService with suppression configured
        deviceService.putDevice(device(DID3, DefaultAnnotations.builder()
                .set(LldpLinkProvider.NO_LLDP, "true")
                .build()));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID3));

        // non-suppressed port added to suppressed device
        final long portno3 = 3L;
        deviceService.putPorts(DID3, port(DID3, portno3, true));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID3, port(DID3, portno3, true)));

        // discovery on device is expected to be stopped
        LinkDiscovery linkDiscovery = provider.discoverers.get(DID3);
        if (linkDiscovery != null) {
            assertTrue("Discovery expected to be stopped", linkDiscovery.isStopped());
        }

        /// When Device is reconfigured without suppression:OFF,
        /// Port should be included for discovery

        // update device in stub DeviceService without suppression configured
        deviceService.putDevice(device(DID3));
        // update the Port in stub DeviceService. (Port has reference to Device)
        deviceService.putPorts(DID3, port(DID3, portno3, true));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_UPDATED, DID3));

        // discovery should come back on
        assertFalse("Discoverer is expected to start", provider.discoverers.get(DID3).isStopped());
        assertTrue("Discoverer should contain the port there", provider.discoverers.get(DID3).containsPort(portno3));
    }

    /**
     * Checks that discovery on reconfigured switch are properly restarted.
     */
    @Test
    public void portSuppressedByParentDeviceIdBlacklist() {

        /// When Device is configured without suppression:OFF,
        /// Port should be included for discovery

        // add device in stub DeviceService without suppression configured
        deviceService.putDevice(device(DID3));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID3));

        // non-suppressed port added to suppressed device
        final long portno3 = 3L;
        deviceService.putPorts(DID3, port(DID3, portno3, true));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID3, port(DID3, portno3, true)));

        // discovery should succeed
        assertFalse("Discoverer is expected to start", provider.discoverers.get(DID3).isStopped());
        assertTrue("Discoverer should contain the port there", provider.discoverers.get(DID3).containsPort(portno3));

        // add suppression rule for "deviceId: "of:0000000000000003""
        deviceBlacklist.add(DID3);
        configListener.event(new NetworkConfigEvent(Type.CONFIG_ADDED,
                                                    DID3,
                                                    LinkDiscoveryFromDevice.class));


        /// When Device is reconfigured with suppression:ON, Port also is same

        // update device in stub DeviceService with suppression configured
        deviceService.putDevice(device(DID3));
        // update the Port in stub DeviceService. (Port has reference to Device)
        deviceService.putPorts(DID3, port(DID3, portno3, true));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_UPDATED, DID3));

        // discovery helper for device is expected to be gone or stopped
        LinkDiscovery linkDiscovery = provider.discoverers.get(DID3);
        if (linkDiscovery != null) {
            assertTrue("Discovery expected to be stopped", linkDiscovery.isStopped());
        }
    }

    /**
     * Checks that discovery on reconfigured switch are properly restarted.
     */
    @Test
    public void portSuppressedByDeviceTypeConfig() {

        /// When Device is configured without suppression:OFF,
        /// Port should be included for discovery

        // add device in stub DeviceService without suppression configured
        deviceService.putDevice(device(DID1, Device.Type.SWITCH));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));

        // non-suppressed port added to suppressed device
        final long portno3 = 3L;
        deviceService.putPorts(DID1, port(DID1, portno3, true));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID1, port(DID1, portno3, true)));

        // add device in stub DeviceService with suppression configured
        deviceService.putDevice(device(DID2, Device.Type.ROADM));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID2));

        // non-suppressed port added to suppressed device
        final long portno4 = 4L;
        deviceService.putPorts(DID2, port(DID2, portno4, true));
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID2, port(DID2, portno4, true)));

        // discovery should succeed for this device
        assertFalse("Discoverer is expected to start", provider.discoverers.get(DID1).isStopped());
        assertTrue("Discoverer should contain the port there", provider.discoverers.get(DID1).containsPort(portno3));

        // discovery on device is expected to be stopped for this device
        LinkDiscovery linkDiscovery = provider.discoverers.get(DID2);
        if (linkDiscovery != null) {
            assertTrue("Discovery expected to be stopped", linkDiscovery.isStopped());
        }
    }

    /**
     * Checks that discovery on reconfigured port are properly restarted.
     */
    @Test
    public void portSuppressedByPortConfig() {

        // add device in stub DeviceService without suppression configured
        deviceService.putDevice(device(DID3));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID3));

        // suppressed port added to non-suppressed device
        final long portno3 = 3L;
        final Port port3 = port(DID3, portno3, true,
                                DefaultAnnotations.builder()
                                        .set(LldpLinkProvider.NO_LLDP, "true")
                                        .build());
        deviceService.putPorts(DID3, port3);
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID3, port3));

        // discovery helper should be there turned on
        assertFalse("Discoverer is expected to start", provider.discoverers.get(DID3).isStopped());
        assertFalse("Discoverer should not contain the port there",
                    provider.discoverers.get(DID3).containsPort(portno3));
    }

    @Test
    public void portSuppressedByPortBlacklist() {

        // add device in stub DeviceService without suppression configured
        deviceService.putDevice(device(DID3));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID3));

        final long portno3 = 3L;
        final Port port3 = port(DID3, portno3, true);

        final ConnectPoint cpDid3no3 = new ConnectPoint(DID3, PortNumber.portNumber(portno3));
        portBlacklist.add(cpDid3no3);

        // suppressed port added to non-suppressed device
        deviceService.putPorts(DID3, port3);
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID3, port3));

        configListener.event(new NetworkConfigEvent(Type.CONFIG_ADDED,
                                                    cpDid3no3,
                                                    LinkDiscoveryFromPort.class));

        // discovery helper should be there turned on
        assertFalse("Discoverer is expected to start", provider.discoverers.get(DID3).isStopped());
        // but port is not a discovery target
        assertFalse("Discoverer should not contain the port there",
                    provider.discoverers.get(DID3).containsPort(portno3));
    }

    @Test
    public void portUnknown() {
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));
        // Note: DID3 hasn't been added to TestDeviceService, but only port is added
        deviceListener.event(portEvent(DeviceEvent.Type.PORT_ADDED, DID3, port(DID3, 1, false)));


        assertNull("DeviceId exists",
                   provider.discoverers.get(DID3));
    }

    @Test
    public void unknownPktCtx() {

        // Note: DID3 hasn't been added to TestDeviceService
        PacketContext pktCtx = new TestPacketContext(device(DID3));

        testProcessor.process(pktCtx);
        assertFalse("Context should still be free", pktCtx.isHandled());
    }

    @Test
    public void knownPktCtx() {
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID1));
        deviceListener.event(deviceEvent(DeviceEvent.Type.DEVICE_ADDED, DID2));
        PacketContext pktCtx = new TestPacketContext(deviceService.getDevice(DID2));


        testProcessor.process(pktCtx);

        assertTrue("Link not detected", detectedLink(DID1, DID2));

    }


    @After
    public void tearDown() {
        provider.deactivate();
        provider.coreService = null;
        provider.providerRegistry = null;
        provider.deviceService = null;
        provider.packetService = null;
    }

    private DeviceEvent deviceEvent(DeviceEvent.Type type, DeviceId did) {
        return new DeviceEvent(type, deviceService.getDevice(did));

    }

    private DefaultDevice device(DeviceId did) {
        return new DefaultDevice(ProviderId.NONE, did, Device.Type.SWITCH,
                                 "TESTMF", "TESTHW", "TESTSW", "TESTSN", new ChassisId());
    }

    private DefaultDevice device(DeviceId did, Device.Type type) {
        return new DefaultDevice(ProviderId.NONE, did, type,
                                 "TESTMF", "TESTHW", "TESTSW", "TESTSN", new ChassisId());
    }

    private DefaultDevice device(DeviceId did, Annotations annotations) {
        return new DefaultDevice(ProviderId.NONE, did, Device.Type.SWITCH,
                                 "TESTMF", "TESTHW", "TESTSW", "TESTSN", new ChassisId(), annotations);
    }

    @SuppressWarnings(value = {"unused"})
    private DeviceEvent portEvent(DeviceEvent.Type type, DeviceId did, PortNumber port) {
        return new DeviceEvent(type, deviceService.getDevice(did),
                               deviceService.getPort(did, port));
    }

    private DeviceEvent portEvent(DeviceEvent.Type type, DeviceId did, Port port) {
        return new DeviceEvent(type, deviceService.getDevice(did), port);
    }

    private Port port(DeviceId did, long port, boolean enabled) {
        return new DefaultPort(deviceService.getDevice(did),
                               PortNumber.portNumber(port), enabled);
    }

    private Port port(DeviceId did, long port, boolean enabled, Annotations annotations) {
        return new DefaultPort(deviceService.getDevice(did),
                               PortNumber.portNumber(port), enabled, annotations);
    }

    private boolean vanishedDpid(DeviceId... dids) {
        for (int i = 0; i < dids.length; i++) {
            if (!providerService.vanishedDpid().contains(dids[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean vanishedPort(Long... ports) {
        for (int i = 0; i < ports.length; i++) {
            if (!providerService.vanishedPort().contains(ports[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean detectedLink(DeviceId src, DeviceId dst) {
        for (DeviceId key : providerService.discoveredLinks().keySet()) {
            if (key.equals(src)) {
                return providerService.discoveredLinks().get(src).equals(dst);
            }
        }
        return false;
    }

    @Test
    public void addDeviceTypeRule() {
        Device.Type deviceType1 = Device.Type.ROADM;
        Device.Type deviceType2 = Device.Type.SWITCH;

        Set<Device.Type> deviceTypes = new HashSet<>();
        deviceTypes.add(deviceType1);

        cfg.deviceTypes(deviceTypes);

        configEvent(NetworkConfigEvent.Type.CONFIG_ADDED);

        assertTrue(provider.rules().getSuppressedDeviceType().contains(deviceType1));
        assertFalse(provider.rules().getSuppressedDeviceType().contains(deviceType2));
    }

    @Test
    public void updateDeviceTypeRule() {
        Device.Type deviceType1 = Device.Type.ROADM;
        Device.Type deviceType2 = Device.Type.SWITCH;
        Set<Device.Type> deviceTypes = new HashSet<>();

        deviceTypes.add(deviceType1);
        cfg.deviceTypes(deviceTypes);

        configEvent(NetworkConfigEvent.Type.CONFIG_ADDED);

        deviceTypes.add(deviceType2);
        cfg.deviceTypes(deviceTypes);

        configEvent(NetworkConfigEvent.Type.CONFIG_UPDATED);

        assertAfter(EVENT_MS, () -> {
            assertTrue(provider.rules().getSuppressedDeviceType().contains(deviceType1));
            assertTrue(provider.rules().getSuppressedDeviceType().contains(deviceType2));
        });
    }

    @Test
    public void addAnnotationRule() {
        final String key1 = "key1", key2 = "key2";
        final String value1 = "value1";

        Map<String, String> annotation = new HashMap<>();
        annotation.put(key1, value1);

        cfg.annotation(annotation);

        configEvent(NetworkConfigEvent.Type.CONFIG_ADDED);

        assertAfter(EVENT_MS, () -> {
            assertTrue(provider.rules().getSuppressedAnnotation().containsKey(key1));
            assertEquals(value1, provider.rules().getSuppressedAnnotation().get(key1));
            assertFalse(provider.rules().getSuppressedAnnotation().containsKey(key2));
        });
    }

    @Test
    public void updateAnnotationRule() {
        final String key1 = "key1", key2 = "key2";
        final String value1 = "value1", value2 = "value2";
        Map<String, String> annotation = new HashMap<>();

        annotation.put(key1, value1);
        cfg.annotation(annotation);

        configEvent(NetworkConfigEvent.Type.CONFIG_ADDED);

        assertAfter(EVENT_MS, () -> {
            assertTrue(provider.rules().getSuppressedAnnotation().containsKey(key1));
            assertEquals(value1, provider.rules().getSuppressedAnnotation().get(key1));
            assertFalse(provider.rules().getSuppressedAnnotation().containsKey(key2));
        });

        annotation.put(key2, value2);
        cfg.annotation(annotation);

        configEvent(NetworkConfigEvent.Type.CONFIG_UPDATED);

        assertAfter(EVENT_MS, () -> {
            assertTrue(provider.rules().getSuppressedAnnotation().containsKey(key1));
            assertEquals(value1, provider.rules().getSuppressedAnnotation().get(key1));
            assertTrue(provider.rules().getSuppressedAnnotation().containsKey(key2));
            assertEquals(value2, provider.rules().getSuppressedAnnotation().get(key2));
        });
    }

    private void configEvent(NetworkConfigEvent.Type evType) {
        configListener.event(new NetworkConfigEvent(evType,
                                                    appId,
                                                    SuppressionConfig.class));
    }

    private class TestPacketContext implements PacketContext {

        protected Device device;
        protected boolean blocked = false;

        public TestPacketContext(Device dev) {
            device = dev;
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public InboundPacket inPacket() {
            ONOSLLDP lldp = ONOSLLDP.onosLLDP(deviceService.getDevice(DID1).id().toString(),
                                              device.chassisId(),
                                              (int) pd1.number().toLong());

            Ethernet ethPacket = new Ethernet();
            ethPacket.setEtherType(Ethernet.TYPE_LLDP);
            ethPacket.setDestinationMACAddress(MacAddress.ONOS_LLDP);
            ethPacket.setPayload(lldp);
            ethPacket.setPad(true);

            ethPacket.setSourceMACAddress("DE:AD:BE:EF:BA:11");

            ConnectPoint cp = new ConnectPoint(device.id(), pd3.number());

            return new DefaultInboundPacket(cp, ethPacket,
                                            ByteBuffer.wrap(ethPacket.serialize()));

        }

        @Override
        public OutboundPacket outPacket() {
            return null;
        }

        @Override
        public TrafficTreatment.Builder treatmentBuilder() {
            return null;
        }

        @Override
        public void send() {

        }

        @Override
        public boolean block() {
            blocked = true;
            return blocked;
        }

        @Override
        public boolean isHandled() {
            return blocked;
        }

    }

    private class TestPacketService extends PacketServiceAdapter {
        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            testProcessor = processor;
        }
    }

    private class TestDeviceService extends DeviceServiceAdapter {

        private final Map<DeviceId, Device> devices = new HashMap<>();
        private final ArrayListMultimap<DeviceId, Port> ports =
                ArrayListMultimap.create();

        public TestDeviceService() {
            Device d1 = new DefaultDevice(ProviderId.NONE, DID1, Device.Type.SWITCH,
                                          "TESTMF", "TESTHW", "TESTSW", "TESTSN", new ChassisId());
            Device d2 = new DefaultDevice(ProviderId.NONE, DID2, Device.Type.SWITCH,
                                          "TESTMF", "TESTHW", "TESTSW", "TESTSN", new ChassisId());
            devices.put(DID1, d1);
            devices.put(DID2, d2);
            pd1 = new DefaultPort(d1, PortNumber.portNumber(1), true);
            pd2 = new DefaultPort(d1, PortNumber.portNumber(2), true);
            pd3 = new DefaultPort(d2, PortNumber.portNumber(1), true);
            pd4 = new DefaultPort(d2, PortNumber.portNumber(2), true);

            ports.putAll(DID1, Lists.newArrayList(pd1, pd2));
            ports.putAll(DID2, Lists.newArrayList(pd3, pd4));
        }

        private void putDevice(Device device) {
            DeviceId deviceId = device.id();
            devices.put(deviceId, device);
        }

        private void putPorts(DeviceId did, Port... ports) {
            this.ports.putAll(did, Lists.newArrayList(ports));
        }

        @Override
        public int getDeviceCount() {
            return devices.values().size();
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.copyOf(devices.values());
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return devices.get(deviceId);
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return ports.get(deviceId);
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            for (Port p : ports.get(deviceId)) {
                if (p.number().equals(portNumber)) {
                    return p;
                }
            }
            return null;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }

        @Override
        public void addListener(DeviceListener listener) {
            deviceListener = listener;

        }

        @Override
        public void removeListener(DeviceListener listener) {

        }
    }

    private final class TestMasterShipService implements MastershipService {

        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
            return null;
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return null;
        }

        @Override
        public Set<DeviceId> getDevicesOf(NodeId nodeId) {
            return null;
        }

        @Override
        public void addListener(MastershipListener listener) {

        }

        @Override
        public void removeListener(MastershipListener listener) {

        }

        @Override
        public RoleInfo getNodesFor(DeviceId deviceId) {
            return new RoleInfo(new NodeId("foo"), Collections.<NodeId>emptyList());
        }
    }


    private class TestLinkService extends LinkServiceAdapter {
    }

    private final class TestNetworkConfigRegistry
            extends NetworkConfigRegistryAdapter {
        @SuppressWarnings("unchecked")
        @Override
        public <S, C extends Config<S>> C getConfig(S subj, Class<C> configClass) {
            if (configClass == SuppressionConfig.class) {
                return (C) cfg;
            } else if (configClass == LinkDiscoveryFromDevice.class) {
                return (C) new LinkDiscoveryFromDevice() {
                    @Override
                    public boolean enabled() {
                        return !deviceBlacklist.contains(subj);
                    }
                };
            } else if (configClass == LinkDiscoveryFromPort.class) {
                return (C) new LinkDiscoveryFromPort() {
                    @Override
                    public boolean enabled() {
                        return !portBlacklist.contains(subj);
                    }
                };
            } else {
                return null;
            }
        }

        @Override
        public void addListener(NetworkConfigListener listener) {
            configListener = listener;
        }
    }

    private final class TestSuppressionConfig extends SuppressionConfig {
        private Set<Device.Type> deviceTypes = new HashSet<>(DEFAULT_RULES.getSuppressedDeviceType());
        private Map<String, String> annotation = new HashMap<>(DEFAULT_RULES.getSuppressedAnnotation());

        @Override
        public Set<Device.Type> deviceTypes() {
            return ImmutableSet.copyOf(deviceTypes);
        }

        @Override
        public SuppressionConfig deviceTypes(Set<Device.Type> deviceTypes) {
            this.deviceTypes = ImmutableSet.copyOf(deviceTypes);
            return this;
        }

        @Override
        public Map<String, String> annotation() {
            return ImmutableMap.copyOf(annotation);
        }

        @Override
        public SuppressionConfig annotation(Map<String, String> annotation) {
            this.annotation = ImmutableMap.copyOf(annotation);
            return this;
        }
    }
}
