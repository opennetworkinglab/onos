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
package org.onosproject.bgpio.protocol;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.protocol.ver4.BgpPathAttributes;
import org.onosproject.bgpio.types.As4Path;
import org.onosproject.bgpio.types.AsPath;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.BgpLSIdentifierTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.IPReachabilityInformationTlv;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.IsIsPseudonode;
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.types.LocalPref;
import org.onosproject.bgpio.types.Med;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.onosproject.bgpio.types.NextHop;
import org.onosproject.bgpio.types.Origin;
import org.onosproject.bgpio.types.Origin.OriginType;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV4;
import org.onosproject.bgpio.types.attr.BgpLinkAttrName;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrExtRouteTag;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrIgpFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for BGP update Message.
 */
public class BgpUpdateMsgTest {
    protected static final Logger log = LoggerFactory.getLogger(BgpUpdateMsgTest.class);
    public static final byte[] MARKER = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    public static final byte UPDATE_MSG_TYPE = 0x2;

    /**
     * This test case checks update message with no withdrawn routes
     * and path attributes.
     */
    @Test
    public void bgpUpdateMessageTest01() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x17, 0x02, 0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 23));
    }

    /**
     * In this test case, Marker is set as 0 in input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest02() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x17, 0x02, 0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid message length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest03() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x18, 0x02, 0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid message type is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest04() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x17, 0x06, 0x00, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();
        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * This test case checks update message with withdrawn routes.
     */
    @Test
    public void bgpUpdateMessageTest05() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x1b, 0x02, 0x00, 0x04, 0x18, 0x0a, 0x01, 0x01, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 27));

        ListIterator<IpPrefix> listIterator1 = other.withdrawnRoutes().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x01, 0x01, 0x00};

        IpPrefix testPrefixValue = listIterator1.next();
        assertThat(testPrefixValue.prefixLength(), is((int) 24));
        assertThat(testPrefixValue.address().toOctets(), is(prefix));
    }

    /**
     * In this test case, Invalid withdrawn route length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest06() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x1b, 0x02, 0x00, 0x04, 0x19, 0x0a, 0x01, 0x01, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * This test case checks update message with path attributes.
     */
    @Test
    public void bgpUpdateMessageTest07() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x3f, 0x02, 0x00, 0x00, 0x00, 0x1c, 0x40, 0x01, 0x01,
                0x00, 0x40, 0x02, 0x00, 0x40, 0x03, 0x04, 0x03, 0x03, 0x03, 0x03, (byte) 0x80, 0x04, 0x04, 0x00, 0x00,
                0x00, 0x00, 0x40, 0x05, 0x04, 0x00, 0x00, 0x00, 0x64, 0x18, 0x0a, 0x1e, 0x03, 0x18, 0x0a, 0x1e,
                0x02, 0x18, 0x0a, 0x1e, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 63));

        BgpValueType testPathAttribute;
        Origin origin;
        AsPath asPath;
        NextHop nexthop;
        Med med;
        LocalPref localPref;

        List<BgpValueType> pathAttributes = new LinkedList<>();
        BgpPathAttributes actualpathAttribute = other.bgpPathAttributes();
        pathAttributes = actualpathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributes.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next(); // AS PATH value is empty in hex dump
        asPath = (AsPath) testPathAttribute;
        List<Short> asPathValues = asPath.asPathSeq();
        assertThat(asPathValues.isEmpty(), is(true));

        testPathAttribute = listIterator.next();
        nexthop = (NextHop) testPathAttribute;
        byte[] nextHopAddr = new byte[] {0x03, 0x03, 0x03, 0x03};
        assertThat(nexthop.nextHop().toOctets(), is(nextHopAddr));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        localPref = (LocalPref) testPathAttribute;
        assertThat(localPref.localPref(), is(100));

        ListIterator<IpPrefix> listIterator1 = other.nlri().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x1e, 0x03, 0x00};

        IpPrefix testPrefixValue = listIterator1.next();
        assertThat(testPrefixValue.prefixLength(), is((int) 24));
        assertThat(testPrefixValue.address().toOctets(), is(prefix));
    }

    /**
     * In this test case, Invalid ORIGIN flags is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest08() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid ORIGIN value is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest09() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x04, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, update message without path attribute is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest10() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x1a, 0x02, 0x00, 0x04, 0x18, 0x0a, 0x01, 0x01, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, update message with incorrect path attribute length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest11() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x1b, 0x02, 0x00, 0x04, 0x18, 0x0a, 0x01, 0x01, 0x00, 0x01};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid MED flags is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest12() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0xff, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid AS Path flags is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest13() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                (byte) 0xff, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid MP reach flags is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest14() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0xff, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid SAFI is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest15() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x49, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid AFI is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest16() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x06, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid res is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest17() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0xff, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x01, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * This test case checks update message with node NLRI.
     */
    @Test
    public void bgpUpdateMessageTest18() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 96));

        BgpValueType testPathAttribute;
        Origin origin;
        AsPath asPath;
        Med med;
        MpReachNlri mpReach;
        List<BgpValueType> pathAttributes = new LinkedList<>();
        BgpPathAttributes actualpathAttribute = other.bgpPathAttributes();
        pathAttributes = actualpathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributes.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        asPath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = asPath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpReach = (MpReachNlri) testPathAttribute;
        assertThat(mpReach.mpReachNlriLen(), is((int) 52));
        assertThat(mpReach.getType(), is((short) 14));

        List<BgpLSNlri> testMpReachNlri = new LinkedList<>();
        testMpReachNlri = mpReach.mpReachNlri();

        ListIterator<BgpLSNlri> list1 = testMpReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        NlriType nlriType = org.onosproject.bgpio.protocol.NlriType.NODE;
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
        BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getIdentifier(), is((long) 0));
        assertThat(testnlri.getNlriType(), is(nlriType));
        assertThat(testnlri.getProtocolId(), is(protocolId));

        BgpNodeLSNlriVer4 testNodenlri = (BgpNodeLSNlriVer4) testnlri;

        BgpNodeLSIdentifier testLocalNodeDescriptors = testNodenlri.getLocalNodeDescriptors();

        List<BgpValueType> testSubTlvs = new LinkedList<>();
        NodeDescriptors localNodeDescriptors = testLocalNodeDescriptors.getNodedescriptors();
        testSubTlvs = localNodeDescriptors.getSubTlvs();
        ListIterator<BgpValueType> subtlvlist1 = testSubTlvs.listIterator();

        AutonomousSystemTlv testAutonomousSystemTlv = (AutonomousSystemTlv) subtlvlist1.next();
        assertThat(testAutonomousSystemTlv.getAsNum(), is(2222));
        assertThat(testAutonomousSystemTlv.getType(), is((short) 512));

        BgpLSIdentifierTlv testBgpLsIdentifierTlv = (BgpLSIdentifierTlv) subtlvlist1.next();
        assertThat(testBgpLsIdentifierTlv.getBgpLsIdentifier(), is(33686018));
        assertThat(testBgpLsIdentifierTlv.getType(), is((short) 513));

        IsIsNonPseudonode testIsIsNonPseudonode = (IsIsNonPseudonode) subtlvlist1.next();
        byte[] expIsoNodeId = new byte[] {0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58};
        assertThat(testIsIsNonPseudonode.getIsoNodeId(), is(expIsoNodeId));
        assertThat(testIsIsNonPseudonode.getType(), is((short) 515));

    }

    /**
     * This test case checks update message with prefix NLRI.
     */
    @Test
    public void bgpUpdateMessageTest19() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xd6, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, (byte) 0xbb, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x90, 0x0e, 0x00, (byte) 0xa5, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x03, 0x00, 0x30, 0x02, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x1a, 0x02, 0x00, 0x00, 0x04, 0x00, 0x00, 0x08,
                (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02,
                0x02, 0x02, 0x03, 0x00, 0x06, 0x19, 0x21, 0x68,
                0x07, 0x70, 0x01, 0x01, 0x09, 0x00, 0x05, 0x20,
                (byte) 0xc0, (byte) 0xa8, 0x4d, 0x01, 0x00, 0x03, 0x00, 0x30,
                0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00,
                0x06, 0x19, 0x00, (byte) 0x95, 0x02, 0x50, 0x21, 0x01,
                0x09, 0x00, 0x05, 0x20, 0x15, 0x15, 0x15, 0x15,
                0x00, 0x03, 0x00, 0x30, 0x02, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x1a, 0x02, 0x00, 0x00, 0x04, 0x00, 0x00, 0x08,
                (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02,
                0x02, 0x02, 0x03, 0x00, 0x06, 0x02, 0x20, 0x22,
                0x02, 0x20, 0x22, 0x01, 0x09, 0x00, 0x05, 0x20,
                0x16, 0x16, 0x16, 0x16}; // prefix nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 214));

        ListIterator<IpPrefix> listIterator1 = other.withdrawnRoutes().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x01, 0x01, 0x00};

        IpPrefix testPrefixValue = listIterator1.next();
        assertThat(testPrefixValue.prefixLength(), is((int) 24));
        assertThat(testPrefixValue.address().toOctets(), is(prefix));

        BgpValueType testPathAttribute;
        Origin origin;
        AsPath asPath;
        Med med;
        MpReachNlri mpReach;
        List<BgpValueType> pathAttributes = new LinkedList<>();
        BgpPathAttributes actualpathAttribute = other.bgpPathAttributes();
        pathAttributes = actualpathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributes.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        asPath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = asPath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpReach = (MpReachNlri) testPathAttribute;
        assertThat(mpReach.mpReachNlriLen(), is((int) 165));
        assertThat(mpReach.getType(), is((short) 14));

        List<BgpLSNlri> testMpReachNlri = new LinkedList<>();
        testMpReachNlri = mpReach.mpReachNlri();

        ListIterator<BgpLSNlri> list1 = testMpReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        NlriType nlriType = org.onosproject.bgpio.protocol.NlriType.PREFIX_IPV4;
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
                BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getIdentifier(), is((long) 0));
        assertThat(testnlri.getNlriType(), is(nlriType));
        assertThat(testnlri.getProtocolId(), is(protocolId));

        BgpPrefixLSNlri testprefixnlri = (BgpPrefixLSNlri) testnlri;

        NodeDescriptors testLocalNodeDescriptors = testprefixnlri.getLocalNodeDescriptors();

        List<BgpValueType> testSubTlvs = new LinkedList<>();
        testSubTlvs = testLocalNodeDescriptors.getSubTlvs();
        ListIterator<BgpValueType> subtlvlist1 = testSubTlvs.listIterator();

        AutonomousSystemTlv testAutonomousSystemTlv = (AutonomousSystemTlv) subtlvlist1.next();
        assertThat(testAutonomousSystemTlv.getAsNum(), is(2222));
        assertThat(testAutonomousSystemTlv.getType(), is((short) 512));

        BgpLSIdentifierTlv testBgpLsIdentifierTlv = (BgpLSIdentifierTlv) subtlvlist1.next();
        assertThat(testBgpLsIdentifierTlv.getBgpLsIdentifier(), is(33686018));
        assertThat(testBgpLsIdentifierTlv.getType(), is((short) 513));

        IsIsNonPseudonode testIsIsNonPseudonode = (IsIsNonPseudonode) subtlvlist1.next();
        byte[] expIsoNodeId = new byte[] {0x19, 0x21, 0x68, 0x07, 0x70, 0x01};
        assertThat(testIsIsNonPseudonode.getIsoNodeId(), is(expIsoNodeId));
        assertThat(testIsIsNonPseudonode.getType(), is((short) 515));

        List<BgpValueType> testPrefixDescriptors =  new LinkedList<>();
        testPrefixDescriptors = testprefixnlri.getPrefixdescriptor();
        ListIterator<BgpValueType> subtlvlist2 = testPrefixDescriptors.listIterator();
        IPReachabilityInformationTlv testIPReachabilityInformationTlv = (IPReachabilityInformationTlv)
                subtlvlist2.next();
        byte[] address = new byte[] {(byte) 0xc0, (byte) 0xa8, 0x4d, 0x01};
        IpPrefix prefix1 = IpPrefix.valueOf(IpAddress.Version.INET, address, 32);
        assertThat(testIPReachabilityInformationTlv.getPrefixValue(), is(prefix1));
        assertThat(testIPReachabilityInformationTlv.getPrefixLen(), is((byte) 32));
    }

    /**
     * This test case checks update message with link NLRI.
     */
    @Test
    public void bgpUpdateMessageTest20() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x83, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x68, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21//link nlri
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 131));

        ListIterator<IpPrefix> listIterator1 = other.withdrawnRoutes().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x01, 0x01, 0x00};

        IpPrefix testPrefixValue = listIterator1.next();
        assertThat(testPrefixValue.prefixLength(), is((int) 24));
        assertThat(testPrefixValue.address().toOctets(), is(prefix));

        BgpValueType testPathAttribute;
        Origin origin;
        AsPath asPath;
        Med med;
        MpReachNlri mpReach;

        List<BgpValueType> pathAttributes = new LinkedList<>();
        BgpPathAttributes actualpathAttribute = other.bgpPathAttributes();
        pathAttributes = actualpathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributes.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        asPath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = asPath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpReach = (MpReachNlri) testPathAttribute;
        assertThat(mpReach.mpReachNlriLen(), is((int) 83));
        assertThat(mpReach.getType(), is((short) 14));

        List<BgpLSNlri> testMpReachNlri = new LinkedList<>();
        testMpReachNlri = mpReach.mpReachNlri();

        ListIterator<BgpLSNlri> list1 = testMpReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        NlriType nlriType = org.onosproject.bgpio.protocol.NlriType.LINK;
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
            BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getIdentifier(), is((long) 0));
        assertThat(testnlri.getNlriType(), is(nlriType));
        assertThat(testnlri.getProtocolId(), is(protocolId));

        BgpLinkLsNlriVer4 testlinknlri = (BgpLinkLsNlriVer4) testnlri;

        NodeDescriptors testLocalNodeDescriptors = testlinknlri.localNodeDescriptors();

        List<BgpValueType> testSubTlvs = new LinkedList<>();
        testSubTlvs = testLocalNodeDescriptors.getSubTlvs();
        ListIterator<BgpValueType> subtlvlist1 = testSubTlvs.listIterator();

        AutonomousSystemTlv testAutonomousSystemTlv = (AutonomousSystemTlv) subtlvlist1.next();

        assertThat(testAutonomousSystemTlv.getAsNum(), is(2222));
        assertThat(testAutonomousSystemTlv.getType(), is((short) 512));

        BgpLSIdentifierTlv testBgpLsIdentifierTlv = (BgpLSIdentifierTlv) subtlvlist1.next();
        assertThat(testBgpLsIdentifierTlv.getBgpLsIdentifier(), is(33686018));
        assertThat(testBgpLsIdentifierTlv.getType(), is((short) 513));

        IsIsPseudonode testIsIsPseudonode = (IsIsPseudonode) subtlvlist1.next();
        assertThat(testIsIsPseudonode.getPsnIdentifier(), is((byte) 3));
        assertThat(testIsIsPseudonode.getType(), is((short) 515));

        NodeDescriptors testRemoteNodeDescriptors = testlinknlri.remoteNodeDescriptors();
        testSubTlvs = testRemoteNodeDescriptors.getSubTlvs();
        ListIterator<BgpValueType> subtlvlist2 = testSubTlvs.listIterator();

        testAutonomousSystemTlv = (AutonomousSystemTlv) subtlvlist2.next();

        assertThat(testAutonomousSystemTlv.getAsNum(), is(2222));
        assertThat(testAutonomousSystemTlv.getType(), is((short) 512));

        testBgpLsIdentifierTlv = (BgpLSIdentifierTlv) subtlvlist2.next();
        assertThat(testBgpLsIdentifierTlv.getBgpLsIdentifier(), is(33686018));
        assertThat(testBgpLsIdentifierTlv.getType(), is((short) 513));

        IsIsNonPseudonode testIsIsNonPseudonode = (IsIsNonPseudonode) subtlvlist2.next();
        byte[] expIsoNodeId = new byte[] {0x19, 0x00, (byte) 0x95, 0x02, 0x50, 0x21};
        assertThat(testIsIsNonPseudonode.getIsoNodeId(), is(expIsoNodeId));
        assertThat(testIsIsNonPseudonode.getType(), is((short) 515));
    }

    /**
     * In this test case, Invalid withdrawn route length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest21() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x1b, 0x02, 0x00, 0x07, 0x18, 0x0a, 0x01, 0x01, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid withdrawn route length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest22() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x25, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x0e, //path attribute len
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00}; //med

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Mandatory attributes are not given in input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest23() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x29, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x12, //path attribute len
                0x0e, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00}; //med

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid origin length is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest24() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x29, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x12, //path attribute len
                0x04, 0x01, 0x02, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00}; //med

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid origin value is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest25() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x29, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x12, //path attribute len
                0x04, 0x01, 0x01, 0x04, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00}; //med

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid descriptor type in node nlri is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest26() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, Invalid node nlri length field in is given as input and expecting
     * an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest27() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, 0x02, 0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x34, 0x40, 0x04, 0x47, //mpreach with safi = 71
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, withdrawn routes with prefix length 0 is given as input and expecting
     * an exception.
     */
    @Test
    public void bgpUpdateMessageTest28() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, //marker
                0x00, 0x18, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * In this test case, update message without total Path Attribute Length field is given as
     * input and expecting an exception.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest29() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, //marker
                0x00, 0x16, 0x02, 0x00, 0x01, 0x00};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * This test case checks update message with as4 path attribute.
     */
    @Test
    public void bgpUpdateMessageTest30() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                0x00, 0x3a, 0x02, 0x00, 0x00, 0x00, 0x21, 0x40, 0x01, 0x01, 0x00, (byte) 0xc0,
                0x11, 0x0a, 0x02, 0x02, 0x00, 0x0a, 0x00, 0x01, 0x00, 0x28, 0x00, 0x01, 0x40,
                0x02, 0x06, 0x02, 0x02, 0x5b, (byte) 0xa0, 0x5b, (byte) 0xa0, 0x40, 0x03, 0x04,
                (byte) 0xac, 0x10, 0x03, 0x01, 0x08, 0x28};

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 58));

        BgpValueType testPathAttribute;
        Origin origin;
        As4Path as4Path;
        AsPath asPath;
        NextHop nextHop;

        List<BgpValueType> pathAttributes = new LinkedList<>();
        BgpPathAttributes actualpathAttribute = other.bgpPathAttributes();
        pathAttributes = actualpathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributes.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        as4Path = (As4Path) testPathAttribute;
        ListIterator<Integer> listIterator2 = as4Path.as4PathSeq().listIterator();
        assertThat(listIterator2.next(), is(655361));

        testPathAttribute = listIterator.next();
        asPath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator3 = asPath.asPathSeq().listIterator();
        assertThat(listIterator3.next(), is((short) 23456));

        testPathAttribute = listIterator.next();
        nextHop = (NextHop) testPathAttribute;
        byte[] nextHopAddr = new byte[] {(byte) 0xac, 0x10, 0x03, 0x01};
        assertThat(nextHop.nextHop().toOctets(), is(nextHopAddr));

        ListIterator<IpPrefix> listIterator1 = other.nlri().listIterator();
        byte[] prefix = new byte[] {0x28, 0x00, 0x00, 0x00};

        IpPrefix testPrefixValue = listIterator1.next();
        assertThat(testPrefixValue.prefixLength(), is((int) 8));
        assertThat(testPrefixValue.address().toOctets(), is(prefix));
    }

    /**
     * This test case checks update message with MPUnreach.
     */
    @Test
    public void bgpUpdateMessageTest31() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x5e, 0x02, 0x00, 0x04, 0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x43, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0f, 0x2e, 0x40, 0x04, 0x47, //mpunreach with safi = 71
                0x00, 0x01, 0x00,
                0x27, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00,
                0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03,
                0x00, 0x06, 0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58}; //node nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 94));

        ListIterator<IpPrefix> listIterator1 = other.withdrawnRoutes().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x01, 0x01, 0x00};

        IpPrefix testPrefixValue = listIterator1.next();
        assertThat(testPrefixValue.prefixLength(), is((int) 24));
        assertThat(testPrefixValue.address().toOctets(), is(prefix));

        BgpValueType testPathAttribute;
        Origin origin;
        AsPath asPath;
        Med med;
        MpUnReachNlri mpUnReach;
        List<BgpValueType> pathAttributes = new LinkedList<>();
        BgpPathAttributes actualpathAttribute = other.bgpPathAttributes();
        pathAttributes = actualpathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributes.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        asPath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = asPath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpUnReach = (MpUnReachNlri) testPathAttribute;
        assertThat(mpUnReach.mpUnReachNlriLen(), is((int) 46));
        assertThat(mpUnReach.getType(), is((short) 15));

        List<BgpLSNlri> testMpUnReachNlri = new LinkedList<>();
        testMpUnReachNlri = mpUnReach.mpUnReachNlri();

        ListIterator<BgpLSNlri> list1 = testMpUnReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        NlriType nlriType = org.onosproject.bgpio.protocol.NlriType.NODE;
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
               BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getIdentifier(), is((long) 0));
        assertThat(testnlri.getNlriType(), is(nlriType));
        assertThat(testnlri.getProtocolId(), is(protocolId));

        BgpNodeLSNlriVer4 testNodenlri = (BgpNodeLSNlriVer4) testnlri;

        BgpNodeLSIdentifier testLocalNodeDescriptors = testNodenlri.getLocalNodeDescriptors();

        List<BgpValueType> testSubTlvs = new LinkedList<>();
        NodeDescriptors localNodeDescriptors = testLocalNodeDescriptors.getNodedescriptors();
        testSubTlvs = localNodeDescriptors.getSubTlvs();
        ListIterator<BgpValueType> subtlvlist1 = testSubTlvs.listIterator();

        AutonomousSystemTlv testAutonomousSystemTlv = (AutonomousSystemTlv) subtlvlist1.next();

        assertThat(testAutonomousSystemTlv.getAsNum(), is(2222));
        assertThat(testAutonomousSystemTlv.getType(), is((short) 512));

        BgpLSIdentifierTlv testBgpLsIdentifierTlv = (BgpLSIdentifierTlv) subtlvlist1.next();
        assertThat(testBgpLsIdentifierTlv.getBgpLsIdentifier(), is(33686018));
        assertThat(testBgpLsIdentifierTlv.getType(), is((short) 513));

        IsIsNonPseudonode testIsIsNonPseudonode = (IsIsNonPseudonode) subtlvlist1.next();
        byte[] expIsoNodeId = new byte[] {0x19, 0x00, (byte) 0x95, 0x01, (byte) 0x90, 0x58};
        assertThat(testIsIsNonPseudonode.getIsoNodeId(), is(expIsoNodeId));
        assertThat(testIsIsNonPseudonode.getType(), is((short) 515));
    }

    /**
     * This test case checks update message with invalid mpreach packet.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest32() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xd6, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, (byte) 0xbb, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x90, 0x0e, 0x00, (byte) 0xa5, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x03, 0x00, 0x30, 0x02, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00,
                0x1a, 0x02, 0x00, 0x00, 0x04, 0x00, 0x00, 0x08,
                (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02,
                0x02, 0x02, 0x03, 0x00, 0x06, 0x19, 0x21, 0x68,
                0x07, 0x70, 0x01, 0x01, 0x09, 0x00, 0x05, 0x20,
                (byte) 0xc0, (byte) 0xa8, 0x4d, 0x01, 0x00, 0x03, 0x00, 0x30,
                0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00,
                0x06, 0x19, 0x00, (byte) 0x95, 0x02, 0x50, 0x21, 0x01,
                0x09, 0x00, 0x05, 0x20, 0x15, 0x15, 0x15, 0x15,
                0x00, 0x03, 0x00, 0x30, 0x02, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x1a, 0x02, 0x00, 0x00, 0x04, 0x00, 0x00, 0x08,
                (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02,
                0x02, 0x02, 0x03, 0x00, 0x06, 0x02, 0x20, 0x22,
                0x02, 0x20, 0x22, 0x01, 0x09, 0x00, 0x05, 0x20,
                0x16, 0x16, 0x16, 0x16}; // prefix nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * This test case checks update message with invalid prefix nlri length in input.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest33() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xd6, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, (byte) 0xbb, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x90, 0x0e, 0x00, (byte) 0xa5, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x03, 0x00, 0x35, 0x02, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x1a, 0x02, 0x00, 0x00, 0x04, 0x00, 0x00, 0x08,
                (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02,
                0x02, 0x02, 0x03, 0x00, 0x06, 0x19, 0x21, 0x68,
                0x07, 0x70, 0x01, 0x01, 0x09, 0x00, 0x05, 0x20,
                (byte) 0xc0, (byte) 0xa8, 0x4d, 0x01, 0x00, 0x03, 0x00, 0x30,
                0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x00, 0x1a, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00,
                0x06, 0x19, 0x00, (byte) 0x95, 0x02, 0x50, 0x21, 0x01,
                0x09, 0x00, 0x05, 0x20, 0x15, 0x15, 0x15, 0x15,
                0x00, 0x03, 0x00, 0x30, 0x02, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
                0x1a, 0x02, 0x00, 0x00, 0x04, 0x00, 0x00, 0x08,
                (byte) 0xae, 0x02, 0x01, 0x00, 0x04, 0x02, 0x02, 0x02,
                0x02, 0x02, 0x03, 0x00, 0x06, 0x02, 0x20, 0x22,
                0x02, 0x20, 0x22, 0x01, 0x09, 0x00, 0x05, 0x20,
                0x16, 0x16, 0x16, 0x16}; // prefix nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    /**
     * This test case checks update message with invalid link nlri length in input.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest34() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x83, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x68, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x48, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21}; //link nlri

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
    }

    //Negative scenarios
    /**
     * Wrong length BgpAttrRouterIdV4.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest35() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x95,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7A, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x0f,  //linkstate attr
                0x04, 0x04, 0x00, 0x06, (byte) 0xbd, 0x59, 0x4c, 0x62, //BgpAttrRouterIdV4
                0x04, 0x47, 0x00, 0x03, 0x00, 0x00, 0x0a}; //BgpLinkAttrIGPMetric

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpHeader bgpHeader = new BgpHeader();
        reader.readFrom(buffer, bgpHeader);
    }

    /**
     * Wrong length BgpLinkAttrIGPMetric.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest36() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x95,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7A, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x0f,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0xbd, 0x59, 0x4c, 0x62, //BgpAttrRouterIdV4
                0x04, 0x47, 0x00, 0x02, 0x00, 0x00, 0x0a}; //BgpLinkAttrIGPMetric

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpHeader bgpHeader = new BgpHeader();
        reader.readFrom(buffer, bgpHeader);
    }

    /**
     * Wrong length BgpPrefixAttrMetric.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest37() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x96,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7b, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x10,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                  0x04, (byte) 0x83, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00}; //BgpPrefixAttrMetric

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpHeader bgpHeader = new BgpHeader();
        reader.readFrom(buffer, bgpHeader);
    }

    /**
     * Wrong length BgpPrefixAttrMetric.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest38() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x96,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7b, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x10,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                  0x04, (byte) 0x83, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00}; //BgpPrefixAttrMetric
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpHeader bgpHeader = new BgpHeader();
        reader.readFrom(buffer, bgpHeader);
    }

    /**
     * Wrong length BgpPrefixAttrOpaqueData.
     */
    @Test(expected = BgpParseException.class)
    public void bgpUpdateMessageTest39() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x96,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7B, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x10,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, (byte) 0x85, 0x00, 0x06, 0x0a, 0x0a, 0x0a, 0x0a}; //BgpPrefixAttrOpaqueData
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpHeader bgpHeader = new BgpHeader();
        reader.readFrom(buffer, bgpHeader);
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrName.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest40() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x9A,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7F, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x14,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, 0x4A, 0x00, 0x08, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0b}; //BgpLinkAttrName
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        byte[] marker = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff };

        assertThat(other.getHeader().getMarker(), is(marker));
        assertThat(other.getHeader().getType(), is((byte) 2));
        assertThat(other.getHeader().getLength(), is((short) 154));

        ListIterator<IpPrefix> listIterator1 = other.withdrawnRoutes().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x01, 0x01, 0x00};

        while (listIterator1.hasNext()) {
            IpPrefix testPrefixValue = listIterator1.next();
            assertThat(testPrefixValue.prefixLength(), is((int) 24));
            assertThat(testPrefixValue.address().toOctets(), is(prefix));
        }

        BgpValueType testPathAttribute = null;
        Origin origin;
        AsPath aspath;
        Med med;
        MpReachNlri mpReach;
        LinkStateAttributes linkStateAttr;
        List<BgpValueType> pathAttributeList = new LinkedList<>();
        BgpPathAttributes pathAttribute = other.bgpPathAttributes();
        pathAttributeList = pathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributeList.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        aspath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = aspath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpReach = (MpReachNlri) testPathAttribute;
        assertThat(mpReach.mpReachNlriLen(), is((int) 83));
        assertThat(mpReach.getType(), is((short) 14));

        List<BgpLSNlri> testMpReachNlri = new LinkedList<>();
        testMpReachNlri = mpReach.mpReachNlri();

        ListIterator<BgpLSNlri> list1 = testMpReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        NlriType nlriType = org.onosproject.bgpio.protocol.NlriType.LINK;
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
            BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getIdentifier(), is((long) 0));
        assertThat(testnlri.getNlriType(), is(nlriType));
        assertThat(testnlri.getProtocolId(), is(protocolId));

        testPathAttribute = listIterator.next();
        linkStateAttr = (LinkStateAttributes) testPathAttribute;

        assertThat(linkStateAttr.getType(), is((short) 29));
        ListIterator<BgpValueType> list = linkStateAttr.linkStateAttributes().listIterator();
        byte[] ipBytes = new byte[] {(byte) 0x15, 0x15, 0x15, 0x15 };
        Ip4Address ip4RouterId = Ip4Address.valueOf(ipBytes);
        assertThat(((BgpAttrRouterIdV4) list.next()).attrRouterId(), is(ip4RouterId));
        byte[] linkName = new byte[] {0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0b };
        assertThat(((BgpLinkAttrName) list.next()).attrLinkName(), is(linkName));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpPrefixAttrIGPFlags.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest41() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x93,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x78, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, //link nlri
                (byte) 0x80, 0x1d, 0x0D,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, (byte) 0x80, 0x00, 0x01, (byte) 0xA0}; //BgpPrefixAttrIGPFlags
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        byte[] marker = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff };

        assertThat(other.getHeader().getMarker(), is(marker));
        assertThat(other.getHeader().getType(), is((byte) 2));
        assertThat(other.getHeader().getLength(), is((short) 147));

        BgpValueType testPathAttribute = null;
        Origin origin;
        AsPath aspath;
        Med med;
        MpReachNlri mpReach;
        LinkStateAttributes linkStateAttr;
        List<BgpValueType> pathAttributeList = new LinkedList<>();
        BgpPathAttributes pathAttribute = other.bgpPathAttributes();
        pathAttributeList = pathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributeList.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        aspath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = aspath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpReach = (MpReachNlri) testPathAttribute;
        List<BgpLSNlri> testMpReachNlri = new LinkedList<>();
        testMpReachNlri = mpReach.mpReachNlri();

        ListIterator<BgpLSNlri> list1 = testMpReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        NlriType nlriType = org.onosproject.bgpio.protocol.NlriType.LINK;
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
            BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getIdentifier(), is((long) 0));
        assertThat(testnlri.getNlriType(), is(nlriType));
        assertThat(testnlri.getProtocolId(), is(protocolId));

        testPathAttribute = listIterator.next();
        linkStateAttr = (LinkStateAttributes) testPathAttribute;

        assertThat(linkStateAttr.getType(), is((short) 29));
        ListIterator<BgpValueType> list = linkStateAttr.linkStateAttributes().listIterator();
        byte[] ipBytes = new byte[] {(byte) 0x15, 0x15, 0x15, 0x15 };
        Ip4Address ip4RouterId = Ip4Address.valueOf(ipBytes);
        assertThat(((BgpAttrRouterIdV4) list.next()).attrRouterId(), is(ip4RouterId));
        BgpPrefixAttrIgpFlags obj = new BgpPrefixAttrIgpFlags(true, false, true, false);
        assertThat(((BgpPrefixAttrIgpFlags) list.next()).equals(obj), is(true));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpPrefixAttrExtRouteTag.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest42() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xA2, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, 0x00, (byte) 0x87, 0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med
                (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01, 0x00, //reserved
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, (byte) 0x80, 0x1d, 0x1C,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, (byte) 0x82, 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x02, (byte) 0xBB, (byte) 0xE9, 0x0B,
                0x00, 0x00, 0x00, 0x00, 0x03, 0x20, 0x6E, 0x1B}; //BgpPrefixAttrExtRouteTag
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        byte[] marker = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                    (byte) 0xff, (byte) 0xff, (byte) 0xff };

        assertThat(other.getHeader().getMarker(), is(marker));
        assertThat(other.getHeader().getType(), is((byte) 2));
        assertThat(other.getHeader().getLength(), is((short) 162));

        BgpValueType testPathAttribute = null;
        Origin origin;
        AsPath aspath;
        Med med;
        MpReachNlri mpReach;
        LinkStateAttributes linkStateAttr;
        List<BgpValueType> pathAttributeList = new LinkedList<>();
        BgpPathAttributes pathAttribute = other.bgpPathAttributes();
        pathAttributeList = pathAttribute.pathAttributes();
        ListIterator<BgpValueType> listIterator = pathAttributeList.listIterator();
        OriginType originValue = OriginType.IGP;

        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        testPathAttribute = listIterator.next();
        aspath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = aspath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        testPathAttribute = listIterator.next();
        mpReach = (MpReachNlri) testPathAttribute;

        List<BgpLSNlri> testMpReachNlri = new LinkedList<>();
        testMpReachNlri = mpReach.mpReachNlri();
        ListIterator<BgpLSNlri> list1 = testMpReachNlri.listIterator();
        BgpLSNlri testnlri =  list1.next();
        ProtocolType protocolId = org.onosproject.bgpio.protocol.linkstate.
            BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO;
        assertThat(testnlri.getProtocolId(), is(protocolId));

        testPathAttribute = listIterator.next();
        linkStateAttr = (LinkStateAttributes) testPathAttribute;

        assertThat(linkStateAttr.getType(), is((short) 29));
        ListIterator<BgpValueType> list = linkStateAttr.linkStateAttributes().listIterator();
        byte[] ipBytes = new byte[] {(byte) 0x15, 0x15, 0x15, 0x15 };
        Ip4Address ip4RouterId = Ip4Address.valueOf(ipBytes);
        assertThat(((BgpAttrRouterIdV4) list.next()).attrRouterId(), is(ip4RouterId));
        List<Long> extRouteTag = new LinkedList<>();
        extRouteTag.add(45869323L);
        extRouteTag.add(52456987L);
        assertThat(((BgpPrefixAttrExtRouteTag) list.next()).pfxExtRouteTag(), is(extRouteTag));
    }
}
