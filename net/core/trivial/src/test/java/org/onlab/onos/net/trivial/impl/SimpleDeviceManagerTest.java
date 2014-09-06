package org.onlab.onos.net.trivial.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.event.Event;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.device.DeviceAdminService;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.onlab.onos.net.Device.Type.SWITCH;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.device.DeviceEvent.Type.*;

/**
 * Test codifying the device service & device provider service contracts.
 */
public class SimpleDeviceManagerTest {

    private static final ProviderId PID = new ProviderId("foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SW2 = "3.9.5";
    private static final String SN = "43311-12345";

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);


    private SimpleDeviceManager mgr;

    protected DeviceService service;
    protected DeviceAdminService admin;
    protected DeviceProviderRegistry registry;
    protected DeviceProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();

    @Before
    public void setUp() {
        mgr = new SimpleDeviceManager();
        service = mgr;
        admin = mgr;
        registry = mgr;
        mgr.eventDispatcher = new TestEventDispatcher();
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
                                             HW, swVersion, SN);
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
    }

    @Test
    public void deviceDisconnected() {
        connectDevice(DID1, SW1);
        connectDevice(DID2, SW1);
        validateEvents(DEVICE_ADDED, DEVICE_ADDED);

        // Disconnect
        providerService.deviceDisconnected(DID1);
        assertNotNull("device should not be found", service.getDevice(DID1));
        validateEvents(DEVICE_AVAILABILITY_CHANGED);

        // Reconnect
        connectDevice(DID1, SW1);
        validateEvents(DEVICE_AVAILABILITY_CHANGED);
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
        assertEquals("incorrect role", MastershipRole.NONE, service.getRole(DID1));
    }

    @Test
    public void setRole() {
        connectDevice(DID1, SW1);
        admin.setRole(DID1, MastershipRole.MASTER);
        validateEvents(DEVICE_ADDED, DEVICE_MASTERSHIP_CHANGED);
        assertEquals("incorrect role", MastershipRole.MASTER, service.getRole(DID1));
        assertEquals("incorrect device", DID1, provider.deviceReceived.id());
        assertEquals("incorrect role", MastershipRole.MASTER, provider.roleReceived);
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
        admin.removeDevice(DID1);
        assertNull("device should not be found", service.getDevice(DID1));
        assertNotNull("device should be found", service.getDevice(DID2));
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
        private Device deviceReceived;
        private MastershipRole roleReceived;

        public TestProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(Device device) {
        }

        @Override
        public void roleChanged(Device device, MastershipRole newRole) {
            deviceReceived = device;
            roleReceived = newRole;
        }
    }

    private static class TestListener implements DeviceListener {
        final List<DeviceEvent> events = new ArrayList<>();

        @Override
        public void event(DeviceEvent event) {
            events.add(event);
        }
    }

}
