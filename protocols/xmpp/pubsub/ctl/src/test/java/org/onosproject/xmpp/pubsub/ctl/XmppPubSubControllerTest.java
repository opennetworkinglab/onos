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

package org.onosproject.xmpp.pubsub.ctl;

import com.google.common.collect.Lists;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.tree.DefaultElement;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.xmpp.core.XmppController;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppDeviceId;
import org.onosproject.xmpp.core.XmppDeviceListener;
import org.onosproject.xmpp.core.XmppIqListener;
import org.onosproject.xmpp.core.XmppMessageListener;
import org.onosproject.xmpp.core.XmppPresenceListener;
import org.onosproject.xmpp.core.XmppSession;
import org.onosproject.xmpp.pubsub.XmppPubSubConstants;
import org.onosproject.xmpp.pubsub.XmppPublishEventsListener;
import org.onosproject.xmpp.pubsub.XmppSubscribeEventsListener;
import org.onosproject.xmpp.pubsub.model.XmppEventNotification;
import org.onosproject.xmpp.pubsub.model.XmppPubSubError;
import org.onosproject.xmpp.pubsub.model.XmppPublish;
import org.onosproject.xmpp.pubsub.model.XmppRetract;
import org.onosproject.xmpp.pubsub.model.XmppSubscribe;
import org.onosproject.xmpp.pubsub.model.XmppUnsubscribe;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.net.InetSocketAddress;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.onosproject.xmpp.pubsub.XmppPubSubConstants.PubSubApplicationCondition.ITEM_NOT_FOUND;

/**
 * Test class for XmppPubSubController class.
 */
public class XmppPubSubControllerTest {

    private String nodeAttribute = "test";
    private String node = "node";
    private String toJid = "xmpp@onosproject.org";
    private String fromJid = "test@xmpp.org";
    private String pubSub = "pubsub";
    private String publish = "publish";
    private String retract = "retract";
    private String subscribe = "subscribe";
    private String unsubscribe = "unsubscribe";
    private String item = "item";
    private String id = "id";
    private String itemId = "id-000";
    private String entry = "entry";
    private String testNamespace = "jabber:test:item";


    XmppPubSubControllerImpl pubSubController;
    XmppControllerAdapter xmppControllerAdapter;
    XmppDeviceAdapter testDevice;

    TestXmppPublishEventsListener testXmppPublishEventsListener;
    TestXmppSubscribeEventsListener testXmppSubscribeEventsListener;

    static class TestXmppPublishEventsListener implements XmppPublishEventsListener {

        final List<XmppPublish> handledPublishMsgs = Lists.newArrayList();
        final List<XmppRetract> handledRetractMsgs = Lists.newArrayList();

        @Override
        public void handlePublish(XmppPublish publishEvent) {
            handledPublishMsgs.add(publishEvent);
        }

        @Override
        public void handleRetract(XmppRetract retractEvent) {
            handledRetractMsgs.add(retractEvent);
        }
    }

    static class TestXmppSubscribeEventsListener implements XmppSubscribeEventsListener {

        final List<XmppSubscribe> handledSubscribeMsgs = Lists.newArrayList();
        final List<XmppUnsubscribe> handledUnsubscribeMsgs = Lists.newArrayList();


        @Override
        public void handleSubscribe(XmppSubscribe subscribeEvent) {
            handledSubscribeMsgs.add(subscribeEvent);
        }

        @Override
        public void handleUnsubscribe(XmppUnsubscribe unsubscribeEvent) {
            handledUnsubscribeMsgs.add(unsubscribeEvent);
        }
    }

    @Before
    public void setUp() {
        testDevice = new XmppDeviceAdapter();
        xmppControllerAdapter = new XmppControllerAdapter();
        pubSubController = new XmppPubSubControllerImpl();
        pubSubController.xmppController = xmppControllerAdapter;
        testXmppPublishEventsListener = new TestXmppPublishEventsListener();
        testXmppSubscribeEventsListener = new TestXmppSubscribeEventsListener();
        pubSubController.activate();
    }

    @Test
    public void testActivate() {
        assertThat(xmppControllerAdapter.iqListener, is(notNullValue()));
    }

    @Test
    public void testDeactivate() {
        pubSubController.deactivate();
        assertThat(xmppControllerAdapter.iqListener, is(nullValue()));
    }

    @Test
    public void testAddRemoveListeners() {
        pubSubController.addXmppPublishEventsListener(testXmppPublishEventsListener);
        assertThat(pubSubController.xmppPublishEventsListeners.size(), is(1));
        pubSubController.addXmppSubscribeEventsListener(testXmppSubscribeEventsListener);
        assertThat(pubSubController.xmppSubscribeEventsListeners.size(), is(1));
        pubSubController.removeXmppPublishEventsListener(testXmppPublishEventsListener);
        assertThat(pubSubController.xmppPublishEventsListeners.size(), is(0));
        pubSubController.removeXmppSubscribeEventsListener(testXmppSubscribeEventsListener);
        assertThat(pubSubController.xmppSubscribeEventsListeners.size(), is(0));
    }

    @Test
    public void testNotifyEvent() {
        XmppEventNotification eventNotification = new XmppEventNotification(nodeAttribute,
                                                                            new DefaultElement(nodeAttribute));
        pubSubController.notify(DeviceId.NONE, eventNotification);
        assertThat(testDevice.sentPackets.size(), is(1));
        assertThat(testDevice.sentPackets.get(0), is(eventNotification));
    }

    @Test
    public void testNotifyError() {
        XmppPubSubError xmppPubSubError =
                new XmppPubSubError(ITEM_NOT_FOUND);
        pubSubController.notifyError(DeviceId.NONE, xmppPubSubError);
        assertThat(testDevice.sentErrors.size(), is(1));
    }

    @Test
    public void testHandlePubSubMessages() {
        pubSubController.addXmppPublishEventsListener(testXmppPublishEventsListener);
        pubSubController.addXmppSubscribeEventsListener(testXmppSubscribeEventsListener);
        XmppSubscribe xmppSubscribe = buildXmppSubscribe();
        xmppControllerAdapter.iqListener.handleIqStanza(xmppSubscribe);
        assertThat(testXmppSubscribeEventsListener.handledSubscribeMsgs.size(), is(1));
        XmppUnsubscribe xmppUnsubscribe = buildXmppUnsubscribe();
        xmppControllerAdapter.iqListener.handleIqStanza(xmppUnsubscribe);
        assertThat(testXmppSubscribeEventsListener.handledUnsubscribeMsgs.size(), is(1));
        XmppPublish xmppPublish = buildXmppPublish();
        xmppControllerAdapter.iqListener.handleIqStanza(xmppPublish);
        assertThat(testXmppPublishEventsListener.handledPublishMsgs.size(), is(1));
        XmppRetract xmppRetract = buildXmppRetract();
        xmppControllerAdapter.iqListener.handleIqStanza(xmppRetract);
        assertThat(testXmppPublishEventsListener.handledRetractMsgs.size(), is(1));
    }

    private XmppSubscribe buildXmppSubscribe() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(toJid);
        iq.setFrom(fromJid);
        Element element = new DefaultElement(pubSub, Namespace.get(XmppPubSubConstants.PUBSUB_NAMESPACE));
        Element childElement = new DefaultElement(subscribe);
        childElement.addAttribute(node, nodeAttribute);
        element.add(childElement);
        iq.setChildElement(element);
        XmppSubscribe xmppSubscribe = new XmppSubscribe(iq);
        return xmppSubscribe;
    }

    private XmppUnsubscribe buildXmppUnsubscribe() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(toJid);
        iq.setFrom(fromJid);
        Element element = new DefaultElement(pubSub, Namespace.get(XmppPubSubConstants.PUBSUB_NAMESPACE));
        Element childElement = new DefaultElement(unsubscribe);
        childElement.addAttribute(node, nodeAttribute);
        element.add(childElement);
        iq.setChildElement(element);
        XmppUnsubscribe xmppUnsubscribe = new XmppUnsubscribe(iq);
        return xmppUnsubscribe;
    }

    private XmppPublish buildXmppPublish() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(toJid);
        iq.setFrom(fromJid);
        Element element = new DefaultElement(pubSub, Namespace.get(XmppPubSubConstants.PUBSUB_NAMESPACE));
        Element publishElement = new DefaultElement(publish).addAttribute(node, nodeAttribute);
        Element itemElement = new DefaultElement(item).addAttribute(id, itemId);
        Element entryElement = new DefaultElement(entry, Namespace.get(testNamespace));
        itemElement.add(entryElement);
        publishElement.add(itemElement);
        element.add(publishElement);
        iq.setChildElement(element);
        XmppPublish xmppPublish = new XmppPublish(iq);
        return xmppPublish;
    }

    private XmppRetract buildXmppRetract() {
        IQ iq = new IQ(IQ.Type.set);
        iq.setTo(toJid);
        iq.setFrom(fromJid);
        Element element = new DefaultElement(pubSub, Namespace.get(XmppPubSubConstants.PUBSUB_NAMESPACE));
        Element retractElement = new DefaultElement(retract).addAttribute(node, nodeAttribute);
        Element itemElement = new DefaultElement(item).addAttribute(id, itemId);
        retractElement.add(itemElement);
        element.add(retractElement);
        iq.setChildElement(element);
        XmppRetract xmppRetract = new XmppRetract(iq);
        return xmppRetract;
    }


    private class XmppControllerAdapter implements XmppController {

        XmppIqListener iqListener;

        @Override
        public XmppDevice getDevice(XmppDeviceId xmppDeviceId) {
            return testDevice;
        }

        @Override
        public void addXmppDeviceListener(XmppDeviceListener deviceListener) {

        }

        @Override
        public void removeXmppDeviceListener(XmppDeviceListener deviceListener) {

        }

        @Override
        public void addXmppIqListener(XmppIqListener iqListener, String namespace) {
            this.iqListener = iqListener;
        }

        @Override
        public void removeXmppIqListener(XmppIqListener iqListener, String namespace) {
            this.iqListener = null;
        }

        @Override
        public void addXmppMessageListener(XmppMessageListener messageListener) {

        }

        @Override
        public void removeXmppMessageListener(XmppMessageListener messageListener) {

        }

        @Override
        public void addXmppPresenceListener(XmppPresenceListener presenceListener) {

        }

        @Override
        public void removeXmppPresenceListener(XmppPresenceListener presenceListener) {

        }
    }

    private class XmppDeviceAdapter implements XmppDevice {

        final List<Packet> sentPackets = Lists.newArrayList();
        final List<PacketError> sentErrors = Lists.newArrayList();

        @Override
        public XmppSession getSession() {
            return null;
        }

        @Override
        public InetSocketAddress getIpAddress() {
            return null;
        }

        @Override
        public void registerConnectedDevice() {

        }

        @Override
        public void disconnectDevice() {

        }

        @Override
        public void sendPacket(Packet packet) {
            sentPackets.add(packet);
        }

        @Override
        public void writeRawXml(Document document) {

        }

        @Override
        public void handlePacket(Packet packet) {

        }

        @Override
        public void sendError(PacketError packetError) {
            sentErrors.add(packetError);
        }

    }

}
