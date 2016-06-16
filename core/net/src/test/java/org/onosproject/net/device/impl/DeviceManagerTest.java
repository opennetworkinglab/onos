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
package org.onosproject.net.device.impl;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterServiceAdapter;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.event.Event;
import org.onosproject.net.config.NetworkConfigServiceAdapter;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.trivial.SimpleDeviceStore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.device.DeviceEvent.Type.*;

/**
 * Test codifying the device service & device provider service contracts.
 */
public class DeviceManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SW2 = "3.9.5";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);
    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final IpAddress LOCALHOST = IpAddress.valueOf("127.0.0.1");

    private DeviceManager mgr;

    protected DeviceService service;
    protected DeviceAdminService admin;
    protected DeviceProviderRegistry registry;
    protected DeviceProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new DeviceManager();
        service = mgr;
        admin = mgr;
        registry = mgr;
        mgr.store = new SimpleDeviceStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        TestMastershipManager mastershipManager = new TestMastershipManager();
        mgr.mastershipService = mastershipManager;
        mgr.termService = mastershipManager;
        mgr.clusterService = new TestClusterService();
        mgr.networkConfigService = new TestNetworkConfigService();
        mgr.activate();


        service.addListener(listener);

        provider = new TestProvider();
        providerService = registry.register(provider);
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivate();
    }

    private void connectDevice(DeviceId deviceId, String swVersion) {
        DeviceDescription description =
                new DefaultDeviceDescription(deviceId.uri(), SWITCH, MFR,
                                             HW, swVersion, SN, CID);
        providerService.deviceConnected(deviceId, description);
        assertNotNull("device should be found", service.getDevice(DID1));
    }

    @Test
    public void deviceConnected() {
        assertNull("device should not be found", service.getDevice(DID1));
        connectDevice(DID1, SW1);
        validateEvents(DEVICE_ADDED);

        Iterator<Device> it = service.getDevices().iterator();
        assertNotNull("one device expected", it.next());
        assertFalse("only one device expected", it.hasNext());
        assertEquals("incorrect device count", 1, service.getDeviceCount());
        assertTrue("device should be available", service.isAvailable(DID1));
    }

    @Test
    public void deviceDisconnected() {
        connectDevice(DID1, SW1);
        connectDevice(DID2, SW1);
        validateEvents(DEVICE_ADDED, DEVICE_ADDED);
        assertTrue("device should be available", service.isAvailable(DID1));

        // Disconnect
        providerService.deviceDisconnected(DID1);
        assertNotNull("device should not be found", service.getDevice(DID1));
        assertFalse("device should not be available", service.isAvailable(DID1));
        validateEvents(DEVICE_AVAILABILITY_CHANGED);

        // Reconnect
        connectDevice(DID1, SW1);
        validateEvents(DEVICE_AVAILABILITY_CHANGED);

        assertEquals("incorrect device count", 2, service.getDeviceCount());
    }

    @Test
    public void deviceUpdated() {
        connectDevice(DID1, SW1);
        validateEvents(DEVICE_ADDED);

        connectDevice(DID1, SW2);
        validateEvents(DEVICE_UPDATED);
    }

    @Test
    public void getRole() {
        connectDevice(DID1, SW1);
        assertEquals("incorrect role", MastershipRole.MASTER, service.getRole(DID1));
    }

    @Test
    public void updatePorts() {
        connectDevice(DID1, SW1);
        List<PortDescription> pds = new ArrayList<>();
        pds.add(new DefaultPortDescription(P1, true));
        pds.add(new DefaultPortDescription(P2, true));
        pds.add(new DefaultPortDescription(P3, true));
        providerService.updatePorts(DID1, pds);
        validateEvents(DEVICE_ADDED, PORT_ADDED, PORT_ADDED, PORT_ADDED);
        pds.clear();

        pds.add(new DefaultPortDescription(P1, false));
        pds.add(new DefaultPortDescription(P3, true));
        providerService.updatePorts(DID1, pds);
        validateEvents(PORT_UPDATED, PORT_REMOVED);
    }

    @Test
    public void updatePortStatus() {
        connectDevice(DID1, SW1);
        List<PortDescription> pds = new ArrayList<>();
        pds.add(new DefaultPortDescription(P1, true));
        pds.add(new DefaultPortDescription(P2, true));
        providerService.updatePorts(DID1, pds);
        validateEvents(DEVICE_ADDED, PORT_ADDED, PORT_ADDED);

        providerService.portStatusChanged(DID1, new DefaultPortDescription(P1, false));
        validateEvents(PORT_UPDATED);
        providerService.portStatusChanged(DID1, new DefaultPortDescription(P1, false));
        assertTrue("no events expected", listener.events.isEmpty());
    }

    @Test
    public void getPorts() {
        connectDevice(DID1, SW1);
        List<PortDescription> pds = new ArrayList<>();
        pds.add(new DefaultPortDescription(P1, true));
        pds.add(new DefaultPortDescription(P2, true));
        providerService.updatePorts(DID1, pds);
        validateEvents(DEVICE_ADDED, PORT_ADDED, PORT_ADDED);
        assertEquals("wrong port count", 2, service.getPorts(DID1).size());

        Port port = service.getPort(DID1, P1);
        assertEquals("incorrect port", P1, service.getPort(DID1, P1).number());
        assertEquals("incorrect state", true, service.getPort(DID1, P1).isEnabled());
    }

    @Test
    public void removeDevice() {
        connectDevice(DID1, SW1);
        connectDevice(DID2, SW2);
        assertEquals("incorrect device count", 2, service.getDeviceCount());
        admin.removeDevice(DID1);
        assertNull("device should not be found", service.getDevice(DID1));
        assertNotNull("device should be found", service.getDevice(DID2));
        assertEquals("incorrect device count", 1, service.getDeviceCount());

    }

    protected void validateEvents(Enum... types) {
        int i = 0;
        assertEquals("wrong events received", types.length, listener.events.size());
        for (Event event : listener.events) {
            assertEquals("incorrect event type", types[i], event.type());
            i++;
        }
        listener.events.clear();
    }


    private class TestProvider extends AbstractProvider implements DeviceProvider {
        private DeviceId deviceReceived;
        private MastershipRole roleReceived;

        public TestProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }

        @Override
        public void roleChanged(DeviceId device, MastershipRole newRole) {
            deviceReceived = device;
            roleReceived = newRole;
        }

        @Override
        public boolean isReachable(DeviceId device) {
            return false;
        }

        @Override
        public void enablePort(DeviceId deviceId, PortNumber portNumber) {
            // TODO
        }

        @Override
        public void disablePort(DeviceId deviceId, PortNumber portNumber) {
            // TODO
        }
    }

    private static class TestListener implements DeviceListener {
        final List<DeviceEvent> events = new ArrayList<>();

        @Override
        public void event(DeviceEvent event) {
            events.add(event);
        }
    }

    private static class TestMastershipManager
            extends MastershipServiceAdapter implements MastershipTermService {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }

        @Override
        public Set<DeviceId> getDevicesOf(NodeId nodeId) {
            return Sets.newHashSet(DID1, DID2);
        }

        @Override
        public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
            return CompletableFuture.completedFuture(MastershipRole.MASTER);
        }

        @Override
        public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public MastershipTerm getMastershipTerm(DeviceId deviceId) {
            // FIXME: just returning something not null
            return MastershipTerm.of(NID_LOCAL, 1);
        }
    }

    // code clone
    private final class TestClusterService extends ClusterServiceAdapter {

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

    }

    private class TestNetworkConfigService extends NetworkConfigServiceAdapter {
    }
}
