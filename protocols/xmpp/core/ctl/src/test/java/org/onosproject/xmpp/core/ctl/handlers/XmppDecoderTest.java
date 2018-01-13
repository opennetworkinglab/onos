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

package org.onosproject.xmpp.core.ctl.handlers;

import com.google.common.collect.Lists;
import io.netty.channel.ChannelHandlerContext;
import org.codehaus.stax2.ri.evt.AttributeEventImpl;
import org.codehaus.stax2.ri.evt.NamespaceEventImpl;
import org.codehaus.stax2.ri.evt.StartElementEventImpl;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.xmpp.core.ctl.exception.UnsupportedStanzaTypeException;
import org.onosproject.xmpp.core.stream.XmppStreamOpen;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test class for XmppDecoder testing.
 */
public class XmppDecoderTest {

    XmppDecoder xmppDecoder;
    Element xmppStanzaElement;
    XMLEvent streamOpen;

    Element iqElement = new IQ().getElement();
    Element messageElement = new Message().getElement();
    Element presenceElement = new Presence().getElement();

    ChannelHandlerContext mockChannelHandlerContext;
    Location mockLocation;

    @Before
    public void setUp() {
        xmppDecoder = new XmppDecoder();
        mockChannelHandlerContext = EasyMock.createMock(ChannelHandlerContext.class);
        mockLocation = EasyMock.createMock(Location.class);
        buildXmppStanza();
        buildXmppStreamOpen();

    }

    private void buildXmppStreamOpen() {
        QName qName = new QName("http://etherx.jabber.org/streams", "stream", "stream");
        Attribute attrTo = new AttributeEventImpl(mockLocation, QName.valueOf("to"), "xmpp.onosproject.org", true);
        Attribute attrFrom = new AttributeEventImpl(mockLocation, QName.valueOf("from"), "test@xmpp.org", true);
        List<Attribute> attributes = Lists.newArrayList();
        attributes.add(attrTo);
        attributes.add(attrFrom);
        Namespace streamNs = NamespaceEventImpl.constructNamespace(mockLocation, "stream",
                                                                   "http://etherx.jabber.org/streams");
        Namespace jabberNs = NamespaceEventImpl.constructDefaultNamespace(mockLocation, "jabber:client");
        List<Namespace> namespaces = Lists.newArrayList();
        namespaces.add(streamNs);
        namespaces.add(jabberNs);
        streamOpen = StartElementEventImpl.construct(mockLocation, qName, attributes.iterator(),
                                                     namespaces.iterator(), null);
    }


    private void buildXmppStanza() {
        xmppStanzaElement = new DefaultElement("iq");
        xmppStanzaElement.addAttribute("type", "set");
        xmppStanzaElement.addAttribute("from", "test@xmpp.org");
        xmppStanzaElement.addAttribute("to", "xmpp.onosproject.org");
        Element pubsub = new DefaultElement("pubsub",
                                            new org.dom4j.Namespace("", "http://jabber.org/protocol/pubsub"));
        Element subscribe = new DefaultElement("subscribe");
        subscribe.addAttribute("node", "test");
        pubsub.add(subscribe);
        xmppStanzaElement.add(pubsub);
    }

    @Test
    public void testDecodeStream() throws Exception {
        List<Object> out = Lists.newArrayList();
        xmppDecoder.decode(mockChannelHandlerContext, streamOpen, out);
        assertThat(out.size(), is(1));
        assertThat(out.get(0), is(instanceOf(XmppStreamOpen.class)));
        XmppStreamOpen stream = (XmppStreamOpen) out.get(0);
        assertThat(stream.getElement(), is(notNullValue()));
        assertThat(stream.getToJid(), is(new JID("xmpp.onosproject.org")));
        assertThat(stream.getFromJid(), is(new JID("test@xmpp.org")));
    }

    @Test
    public void testDecodeXmppStanza() throws Exception {
        // TODO: complete it
        List<Object> out = Lists.newArrayList();
        xmppDecoder.decode(mockChannelHandlerContext, xmppStanzaElement, out);
        assertThat(out.size(), is(1));
        assertThat(out.get(0), is(instanceOf(Packet.class)));
        assertThat(out.get(0), is(instanceOf(IQ.class)));
        IQ iq = (IQ) out.get(0);
        assertThat(iq.getElement(), is(notNullValue()));
        assertThat(iq.getFrom(), is(new JID("test@xmpp.org")));
        assertThat(iq.getTo(), is(new JID("xmpp.onosproject.org")));
        assertThat(iq.getType(), is(IQ.Type.set));
    }

    @Test
    public void testRecognizePacket() throws Exception {
        Packet iqPacket = xmppDecoder.recognizeAndReturnXmppPacket(iqElement);
        assertThat(iqPacket, is(instanceOf(IQ.class)));
        Packet messagePacket = xmppDecoder.recognizeAndReturnXmppPacket(messageElement);
        assertThat(messagePacket, is(instanceOf(Message.class)));
        Packet presencePacket = xmppDecoder.recognizeAndReturnXmppPacket(presenceElement);
        assertThat(presencePacket, is(instanceOf(Presence.class)));
        Element wrongElement = new DefaultElement("test");
        try {
            xmppDecoder.recognizeAndReturnXmppPacket(wrongElement);
        } catch (Exception e) {
            assertThat(e, is(instanceOf(UnsupportedStanzaTypeException.class)));
        }
    }

}
