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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.apache.commons.io.Charsets;
import org.junit.Test;
import org.onosproject.xmpp.core.ctl.ChannelAdapter;
import org.onosproject.xmpp.core.ctl.ChannelHandlerContextAdapter;

import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Test class for XmlStreamDecoder.
 */
public class XmlStreamDecoderTest {

    private String streamOpenMsg = String.format("<stream:stream to='%s' %s %s %s %s %s>", "xmpp.onosproject.org",
                                                 "from='test@xmpp.org'",
                                                 "xmlns:stream='http://etherx.jabber.org/streams'",
                                                 "xmlns='jabber:client'", "xml:lang='en'", "version='1.0'");

    private String streamCloseMsg = "</stream:stream>";

    private String subscribeMsg = "<iq type='set'" +
            "    from='test@xmpp.org'" +
            "    to='xmpp.onosproject.org'" +
            "    id='sub1'>" +
            "    <pubsub xmlns='http://jabber.org/protocol/pubsub'>" +
            "        <subscribe node='test'/>" +
            "    </pubsub>" +
            "</iq>";


    public class ActiveChannelHandlerContextAdapter
            extends ChannelHandlerContextAdapter {
        @Override
        public Channel channel() {
            return new ChannelAdapter() {
                @Override
                public boolean isActive() {
                    return true;
                }
            };
        }
    }

    @Test
    public void testDecodeNoChannel() throws Exception {
        XmlStreamDecoder decoder = new XmlStreamDecoder();

        List<Object> list = Lists.newArrayList();
        decoder.decode(new ActiveChannelHandlerContextAdapter(),
                       Unpooled.buffer(), list);
        assertThat(list.size(), is(0));
    }

    @Test
    public void testDecodeStreamOpen() throws Exception {
        XmlStreamDecoder decoder = new XmlStreamDecoder();
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(streamOpenMsg.getBytes(Charsets.UTF_8));
        List<Object> list = Lists.newArrayList();
        decoder.decode(new ChannelHandlerContextAdapter(), buffer, list);
        list.forEach(object -> {
            assertThat(object, is(instanceOf(XMLEvent.class)));
        });
        assertThat(list.size(), is(2));
        assertThat(((XMLEvent) list.get(0)).isStartDocument(), is(true));
        ((XMLEvent) list.get(0)).isStartElement();
    }

    @Test
    public void testDecodeStreamClose() throws Exception {
        XmlStreamDecoder decoder = new XmlStreamDecoder();
        // open stream
        ByteBuf buffer1 = Unpooled.buffer();
        buffer1.writeBytes(streamOpenMsg.getBytes(Charsets.UTF_8));
        List<Object> list1 = Lists.newArrayList();
        decoder.decode(new ChannelHandlerContextAdapter(), buffer1, list1);

        // close stream
        ByteBuf buffer2 = Unpooled.buffer();
        buffer2.writeBytes(streamCloseMsg.getBytes(Charsets.UTF_8));
        List<Object> list2 = Lists.newArrayList();
        decoder.decode(new ChannelHandlerContextAdapter(), buffer2, list2);
        list2.forEach(object -> {
            assertThat(object, is(instanceOf(XMLEvent.class)));
        });
        assertThat(list2.size(), is(1));
        assertThat(((XMLEvent) list2.get(0)).isEndElement(), is(true));
    }

    @Test
    public void testDecodeXmppStanza() throws Exception {
        XmlStreamDecoder decoder = new XmlStreamDecoder();
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(subscribeMsg.getBytes(Charsets.UTF_8));
        List<Object> list = Lists.newArrayList();
        decoder.decode(new ChannelHandlerContextAdapter(), buffer, list);
        assertThat(list.size(), is(10));
        list.forEach(object -> {
            assertThat(object, is(instanceOf(XMLEvent.class)));
        });
        assertThat(((XMLEvent) list.get(0)).isStartDocument(), is(true));
        XMLEvent secondEvent = (XMLEvent) list.get(1);
        assertThat(secondEvent.isStartElement(), is(true));
        StartElement secondEventAsStartElement = (StartElement) secondEvent;
        assertThat(secondEventAsStartElement.getName().getLocalPart(), is("iq"));
        assertThat(Lists.newArrayList(secondEventAsStartElement.getAttributes()).size(), is(4));
        assertThat(secondEventAsStartElement.getAttributeByName(QName.valueOf("type")).getValue(), is("set"));
        assertThat(secondEventAsStartElement.getAttributeByName(QName.valueOf("from")).getValue(),
                   is("test@xmpp.org"));
        assertThat(secondEventAsStartElement.getAttributeByName(QName.valueOf("to")).getValue(),
                   is("xmpp.onosproject.org"));
        assertThat(secondEventAsStartElement.getAttributeByName(QName.valueOf("id")).getValue(),
                   is("sub1"));
        XMLEvent fourthEvent = (XMLEvent) list.get(3);
        assertThat(fourthEvent.isStartElement(), is(true));
        StartElement fourthEventAsStartElement = (StartElement) fourthEvent;
        assertThat(fourthEventAsStartElement.getName().getLocalPart(), is("pubsub"));
        assertThat(fourthEventAsStartElement.getNamespaceURI(""),
                   is("http://jabber.org/protocol/pubsub"));
        XMLEvent fifthEvent = (XMLEvent) list.get(5);
        assertThat(fifthEvent.isStartElement(), is(true));
        StartElement fifthEventAsStartElement = (StartElement) fifthEvent;
        assertThat(fifthEventAsStartElement.getName().getLocalPart(), is("subscribe"));
        assertThat(fifthEventAsStartElement.getAttributeByName(QName.valueOf("node")).getValue(), is("test"));
        XMLEvent sixthEvent = (XMLEvent) list.get(6);
        assertThat(sixthEvent.isEndElement(), is(true));
        EndElement sixthEventAsEndElement = (EndElement) sixthEvent;
        assertThat(sixthEventAsEndElement.getName().getLocalPart(), is("subscribe"));
        XMLEvent seventhEvent = (XMLEvent) list.get(8);
        assertThat(seventhEvent.isEndElement(), is(true));
        EndElement seventhEventAsEndElement = (EndElement) seventhEvent;
        assertThat(seventhEventAsEndElement.getName().getLocalPart(), is("pubsub"));
        XMLEvent eighthEvent = (XMLEvent) list.get(9);
        assertThat(eighthEvent.isEndElement(), is(true));
        EndElement eighthEventAsEndElement = (EndElement) eighthEvent;
        assertThat(eighthEventAsEndElement.getName().getLocalPart(), is("iq"));
    }
}
