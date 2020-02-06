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


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import java.util.List;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixIPv4LSNlriVer4;
import org.onosproject.bgpio.types.attr.BgpAttrNodeMultiTopologyId;
import org.onosproject.bgpio.types.IPReachabilityInformationTlv;

/*
* Since BgpUpdateMsgTest.java was exceeding 2000 lines, this file is created so that
* more test cases could be added.
* This file contains test cases specifically for fixes for ONOS JIRA bug 8036
* (https://jira.onosproject.org/browse/ONOS-8036)
*/

public class BgpUpdateMsg2Test {


    /**
     * This test case checks the changes made in.
     * BgpLinkLSIdentifier
     * BgpAttrNodeMultiTopologyId
     * as bug fix for bug 8036
     */
    @Test
    public void bgpUpdateMessage2Test1() throws BgpParseException {
        byte[] updateMsg = new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x00, (byte) 0xe6, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xcf, (byte) 0x90,
                (byte) 0x0e, (byte) 0x00, (byte) 0xb3, (byte) 0x40, (byte) 0x04, (byte) 0x47, (byte) 0x10, (byte) 0x20,
                (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x50,
                (byte) 0x54, (byte) 0x00, (byte) 0xff, (byte) 0xfe, (byte) 0xb8, (byte) 0x35, (byte) 0x2b, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x4b, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x1b, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x1a, (byte) 0x02,
                (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x02,
                (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
                (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x2b,
                //---------- BgpAttrNodeMultiTopologyId for Link 1-----------------------------------------------------
                (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x4b, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x2b, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x1a, (byte) 0x02,
                (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x02,
                (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
                (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x1b,
                //------------ BgpAttrNodeMultiTopologyId for Link 2 --------------------------------------------------
                (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x40,
                (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x80, (byte) 0x1d,
                (byte) 0x07, (byte) 0x04, (byte) 0x47, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x0a
        };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg receivedMsg = (BgpUpdateMsg) message;
        List<BgpValueType> pathAttr = receivedMsg.bgpPathAttributes().pathAttributes();
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof MpReachNlri) {
                List<BgpLSNlri> nlri = ((MpReachNlri) attr).mpReachNlri();
                ListIterator<BgpLSNlri> listIterator = nlri.listIterator();
                while (listIterator.hasNext()) {
                    BgpLSNlri nlriInfo = listIterator.next();
                    if (nlriInfo instanceof BgpLinkLsNlriVer4) {
                        List<BgpValueType> tlvs = ((BgpLinkLsNlriVer4) nlriInfo).getLinkIdentifier().linkDescriptors();
                        for (BgpValueType tlv : tlvs) {
                            if (tlv instanceof BgpAttrNodeMultiTopologyId) {
                                assertThat(((BgpAttrNodeMultiTopologyId) tlv).attrMultiTopologyId().size(),
                                        is(1));
                                assertThat(((BgpAttrNodeMultiTopologyId) tlv).attrMultiTopologyId().get(0),
                                        is((short) 2));
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * This test case checks the changes made in.
     * BgpPrefixLSIdentifier
     * MpReachNlri (read method AFI 16388/SAFI 71 if part)
     * BgpPrefixIPv4LSNlriVer4
     * LinkStateAttributes (ATTR_PREFIX_FLAGS check)
     * BgpAttrNodeMultiTopologyId
     * Validation
     * IPReachabilityInformationTlv
     * as bug fix for bug 8036
     */
    @Test
    public void bgpUpdateMessage2Test2() throws BgpParseException {
        byte[] updateMsg = new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x01, (byte) 0x08, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf1, (byte) 0x90,
                (byte) 0x0e, (byte) 0x00, (byte) 0xcf, (byte) 0x40, (byte) 0x04, (byte) 0x47, (byte) 0x10,
                //------------ MpReachNlri Nexthop --------------------------------------------------------------------
                (byte) 0x20, (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00,
                (byte) 0x50,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x54, (byte) 0x00, (byte) 0xff, (byte) 0xfe, (byte) 0xb8, (byte) 0x35, (byte) 0x2b, (byte) 0x00,
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x3a, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x2b,
                //------------ BgpAttrNodeMultiTopologyId for Prefix 1-------------------------------------------------
                (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                //------------ IPReachabilityInformationTlv for Prefix 1-----------------------------------------------
                (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x09, (byte) 0x40, (byte) 0x20, (byte) 0x01, (byte) 0x05,
                (byte) 0xb0, (byte) 0x43, (byte) 0x21, (byte) 0x00, (byte) 0x00,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x3a, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1b,
                //------------ BgpAttrNodeMultiTopologyId for Prefix 2-------------------------------------------------
                (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                //------------ IPReachabilityInformationTlv for Prefix 2-----------------------------------------------
                (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x09, (byte) 0x40, (byte) 0x20, (byte) 0x01, (byte) 0x05,
                (byte) 0xb0, (byte) 0x43, (byte) 0x21, (byte) 0x00, (byte) 0x00,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x3a, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x1b,
                //------------ BgpAttrNodeMultiTopologyId for Prefix 3-------------------------------------------------
                (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                //------------ IPReachabilityInformationTlv for Prefix 3-----------------------------------------------
                (byte) 0x01, (byte) 0x09, (byte) 0x00, (byte) 0x09, (byte) 0x40, (byte) 0x20, (byte) 0x01, (byte) 0x05,
                (byte) 0xb0, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x40,
                (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x80, (byte) 0x1d,
                (byte) 0x0d, (byte) 0x04, (byte) 0x83, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x0a,
                //------------ BGP-LS ATTR_PREFIX_FLAGS Attribute-----------------------------------------------
                (byte) 0x04, (byte) 0x92, (byte) 0x00, (byte) 0x01, (byte) 0x00
                //-----------------------------------------------------------------------------------------------------
        };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg receivedMsg = (BgpUpdateMsg) message;
        List<BgpValueType> pathAttr = receivedMsg.bgpPathAttributes().pathAttributes();
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        short count = 0;
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof MpReachNlri) {
                List<BgpLSNlri> nlri = ((MpReachNlri) attr).mpReachNlri();
                assertThat(((MpReachNlri) attr).nexthop().toString(), is("2001:5b0:8:0:5054:ff:feb8:352b"));
                ListIterator<BgpLSNlri> listIterator = nlri.listIterator();
                while (listIterator.hasNext()) {
                    BgpLSNlri nlriInfo = listIterator.next();
                    assertThat(nlriInfo, instanceOf(BgpPrefixIPv4LSNlriVer4.class));
                    if (nlriInfo instanceof BgpPrefixIPv4LSNlriVer4) {
                        List<BgpValueType> tlvs =
                                ((BgpPrefixIPv4LSNlriVer4) nlriInfo).getPrefixIdentifier().getPrefixdescriptor();
                        for (BgpValueType tlv : tlvs) {
                            if (tlv instanceof BgpAttrNodeMultiTopologyId) {
                                assertThat(((BgpAttrNodeMultiTopologyId) tlv).attrMultiTopologyId().size(),
                                        is(1));
                                assertThat(((BgpAttrNodeMultiTopologyId) tlv).attrMultiTopologyId().get(0),
                                        is((short) 2));
                            } else if (tlv instanceof IPReachabilityInformationTlv) {
                                if (count == 0) {
                                    assertThat(((IPReachabilityInformationTlv) tlv).getPrefixValue().toString(),
                                            is("2001:5b0:4321::/64"));
                                } else if (count == 1) {
                                    assertThat(((IPReachabilityInformationTlv) tlv).getPrefixValue().toString(),
                                            is("2001:5b0:4321::/64"));
                                } else {
                                    assertThat(((IPReachabilityInformationTlv) tlv).getPrefixValue().toString(),
                                            is("2001:5b0:8::/64"));
                                }
                                count++;
                            }
                        }
                    }
                }
            } else if (attr instanceof LinkStateAttributes) {
                assertThat(((LinkStateAttributes) attr).linkStateAttributes().size(), is(2));
            }
        }
    }

    /**
     * This test case checks the changes made in.
     * LinkStateAttributes (ATTR_NODE_MT_TOPOLOGY_ID check)
     * as bug fix for bug 8036
     */
    @Test
    public void bgpUpdateMessage2Test3() throws BgpParseException {
        byte[] updateMsg = new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x00, (byte) 0x8e, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x77, (byte) 0x90,
                (byte) 0x0e, (byte) 0x00, (byte) 0x40, (byte) 0x40, (byte) 0x04, (byte) 0x47, (byte) 0x10, (byte) 0x20,
                (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x50,
                (byte) 0x54, (byte) 0x00, (byte) 0xff, (byte) 0xfe, (byte) 0xb8, (byte) 0x35, (byte) 0x2b, (byte) 0x00,
                (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x27, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x2b, (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40,
                (byte) 0x02, (byte) 0x00, (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x80, (byte) 0x1d, (byte) 0x22,
                //------------ BGP-LS BgpAttrNodeMultiTopologyId Attribute---------------------------------------------
                (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x04, (byte) 0x02, (byte) 0x00, (byte) 0x11, (byte) 0x62, (byte) 0x67, (byte) 0x70, (byte) 0x5f,
                (byte) 0x6f, (byte) 0x6e, (byte) 0x6f, (byte) 0x73, (byte) 0x5f, (byte) 0x72, (byte) 0x6f, (byte) 0x75,
                (byte) 0x74, (byte) 0x65, (byte) 0x72, (byte) 0x2d, (byte) 0x32, (byte) 0x04, (byte) 0x03, (byte) 0x00,
                (byte) 0x03, (byte) 0x49, (byte) 0x00, (byte) 0x01
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg receivedMsg = (BgpUpdateMsg) message;
        List<BgpValueType> pathAttr = receivedMsg.bgpPathAttributes().pathAttributes();
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof LinkStateAttributes) {
                List<BgpValueType> linkStateAttribList = ((LinkStateAttributes) attr).linkStateAttributes();
                ListIterator<BgpValueType> listIterator = linkStateAttribList.listIterator();
                while (listIterator.hasNext()) {
                    BgpValueType bgpLSAttrib = listIterator.next();
                    if (bgpLSAttrib instanceof BgpAttrNodeMultiTopologyId) {
                        assertThat(((BgpAttrNodeMultiTopologyId) bgpLSAttrib).attrMultiTopologyId().get(0),
                                is((short) 2));
                    }
                }
            }
        }
    }

    /**
     * This test case checks the changes made in.
     * LinkStateAttributes (For Link State Attribute Type 1173 i.e. Extended Administrative Group)
     * as bug fix for bug 8036
     */
    @Test
    public void bgpUpdateMessage2Test4() throws BgpParseException {
        byte[] updateMsg = new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x01, (byte) 0x19, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x90,
                (byte) 0x0e, (byte) 0x00, (byte) 0x6e, (byte) 0x40, (byte) 0x04, (byte) 0x47, (byte) 0x10, (byte) 0x20,
                (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x50,
                (byte) 0x54, (byte) 0x00, (byte) 0xff, (byte) 0xfe, (byte) 0xb8, (byte) 0x35, (byte) 0x2b, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x55, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0xe9, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x2b, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x1a, (byte) 0x02,
                (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x02,
                (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
                (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x1b, (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x70, (byte) 0x70, (byte) 0x70,
                (byte) 0xd5, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x70, (byte) 0x70, (byte) 0x70,
                (byte) 0xd4, (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40, (byte) 0x02, (byte) 0x00,
                (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x80,
                (byte) 0x1d, (byte) 0x7f, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0xc0, (byte) 0xa8,
                (byte) 0x07, (byte) 0xca, (byte) 0x04, (byte) 0x06, (byte) 0x00, (byte) 0x04, (byte) 0xc0, (byte) 0xa8,
                (byte) 0x07, (byte) 0xc9, (byte) 0x04, (byte) 0x40, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x41, (byte) 0x00, (byte) 0x04, (byte) 0x49, (byte) 0xb7,
                (byte) 0x1b, (byte) 0x00, (byte) 0x04, (byte) 0x42, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x43, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x44,
                (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x04, (byte) 0x47,
                (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
                //------------ BGP-LS ATTR_EXTNDED_ADMNSTRATIVE_GRP Attribute------------------------------------------
                (byte) 0x04, (byte) 0x95, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                //-----------------------------------------------------------------------------------------------------
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg receivedMsg = (BgpUpdateMsg) message;
        List<BgpValueType> pathAttr = receivedMsg.bgpPathAttributes().pathAttributes();
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof LinkStateAttributes) {
                assertThat(((LinkStateAttributes) attr).linkStateAttributes().size(), is(9));
            }
        }
    }


    /**
     * This test case checks the changes made in.
     * MpReachNlri (read method AFI 2/SAFI 1 else if part)
     * as bug fix for bug 8036
     */
    @Test
    public void bgpUpdateMessage2Test5() throws BgpParseException {
        byte[] updateMsg = new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x00, (byte) 0x57, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x90,
                (byte) 0x0e, (byte) 0x00, (byte) 0x27,
                //------------ AFI = 2 --------------------------------------------------------------------------------
                (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                //------------ SAFI = 1 -------------------------------------------------------------------------------
                (byte) 0x01,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x10,
                //------------ MpReachNlri nexthop --------------------------------------------------------------------
                (byte) 0x20, (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00,
                (byte) 0x50, (byte) 0x54, (byte) 0x00, (byte) 0xff, (byte) 0xfe, (byte) 0xb8, (byte) 0x35, (byte) 0x2b,
                //-----------------------------------------------------------------------------------------------------
                (byte) 0x00, (byte) 0x40, (byte) 0x20, (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x43, (byte) 0x21,
                (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x20, (byte) 0x01, (byte) 0x05, (byte) 0xb0, (byte) 0x00,
                (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x40,
                (byte) 0x02, (byte) 0x00, (byte) 0x80, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x64, (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg receivedMsg = (BgpUpdateMsg) message;
        List<BgpValueType> pathAttr = receivedMsg.bgpPathAttributes().pathAttributes();
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof MpReachNlri) {
                assertThat(((MpReachNlri) attr).nexthop().toString(), is("2001:5b0:8:0:5054:ff:feb8:352b"));
                assertThat(((MpReachNlri) attr).getAfi(), is((short) 2));
                assertThat(((MpReachNlri) attr).getSafi(), is((byte) 1));
            }
        }
    }


    /**
     * This test case checks the changes made in.
     * MpUnReachNlri (read method AFI 2/SAFI 1 else if part)
     * as bug fix for bug 8036
     */
    @Test
    public void bgpUpdateMessage2Test6() throws BgpParseException {
        byte[] updateMsg = new byte[]{
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0x00, (byte) 0x1d, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x80,
                (byte) 0x0f, (byte) 0x03,
                //------------ AFI = 2 --------------------------------------------------------------------------------
                (byte) 0x00, (byte) 0x02,
                //-----------------------------------------------------------------------------------------------------
                //------------ SAFI = 1 -------------------------------------------------------------------------------
                (byte) 0x01
                //-----------------------------------------------------------------------------------------------------
        };

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message = null;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);

        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg receivedMsg = (BgpUpdateMsg) message;
        List<BgpValueType> pathAttr = receivedMsg.bgpPathAttributes().pathAttributes();
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof MpUnReachNlri) {
                assertThat(((MpUnReachNlri) attr).getAfi(), is((short) 2));
                assertThat(((MpUnReachNlri) attr).getSafi(), is((byte) 1));
            }
        }
    }
}
