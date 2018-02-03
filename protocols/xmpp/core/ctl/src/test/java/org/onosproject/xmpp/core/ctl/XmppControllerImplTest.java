/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core.ctl;

import java.util.List;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import com.google.common.collect.ImmutableSet;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.tree.DefaultElement;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppDeviceId;
import org.onosproject.xmpp.core.XmppDeviceListener;
import org.onosproject.xmpp.core.XmppIqListener;
import org.onosproject.xmpp.core.XmppMessageListener;
import org.onosproject.xmpp.core.XmppPresenceListener;
import org.onosproject.xmpp.core.XmppDeviceAgent;
import org.osgi.service.component.ComponentContext;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;


/**
 * Test class for XmppControllerImpl.
 */
public class XmppControllerImplTest {

    XmppControllerImpl controller;
    XmppDeviceAgent agent;
    TestXmppDeviceListener testXmppDeviceListener;
    TestXmppIqListener testXmppIqListener;
    TestXmppMessageListener testXmppMessageListener;
    TestXmppPresenceListener testXmppPresenceListener;

    XmppDevice device1;
    XmppDeviceId jid1;
    XmppDevice device2;
    XmppDeviceId jid2;
    XmppDevice device3;
    XmppDeviceId jid3;

    final String testNamespace = "testns";

    /**
     * Test harness for a device listener.
     */
    static class TestXmppDeviceListener implements XmppDeviceListener {
        final List<XmppDeviceId> removedDevices = new ArrayList<>();
        final List<XmppDeviceId> addedDevices = new ArrayList<>();

        @Override
        public void deviceConnected(XmppDeviceId deviceId) {
            addedDevices.add(deviceId);
        }

        @Override
        public void deviceDisconnected(XmppDeviceId deviceId) {
            removedDevices.add(deviceId);
        }
    }

    static class TestXmppIqListener implements XmppIqListener {
        final List<IQ> handledIqs = new ArrayList<>();

        @Override
        public void handleIqStanza(IQ iq) {
            handledIqs.add(iq);
        }

    }

    static class TestXmppMessageListener implements XmppMessageListener {
        final List<Message> handledMessages = new ArrayList<>();

        @Override
        public void handleMessageStanza(Message message) {
            handledMessages.add(message);
        }
    }

    static class TestXmppPresenceListener implements XmppPresenceListener {
        final List<Presence> handledPresenceStanzas = new ArrayList<>();

        @Override
        public void handlePresenceStanza(Presence presence) {
            handledPresenceStanzas.add(presence);
        }
    }

    /**
     * Sets up devices to use as data, mocks and launches a controller instance.
     */
    @Before
    public void setUp() {
        device1 = new XmppDeviceAdapter();
        jid1 = new XmppDeviceId(new JID("agent1@testxmpp.org"));
        device2 = new XmppDeviceAdapter();
        jid2 = new XmppDeviceId(new JID("agent2@testxmpp.org"));
        device3 = new XmppDeviceAdapter();
        jid3 = new XmppDeviceId(new JID("agent3@testxmpp.org"));

        controller = new XmppControllerImpl();
        agent = controller.agent;

        testXmppDeviceListener = new TestXmppDeviceListener();
        controller.addXmppDeviceListener(testXmppDeviceListener);
        testXmppIqListener = new TestXmppIqListener();
        controller.addXmppIqListener(testXmppIqListener, testNamespace);
        testXmppMessageListener = new TestXmppMessageListener();
        controller.addXmppMessageListener(testXmppMessageListener);
        testXmppPresenceListener = new TestXmppPresenceListener();
        controller.addXmppPresenceListener(testXmppPresenceListener);

        CoreService mockCoreService =
                EasyMock.createMock(CoreService.class);
        controller.coreService = mockCoreService;

        ComponentConfigService mockCfgService =
                EasyMock.createMock(ComponentConfigService.class);
        expect(mockCfgService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        mockCfgService.registerProperties(controller.getClass());
        expectLastCall();
        mockCfgService.unregisterProperties(controller.getClass(), false);
        expectLastCall();
        expect(mockCfgService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        controller.cfgService = mockCfgService;
        replay(mockCfgService);

        ComponentContext mockContext = EasyMock.createMock(ComponentContext.class);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("xmppPort",
                       "5269");
        expect(mockContext.getProperties()).andReturn(properties);
        replay(mockContext);
        controller.activate(mockContext);
    }

    @After
    public void tearDown() {
        controller.removeXmppDeviceListener(testXmppDeviceListener);
        controller.removeXmppIqListener(testXmppIqListener, testNamespace);
        controller.removeXmppMessageListener(testXmppMessageListener);
        controller.removeXmppPresenceListener(testXmppPresenceListener);
        controller.deactivate();
    }

    /**
     * Tests adding and removing connected devices.
     */
    @Test
    public void testAddRemoveConnectedDevice() {
        // test adding connected devices
        boolean add1 = agent.addConnectedDevice(jid1, device1);
        assertThat(add1, is(true));
        assertThat(testXmppDeviceListener.addedDevices, hasSize(1));
        boolean add2 = agent.addConnectedDevice(jid2, device2);
        assertThat(add2, is(true));
        assertThat(testXmppDeviceListener.addedDevices, hasSize(2));
        boolean add3 = agent.addConnectedDevice(jid3, device3);
        assertThat(add3, is(true));
        assertThat(testXmppDeviceListener.addedDevices, hasSize(3));

        assertThat(testXmppDeviceListener.addedDevices, hasItems(jid1, jid2, jid3));

        // Test adding a device twice - it should fail
        boolean addError1 = agent.addConnectedDevice(jid1, device1);
        assertThat(addError1, is(false));

        assertThat(controller.connectedDevices.size(), is(3));

        // test querying the individual device
        XmppDevice queriedDevice = controller.getDevice(jid1);
        assertThat(queriedDevice, is(device1));

        // test removing device
        agent.removeConnectedDevice(jid3);
        assertThat(controller.connectedDevices.size(), is(2));

        // Make sure the listener delete callbacks fired
        assertThat(testXmppDeviceListener.removedDevices, hasSize(1));
        assertThat(testXmppDeviceListener.removedDevices, hasItems(jid3));
    }

    /**
     * Tests adding, removing IQ listeners and handling IQ stanzas.
     */
    @Test
    public void handlePackets() {
        // IQ packets
        IQ iq = new IQ();
        Element element = new DefaultElement("pubsub", Namespace.get(testNamespace));
        iq.setChildElement(element);
        agent.processUpstreamEvent(jid1, iq);
        assertThat(testXmppIqListener.handledIqs, hasSize(1));
        agent.processUpstreamEvent(jid2, iq);
        assertThat(testXmppIqListener.handledIqs, hasSize(2));
        // Message packets
        Packet message = new Message();
        agent.processUpstreamEvent(jid1, message);
        assertThat(testXmppMessageListener.handledMessages, hasSize(1));
        agent.processUpstreamEvent(jid2, message);
        assertThat(testXmppMessageListener.handledMessages, hasSize(2));
        Packet presence = new Presence();
        agent.processUpstreamEvent(jid1, presence);
        assertThat(testXmppPresenceListener.handledPresenceStanzas, hasSize(1));
        agent.processUpstreamEvent(jid2, presence);
        assertThat(testXmppPresenceListener.handledPresenceStanzas, hasSize(2));
    }


}
