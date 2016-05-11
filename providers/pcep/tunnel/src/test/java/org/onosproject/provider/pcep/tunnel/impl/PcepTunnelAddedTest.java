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
import static org.onosproject.provider.pcep.tunnel.impl.PcepAnnotationKeys.BANDWIDTH;
import static org.onosproject.provider.pcep.tunnel.impl.PcepAnnotationKeys.LOCAL_LSP_ID;
import static org.onosproject.provider.pcep.tunnel.impl.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.provider.pcep.tunnel.impl.PcepAnnotationKeys.PCC_TUNNEL_ID;
import static org.onosproject.provider.pcep.tunnel.impl.PcepAnnotationKeys.PLSP_ID;
import static org.onosproject.provider.pcep.tunnel.impl.LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR;
import static org.onosproject.pcep.controller.PcepSyncStatus.SYNCED;
import static org.onosproject.pcep.controller.PcepSyncStatus.IN_SYNC;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;

import com.google.common.collect.ImmutableSet;

/**
 * Tests handling of PCEP report message.
 */
public class PcepTunnelAddedTest {

    static final String PROVIDER_ID = "org.onosproject.provider.tunnel.pcep";
    PcepTunnelProvider tunnelProvider = new PcepTunnelProvider();
    private final MockTunnelProviderRegistryAdapter registry = new MockTunnelProviderRegistryAdapter();
    private final PcepClientControllerAdapter controller = new PcepClientControllerAdapter();
    private final PcepControllerAdapter ctl = new PcepControllerAdapter();
    private final PcepTunnelApiMapper pcepTunnelAPIMapper = new PcepTunnelApiMapper();
    private final MockTunnelServiceAdapter tunnelService = new MockTunnelServiceAdapter();

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
                return TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
            }

            @Override
            public TunnelId tunnelAdded(TunnelDescription tunnel, State state) {
                return TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
            }

            @Override
            public void tunnelRemoved(TunnelDescription tunnel) {
            }

            @Override
            public void tunnelUpdated(TunnelDescription tunnel) {
            }

            @Override
            public void tunnelUpdated(TunnelDescription tunnel, State state) {
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
        tunnelProvider.pcepTunnelApiMapper = pcepTunnelAPIMapper;
        tunnelProvider.cfgService = new ComponentConfigAdapter();
        tunnelProvider.tunnelService = tunnelService;
        tunnelProvider.service = registry.register(tunnelProvider);
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
                .set(LOCAL_LSP_ID, String.valueOf(1)).build();

        Tunnel tunnel = new DefaultTunnel(null, tunnelEndPointSrc, tunnelEndPointDst, MPLS, INIT, null, null,
                                          TunnelName.tunnelName("T123"), null, annotations);
        tunnelService.setupTunnel(null, null, tunnel, null);

        PccId pccId = PccId.pccId(IpAddress.valueOf(0x4e1f0400));
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true));
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

        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));
        controller.getClient(pccId).setLspDbSyncStatus(SYNCED);
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true));
        controller.processClientMessage(pccId, message);

        assertThat(registry.tunnelIdCounter, is((long) 1));
    }

    /**
     * Tests LSPDB sync where PCC reports less LSPs than known by PCE and PCE deletes at the end of DB sync.
     */
    @Test
    public void testCaseLspDbSync1() throws PcepParseException, PcepOutOfBoundMessageException {
        /* Step 1 create 2 LSPs */
        byte[] reportMsg1 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x00,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x19, // LSP object
                                       0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
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

        ChannelBuffer buffer1 = ChannelBuffers.dynamicBuffer();
        buffer1.writeBytes(reportMsg1);

        PcepMessageReader<PcepMessage> reader1 = PcepFactories.getGenericReader();
        PcepMessage message1 = reader1.readFrom(buffer1);

        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true));
        controller.processClientMessage(pccId, message1);

        /* create 2nd LSP */
        byte[] reportMsg2 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x00,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x20, 0x19, // LSP object
                                       0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
                                       0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                       0x01, 0x01, 0x01, 0x01,
                                       0x00, 0x02, 0x00, 0x02,
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

        ChannelBuffer buffer2 = ChannelBuffers.dynamicBuffer();
        buffer2.writeBytes(reportMsg2);

        PcepMessageReader<PcepMessage> reader2 = PcepFactories.getGenericReader();
        PcepMessage message2 = reader2.readFrom(buffer2);

        controller.processClientMessage(pccId, message2);

        /* Assert number of LSPs in DB to be 2. */
        assertThat(registry.tunnelIdCounter, is((long) 2));

        /* Step 2 send sync begin message and LSP 1. */
        byte[] reportMsg3 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                        0x21, 0x10, 0x00, 0x14, //SRP object
                                        0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                        0x00, 0x00, 0x00, 0x00,
                                        0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x1B, // LSP object
                                        0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
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

         ChannelBuffer buffer3 = ChannelBuffers.dynamicBuffer();
         buffer3.writeBytes(reportMsg3);
         PcepMessageReader<PcepMessage> reader3 = PcepFactories.getGenericReader();
         PcepMessage message3 = reader3.readFrom(buffer3);
         controller.processClientMessage(pccId, message3);

         assertThat(controller.getClient(pccId).lspDbSyncStatus(), is(IN_SYNC));

        /* Step 3 send end of sync marker */
         byte[] reportMsg4 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x24,
                                         0x20, 0x10, 0x00, 0x1C, // LSP object
                                         0x00, 0x00, 0x10, 0x19,
                                         0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                         0x00, 0x00, 0x00, 0x00,
                                         0x00, 0x00, 0x00, 0x00,
                                         0x00, 0x00, 0x00, 0x00,
                                         0x00, 0x00, 0x00, 0x00,
                                         0x07, 0x10, 0x00, 0x04, //ERO object
                                         };

          ChannelBuffer buffer4 = ChannelBuffers.dynamicBuffer();
          buffer4.writeBytes(reportMsg4);
          PcepMessageReader<PcepMessage> reader4 = PcepFactories.getGenericReader();
          PcepMessage message4 = reader4.readFrom(buffer4);
          controller.processClientMessage(pccId, message4);

        assertThat(controller.getClient(pccId).lspDbSyncStatus(), is(SYNCED));
    }

    /**
     * Tests PCC PCRpt PCE initiated LSP which PCE doesn't know and hence should send PCInit delete msg.
     */
    @Test
    public void testCaseLspDbSync2() throws PcepParseException, PcepOutOfBoundMessageException {
        /* Step 1 create 2 LSPs */
        byte[] reportMsg1 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x00,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, 0x19, // LSP object
                                       0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
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

        ChannelBuffer buffer1 = ChannelBuffers.dynamicBuffer();
        buffer1.writeBytes(reportMsg1);

        PcepMessageReader<PcepMessage> reader1 = PcepFactories.getGenericReader();
        PcepMessage message1 = reader1.readFrom(buffer1);

        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));
        controller.getClient(pccId).setCapability(new ClientCapability(true, true, true));
        controller.processClientMessage(pccId, message1);

        /* create 2nd LSP */
        byte[] reportMsg2 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                       0x21, 0x10, 0x00, 0x14, //SRP object
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                       0x00, 0x00, 0x00, 0x00,
                                       0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x20, 0x19, // LSP object
                                       0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
                                       0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                       0x01, 0x01, 0x01, 0x01,
                                       0x00, 0x02, 0x00, 0x02,
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

        ChannelBuffer buffer2 = ChannelBuffers.dynamicBuffer();
        buffer2.writeBytes(reportMsg2);

        PcepMessageReader<PcepMessage> reader2 = PcepFactories.getGenericReader();
        PcepMessage message2 = reader2.readFrom(buffer2);

        controller.processClientMessage(pccId, message2);

        /* Assert number of LSPs in DB to be 2. */
        assertThat(registry.tunnelIdCounter, is((long) 2));

        /* Step 2 send sync begin message and LSP 1. */
        byte[] reportMsg3 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x84,
                                        0x21, 0x10, 0x00, 0x14, //SRP object
                                        0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                        0x00, 0x00, 0x00, 0x00,
                                        0x20, 0x10, 0x00, 0x24, 0x00, 0x00, 0x10, (byte) 0x9B, // LSP object
                                        0x00, 0x11, 0x00, 0x04, 0x54, 0x31, 0x00, 0x00, // symbolic path TLV
                                        0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                        0x01, 0x01, 0x01, 0x01,
                                        0x00, 0x01, 0x00, 0x03,
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

        ChannelBuffer buffer3 = ChannelBuffers.dynamicBuffer();
        buffer3.writeBytes(reportMsg3);
        PcepMessageReader<PcepMessage> reader3 = PcepFactories.getGenericReader();
        PcepMessage message3 = reader3.readFrom(buffer3);
        controller.processClientMessage(pccId, message3);

        assertThat(controller.getClient(pccId).lspDbSyncStatus(), is(IN_SYNC));

        /* Step 3 send end of sync marker */
        byte[] reportMsg4 = new byte[] {0x20, 0x0a, 0x00, (byte) 0x24,
                                       0x20, 0x10, 0x00, 0x1C, // LSP object
                                       0x00, 0x00, 0x10, 0x19,
                                       0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x00, 0x00, 0x00, 0x00,
                                       0x07, 0x10, 0x00, 0x04, //ERO object
                                       };

        ChannelBuffer buffer4 = ChannelBuffers.dynamicBuffer();
        buffer4.writeBytes(reportMsg4);
        PcepMessageReader<PcepMessage> reader4 = PcepFactories.getGenericReader();
        PcepMessage message4 = reader4.readFrom(buffer4);
        controller.processClientMessage(pccId, message4);

        assertThat(controller.getClient(pccId).lspDbSyncStatus(), is(SYNCED));
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
        tunnelProvider.service = null;
    }
}
