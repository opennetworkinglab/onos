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

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.Lists;
import org.codehaus.stax2.ri.evt.Stax2EventAllocatorImpl;
import org.dom4j.Element;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.xmpp.core.ctl.ChannelHandlerContextAdapter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for XmlMerger class.
 */
public class XmlMergerTest {

    List<Object> streamOpenXmlEventList;
    List<Object> streamCloseXmlEventList;
    List<Object> subscribeMsgEventList;
    List<Object> publishMsgEventList;

    XmlMerger xmlMerger;

    private String streamOpenMsg = String.format("<stream:stream to='%s' %s %s %s %s %s>", "xmpp.onosproject.org",
                                                 "from='test@xmpp.org'",
                                                 "xmlns:stream='http://etherx.jabber.org/streams'",
                                                 "xmlns='jabber:client'", "xml:lang='en'", "version='1.0'");

    private String streamCloseMsg = "</stream:stream>";

    private String publishMsg = "<iq type='set'\n" +
            "       from='test@xmpp.org'\n" +
            "       to='xmpp.onosproject.org'\n" +
            "       id='request1'>\n" +
            "    <pubsub xmlns='http://jabber.org/protocol/pubsub'>\n" +
            "      <publish node='test'>\n" +
            "        <item id=\"test\">\n" +
            "          <entry xmlns='http://ietf.org/protocol/bgpvpn'>\n" +
            "            <nlri af='1'>10.0.0.1</nlri>\n" +
            "            <next-hop af='1'>169.1.1.1</next-hop>\n" +
            "            <version id='1'/>\n" +
            "            <label>10000</label>\n" +
            "          </entry>  \n" +
            "        </item>\n" +
            "      </publish>\n" +
            "    </pubsub>\n" +
            "</iq>\n";

    private String subscribeMsg = "<iq type='set'\n" +
            "    from='test@xmpp.org'\n" +
            "    to='xmpp.onosproject.org/other-peer'\n" +
            "    id='sub1'>\n" +
            "    <pubsub xmlns='http://jabber.org/protocol/pubsub'>\n" +
            "        <subscribe node='test'/>\n" +
            "    </pubsub>\n" +
            "</iq>";

    AsyncXMLStreamReader<AsyncByteArrayFeeder> streamReader =
            new InputFactoryImpl().createAsyncForByteArray();
    Stax2EventAllocatorImpl allocator = new Stax2EventAllocatorImpl();

    @Before
    public void setUp() throws Exception {
        xmlMerger = new XmlMerger();
        streamOpenXmlEventList = Lists.newArrayList();
        streamCloseXmlEventList = Lists.newArrayList();
        subscribeMsgEventList = Lists.newArrayList();
        publishMsgEventList = Lists.newArrayList();
        initXmlEventList(streamOpenXmlEventList, streamOpenMsg);
        initXmlEventList(subscribeMsgEventList, subscribeMsg);
        initXmlEventList(publishMsgEventList, publishMsg);
        initXmlEventList(streamCloseXmlEventList, streamCloseMsg);
        streamReader.closeCompletely();
    }

    private void initXmlEventList(List<Object> xmlEventList, String xmlMessage)
            throws XMLStreamException, UnsupportedEncodingException {
        AsyncByteArrayFeeder streamFeeder = streamReader.getInputFeeder();
        byte[] buffer = xmlMessage.getBytes("UTF-8");
        streamFeeder.feedInput(buffer, 0, buffer.length);
        while (streamReader.hasNext() && streamReader.next() != AsyncXMLStreamReader.EVENT_INCOMPLETE) {
            xmlEventList.add(allocator.allocate(streamReader));
        }
    }

    @Test
    public void testInit() throws Exception {
        assertThat(xmlMerger.docBuilder, is(notNullValue()));
        assertThat(xmlMerger.document, is(notNullValue()));
        assertThat(xmlMerger.result, is(notNullValue()));
        assertThat(xmlMerger.writer, is(notNullValue()));
        assertThat(xmlMerger.xmlOutputFactory, is(notNullValue()));
    }

    @Test
    public void testMergeStreamOpen() throws Exception {
        List<Object> list = Lists.newArrayList();
        streamOpenXmlEventList.forEach(xmlEvent -> {
            try {
                xmlMerger.decode(new ChannelHandlerContextAdapter(), xmlEvent, list);
            } catch (Exception e) {
                fail();
            }
        });
        // StreamOpen should not be merged, should be passed as XMLEvent
        assertThat(list.size(), Matchers.is(1));
        assertThat(list.get(0), Matchers.is(instanceOf(XMLEvent.class)));
        assertThat(((XMLEvent) list.get(0)).isStartElement(), Matchers.is(true));
    }

    @Test
    public void testMergeSubscribeMsg() throws Exception {
        List<Object> list = Lists.newArrayList();
        xmlMerger.depth = 1;
        subscribeMsgEventList.forEach(xmlEvent -> {
            try {
                xmlMerger.decode(new ChannelHandlerContextAdapter(), xmlEvent, list);
            } catch (Exception e) {
                fail();
            }
        });
        assertThat("Output list should have size of 1", list.size(), Matchers.is(1));
        assertThat("Output object should be of type org.dom4j.Element",
                   list.get(0), Matchers.is(instanceOf(Element.class)));
        Element root = (Element) list.get(0);
        assertThat("Top level element should be of type IQ",
                   root.getQName().getName(), Matchers.is("iq"));
        assertThat(root.attributes().size(), Matchers.is(4));
        assertThat(root.attribute("type").getValue(), Matchers.is("set"));
        assertNotNull("<pubsub> element should be accessible", root.element("pubsub"));
        assertThat(root.element("pubsub").getNamespaceURI(), Matchers.is("http://jabber.org/protocol/pubsub"));
        assertNotNull("<subscribe> element should be accessible",
                      root.element("pubsub").element("subscribe"));

    }

    @Test
    public void testMergePublishMsg() throws Exception {
        List<Object> list = Lists.newArrayList();
        xmlMerger.depth = 1;
        publishMsgEventList.forEach(xmlEvent -> {
            try {
                xmlMerger.decode(new ChannelHandlerContextAdapter(), xmlEvent, list);
            } catch (Exception e) {
                fail();
            }
        });
        Element root = (Element) list.get(0);
        assertThat("Top level element should be of type IQ",
                   root.getQName().getName(), Matchers.is("iq"));
        assertThat(root.attributes().size(), Matchers.is(4));
        assertNotNull("<pubsub> element should be accessible", root.element("pubsub"));
        assertNotNull("<publish> element should be accessible",
                      root.element("pubsub").element("publish"));
        assertThat(root.element("pubsub").getNamespaceURI(), Matchers.is("http://jabber.org/protocol/pubsub"));
        assertThat(root.element("pubsub").element("publish").attribute("node").getValue(),
                   Matchers.is("test"));
    }

    @Test
    public void testMergeStreamClose() throws Exception {
        List<Object> list = Lists.newArrayList();
        streamCloseXmlEventList.forEach(xmlEvent -> {
            try {
                xmlMerger.decode(new ChannelHandlerContextAdapter(), xmlEvent, list);
            } catch (Exception e) {
                fail();
            }
        });
        // StreamClose should not be merged, should be passed as XMLEvent
        assertThat(list.size(), Matchers.is(1));
        assertThat(list.get(0), Matchers.is(instanceOf(XMLEvent.class)));
        assertThat(((XMLEvent) list.get(0)).isEndElement(), Matchers.is(true));

    }





}
