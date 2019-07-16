/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.openflow.ChannelHandlerContextAdapter;
import org.onosproject.openflow.MockOfPortStatus;
import org.onosproject.openflow.OFDescStatsReplyAdapter;
import org.onosproject.openflow.OpenflowSwitchDriverAdapter;
import org.onosproject.openflow.controller.Dpid;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.onosproject.openflow.controller.impl.OFChannelHandler.ChannelState.ACTIVE;
import static org.onosproject.openflow.controller.impl.OFChannelHandler.ChannelState.WAIT_DESCRIPTION_STAT_REPLY;
import static org.onosproject.openflow.controller.impl.OFChannelHandler.ChannelState.WAIT_SWITCH_DRIVER_SUB_HANDSHAKE;
import static org.projectfloodlight.openflow.protocol.OFVersion.OF_13;

/**
 * Unit tests for the OpenFlow channel handler.
 */
public class OFChannelHandlerTest {

    private Controller controller;
    private OFChannelHandler channelHandler;
    private ChannelHandlerContext channelHandlerContext;

    @Before
    public void setUp() {
        controller = createMock(Controller.class);
        for (int i = 0; i < OFChannelHandler.NUM_OF_QUEUES; i++) {
            expect(controller.getQueueSize(i)).andReturn(0);
        }
        replay(controller);
        channelHandler = new OFChannelHandler(controller);
        channelHandler.ofVersion = OF_13;
        channelHandlerContext = new ChannelHandlerContextAdapter();
        verify(controller);
        reset(controller);
    }

    // Normal workflow - connect
    @Test
    public void testActiveDpid() {
        // Expected behavior
        OFDescStatsReply reply = new OFDescStatsReplyAdapter();
        expect(controller.getOFSwitchInstance(0, reply, OF_13)).andReturn(
                new OpenflowSwitchDriverAdapter(ImmutableSet.of(), Dpid.dpid(Dpid.uri(0)), true));
        replay(controller);

        try {
            channelHandler.channelActive(channelHandlerContext);
            channelHandler.setState(WAIT_DESCRIPTION_STAT_REPLY);
            channelHandler.channelRead(channelHandlerContext, reply);
        } catch (Exception e) {
            channelHandler = null;
        }
        // exception should not be fired
        assertNotNull(channelHandler);
        assertThat(channelHandler.getStateForTesting(), is(ACTIVE));

        // Finally verify
        verify(controller);
    }

    // Normal workflow - duplicate Dpid
    @Test
    public void testDuplicateDpid() {
        // Expected behavior
        OFDescStatsReply reply = new OFDescStatsReplyAdapter();
        expect(controller.getOFSwitchInstance(0, reply, OF_13)).andReturn(new OpenflowSwitchDriverAdapter(
                ImmutableSet.of(Dpid.dpid(Dpid.uri(0))), Dpid.dpid(Dpid.uri(0)), true));
        replay(controller);

        try {
            channelHandler.channelActive(channelHandlerContext);
            channelHandler.setState(WAIT_DESCRIPTION_STAT_REPLY);
            channelHandler.channelRead(channelHandlerContext, reply);
        } catch (Exception e) {
            channelHandler = null;
        }
        // exception should not be fired
        assertNotNull(channelHandler);
        assertThat(channelHandler.getStateForTesting(), is(WAIT_DESCRIPTION_STAT_REPLY));

        // Finally verify
        verify(controller);
    }

    // Through subhandshake
    @Test
    public void testActiveDpidSub() {
        // Expected behavior
        OFDescStatsReply reply = new OFDescStatsReplyAdapter();
        expect(controller.getOFSwitchInstance(0, reply, OF_13)).andReturn(new OpenflowSwitchDriverAdapter(
                ImmutableSet.of(), Dpid.dpid(Dpid.uri(0)), false));
        replay(controller);

        try {
            channelHandler.channelActive(channelHandlerContext);
            channelHandler.setState(WAIT_DESCRIPTION_STAT_REPLY);
            channelHandler.channelRead(channelHandlerContext, reply);
            channelHandler.channelRead(channelHandlerContext, new MockOfPortStatus());
        } catch (Exception e) {
            channelHandler = null;
        }
        // exception should not be fired
        assertNotNull(channelHandler);
        assertThat(channelHandler.getStateForTesting(), is(ACTIVE));

        // Finally verify
        verify(controller);
    }

    // Through subhandshake - duplicate dpid
    @Test
    public void testDuplicateDpidSub() {
        // Expected behavior
        OFDescStatsReply reply = new OFDescStatsReplyAdapter();
        expect(controller.getOFSwitchInstance(0, reply, OF_13)).andReturn(new OpenflowSwitchDriverAdapter(
                ImmutableSet.of(Dpid.dpid(Dpid.uri(0))), Dpid.dpid(Dpid.uri(0)), false));
        replay(controller);

        try {
            channelHandler.channelActive(channelHandlerContext);
            channelHandler.setState(WAIT_DESCRIPTION_STAT_REPLY);
            channelHandler.channelRead(channelHandlerContext, reply);
            channelHandler.channelRead(channelHandlerContext, new MockOfPortStatus());
        } catch (Exception e) {
            channelHandler = null;
        }
        // exception should not be fired
        assertNotNull(channelHandler);
        assertThat(channelHandler.getStateForTesting(), is(WAIT_SWITCH_DRIVER_SUB_HANDSHAKE));

        // Finally verify
        verify(controller);
    }

}
