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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.openflow.OpenflowSwitchDriverAdapter;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.OpenFlowService;
import org.osgi.service.component.ComponentContext;
import org.projectfloodlight.openflow.protocol.OFPortStatus;

import com.google.common.collect.ImmutableSet;

import static junit.framework.TestCase.fail;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for the open flow controller implementation test.
 */
public class OpenFlowControllerImplTest {

    /**
     * Let system pick ephemeral port.
     *
     * @see InetSocketAddress#InetSocketAddress(int)
     */
    private static final int EPHEMERAL_PORT = 0;

    OpenFlowSwitch switch1;
    Dpid dpid1;
    OpenFlowSwitch switch2;
    Dpid dpid2;
    OpenFlowSwitch switch3;
    Dpid dpid3;

    OpenFlowControllerImpl controller;
    OpenFlowControllerImpl.OpenFlowSwitchAgent agent;
    TestSwitchListener switchListener;

    /**
     * Test harness for a switch listener.
     */
    static class TestSwitchListener implements OpenFlowSwitchListener {
        final List<Dpid> removedDpids = new ArrayList<>();
        final List<Dpid> addedDpids = new ArrayList<>();
        final List<Dpid> changedDpids = new ArrayList<>();

        @Override
        public void switchAdded(Dpid dpid) {
            addedDpids.add(dpid);
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            removedDpids.add(dpid);
        }

        @Override
        public void switchChanged(Dpid dpid) {
            changedDpids.add(dpid);
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            // Stub
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
            // Stub
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
            switch2 = new OpenflowSwitchDriverAdapter();
            dpid2 = Dpid.dpid(new URI("of:0000000000000222"));
            switch3 = new OpenflowSwitchDriverAdapter();
            dpid3 = Dpid.dpid(new URI("of:0000000000000333"));
        } catch (URISyntaxException ex) {
            //  Does not happen
            fail();
        }

        controller = new OpenFlowControllerImpl();
        agent = controller.agent;

        switchListener = new TestSwitchListener();
        controller.addListener(switchListener);

        CoreService mockCoreService =
                EasyMock.createMock(CoreService.class);
        controller.coreService = mockCoreService;

        OpenFlowService mockOpenFlowService =
                EasyMock.createMock(OpenFlowService.class);
        controller.openFlowManager = mockOpenFlowService;

        ComponentConfigService mockConfigService =
                EasyMock.createMock(ComponentConfigService.class);
        expect(mockConfigService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        mockConfigService.registerProperties(controller.getClass());
        expectLastCall();
        mockConfigService.unregisterProperties(controller.getClass(), false);
        expectLastCall();
        expect(mockConfigService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        controller.cfgService = mockConfigService;
        replay(mockConfigService);

        NetworkConfigRegistry netConfigService = EasyMock.createMock(NetworkConfigRegistry.class);
        controller.netCfgService = netConfigService;

        ComponentContext mockContext = EasyMock.createMock(ComponentContext.class);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("openflowPorts",
                       Integer.toString(EPHEMERAL_PORT));
        expect(mockContext.getProperties()).andReturn(properties);
        replay(mockContext);
        controller.activate(mockContext);
    }

    @After
    public void tearDown() {
        controller.removeListener(switchListener);
        controller.deactivate();
    }

    /**
     * Converts an Iterable of some type into a stream of that type.
     *
     * @param items Iterable of objects
     * @param <T> type of the items in the iterable
     * @return stream of objects of type T
     */
    private <T> Stream<T> makeIntoStream(Iterable<T> items) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        items.iterator(), Spliterator.ORDERED), false);
    }


    /**
     * Tests adding and removing connected switches.
     */
    @Test
    public void testAddRemoveConnectedSwitch() {

        // test adding connected switches
        boolean addSwitch1 = agent.addConnectedSwitch(dpid1, switch1);
        assertThat(addSwitch1, is(true));
        boolean addSwitch2 = agent.addConnectedSwitch(dpid2, switch2);
        assertThat(addSwitch2, is(true));
        boolean addSwitch3 = agent.addConnectedSwitch(dpid3, switch3);
        assertThat(addSwitch3, is(true));

        // Make sure the listener add callbacks fired
        assertThat(switchListener.addedDpids, hasSize(3));
        assertThat(switchListener.addedDpids, hasItems(dpid1, dpid2, dpid3));

        // Test adding a switch twice - it should fail
        boolean addBadSwitch1 = agent.addConnectedSwitch(dpid1, switch1);
        assertThat(addBadSwitch1, is(false));

        assertThat(controller.connectedSwitches.size(), is(3));

        // test querying the switch list
        Stream<OpenFlowSwitch> fetchedSwitches =
                makeIntoStream(controller.getSwitches());
        long switchCount = fetchedSwitches.count();
        assertThat(switchCount, is(3L));

        // test querying the individual switch
        OpenFlowSwitch queriedSwitch = controller.getSwitch(dpid1);
        assertThat(queriedSwitch, is(switch1));

        // Remove a switch
        agent.removeConnectedSwitch(dpid3);
        Stream<OpenFlowSwitch> fetchedSwitchesAfterRemove =
                makeIntoStream(controller.getSwitches());
        long switchCountAfterRemove = fetchedSwitchesAfterRemove.count();
        assertThat(switchCountAfterRemove, is(2L));

        // Make sure the listener delete callbacks fired
        assertThat(switchListener.removedDpids, hasSize(1));
        assertThat(switchListener.removedDpids, hasItems(dpid3));

        // test querying the removed switch
        OpenFlowSwitch queriedSwitchAfterRemove = controller.getSwitch(dpid3);
        assertThat(queriedSwitchAfterRemove, nullValue());
    }

    /**
     * Tests adding master switches.
     */
    @Test
    public void testMasterSwitch() {
        agent.addConnectedSwitch(dpid1, switch1);
        agent.transitionToMasterSwitch(dpid1);

        Stream<OpenFlowSwitch> fetchedMasterSwitches =
                makeIntoStream(controller.getMasterSwitches());
        assertThat(fetchedMasterSwitches.count(), is(1L));
        Stream<OpenFlowSwitch> fetchedActivatedSwitches =
                makeIntoStream(controller.getEqualSwitches());
        assertThat(fetchedActivatedSwitches.count(), is(0L));
        OpenFlowSwitch fetchedSwitch1 = controller.getMasterSwitch(dpid1);
        assertThat(fetchedSwitch1, is(switch1));

        agent.addConnectedSwitch(dpid2, switch2);
        boolean addSwitch2 = agent.addActivatedMasterSwitch(dpid2, switch2);
        assertThat(addSwitch2, is(true));
        OpenFlowSwitch fetchedSwitch2 = controller.getMasterSwitch(dpid2);
        assertThat(fetchedSwitch2, is(switch2));
    }

    /**
     * Tests adding equal switches.
     */
    @Test
    public void testEqualSwitch() {
        agent.addConnectedSwitch(dpid1, switch1);
        agent.transitionToEqualSwitch(dpid1);

        Stream<OpenFlowSwitch> fetchedEqualSwitches =
                makeIntoStream(controller.getEqualSwitches());
        assertThat(fetchedEqualSwitches.count(), is(1L));
        Stream<OpenFlowSwitch> fetchedActivatedSwitches =
                makeIntoStream(controller.getMasterSwitches());
        assertThat(fetchedActivatedSwitches.count(), is(0L));
        OpenFlowSwitch fetchedSwitch1 = controller.getEqualSwitch(dpid1);
        assertThat(fetchedSwitch1, is(switch1));

        agent.addConnectedSwitch(dpid2, switch2);
        boolean addSwitch2 = agent.addActivatedEqualSwitch(dpid2, switch2);
        assertThat(addSwitch2, is(true));
        OpenFlowSwitch fetchedSwitch2 = controller.getEqualSwitch(dpid2);
        assertThat(fetchedSwitch2, is(switch2));
    }

    /**
     * Tests changing switch role.
     */
    @Test
    public void testRoleSetting() {
        agent.addConnectedSwitch(dpid2, switch2);

        // check that state can be changed for a connected switch
        assertThat(switch2.getRole(), is(RoleState.MASTER));
        controller.setRole(dpid2, RoleState.EQUAL);
        assertThat(switch2.getRole(), is(RoleState.EQUAL));

        // check that changing state on an unconnected switch does not crash
        controller.setRole(dpid3, RoleState.SLAVE);
    }
}
