/*
 * Copyright 2015 Open Networking Laboratory
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
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.netty.ChannelAdapter;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.projectfloodlight.openflow.protocol.OFMessage;

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
        Channel channel = new ChannelAdapter();
        ofSwitch.setChannel(channel);
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
}
