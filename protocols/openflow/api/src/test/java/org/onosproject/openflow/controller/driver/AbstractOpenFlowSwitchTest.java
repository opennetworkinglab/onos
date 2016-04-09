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
package org.onosproject.openflow.controller.driver;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFMessage;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Tests for packet processing in the abstract openflow switch class.
 */
public class AbstractOpenFlowSwitchTest {

    OpenFlowSwitchImpl ofSwitch;
    TestExecutorService executorService;

    /**
     * Mock executor service that tracks submits.
     */
    static class TestExecutorService extends ExecutorServiceAdapter {
        private List<OFMessage> submittedMessages = new ArrayList<>();

        List<OFMessage> submittedMessages() {
            return submittedMessages;
        }

        @Override
        public void execute(Runnable task) {
            AbstractOpenFlowSwitch.OFMessageHandler handler =
                    (AbstractOpenFlowSwitch.OFMessageHandler) task;
            submittedMessages.add(handler.msg);
        }
    }

    /**
     * Sets up switches to use as data.
     */
    @Before
    public void setUp() {
        ofSwitch = new OpenFlowSwitchImpl();

        executorService = new TestExecutorService();
        ofSwitch.executorMsgs = executorService;
        Channel channel = new MockChannel();
        ofSwitch.setChannel(channel);
        ofSwitch.role = RoleState.MASTER;
        ofSwitch.addEventListener(new OpenFlowEventListenerAdapter());
    }

    /**
     * Tests a packet out operation.
     */
    @Test
    public void testPacketOut() {
        OFMessage ofPacketOut = new MockOfPacketOut();
        ofSwitch.sendMsg(ofPacketOut);
        assertThat(executorService.submittedMessages(), hasSize(1));
        assertThat(executorService.submittedMessages().get(0), is(ofPacketOut));
    }

    /**
     * Tests a flow mod operation.
     */
    @Test
    public void testFlowMod() {
        OFMessage ofFlowMod = new MockOfFlowMod();
        ofSwitch.sendMsg(ofFlowMod);
        assertThat(executorService.submittedMessages(), hasSize(1));
        assertThat(executorService.submittedMessages().get(0), is(ofFlowMod));
    }

    /**
     * Tests a stats request operation.
     */
    @Test
    public void testStatsRequest() {
        OFMessage ofStatsRequest = new MockOfStatsRequest();
        ofSwitch.sendMsg(ofStatsRequest);
        assertThat(executorService.submittedMessages(), hasSize(1));
        assertThat(executorService.submittedMessages().get(0), is(ofStatsRequest));
    }

    protected class OpenFlowSwitchImpl extends AbstractOpenFlowSwitch {

        @Override
        public Boolean supportNxRole() {
            return null;
        }

        @Override
        public void startDriverHandshake() {
        }

        @Override
        public boolean isDriverHandshakeComplete() {
            return false;
        }

        @Override
        public void processDriverHandshakeMessage(OFMessage m) {
        }
    }

    private class OpenFlowEventListenerAdapter implements OpenFlowEventListener {

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
        }
    }

    private class MockChannel implements Channel {

        @Override
        public Integer getId() {
            return null;
        }

        @Override
        public ChannelFactory getFactory() {
            return null;
        }

        @Override
        public Channel getParent() {
            return null;
        }

        @Override
        public ChannelConfig getConfig() {
            return null;
        }

        @Override
        public ChannelPipeline getPipeline() {
            return null;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public boolean isBound() {
            return false;
        }

        @Override
        public boolean isConnected() {
            // we assume that the channel is connected
            return true;
        }

        @Override
        public SocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public ChannelFuture write(Object message) {
            return null;
        }

        @Override
        public ChannelFuture write(Object message, SocketAddress remoteAddress) {
            return null;
        }

        @Override
        public ChannelFuture bind(SocketAddress localAddress) {
            return null;
        }

        @Override
        public ChannelFuture connect(SocketAddress remoteAddress) {
            return null;
        }

        @Override
        public ChannelFuture disconnect() {
            return null;
        }

        @Override
        public ChannelFuture unbind() {
            return null;
        }

        @Override
        public ChannelFuture close() {
            return null;
        }

        @Override
        public ChannelFuture getCloseFuture() {
            return null;
        }

        @Override
        public int getInterestOps() {
            return 0;
        }

        @Override
        public boolean isReadable() {
            return false;
        }

        @Override
        public boolean isWritable() {
            return false;
        }

        @Override
        public ChannelFuture setInterestOps(int interestOps) {
            return null;
        }

        @Override
        public ChannelFuture setReadable(boolean readable) {
            return null;
        }

        @Override
        public boolean getUserDefinedWritability(int index) {
            return false;
        }

        @Override
        public void setUserDefinedWritability(int index, boolean isWritable) {

        }

        @Override
        public Object getAttachment() {
            return null;
        }

        @Override
        public void setAttachment(Object attachment) {

        }

        @Override
        public int compareTo(Channel o) {
            return 0;
        }
    }
}
