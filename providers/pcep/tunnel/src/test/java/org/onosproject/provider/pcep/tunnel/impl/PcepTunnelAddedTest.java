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
package org.onosproject.provider.pcep.tunnel.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onosproject.incubator.net.tunnel.Tunnel.Type.MPLS;
import static org.onosproject.incubator.net.tunnel.Tunnel.State.INIT;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.BANDWIDTH;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LOCAL_LSP_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCC_TUNNEL_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PLSP_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.DELEGATE;
import static org.onosproject.pcep.controller.LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR;
import static org.onosproject.pcep.controller.PcepSyncStatus.SYNCED;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.MastershipRole.MASTER;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;
import org.onosproject.incubator.net.tunnel.TunnelAdminService;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.LspKey;
import org.onosproject.pcep.controller.PccId;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * Tests handling of PCEP report message.
 */
public class PcepTunnelAddedTest {

    public static final String PROVIDER_ID = "org.onosproject.provider.tunnel.pcep";
    public static final String UNKOWN = "UNKOWN";
    PcepTunnelProvider tunnelProvider = new PcepTunnelProvider();
    private final MockTunnelProviderRegistryAdapter registry = new MockTunnelProviderRegistryAdapter();
    private final PcepClientControllerAdapter controller = new PcepClientControllerAdapter();
    private final PcepControllerAdapter ctl = new PcepControllerAdapter();
    private final PcepTunnelApiMapper pcepTunnelAPIMapper = new PcepTunnelApiMapper();
    private final MockTunnelServiceAdapter tunnelService = new MockTunnelServiceAdapter();
    public final MockDeviceService deviceService = new MockDeviceService();
    private final MockMasterShipService masterShipService = new MockMasterShipService();
    private final MockLinkService linkService = new MockLinkService();
    private final MockTunnelAdminService tunnelAdminService = new MockTunnelAdminService();

    private class MockLinkService extends LinkServiceAdapter {
        LinkedList<Link> links = new LinkedList<>();
        void addLink(Link link) {
            links.add(link);
        }

        @Override
        public Iterable<Link> getActiveLinks() {

            return FluentIterable.from(links)
                    .filter(input -> input.state() == ACTIVE);
        }
    }

    private class MockTunnelAdminService implements TunnelAdminService {

        @Override
        public void removeTunnel(TunnelId tunnelId) {
            // TODO Auto-generated method stub
        }

        @Override
        public void removeTunnels(TunnelEndPoint src, TunnelEndPoint dst, ProviderId producerName) {
            // TODO Auto-generated method stub
        }

        @Override
        public void removeTunnels(TunnelEndPoint src, TunnelEndPoint dst, Type type, ProviderId producerName) {
            // TODO Auto-generated method stub
        }

        @Override
        public void updateTunnel(Tunnel tunnel, Path path) {
            if (tunnelService.tunnelIdAsKeyStore.containsKey(tunnel.tunnelId())) {
                tunnelService.tunnelIdAsKeyStore.replace(tunnel.tunnelId(), tunnel);
            }
        }
    }

    private class MockMasterShipService extends MastershipServiceAdapter {
        boolean set;

        private void setMaster(boolean isMaster) {
            this.set = isMaster;
        }

        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return set ? MastershipRole.MASTER : MastershipRole.STANDBY;
        }

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return getLocalRole(deviceId) == MASTER;
        }
    }

    private class MockDeviceService extends DeviceServiceAdapter {
        List<Device> devices = new LinkedList<>();

        private void addDevice(Device dev) {
            devices.add(dev);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return devices;
        }
    }

    private class MockTunnelProviderRegistryAdapter extends TunnelProviderRegistryAdapter {
        public long tunnelIdCounter;

        @Override
        public TunnelProviderService register(TunnelProvider provider) {
            this.provider = provider;
            return new TestProviderService();
        }

        private class TestProviderService implements TunnelProviderService {

            @Override
            public TunnelProvider provider() {
                return null;
            }

            @Override
            public TunnelId tunnelAdded(TunnelDescription tunnel) {
                TunnelId id = TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
                Tunnel storedTunnel = new DefaultTunnel(ProviderId.NONE,
                        tunnel.src(), tunnel.dst(),
                        tunnel.type(),
                        tunnel.groupId(),
                        id,
                        tunnel.tunnelName(),
                        tunnel.path(),
                        tunnel.resource(),
                        tunnel.annotations());
                tunnelService.tunnelIdAsKeyStore.put(id, storedTunnel);
                return id;
            }

            @Override
            public TunnelId tunnelAdded(TunnelDescription tunnel, State state) {
                TunnelId id = TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
                Tunnel storedTunnel = new DefaultTunnel(ProviderId.NONE,
                        tunnel.src(), tunnel.dst(),
                        tunnel.type(),
                        tunnel.groupId(),
                        id,
                        tunnel.tunnelName(),
                        tunnel.path(),
                        tunnel.resource(),
                        tunnel.annotations());
                tunnelService.tunnelIdAsKeyStore.put(id, storedTunnel);
                return id;
            }

            @Override
            public void tunnelRemoved(TunnelDescription tunnel) {
            }

            @Override
            public void tunnelUpdated(TunnelDescription tunnel) {
            }

            @Override
            public void tunnelUpdated(TunnelDescription tunnel, State state) {
                TunnelId id = TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
                Tunnel storedTunnel = new DefaultTunnel(ProviderId.NONE,
                        tunnel.src(), tunnel.dst(),
                        tunnel.type(),
                        tunnel.groupId(),
                        id,
                        tunnel.tunnelName(),
                        tunnel.path(),
                        tunnel.resource(),
                        tunnel.annotations());
                tunnelService.tunnelIdAsKeyStore.put(id, storedTunnel);
            }

            @Override
            public Tunnel tunnelQueryById(TunnelId tunnelId) {
                return null;
            }
        }
    }

    private class MockTunnelServiceAdapter extends TunnelServiceAdapter {
        private HashMap<TunnelId, Tunnel> tunnelIdAsKeyStore = new HashMap<>();
        private int tunnelIdCounter = 0;

        @Override
        public TunnelId setupTunnel(ApplicationId producerId, ElementId srcElementId, Tunnel tunnel, Path path) {
            TunnelId tunnelId = TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
            tunnelIdAsKeyStore.put(tunnelId, tunnel);
            return tunnelId;
        }

        @Override
        public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
            Collection<Tunnel> result = new HashSet<>();
            Tunnel tunnel = null;
            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                tunnel = tunnelIdAsKeyStore.get(tunnelId);

                if ((null != tunnel) && (src.equals(tunnel.src())) && (dst.equals(tunnel.dst()))) {
                    result.add(tunnel);
                }
            }

            return result.isEmpty() ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public Collection<Tunnel> queryAllTunnels() {
            Collection<Tunnel> result = new HashSet<>();

            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                result.add(tunnelIdAsKeyStore.get(tunnelId));
            }

            return result.isEmpty() ? Collections.emptySet() : ImmutableSet.copyOf(result);

        }
    }

    @Before
    public void preSetup() {
        tunnelProvider.tunnelProviderRegistry = registry;
        tunnelProvider.pcepClientController = controller;
        tunnelProvider.controller = ctl;
        tunnelProvider.deviceService = deviceService;
        tunnelProvider.mastershipService = masterShipService;
        tunnelProvider.pcepTunnelApiMapper = pcepTunnelAPIMapper;
        tunnelProvider.cfgService = new ComponentConfigAdapter();
        tunnelProvider.tunnelService = tunnelService;
        tunnelProvider.tunnelAdminService = tunnelAdminService;
        tunnelProvider.service = registry.register(tunnelProvider);
        tunnelProvider.linkService = linkService;
        tunnelProvider.activate();
    }

    /**
     * Tests PCRpt msg with sync flag set.
     */
    @Test
    public void tunnelProviderAddedTest1() throws PcepParseException, PcepOutOfBoundMessageException {
        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, 0x01, //SRP object
                0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                0x00, 0x00, 0x00, 0x02,
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //symbolic path tlv
                0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x01, 0x00, 0x01,
                0x01, 0x01, 0x01, 0x01,
                0x05, 0x05, 0x05, 0x05,

                0x07, 0x10, 0x00, 0x14, //ERO object
                0x01, 0x08, (byte) 0x01, 0x01, 0x01, 0x01, 0x04, 0x00, // ERO IPv4 sub objects
                0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00,

                0x08, 0x10, 0x00, 0x34, //RRO object
                0x01, 0x08, 0x11, 0x01, 0x01, 0x01, 0x04, 0x00, // RRO IPv4 sub objects
                0x01, 0x08, 0x11, 0x01, 0x01, 0x02, 0x04, 0x00,
                0x01, 0x08, 0x06, 0x06, 0x06, 0x06, 0x04, 0x00,
                0x01, 0x08, 0x12, 0x01, 0x01, 0x02, 0x04, 0x00,
                0x01, 0x08, 0x12, 0x01, 0x01, 0x01, 0x04, 0x00,
                0x01, 0x08, 0x05, 0x05, 0x05, 0x05, 0x04, 0x00
                };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
        newBuilder.set(PcepTunnelProvider.LSRID, "1.1.1.1");
        newBuilder.set(AnnotationKeys.TYPE, "L3");
        Device device = new DefaultDevice(ProviderId.NONE, DeviceId.deviceId("1.1.1.1"), ROUTER,
                UNKOWN, UNKOWN, UNKOWN,
                UNKOWN, new ChassisId(),
                newBuilder.build());

        deviceService.addDevice(device);
        controller.getClient(PccId.pccId(IpAddress.valueOf("1.1.1.1"))).setCapability(
                new ClientCapability(true, true, true, true, true));
        masterShipService.setMaster(true);
        Link link = DefaultLink.builder()
                .src(new ConnectPoint(device.id(), PortNumber.portNumber(16843009)))
                .dst(new ConnectPoint(device.id(), PortNumber.portNumber(84215045)))
                .state(ACTIVE)
                .type(Link.Type.DIRECT)
                .providerId(ProviderId.NONE)
                .build();
        linkService.addLink(link);
        controller.processClientMessage(PccId.pccId(IpAddress.valueOf("1.1.1.1")), message);

        assertThat(registry.tunnelIdCounter, is((long) 1));
    }

    /**
     * Tests updating an existing tunnel on receiving asynchronous PCRpt msg,
     * i.e. without any SRP id.
     */
    @Test
    public void tunnelProviderAddedTest2() throws PcepParseException, PcepOutOfBoundMessageException {
        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x50,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x02,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x19, //LSP object
                                       0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //symbolic path TLV
                                       0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                       0x4e, 0x1f, 0x04, 0x00,
                                       0x00, 0x01, 0x00, 0x01,
                                       0x4e, 0x1f, 0x04, 0x00,
                                       0x4e, 0x20, 0x04, 0x00,
                                       0x07, 0x10, 0x00, 0x14, //ERO object
                                       0x01, 0x08, (byte) 0xb6, 0x02, 0x4e, 0x1f, 0x04, 0x00, // ERO IPv4 sub objects
                                       0x01, 0x08, (byte) 0xb6, 0x02, 0x4e, 0x20, 0x04, 0x00,
                                       };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        // create an existing tunnel.
        IpTunnelEndPoint tunnelEndPointSrc = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(0x4e1f0400));
        IpTunnelEndPoint tunnelEndPointDst = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(0x4e200400));

        SparseAnnotations annotations = DefaultAnnotations.builder()
                .set(BANDWIDTH, (new Integer(1)).toString())
                .set(LSP_SIG_TYPE, WITHOUT_SIGNALLING_AND_WITHOUT_SR.name())
                .set(PCC_TUNNEL_ID, String.valueOf(1))
                .set(PLSP_ID, String.valueOf(1))
                .set(LOCAL_LSP_ID, String.valueOf(1))
                .set(DELEGATE, String.valueOf("true"))
                .build();

        Tunnel tunnel = new DefaultTunnel(null, tunnelEndPointSrc, tunnelEndPointDst, MPLS, INIT, null, null,
                                          TunnelName.tunnelName("T123"), null, annotations);
        tunnelService.setupTunnel(null, null, tunnel, null);

        PccId pccId = PccId.pccId(IpAddress.valueOf(0x4e1f0400));
        PcepClientAdapter pc = new PcepClientAdapter();
        pc.init(pccId, PcepVersion.PCEP_1);
        masterShipService.setMaster(true);
        controller.getClient(pccId).setLspAndDelegationInfo(new LspKey(1, (short) 1), true);
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true, true, true));
        controller.getClient(pccId).setLspDbSyncStatus(SYNCED);

        // Process update message.
        controller.processClientMessage(pccId, message);
        assertThat(tunnelService.queryAllTunnels().size(), is(1));
    }

    /**
     * Tests adding a new tunnel on receiving asynchronous PCRpt msg,
     * i.e. without any SRP id.
     */
    @Test
    public void tunnelProviderAddedTest3() throws PcepParseException, PcepOutOfBoundMessageException {
        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x02,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x1B, // LSP object
                                       0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
                                       0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                       0x01, 0x01, 0x01, 0x01,
                                       0x00, 0x01, 0x00, 0x01,
                                       0x01, 0x01, 0x01, 0x01,
                                       0x05, 0x05, 0x05, 0x05,

                                       0x07, 0x10, 0x00, 0x14, //ERO object
                                       0x01, 0x08, (byte) 0x01, 0x01, 0x01, 0x01, 0x04, 0x00, // ERO IPv4 sub objects
                                       0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00,

                                       0x08, 0x10, 0x00, 0x34, //RRO object
                                       0x01, 0x08, 0x11, 0x01, 0x01, 0x01, 0x04, 0x00, // RRO IPv4 sub objects
                                       0x01, 0x08, 0x11, 0x01, 0x01, 0x02, 0x04, 0x00,
                                       0x01, 0x08, 0x06, 0x06, 0x06, 0x06, 0x04, 0x00,
                                       0x01, 0x08, 0x12, 0x01, 0x01, 0x02, 0x04, 0x00,
                                       0x01, 0x08, 0x12, 0x01, 0x01, 0x01, 0x04, 0x00,
                                       0x01, 0x08, 0x05, 0x05, 0x05, 0x05, 0x04, 0x00
                                       };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
        newBuilder.set(PcepTunnelProvider.LSRID, "1.1.1.1");
        newBuilder.set(AnnotationKeys.TYPE, "L3");
        Device device = new DefaultDevice(ProviderId.NONE, DeviceId.deviceId("1.1.1.1"), ROUTER,
                UNKOWN, UNKOWN, UNKOWN,
                UNKOWN, new ChassisId(),
                newBuilder.build());

        deviceService.addDevice(device);

        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));
        controller.getClient(pccId).setLspDbSyncStatus(SYNCED);
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true, true, true));

        Link link = DefaultLink.builder()
                .src(new ConnectPoint(device.id(), PortNumber.portNumber(16843009)))
                .dst(new ConnectPoint(device.id(), PortNumber.portNumber(84215045)))
                .state(ACTIVE)
                .type(Link.Type.DIRECT)
                .providerId(ProviderId.NONE)
                .build();
        linkService.addLink(link);
        PcepClientAdapter pc = new PcepClientAdapter();
        pc.init(pccId, PcepVersion.PCEP_1);
        controller.getClient(pccId).setLspAndDelegationInfo(new LspKey(1, (short) 1), true);
        masterShipService.setMaster(true);
        controller.processClientMessage(pccId, message);

        assertThat(registry.tunnelIdCounter, is((long) 1));
    }

    /**
     * Tests PCRpt msg with D flag set and delegated to non-master.
     *
     * @throws InterruptedException while waiting for delay
     */
    @Test
    public void tunnelProviderAddedTest4() throws PcepParseException, PcepOutOfBoundMessageException,
            InterruptedException {
        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, 0x01, //SRP object
                0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                0x00, 0x00, 0x00, 0x02,
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x02, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //symbolic path tlv
                0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x01, 0x00, 0x01,
                0x01, 0x01, 0x01, 0x01,
                0x05, 0x05, 0x05, 0x05,

                0x07, 0x10, 0x00, 0x14, //ERO object
                0x01, 0x08, (byte) 0x01, 0x01, 0x01, 0x01, 0x04, 0x00, // ERO IPv4 sub objects
                0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00,

                0x08, 0x10, 0x00, 0x34, //RRO object
                0x01, 0x08, 0x11, 0x01, 0x01, 0x01, 0x04, 0x00, // RRO IPv4 sub objects
                0x01, 0x08, 0x11, 0x01, 0x01, 0x02, 0x04, 0x00,
                0x01, 0x08, 0x06, 0x06, 0x06, 0x06, 0x04, 0x00,
                0x01, 0x08, 0x12, 0x01, 0x01, 0x02, 0x04, 0x00,
                0x01, 0x08, 0x12, 0x01, 0x01, 0x01, 0x04, 0x00,
                0x01, 0x08, 0x05, 0x05, 0x05, 0x05, 0x04, 0x00
                };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        //PCC 1.1.1.1, D=0, ONOS as master
        masterShipService.setMaster(true);
        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
        newBuilder.set(PcepTunnelProvider.LSRID, "1.1.1.1");
        newBuilder.set(AnnotationKeys.TYPE, "L3");
        Device device = new DefaultDevice(ProviderId.NONE, DeviceId.deviceId("1.1.1.1"), ROUTER,
                UNKOWN, UNKOWN, UNKOWN,
                UNKOWN, new ChassisId(),
                newBuilder.build());

        deviceService.addDevice(device);
        controller.getClient(PccId.pccId(IpAddress.valueOf("1.1.1.1"))).setCapability(
                new ClientCapability(true, true, true, true, true));
        Link link = DefaultLink.builder()
                .src(new ConnectPoint(device.id(), PortNumber.portNumber(16843009)))
                .dst(new ConnectPoint(device.id(), PortNumber.portNumber(84215045)))
                .state(ACTIVE)
                .type(Link.Type.DIRECT)
                .providerId(ProviderId.NONE)
                .build();
        linkService.addLink(link);
        controller.processClientMessage(PccId.pccId(IpAddress.valueOf("1.1.1.1")), message);
        assertThat(tunnelService.tunnelIdAsKeyStore.values().iterator().next().annotations().value(DELEGATE),
                is("false"));

        //PCC 1.1.1.1, D=1, non-master
        masterShipService.setMaster(false);

        reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                0x21, 0x10, 0x00, 0x14,  0x00, 0x00, 0x00, 0x00,  0x00, 0x00, 0x00, 0x01, //SRP object
                0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                0x00, 0x00, 0x00, 0x02,
                0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x03, //LSP object
                0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, //symbolic path tlv
                0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                0x01, 0x01, 0x01, 0x01,
                0x00, 0x01, 0x00, 0x01,
                0x01, 0x01, 0x01, 0x01,
                0x05, 0x05, 0x05, 0x05,

                0x07, 0x10, 0x00, 0x14, //ERO object
                0x01, 0x08, (byte) 0x01, 0x01, 0x01, 0x01, 0x04, 0x00, // ERO IPv4 sub objects
                0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00,

                0x08, 0x10, 0x00, 0x34, //RRO object
                0x01, 0x08, 0x11, 0x01, 0x01, 0x01, 0x04, 0x00, // RRO IPv4 sub objects
                0x01, 0x08, 0x11, 0x01, 0x01, 0x02, 0x04, 0x00,
                0x01, 0x08, 0x06, 0x06, 0x06, 0x06, 0x04, 0x00,
                0x01, 0x08, 0x12, 0x01, 0x01, 0x02, 0x04, 0x00,
                0x01, 0x08, 0x12, 0x01, 0x01, 0x01, 0x04, 0x00,
                0x01, 0x08, 0x05, 0x05, 0x05, 0x05, 0x04, 0x00
                };

        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        reader = PcepFactories.getGenericReader();
        message = reader.readFrom(buffer);

        controller.processClientMessage(PccId.pccId(IpAddress.valueOf("1.1.1.1")), message);
        TimeUnit.MILLISECONDS.sleep(4000);
        assertThat(registry.tunnelIdCounter, is((long) 2));

        Iterator<Tunnel> iterator = tunnelService.tunnelIdAsKeyStore.values().iterator();
        iterator.next();
        assertThat(iterator.next().annotations().value(DELEGATE),
                is("true"));
    }

    /**
     * Tests adding PCC Init LSP after LSP DB sync is over.
     */
    @Test
    public void tunnelProviderAddedTest5() throws PcepParseException, PcepOutOfBoundMessageException {
        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x02,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x19, // LSP object
                                       0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
                                       0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                       0x01, 0x01, 0x01, 0x01,
                                       0x00, 0x01, 0x00, 0x01,
                                       0x01, 0x01, 0x01, 0x01,
                                       0x05, 0x05, 0x05, 0x05,

                                       0x07, 0x10, 0x00, 0x14, //ERO object
                                       0x01, 0x08, (byte) 0x01, 0x01, 0x01, 0x01, 0x04, 0x00, // ERO IPv4 sub objects
                                       0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00,

                                       0x08, 0x10, 0x00, 0x34, //RRO object
                                       0x01, 0x08, 0x11, 0x01, 0x01, 0x01, 0x04, 0x00, // RRO IPv4 sub objects
                                       0x01, 0x08, 0x11, 0x01, 0x01, 0x02, 0x04, 0x00,
                                       0x01, 0x08, 0x06, 0x06, 0x06, 0x06, 0x04, 0x00,
                                       0x01, 0x08, 0x12, 0x01, 0x01, 0x02, 0x04, 0x00,
                                       0x01, 0x08, 0x12, 0x01, 0x01, 0x01, 0x04, 0x00,
                                       0x01, 0x08, 0x05, 0x05, 0x05, 0x05, 0x04, 0x00
                                       };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
        newBuilder.set(PcepTunnelProvider.LSRID, "1.1.1.1");
        newBuilder.set(AnnotationKeys.TYPE, "L3");
        Device device = new DefaultDevice(ProviderId.NONE, DeviceId.deviceId("1.1.1.1"), ROUTER,
                UNKOWN, UNKOWN, UNKOWN,
                UNKOWN, new ChassisId(),
                newBuilder.build());

        deviceService.addDevice(device);

        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));
        controller.getClient(pccId).setLspDbSyncStatus(SYNCED);
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true, true, true));

        PcepClientAdapter pc = new PcepClientAdapter();
        pc.init(pccId, PcepVersion.PCEP_1);
        controller.getClient(pccId).setLspAndDelegationInfo(new LspKey(1, (short) 1), true);
        masterShipService.setMaster(true);
        controller.processClientMessage(pccId, message);

        assertThat(registry.tunnelIdCounter, is((long) 0));
    }

    @After
    public void tearDown() throws IOException {
        tunnelProvider.deactivate();
        tunnelProvider.controller = null;
        tunnelProvider.pcepClientController = null;
        tunnelProvider.tunnelProviderRegistry = null;

        tunnelProvider.pcepTunnelApiMapper = null;
        tunnelProvider.cfgService = null;
        tunnelProvider.tunnelService = null;
        tunnelProvider.tunnelAdminService = null;
        tunnelProvider.deviceService = null;
        tunnelProvider.mastershipService = null;
        tunnelProvider.linkService = null;
        tunnelProvider.service = null;
    }
}
