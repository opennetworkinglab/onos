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
package org.onosproject.bgpio.protocol;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.protocol.ver4.BgpPathAttributes;
import org.onosproject.bgpio.types.AsPath;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.types.Med;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.Origin;
import org.onosproject.bgpio.types.Origin.OriginType;
import org.onosproject.bgpio.types.attr.BgpAttrNodeFlagBitTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeIsIsAreaId;
import org.onosproject.bgpio.types.attr.BgpAttrNodeMultiTopologyId;
import org.onosproject.bgpio.types.attr.BgpAttrNodeName;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV4;
import org.onosproject.bgpio.types.attr.BgpAttrOpaqueNode;
import org.onosproject.bgpio.types.attr.BgpLinkAttrIsIsAdminstGrp;
import org.onosproject.bgpio.types.attr.BgpLinkAttrMplsProtocolMask;
import org.onosproject.bgpio.types.attr.BgpLinkAttrOpaqLnkAttrib;
import org.onosproject.bgpio.types.attr.BgpLinkAttrProtectionType.ProtectionType;
import org.onosproject.bgpio.types.attr.BgpLinkAttrSrlg;
import org.onosproject.bgpio.types.attr.BgpLinkAttrTeDefaultMetric;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrOpaqueData;
import org.onosproject.bgpio.types.attr.BgpLinkAttrProtectionType;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrRouteTag;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * UT for Update Message (Link State Attribute and all its TLVs).
 */
public class BgpUpdateLinkStateAttrTest {

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrIGPMetric.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest1() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0xbd, 0x59, 0x4c, 0x62, //BgpAttrNodeRouterId
                0x04, 0x47, 0x00, 0x03, 0x00, 0x00, 0x0a}; //BgpLinkAttrIGPMetric

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
        assertThat(other.getHeader().getLength(), is((short) 149));

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

        //compare Origin
        testPathAttribute = listIterator.next();
        origin = (Origin) testPathAttribute;
        assertThat(origin.origin(), is(originValue));

        //compare Aspath
        testPathAttribute = listIterator.next();
        aspath = (AsPath) testPathAttribute;
        ListIterator<Short> listIterator2 = aspath.asPathSeq().listIterator();
        assertThat(listIterator2.next(), is((short) 65001));

        //compare MED
        testPathAttribute = listIterator.next();
        med = (Med) testPathAttribute;
        assertThat(med.med(), is(0));

        //compare Mpreach
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

        //compare LinkStateAttributes
        testPathAttribute = listIterator.next();
        linkStateAttr = (LinkStateAttributes) testPathAttribute;
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x0f, 0x04, 0x04, 0x00, 0x04, (byte) 0xbd, 0x59, 0x4c,
                0x62, 0x04, 0x47, 0x00, 0x03, 0x00, 0x00, 0x0a };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        assertThat(linkStateAttr.linkStateAttributes(), is(obj.linkStateAttributes()));
        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrNodeRouterId.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest2() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, 0x06, 0x00, 0x04, 0x16, 0x16, 0x16, 0x16}; //BgpAttrNodeRouterId
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
        assertThat(other.getHeader().getLength(), is((short) 150));

        ListIterator<IpPrefix> listIterator1 = other.withdrawnRoutes().listIterator();
        byte[] prefix = new byte[] {0x0a, 0x01, 0x01, 0x00};

        while (listIterator1.hasNext()) {
            IpPrefix testPrefixValue = listIterator1.next();
            assertThat(testPrefixValue.prefixLength(), is((int) 24));
            assertThat(testPrefixValue.address().toOctets(), is(prefix));
        }

        BgpValueType testPathAttribute = null;
        Origin origin;
        AsPath asPath;
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

        testPathAttribute = listIterator.next();
        linkStateAttr = (LinkStateAttributes) testPathAttribute;
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x10, 0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15,
                0x15, 0x04, 0x06, 0x00, 0x04, 0x16, 0x16, 0x16, 0x16 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        assertThat(linkStateAttr.linkStateAttributes(), is(obj.linkStateAttributes()));
        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpPrefixAttrMetric.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest3() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, (byte) 0x83, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00}; //BgpPrefixAttrMetric
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
        assertThat(other.getHeader().getLength(), is((short) 150));

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
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x10, 0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15,
                0x15, 0x04, (byte) 0x83, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        assertThat(linkStateAttr.linkStateAttributes(), is(obj.linkStateAttributes()));
        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrNodeName.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest4() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x98,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7d, //path attribute len
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
                (byte) 0x80, 0x1d, 0x12,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, 0x02, 0x00, 0x06, 0x37, 0x37, 0x35, 0x30, 0x2d, 0x31 }; //BgpAttrNodeName

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
        assertThat(other.getHeader().getLength(), is((short) 152));

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
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x12, 0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15,
                0x15, 0x04, 0x02, 0x00, 0x06, 0x37, 0x37, 0x35, 0x30, 0x2d, 0x31 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        ListIterator<BgpValueType> list = obj.linkStateAttributes().listIterator();
        ListIterator<BgpValueType> list2 = linkStateAttr.linkStateAttributes().listIterator();
        assertThat(list.next(), is(list2.next()));
        assertThat(((BgpAttrNodeName) list2.next()).attrNodeName(), is(((BgpAttrNodeName) list.next()).attrNodeName()));
        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrNodeIsIsAreaId.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest5() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0xbd, 0x59, 0x4c, 0x62, //BgpAttrNodeRouterId
                0x04, 0x03, 0x00, 0x03, 0x40, 0x01, 0x00}; //BgpAttrNodeIsIsAreaId

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
        assertThat(other.getHeader().getLength(), is((short) 149));

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
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x0f, 0x04, 0x04, 0x00, 0x04, (byte) 0xbd, 0x59, 0x4c,
                0x62, 0x04, 0x03, 0x00, 0x03, 0x40, 0x01, 0x00 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        ListIterator<BgpValueType> list = obj.linkStateAttributes().listIterator();
        ListIterator<BgpValueType> list2 = linkStateAttr.linkStateAttributes().listIterator();
        assertThat(list.next(), is(list2.next()));
        assertThat(((BgpAttrNodeIsIsAreaId) list2.next()).attrNodeIsIsAreaId(),
                is(((BgpAttrNodeIsIsAreaId) list.next()).attrNodeIsIsAreaId()));
        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrRouterIdV6.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest6() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0xA2,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, (byte) 0x87, //path attribute len
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
                (byte) 0x80, 0x1d, 0x1C,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, 0x05, 0x00, 0x10, 0x01, 0x66, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01}; //BgpAttrRouterIdV6
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
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x1C, 0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15,
                0x15, 0x04, 0x05, 0x00, 0x10, 0x01, 0x66, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        assertThat(linkStateAttr.linkStateAttributes(), is(obj.linkStateAttributes()));
        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrNodeMultiTopologyId.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest7() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x01, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x02}; //BgpAttrNodeMultiTopologyId
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
        assertThat(other.getHeader().getLength(), is((short) 150));

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
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x10, 0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15,
                0x15, 0x01, 0x07, 0x00, 0x04, 0x00, 0x00, 0x00, 0x02 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        ListIterator<BgpValueType> list = obj.linkStateAttributes().listIterator();
        ListIterator<BgpValueType> list2 = linkStateAttr.linkStateAttributes().listIterator();
        assertThat(list.next(), is(list2.next()));
        assertThat(((BgpAttrNodeMultiTopologyId) list2.next()).attrMultiTopologyId(),
                is(((BgpAttrNodeMultiTopologyId) list.next()).attrMultiTopologyId()));

        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpPrefixAttrRouteTag.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest8() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, (byte) 0x81, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01}; //BgpPrefixAttrRouteTag
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
        assertThat(other.getHeader().getLength(), is((short) 150));

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
        byte[] linkAttrbs = new byte[] {(byte) 0x80, 0x1d, 0x10, 0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15,
                0x15, 0x04, (byte) 0x81, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01 };
        ChannelBuffer cb = ChannelBuffers.dynamicBuffer();
        cb.writeBytes(linkAttrbs);
        LinkStateAttributes obj = LinkStateAttributes.read(cb);

        ListIterator<BgpValueType> list = obj.linkStateAttributes().listIterator();
        ListIterator<BgpValueType> list2 = linkStateAttr.linkStateAttributes().listIterator();
        assertThat(list.next(), is(list2.next()));
        assertThat(((BgpPrefixAttrRouteTag) list2.next()).getPfxRouteTag(),
                is(((BgpPrefixAttrRouteTag) list.next()).getPfxRouteTag()));

        assertThat(linkStateAttr.getType(), is((short) 29));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrIsIsAdminstGrp.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest9() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, 0x40, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00}; //BgpLinkAttrIsIsAdminstGrp
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
        assertThat(other.getHeader().getLength(), is((short) 150));

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
        assertThat(((BgpLinkAttrIsIsAdminstGrp) list.next()).linkAttrIsIsAdminGrp(), is((long) 0));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrNodeFlagBitTlv.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest10() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, 0x00, 0x00, 0x01, 0x20}; //BgpAttrNodeFlagBitTlv
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
        BgpAttrNodeFlagBitTlv obj = new BgpAttrNodeFlagBitTlv(false, false, true, false);
        assertThat(((BgpAttrNodeFlagBitTlv) list.next()).equals(obj), is(true));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrTeDefaultMetric.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest11() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrNodeRouterId
                0x04, 0x44, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0a}; //BgpLinkAttrTeDefaultMetric
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
        assertThat(other.getHeader().getLength(), is((short) 150));

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
        assertThat(((BgpLinkAttrTeDefaultMetric) list.next()).attrLinkDefTeMetric(), is(10));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpAttrOpaqueNode.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest12() throws BgpParseException {
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
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, 0x0a}; //BgpAttrOpaqueNode
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
        assertThat(other.getHeader().getLength(), is((short) 150));

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
        byte[] opaqueNode = new byte[] {0x00, 0x00, 0x00, 0x0a };
        assertThat(((BgpAttrOpaqueNode) list.next()).attrOpaqueNode(), is(opaqueNode));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrProtectionType.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest13() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x94,
                0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x79, //path attribute len
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
                (byte) 0x80, 0x1d, 0x0E,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, 0x45, 0x00, 0x02, 0x10, 0x00}; //BgpLinkAttrProtectionType
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
        assertThat(other.getHeader().getLength(), is((short) 148));

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
        assertThat(((BgpLinkAttrProtectionType) list.next()).protectionType(),
                is(ProtectionType.DEDICATED_ONE_PLUS_ONE));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrMplsProtocolMask.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest14() throws BgpParseException {
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
                0x04, 0x46, 0x00, 0x01, (byte) 0xC0}; //BgpLinkAttrMplsProtocolMask
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
        BgpLinkAttrMplsProtocolMask obj = new BgpLinkAttrMplsProtocolMask(true, true);
        assertThat(((BgpLinkAttrMplsProtocolMask) list.next()).equals(obj), is(true));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrSRLG.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest15() throws BgpParseException {
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
                0x04, 0x48, 0x00, 0x08, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0b}; //BgpLinkAttrSRLG
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
        List<Integer> attrSrlg = new ArrayList<Integer>();
        attrSrlg.add(10);
        attrSrlg.add(11);
        assertThat(((BgpLinkAttrSrlg) list.next()).attrSrlg(), is(attrSrlg));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpLinkAttrOpaqLnkAttrib.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest16() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x9A,
                0x02, 0x00, 0x04, 0x18, 0x0a, 0x01, 0x01, //withdrawn routes
                0x00, 0x7F, 0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, (byte) 0x80, 0x0e, 0x53, 0x40, 0x04, 0x47, //mpreach
                0x04, 0x04, 0x00, 0x00, 0x01,  0x00,
                0x00, 0x02, 0x00, 0x46, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x1b, 0x02, 0x00, 0x00,
                0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00, 0x04,
                0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x07, 0x19, 0x00,
                (byte) 0x95, 0x02, 0x50, 0x21, 0x03, 0x01, 0x01, 0x00, 0x1a, 0x02,
                0x00, 0x00, 0x04, 0x00, 0x00, 0x08, (byte) 0xae, 0x02, 0x01, 0x00,
                0x04, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x00, 0x06, 0x19,
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, (byte) 0x80, 0x1d, 0x14,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, (byte) 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, 0x49, 0x00, 0x08, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0b}; //BgpLinkAttrOpaqLnkAttrib
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
        byte[] attrOpaqLnkAttrib = new byte[] {0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0b };
        assertThat(((BgpLinkAttrOpaqLnkAttrib) list.next()).attrOpaqueLnk(), is(attrOpaqLnkAttrib));
    }

    /**
     * Test for LinkStateattribute BgpAttrNodeRouterId and BgpPrefixAttrOpaqueData.
     *
     * @throws BgpParseException while parsing update message
     */
    @Test
    public void bgpUpdateMessageTest17() throws BgpParseException {
        byte[] updateMsg = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, (byte) 0x96, 0x02, 0x00, 0x04,
                0x18, 0x0a, 0x01, 0x01, 0x00, 0x7B, //path attribute len
                0x04, 0x01, 0x01, 0x00, 0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
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
                0x00, (byte) 0x95, 0x02, 0x50, 0x21, (byte) 0x80, 0x1d, 0x10,  //linkstate attr
                0x04, 0x04, 0x00, 0x04, 0x15, 0x15, 0x15, 0x15, //BgpAttrRouterIdV4
                0x04, (byte) 0x85, 0x00, 0x04, 0x0a, 0x0a, 0x0a, 0x0a}; //BgpPrefixAttrOpaqueData
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
        assertThat(other.getHeader().getLength(), is((short) 150));
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
        byte[] opaquePrefixAttr = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        assertThat(((BgpPrefixAttrOpaqueData) list.next()).getOpaquePrefixAttribute(), is(opaquePrefixAttr));
    }
}
