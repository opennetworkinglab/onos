/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_NETWORK_ID;
import static org.onosproject.openstacknetworking.api.Constants.ANNOTATION_PORT_ID;

/**
 * Unit tests for Openstack Switching Host Provider.
 */
public class OpenstackSwitchingHostProviderTest {

    private static final String PORT_ID = "65c0ee9f-d634-4522-8954-51021b570b0d";
    private static final String PORT_NAME = "tap123456";

    private static final String NETWORK_ID = "396f12f8-521e-4b91-8e21-2e003500433a";
    private static final String IP_ADDRESS = "10.10.10.2";
    private static final String SUBNET_ID = "d32019d3-bc6e-4319-9c1d-6722fc136a22";
    private static final String MAC_ADDRESS = "00:11:22:33:44:55";
    private static final String MAC_ADDRESS2 = "11:22:33:44:55:66";

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DefaultAnnotations ANNOTATIONS =
                                    DefaultAnnotations.builder()
                                            .set(ANNOTATION_NETWORK_ID, NETWORK_ID)
                                            .set(ANNOTATION_PORT_ID, PORT_ID)
                                            .set("portName", PORT_NAME).build();

    // Host Mac, VLAN
    private static final ProviderId PROVIDER_ID = ProviderId.NONE;
    private static final MacAddress HOST_MAC = MacAddress.valueOf(MAC_ADDRESS);
    private static final MacAddress HOST_MAC2 = MacAddress.valueOf(MAC_ADDRESS2);

    private static final VlanId HOST_VLAN_UNTAGGED = VlanId.NONE;
    private static final HostId HOST_ID_UNTAGGED = HostId.hostId(HOST_MAC, HOST_VLAN_UNTAGGED);

    private static final String SEGMENT_ID = "1";

    // Host IP
    private static final IpAddress HOST_IP11 = IpAddress.valueOf("10.0.1.1");

    // Device
    private static final DeviceId DEV_ID1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DEV_ID2 = DeviceId.deviceId("of:0000000000000002");

    private static final Device DEV1 =
            new DefaultDevice(PID, DEV_ID1, Device.Type.SWITCH, "", "", "", "", null);
    private static final Device DEV2 =
            new DefaultDevice(PID, DEV_ID2, Device.Type.SWITCH, "", "", "", "", null);

    // Port
    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);

    // Connect Point
    private static final ConnectPoint CP11 = new ConnectPoint(DEV_ID1, P1);
    private static final HostLocation HOST_LOC11 = new HostLocation(CP11, 0);
    private static final ConnectPoint CP12 = new ConnectPoint(DEV_ID1, P2);
    private static final HostLocation HOST_LOC12 = new HostLocation(CP12, 0);
    private static final ConnectPoint CP21 = new ConnectPoint(DEV_ID2, P1);

    private Map<HostId, Host> hostMap = Maps.newHashMap();
    private Map<ConnectPoint, MacAddress> macMap = Maps.newHashMap();

    private OpenstackSwitchingHostProvider target;

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {
        target = new OpenstackSwitchingHostProvider();
        TestUtils.setField(target, "coreService", new TestCoreService());
        TestUtils.setField(target, "deviceService", new TestDeviceService());
        TestUtils.setField(target, "hostService", new TestHostService());
        TestUtils.setField(target, "mastershipService", new TestMastershipService());
        TestUtils.setField(target, "osNodeService", new TestOpenstackNodeService());
        TestUtils.setField(target, "osNetworkService", new TestOpenstackNetworkService());
        TestUtils.setField(target, "hostProviderRegistry", new TestHostProviderRegistry());
        TestUtils.setField(target, "executor", MoreExecutors.newDirectExecutorService());

        macMap.put(CP11, HOST_MAC);
        macMap.put(CP12, HOST_MAC);
        macMap.put(CP21, HOST_MAC2);

        target.activate();
    }

    /**
     * Tears down this unit test.
     */
    @After
    public void tearDown() {
        target.deactivate();
        target = null;
    }

    /**
     * Tests the process port added method for new addition case.
     */
    @Test
    public void testProcessPortAddedForNewAddition() {
        org.onosproject.net.Port port = new DefaultPort(DEV2, P1, true, ANNOTATIONS);
        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.PORT_ADDED, DEV2, port);

        target.portAddedHelper(event);

        HostId hostId = HostId.hostId(HOST_MAC2);
        HostDescription hostDesc = new DefaultHostDescription(
                HOST_MAC2,
                VlanId.NONE,
                new HostLocation(CP21, System.currentTimeMillis()),
                ImmutableSet.of(HOST_IP11),
                ANNOTATIONS

        );

        verifyHostResult(hostId, hostDesc);
    }

    /**
     * Tests the process port added method for updating case.
     */
    @Test
    public void testProcessPortAddedForUpdate() {
        org.onosproject.net.Port addedPort = new DefaultPort(DEV1, P1, true, ANNOTATIONS);
        DeviceEvent addedEvent = new DeviceEvent(DeviceEvent.Type.PORT_ADDED, DEV1, addedPort);

        target.portAddedHelper(addedEvent);

        //org.onosproject.net.Port updatedPort = new DefaultPort(DEV1, P2, true, ANNOTATIONS);
        //DeviceEvent updatedEvent = new DeviceEvent(DeviceEvent.Type.PORT_ADDED, DEV1, updatedPort);

        target.portAddedHelper(addedEvent);


        HostId hostId = HostId.hostId(HOST_MAC);
        HostDescription hostDesc = new DefaultHostDescription(
                HOST_MAC,
                VlanId.NONE,
                new HostLocation(CP11, System.currentTimeMillis()),
                ImmutableSet.of(HOST_IP11),
                ANNOTATIONS
        );

        verifyHostResult(hostId, hostDesc);
    }

    @Test
    public void testProcessPortRemoved() {
        org.onosproject.net.Port addedPort = new DefaultPort(DEV1, P1, true, ANNOTATIONS);
        DeviceEvent addedEvent = new DeviceEvent(DeviceEvent.Type.PORT_ADDED, DEV1, addedPort);

        target.portAddedHelper(addedEvent);

        org.onosproject.net.Port removedPort = new DefaultPort(DEV2, P1, true, ANNOTATIONS);
        DeviceEvent removedEvent = new DeviceEvent(DeviceEvent.Type.PORT_REMOVED, DEV2, removedPort);

        target.portRemovedHelper(removedEvent);

        assertNull(target.hostService.getHost(HostId.hostId(HOST_MAC)));
    }

    /**
     * Tests the process port added method for migration case.
     */
    @Test
    public void testProcessPortAddedForMigration() {
        org.onosproject.net.Port port = new DefaultPort(DEV1, P2, true, ANNOTATIONS);
        target.processPortAdded(port);

        HostId hostId = HostId.hostId(HOST_MAC);

        verifyHostLocationResult(hostId, HOST_LOC12);
    }

    /**
     * Mocks the CoreService.
     */
    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(100, "hostProviderTestApp");
        }
    }

    /**
     * Mocks the DeviceService.
     */
    private class TestDeviceService extends DeviceServiceAdapter {
    }

    /**
     * Mocks the HostService.
     */
    private class TestHostService extends HostServiceAdapter {

        @Override
        public Host getHost(HostId hostId) {
            return hostMap.get(hostId);
        }

        @Override
        public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
            return ImmutableSet.copyOf(hostMap.values());
        }
    }

    /**
     * Mocks the HostProviderService.
     */
    private class TestHostProviderService implements HostProviderService {

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription,
                                 boolean replaceIps) {
            Host host = new DefaultHost(PROVIDER_ID,
                    hostId,
                    hostDescription.hwAddress(),
                    hostDescription.vlan(),
                    hostDescription.locations(),
                    ImmutableSet.copyOf(hostDescription.ipAddress()),
                    hostDescription.innerVlan(),
                    hostDescription.tpid(),
                    hostDescription.configured(),
                    hostDescription.annotations());

            hostMap.put(hostId, host);
        }

        @Override
        public void hostVanished(HostId hostId) {
            hostMap.remove(hostId);
        }

        @Override
        public void removeIpFromHost(HostId hostId, IpAddress ipAddress) {

        }

        @Override
        public void removeLocationFromHost(HostId hostId, HostLocation location) {

        }

        @Override
        public void addLocationToHost(HostId hostId, HostLocation location) {
            Host oldHost = hostMap.get(hostId);

            Set<HostLocation> newHostlocations = oldHost.locations();
            newHostlocations.add(location);

            Host newHost = new DefaultHost(oldHost.providerId(),
                    oldHost.id(),
                    oldHost.mac(),
                    oldHost.vlan(),
                    newHostlocations,
                    oldHost.ipAddresses(),
                    oldHost.innerVlan(),
                    oldHost.tpid(),
                    oldHost.configured(),
                    oldHost.annotations());

            hostMap.put(hostId, newHost);
        }

        @Override
        public HostProvider provider() {
            return null;
        }
    }

    /**
     * Mocks the HostProviderRegistry.
     */
    private class TestHostProviderRegistry implements HostProviderRegistry {

        @Override
        public HostProviderService register(HostProvider provider) {
            return new TestHostProviderService();
        }

        @Override
        public void unregister(HostProvider provider) {

        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }
    }

    /**
     * Mocks the MastershipService.
     */
    private class TestMastershipService extends MastershipServiceAdapter {
    }

    /**
     * Mocks the OpenstackNodeService.
     */
    private class TestOpenstackNodeService extends OpenstackNodeServiceAdapter {
    }

    /**
     * Mocks the OpenstackNetworkService.
     */
    private class TestOpenstackNetworkService extends OpenstackNetworkServiceAdapter {

        @Override
        public Port port(org.onosproject.net.Port port) {
            Port osPort = NeutronPort.builder()
                                        .networkId(NETWORK_ID)
                                        .fixedIp(IP_ADDRESS, SUBNET_ID)
                                        .macAddress(macMap.get(
                                                new ConnectPoint(port.element().id(),
                                                        port.number())).toString())
                                        .build();
            osPort.setId(PORT_ID);

            return osPort;
        }

        @Override
        public Network network(String networkId) {
            Network osNetwork = NeutronNetwork.builder()
                                        .networkType(NetworkType.VXLAN)
                                        .segmentId(SEGMENT_ID)
                                        .build();
            osNetwork.setId(NETWORK_ID);
            return osNetwork;
        }
    }

    /**
     * Verifies the HostId and HostDescription.
     *
     * @param hostId        host identifier
     * @param hostDesc      host description
     */
    private void verifyHostResult(HostId hostId, HostDescription hostDesc) {
        Host host = hostMap.get(hostId);

        assertEquals(hostId, host.id());
        assertEquals(hostDesc.hwAddress(), host.mac());
        assertEquals(hostDesc.annotations().value(NETWORK_ID),
                host.annotations().value(NETWORK_ID));
    }

    /**
     * Verifies the HostId and HostLocation.
     *
     * @param hostId        host identifier
     * @param hostLocation  host location
     */
    private void verifyHostLocationResult(HostId hostId, HostLocation hostLocation) {
        Host host = hostMap.get(hostId);
        assertTrue(host.locations().stream().anyMatch(location -> location.equals(hostLocation)));
    }
}
