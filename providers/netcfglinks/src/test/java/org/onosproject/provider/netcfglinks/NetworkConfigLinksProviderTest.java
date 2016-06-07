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
package org.onosproject.provider.netcfglinks;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONOSLLDP;
import org.onosproject.cluster.ClusterMetadataServiceAdapter;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderRegistryAdapter;
import org.onosproject.net.link.LinkProviderServiceAdapter;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketServiceAdapter;

import java.nio.ByteBuffer;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for the network config links provider.
 */
public class NetworkConfigLinksProviderTest {

    private NetworkConfigLinksProvider provider;

    private PacketProcessor testProcessor;
    private LinkProviderServiceAdapter providerService;
    private NetworkConfigListener configListener;
    private final TestNetworkConfigRegistry configRegistry =
            new TestNetworkConfigRegistry();

    static Device dev1 = NetTestTools.device("sw1");
    static Device dev2 = NetTestTools.device("sw2");
    static Device dev3 = NetTestTools.device("sw3");
    static PortNumber portNumber1 = PortNumber.portNumber(1);
    static PortNumber portNumber2 = PortNumber.portNumber(2);
    static PortNumber portNumber3 = PortNumber.portNumber(3);
    static ConnectPoint src = new ConnectPoint(dev1.id(), portNumber2);
    static ConnectPoint dst = new ConnectPoint(dev2.id(), portNumber2);

    static DeviceListener deviceListener;

    /**
     * Test device manager. Returns a known set of devices and ports.
     */
    static class TestDeviceManager extends DeviceServiceAdapter {

        @Override
        public Iterable<Device> getAvailableDevices() {
            return ImmutableList.of(dev1, dev2);
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return ImmutableList.of(new DefaultPort(dev1, portNumber1, true),
                                    new DefaultPort(dev2, portNumber2, true));
        }

        @Override
        public void addListener(DeviceListener listener) {
            deviceListener = listener;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            if (deviceId.equals(dev1.id())) {
                return dev1;
            } else {
                return dev2;
            }
        }
    }

    /**
     * Test mastership service. All devices owned by the local node for testing.
     */
    static class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

    /**
     * Test packet context for generation of LLDP packets.
     */
    private class TestPacketContext implements PacketContext {

        protected ConnectPoint src;
        protected ConnectPoint dst;
        protected boolean blocked = false;

        public TestPacketContext(ConnectPoint src, ConnectPoint dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public long time() {
            return 0;
        }

        @Override
        public InboundPacket inPacket() {
            ONOSLLDP lldp = ONOSLLDP.onosLLDP(src.deviceId().toString(),
                                              new ChassisId(),
                                              (int) src.port().toLong());

            Ethernet ethPacket = new Ethernet();
            ethPacket.setEtherType(Ethernet.TYPE_LLDP);
            ethPacket.setDestinationMACAddress(ONOSLLDP.LLDP_ONLAB);
            ethPacket.setPayload(lldp);
            ethPacket.setPad(true);

            ethPacket.setSourceMACAddress("DE:AD:BE:EF:BA:11");

            return new DefaultInboundPacket(dst, ethPacket,
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
            return true;
        }

        @Override
        public boolean isHandled() {
            return blocked;
        }

    }

    /**
     * Test packet service for capturing the packet processor from the service
     * under test.
     */
    private class TestPacketService extends PacketServiceAdapter {
        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            testProcessor = processor;
        }
    }

    /**
     * Test network config registry. Captures the network config listener from
     * the service under test.
     */
    private final class TestNetworkConfigRegistry
            extends NetworkConfigRegistryAdapter {


        @Override
        public void addListener(NetworkConfigListener listener) {
            configListener = listener;
        }
    }

    /**
     * Sets up a network config links provider under test and the services
     * required to run it.
     */
    @Before
    public void setUp() {
        provider = new NetworkConfigLinksProvider();

        provider.coreService = new CoreServiceAdapter();
        provider.packetService = new PacketServiceAdapter();
        LinkProviderRegistryAdapter linkRegistry =
                new LinkProviderRegistryAdapter();
        provider.providerRegistry = linkRegistry;
        provider.deviceService = new TestDeviceManager();
        provider.masterService = new TestMastershipService();
        provider.packetService = new TestPacketService();
        provider.metadataService = new ClusterMetadataServiceAdapter();
        provider.netCfgService = configRegistry;

        provider.activate();

        providerService = linkRegistry.registeredProvider();
    }

    /**
     * Tears down the provider under test.
     */
    @After
    public void tearDown() {
        provider.deactivate();
    }

    /**
     * Tests that a network config links provider object can be created.
     * The actual creation is done in the setUp() method.
     */
    @Test
    public void testCreation() {
        assertThat(provider, notNullValue());
        assertThat(provider.configuredLinks, empty());
    }

    /**
     * Tests loading of devices from the device manager.
     */
    @Test
    public void testDeviceLoad() {
        assertThat(provider, notNullValue());
        assertThat(provider.discoverers.entrySet(), hasSize(2));
    }

    /**
     * Tests discovery of a link that is not expected in the configuration.
     */
    @Test
    public void testNotConfiguredLink() {
        PacketContext pktCtx = new TestPacketContext(src, dst);

        testProcessor.process(pktCtx);

        assertThat(providerService.discoveredLinks().entrySet(), hasSize(1));
        DeviceId destination = providerService.discoveredLinks().get(dev1.id());
        assertThat(destination, notNullValue());
        LinkKey key = LinkKey.linkKey(src, dst);
        LinkDescription linkDescription = providerService
                .discoveredLinkDescriptions().get(key);
        assertThat(linkDescription, notNullValue());
        assertThat(linkDescription.isExpected(), is(false));
    }

    /**
     * Tests discovery of an expected link.
     */
    @Test
    public void testConfiguredLink() {
        LinkKey key = LinkKey.linkKey(src, dst);
        configListener.event(new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                    key,
                                                    BasicLinkConfig.class));

        PacketContext pktCtx = new TestPacketContext(src, dst);

        testProcessor.process(pktCtx);

        assertThat(providerService.discoveredLinks().entrySet(), hasSize(1));
        DeviceId destination = providerService.discoveredLinks().get(dev1.id());
        assertThat(destination, notNullValue());
        LinkDescription linkDescription = providerService
                .discoveredLinkDescriptions().get(key);
        assertThat(linkDescription, notNullValue());
        assertThat(linkDescription.isExpected(), is(true));
    }

    /**
     * Tests removal of a link from the configuration.
     */
    @Test
    public void testRemoveLink() {
        LinkKey key = LinkKey.linkKey(src, dst);
        configListener.event(new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED,
                                                    key,
                                                    BasicLinkConfig.class));

        assertThat(provider.configuredLinks, hasSize(1));

        configListener.event(new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_REMOVED,
                                                    key,
                                                    BasicLinkConfig.class));
        assertThat(provider.configuredLinks, hasSize(0));
    }

    /**
     * Tests adding a new device via an event.
     */
    @Test
    public void testAddDevice() {
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, dev3));
        assertThat(provider.discoverers.entrySet(), hasSize(3));
    }

    /**
     * Tests adding a new port via an event.
     */
    @Test
    public void testAddPort() {
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.PORT_ADDED, dev3,
                                             new DefaultPort(dev3, portNumber3, true)));
        assertThat(provider.discoverers.entrySet(), hasSize(3));
    }

    /**
     * Tests removing a device via an event.
     */
    @Test
    public void testRemoveDevice() {
        assertThat(provider.discoverers.entrySet(), hasSize(2));
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, dev3));
        assertThat(provider.discoverers.entrySet(), hasSize(3));
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_REMOVED, dev3));
        assertThat(provider.discoverers.entrySet(), hasSize(2));
    }

    /**
     * Tests removing a port via an event.
     */
    @Test
    public void testRemovePort() {
        assertThat(provider.discoverers.entrySet(), hasSize(2));
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.PORT_ADDED, dev3,
                                             new DefaultPort(dev3, portNumber3, true)));
        assertThat(provider.discoverers.entrySet(), hasSize(3));
        deviceListener.event(new DeviceEvent(DeviceEvent.Type.PORT_REMOVED, dev3,
                                             new DefaultPort(dev3, portNumber3, true)));
        assertThat(provider.discoverers.entrySet(), hasSize(3));
    }

    /**
     * Tests changing device availability via an event.
     */
    @Test
    public void testDeviceAvailabilityChanged() {
        assertThat(providerService.vanishedDpid(), hasSize(0));

        deviceListener.event(
                new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, dev3));
        assertThat(providerService.vanishedDpid(), hasSize(0));

        deviceListener.event(
                new DeviceEvent(DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED, dev3));
        assertThat(providerService.vanishedDpid(), hasSize(1));
    }
}
