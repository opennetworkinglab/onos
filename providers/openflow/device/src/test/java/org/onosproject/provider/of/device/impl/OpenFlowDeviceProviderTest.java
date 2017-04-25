/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.provider.of.device.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.MastershipRole.*;

public class OpenFlowDeviceProviderTest {

    private static final ProviderId PID = new ProviderId("of", "test");
    private static final DeviceId DID1 = DeviceId.deviceId("of:0000000000000001");
    private static final Dpid DPID1 = Dpid.dpid(DID1.uri());

    private static final OFPortDesc PD1 = portDesc(1);
    private static final OFPortDesc PD2 = portDesc(2);
    private static final OFPortDesc PD3 = portDesc(3);

    private static final List<OFPortDesc> PLIST = Lists.newArrayList(PD1, PD2);

    private static final Device DEV1 =
            new DefaultDevice(PID, DID1, SWITCH, "", "", "", "", null);

    private static final TestOpenFlowSwitch SW1 = new TestOpenFlowSwitch();

    private final OpenFlowDeviceProvider provider = new OpenFlowDeviceProvider();
    private final TestDeviceRegistry registry = new TestDeviceRegistry();
    private final TestController controller = new TestController();

    @Before
    public void startUp() {
        provider.providerRegistry = registry;
        provider.controller = controller;
        provider.cfgService = new ComponentConfigAdapter();
        provider.driverService = new DriverServiceAdapter();
        controller.switchMap.put(DPID1, SW1);
        provider.activate(null);
        assertNotNull("provider should be registered", registry.provider);
        assertNotNull("listener should be registered", controller.listener);
        assertEquals("devices not added", 1, registry.connected.size());
        assertEquals("ports not added", 2, registry.ports.get(DID1).size());
    }

    @After
    public void tearDown() {
        provider.deactivate(null);
        assertNull("listener should be removed", controller.listener);
        provider.controller = null;
        provider.providerRegistry = null;
    }

    @Test
    public void roleChanged() {
        provider.roleChanged(DID1, MASTER);
        assertEquals("Should be MASTER", RoleState.MASTER, controller.roleMap.get(DPID1));
        provider.roleChanged(DID1, STANDBY);
        assertEquals("Should be EQUAL", RoleState.EQUAL, controller.roleMap.get(DPID1));
        provider.roleChanged(DID1, NONE);
        assertEquals("Should be SLAVE", RoleState.SLAVE, controller.roleMap.get(DPID1));
    }

    //sending a features req, msg will be added to sent
    @Test
    public void triggerProbe() {
        int cur = SW1.sent.size();
        provider.triggerProbe(DID1);
        assertEquals("OF message not sent", cur + 1, SW1.sent.size());
    }

    //test receiving features reply
    @Test
    public void switchChanged() {
        controller.listener.switchChanged(DPID1);
        Collection<PortDescription> updatedDescr = registry.ports.values();
        for (PortDescription pd : updatedDescr) {
            assertNotNull("Switch change not handled by the provider service", pd);
        }
    }

    @Test
    public void switchRemoved() {
        controller.listener.switchRemoved(DPID1);
        assertTrue("device not removed", registry.connected.isEmpty());
    }

    @Test
    public void portChanged() {
        OFPortStatus stat = SW1.factory().buildPortStatus()
                .setReason(OFPortReason.ADD)
                .setDesc(PD3)
                .build();
        controller.listener.portChanged(DPID1, stat);
        assertNotNull("never went throught the provider service", registry.descr);
        assertEquals("port status unhandled", 3, registry.ports.get(DID1).size());
    }

    @Test
    public void receivedRoleReply() {
        // check translation capabilities
        controller.listener.receivedRoleReply(DPID1, RoleState.MASTER, RoleState.MASTER);
        assertEquals("wrong role reported", DPID1, registry.roles.get(MASTER));
        controller.listener.receivedRoleReply(DPID1, RoleState.EQUAL, RoleState.MASTER);
        assertEquals("wrong role reported", DPID1, registry.roles.get(STANDBY));
        controller.listener.receivedRoleReply(DPID1, RoleState.SLAVE, RoleState.MASTER);
        assertEquals("wrong role reported", DPID1, registry.roles.get(NONE));
    }

    private static OFPortDesc portDesc(int port) {
        OFPortDesc.Builder builder = OFFactoryVer10.INSTANCE.buildPortDesc();
        builder.setPortNo(OFPort.of(port));

        return builder.build();
    }

    private class TestDeviceRegistry implements DeviceProviderRegistry {
        DeviceProvider provider;

        Set<DeviceId> connected = new HashSet<>();
        Multimap<DeviceId, PortDescription> ports = HashMultimap.create();
        PortDescription descr = null;
        Map<MastershipRole, Dpid> roles = new HashMap<>();

        @Override
        public DeviceProviderService register(DeviceProvider provider) {
            this.provider = provider;
            return new TestProviderService();
        }

        @Override
        public void unregister(DeviceProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

        private class TestProviderService implements DeviceProviderService {

            @Override
            public DeviceProvider provider() {
                return null;
            }

            @Override
            public void deviceConnected(DeviceId deviceId,
                    DeviceDescription deviceDescription) {
                connected.add(deviceId);
            }

            @Override
            public void deviceDisconnected(DeviceId deviceId) {
                connected.remove(deviceId);
                ports.removeAll(deviceId);
            }

            @Override
            public void updatePorts(DeviceId deviceId,
                    List<PortDescription> portDescriptions) {
                for (PortDescription p : portDescriptions) {
                    ports.put(deviceId, p);
                }
            }

            @Override
            public void portStatusChanged(DeviceId deviceId,
                    PortDescription portDescription) {
                ports.put(deviceId, portDescription);
                descr = portDescription;
            }

            @Override
            public void receivedRoleReply(DeviceId deviceId,
                    MastershipRole requested, MastershipRole response) {
                roles.put(requested, Dpid.dpid(deviceId.uri()));
            }

            @Override
            public void updatePortStatistics(DeviceId deviceId, Collection<PortStatistics> portStatistics) {

            }
        }
    }

    private class TestController implements OpenFlowController {
        OpenFlowSwitchListener listener = null;
        Map<Dpid, RoleState> roleMap = new HashMap<Dpid, RoleState>();
        Map<Dpid, OpenFlowSwitch> switchMap = new HashMap<Dpid, OpenFlowSwitch>();

        @Override
        public Iterable<OpenFlowSwitch> getSwitches() {
            return switchMap.values();
        }

        @Override
        public Iterable<OpenFlowSwitch> getMasterSwitches() {
            return ImmutableSet.of();
        }

        @Override
        public Iterable<OpenFlowSwitch> getEqualSwitches() {
            return null;
        }

        @Override
        public OpenFlowSwitch getSwitch(Dpid dpid) {
            return switchMap.get(dpid);
        }

        @Override
        public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
            return null;
        }

        @Override
        public OpenFlowSwitch getEqualSwitch(Dpid dpid) {

            return null;
        }

        @Override
        public void addListener(OpenFlowSwitchListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(OpenFlowSwitchListener listener) {
            this.listener = null;
        }

        @Override
        public void addMessageListener(OpenFlowMessageListener listener) {

        }

        @Override
        public void removeMessageListener(OpenFlowMessageListener listener) {

        }

        @Override
        public void addPacketListener(int priority, PacketListener listener) {
        }

        @Override
        public void removePacketListener(PacketListener listener) {
        }

        @Override
        public void addEventListener(OpenFlowEventListener listener) {
        }

        @Override
        public void removeEventListener(OpenFlowEventListener listener) {
        }

        @Override
        public void write(Dpid dpid, OFMessage msg) {
        }

        @Override
        public CompletableFuture<OFMessage> writeResponse(Dpid dpid, OFMessage msg) {
            return null;
        }

        @Override
        public void processPacket(Dpid dpid, OFMessage msg) {
        }

        @Override
        public void setRole(Dpid dpid, RoleState role) {
            roleMap.put(dpid, role);
        }
    }

    private static class TestOpenFlowSwitch implements OpenFlowSwitch {

        RoleState state;
        List<OFMessage> sent = new ArrayList<OFMessage>();
        OFFactory factory = OFFactoryVer10.INSTANCE;

        @Override
        public void sendMsg(OFMessage msg) {
            sent.add(msg);
        }

        @Override
        public void sendMsg(List<OFMessage> msgs) {
        }

        @Override
        public void handleMessage(OFMessage fromSwitch) {

        }

        @Override
        public void setRole(RoleState role) {
            state = role;
        }

        @Override
        public RoleState getRole() {
            return state;
        }

        @Override
        public List<OFPortDesc> getPorts() {
            return PLIST;
        }

        @Override
        public OFMeterFeatures getMeterFeatures() {
            return null;
        }

        @Override
        public OFFactory factory() {
            return factory;
        }

        @Override
        public String getStringId() {
            return null;
        }

        @Override
        public long getId() {
            return DPID1.value();
        }

        @Override
        public String manufacturerDescription() {
            return null;
        }

        @Override
        public String datapathDescription() {
            return null;
        }

        @Override
        public String hardwareDescription() {
            return null;
        }

        @Override
        public String softwareDescription() {
            return null;
        }

        @Override
        public String serialNumber() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void disconnectSwitch() {
        }

        @Override
        public Device.Type deviceType() {
            return Device.Type.SWITCH;
        }

        @Override
        public void returnRoleReply(RoleState requested, RoleState response) {
        }

        @Override
        public String channelId() {
            return "1.2.3.4:1";
        }

    }

}
