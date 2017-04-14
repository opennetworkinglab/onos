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

package org.onosproject.pcep.controller.impl;

import static org.onosproject.pcep.controller.PcepLspSyncAction.SEND_UPDATE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.UNSTABLE;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelListener;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.TunnelSubscription;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepLspSyncAction;
import org.onosproject.pcep.controller.PcepPacketStats;
import org.onosproject.pcep.controller.PcepSyncStatus;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFactories;
import org.onosproject.pcepio.protocol.PcepInitiateMsg;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepVersion;
import com.google.common.collect.ImmutableSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PcepClientControllerImplTest {
    PcepClientControllerImpl controllerImpl = new PcepClientControllerImpl();
    TunnelService tunnelService = new MockTunnelService();
    private PcepEventListener listener;
    private Channel channel;

    @Before
    public void startUp() {
        controllerImpl.tunnelService = tunnelService;
        listener = new PcepEventListenerAdapter();
        controllerImpl.addEventListener(listener);
        channel = new MockChannel();
    }

    @After
    public void tearDown() {
        controllerImpl.removeEventListener(listener);
        listener = null;
        controllerImpl.tunnelService = null;
    }

    @Test
    public void tunnelProviderAddedTest1() throws PcepParseException, PcepOutOfBoundMessageException {
        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));

        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x50, 0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x00, 0x00, 0x01, // SRP object
                                        0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                        0x00, 0x00, 0x00, 0x00, 0x20, 0x10, 0x00, 0x24, // LSP object
                                        0x00, 0x00, 0x10, (byte) 0xAB,
                                        0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, // symbolic path tlv
                                        0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                        0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x05,
                                        0x05, 0x05, 0x05,

                                        0x07, 0x10, 0x00, 0x14, // ERO object
                                        0x01, 0x08, (byte) 0x01, 0x01, // ERO IPv4 sub objects
                                        0x01, 0x01, 0x04, 0x00,
                                        0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00, };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        PcepClientImpl pc = new PcepClientImpl();
        PcepPacketStats pktStats = new PcepPacketStatsImpl();

        pc.init(pccId, PcepVersion.PCEP_1, pktStats);
        pc.setChannel(channel);
        pc.setAgent(controllerImpl.agent);
        pc.setConnected(true);
        pc.setCapability(new ClientCapability(true, true, true, true, true));

        controllerImpl.agent.addConnectedClient(pccId, pc);
        controllerImpl.processClientMessage(pccId, message);

        pc.setLspDbSyncStatus(PcepSyncStatus.SYNCED);
        pc.setLabelDbSyncStatus(PcepSyncStatus.IN_SYNC);
        pc.setLabelDbSyncStatus(PcepSyncStatus.SYNCED);

        List<PcepMessage> deleteMsgs = ((MockChannel) channel).msgsWritten();
        assertThat(deleteMsgs.size(), is(1));

        for (PcepMessage msg : deleteMsgs) {
            assertThat(((PcepInitiateMsg) msg).getPcInitiatedLspRequestList().getFirst().getSrpObject().getRFlag(),
                       is(true));
        }
    }

    @Test
    public void tunnelProviderAddedTest2() throws PcepParseException, PcepOutOfBoundMessageException {
        PccId pccId = PccId.pccId(IpAddress.valueOf("1.1.1.1"));

        byte[] reportMsg = new byte[] {0x20, 0x0a, 0x00, (byte) 0x50, 0x21, 0x10, 0x00, 0x14, 0x00, 0x00, 0x00, 0x00,
                                        0x00, 0x00, 0x00, 0x01, // SRP object
                                        0x00, 0x1c, 0x00, 0x04, // PATH-SETUP-TYPE TLV
                                        0x00, 0x00, 0x00, 0x00, 0x20, 0x10, 0x00, 0x24, 0x00, // LSP object
                                        0x00, 0x10, (byte) 0xAB,
                                        0x00, 0x11, 0x00, 0x02, 0x54, 0x31, 0x00, 0x00, // symbolic path tlv
                                        0x00, 0x12, 0x00, 0x10, // IPv4-LSP-IDENTIFIER-TLV
                                        0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x05,
                                        0x05, 0x05, 0x05,

                                        0x07, 0x10, 0x00, 0x14, // ERO object
                                        0x01, 0x08, (byte) 0x01, 0x01, 0x01, 0x01, 0x04, 0x00, // ERO IPv4 sub objects
                                        0x01, 0x08, (byte) 0x05, 0x05, 0x05, 0x05, 0x04, 0x00, };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(reportMsg);

        PcepMessageReader<PcepMessage> reader = PcepFactories.getGenericReader();
        PcepMessage message = reader.readFrom(buffer);

        PcepClientImpl pc = new PcepClientImpl();
        PcepPacketStats pktStats = new PcepPacketStatsImpl();

        pc.init(pccId, PcepVersion.PCEP_1, pktStats);
        pc.setChannel(channel);
        pc.setAgent(controllerImpl.agent);
        pc.setConnected(true);
        pc.setCapability(new ClientCapability(true, true, true, true, true));

        controllerImpl.agent.addConnectedClient(pccId, pc);
        controllerImpl.processClientMessage(pccId, message);

        pc.setLspDbSyncStatus(PcepSyncStatus.SYNCED);
        pc.setLabelDbSyncStatus(PcepSyncStatus.IN_SYNC);
        pc.setLabelDbSyncStatus(PcepSyncStatus.SYNCED);
    }

    class PcepEventListenerAdapter implements PcepEventListener {

        public List<PcepMessage> handledMsg = new ArrayList<>();
        public List<Tunnel> tunnelsToBeUpdatedToNw = new ArrayList<>();
        public List<Tunnel> deletedFromNwTunnels = new ArrayList<>();

        @Override
        public void handleMessage(PccId pccId, PcepMessage msg) {
            handledMsg.add(msg);
        }

        @Override
        public void handleEndOfSyncAction(Tunnel tunnel, PcepLspSyncAction endOfSyncAction) {
            if (endOfSyncAction == SEND_UPDATE) {
                tunnelsToBeUpdatedToNw.add(tunnel);
                return;
            } else if (endOfSyncAction == UNSTABLE) {
                deletedFromNwTunnels.add(tunnel);
            }
        }
    }

    class MockChannel implements Channel {
        private List<PcepMessage> msgOnWire = new ArrayList<>();

        @Override
        public ChannelFuture write(Object o) {
            if (o instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<PcepMessage> msgs = (List<PcepMessage>) o;
                for (PcepMessage msg : msgs) {
                    if (msg instanceof PcepInitiateMsg) {
                        msgOnWire.add(msg);
                    }
                }
            }
            return null;
        }

        public List<PcepMessage> msgsWritten() {
            return msgOnWire;
        }

        @Override
        public int compareTo(Channel o) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Integer getId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFactory getFactory() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Channel getParent() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelConfig getConfig() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelPipeline getPipeline() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean isOpen() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isBound() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isConnected() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public SocketAddress getLocalAddress() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture write(Object message, SocketAddress remoteAddress) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture disconnect() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture unbind() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture close() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture getCloseFuture() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getInterestOps() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isReadable() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isWritable() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public ChannelFuture setInterestOps(int interestOps) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ChannelFuture setReadable(boolean readable) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean getUserDefinedWritability(int index) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setUserDefinedWritability(int index, boolean isWritable) {
            // TODO Auto-generated method stub

        }

        @Override
        public Object getAttachment() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setAttachment(Object attachment) {
            // TODO Auto-generated method stub

        }
    }

    class MockTunnelService implements TunnelService {
        private HashMap<TunnelId, Tunnel> tunnelIdAsKeyStore = new HashMap<TunnelId, Tunnel>();
        private int tunnelIdCounter = 0;

        @Override
        public TunnelId setupTunnel(ApplicationId producerId, ElementId srcElementId, Tunnel tunnel, Path path) {
            TunnelId tunnelId = TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
            Tunnel tunnelToInsert = new DefaultTunnel(tunnel.providerId(), tunnel.src(), tunnel.dst(), tunnel.type(),
                                                      tunnel.state(), tunnel.groupId(), tunnelId, tunnel.tunnelName(),
                                                      path, tunnel.annotations());
            tunnelIdAsKeyStore.put(tunnelId, tunnelToInsert);
            return tunnelId;
        }

        @Override
        public Tunnel queryTunnel(TunnelId tunnelId) {
            for (TunnelId tunnelIdKey : tunnelIdAsKeyStore.keySet()) {
                if (tunnelIdKey.equals(tunnelId)) {
                    return tunnelIdAsKeyStore.get(tunnelId);
                }
            }
            return null;
        }

        @Override
        public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
            Collection<Tunnel> result = new HashSet<Tunnel>();
            Tunnel tunnel = null;
            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                tunnel = tunnelIdAsKeyStore.get(tunnelId);

                if ((null != tunnel) && (src.equals(tunnel.src())) && (dst.equals(tunnel.dst()))) {
                    result.add(tunnel);
                }
            }

            return result.size() == 0 ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public Collection<Tunnel> queryTunnel(Tunnel.Type type) {
            Collection<Tunnel> result = new HashSet<Tunnel>();

            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                result.add(tunnelIdAsKeyStore.get(tunnelId));
            }

            return result.size() == 0 ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public Collection<Tunnel> queryAllTunnels() {
            Collection<Tunnel> result = new HashSet<Tunnel>();

            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                result.add(tunnelIdAsKeyStore.get(tunnelId));
            }

            return result.size() == 0 ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public void addListener(TunnelListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeListener(TunnelListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public Tunnel borrowTunnel(ApplicationId consumerId, TunnelId tunnelId, Annotations... annotations) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelName tunnelName,
                                               Annotations... annotations) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                               Annotations... annotations) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                               Type type, Annotations... annotations) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean downTunnel(ApplicationId producerId, TunnelId tunnelId) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean returnTunnel(ApplicationId consumerId, TunnelId tunnelId, Annotations... annotations) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean returnTunnel(ApplicationId consumerId, TunnelName tunnelName, Annotations... annotations) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst, Type type,
                                    Annotations... annotations) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                    Annotations... annotations) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Collection<TunnelSubscription> queryTunnelSubscription(ApplicationId consumerId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int tunnelCount() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Iterable<Tunnel> getTunnels(DeviceId deviceId) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
