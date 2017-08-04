/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.openflow.controller.impl;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.ExecutorServiceAdapter;
import org.onosproject.openflow.MockOfFeaturesReply;
import org.onosproject.openflow.MockOfPortStatus;
import org.onosproject.openflow.OfMessageAdapter;
import org.onosproject.openflow.OpenFlowSwitchListenerAdapter;
import org.onosproject.openflow.OpenflowSwitchDriverAdapter;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.PacketListener;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Tests for packet processing in the open flow controller impl class.
 */
public class OpenFlowControllerImplPacketsTest {
    OpenFlowControllerImpl controller;
    OpenFlowControllerImpl.OpenFlowSwitchAgent agent;
    Dpid dpid1;
    OpenFlowSwitch switch1;
    OpenFlowSwitchListenerAdapter switchListener;
    TestPacketListener packetListener;
    TestExecutorService statsExecutorService;
    TestExecutorService errorMsgExecutorService;
    /**
     * Mock packet listener that accumulates packets.
     */
    class TestPacketListener implements PacketListener {
        List<OpenFlowPacketContext> contexts = new ArrayList<>();

        @Override
        public void handlePacket(OpenFlowPacketContext pktCtx) {
            contexts.add(pktCtx);
        }

        List<OpenFlowPacketContext> contexts() {
            return contexts;
        }
    }

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
            OpenFlowControllerImpl.OFMessageHandler handler =
                    (OpenFlowControllerImpl.OFMessageHandler) task;
            submittedMessages.add(handler.msg);
        }
    }

    /**
     * Sets up switches to use as data, mocks and launches a controller instance.
     */
    @Before
    public void setUp() {
        try {
            switch1 = new OpenflowSwitchDriverAdapter();
            dpid1 = Dpid.dpid(new URI("of:0000000000000111"));
        } catch (URISyntaxException ex) {
            //  Does not happen
            fail();
        }

        controller = new OpenFlowControllerImpl();
        agent = controller.agent;
        switchListener = new OpenFlowSwitchListenerAdapter();
        controller.addListener(switchListener);

        packetListener = new TestPacketListener();
        controller.addPacketListener(100, packetListener);

        statsExecutorService = new TestExecutorService();
        errorMsgExecutorService = new TestExecutorService();

        controller.executorMsgs = statsExecutorService;
        controller.executorErrorMsgs = errorMsgExecutorService;
    }

    /**
     * Tests a port status operation.
     */
    @Test
    public void testPortStatus() {
        OFMessage portStatusPacket = new MockOfPortStatus();
        controller.processPacket(dpid1, portStatusPacket);
        assertThat(switchListener.portChangedDpids().size(), is(1));
        assertThat(switchListener.portChangedDpids().containsKey(dpid1),
                   is(true));
        assertThat(switchListener.portChangedDpids().get(dpid1),
                   equalTo(portStatusPacket));
    }

    /**
     * Tests a features reply operation.
     */
    @Test
    public void testFeaturesReply() {
        OFMessage ofFeaturesReplyPacket = new MockOfFeaturesReply();
        controller.processPacket(dpid1, ofFeaturesReplyPacket);
        assertThat(switchListener.changedDpids(), hasSize(1));
        assertThat(switchListener.changedDpids().get(0),
                   equalTo(dpid1));
    }

    /**
     * Tests an error operation.
     */
    @Test
    public void testError() {
        agent.addConnectedSwitch(dpid1, switch1);
        OfMessageAdapter errorPacket = new OfMessageAdapter(OFType.ERROR);
        controller.processPacket(dpid1, errorPacket);
        assertThat(errorMsgExecutorService.submittedMessages(), hasSize(1));
        assertThat(errorMsgExecutorService.submittedMessages().get(0), is(errorPacket));
    }
}
