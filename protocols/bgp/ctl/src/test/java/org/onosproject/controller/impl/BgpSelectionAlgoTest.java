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
package org.onosproject.controller.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onosproject.bgp.controller.impl.BgpSelectionAlgo;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4.ProtocolType;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetailsLocalRib;
import org.onosproject.bgpio.types.AsPath;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.LocalPref;
import org.onosproject.bgpio.types.Med;
import org.onosproject.bgpio.types.Origin;

import java.util.LinkedList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for BGP Selection Algorithm.
 */
public class BgpSelectionAlgoTest {

    /**
     * firstPathAttribute and secondPathAttribute has same AS count and firstPathAttribute
     * has shortest Origin value than secondPathAttribute.
     */
    @Test
    public void selectionAlgoTest1() throws BgpParseException {
        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        //origin with IGP
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        //AsPath with AS_SEQ with one AS
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xea };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        int bgpId = 168427777;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        //origin with INCOMPLETE
        origin = new byte[] {0x40, 0x01, 0x01, 0x02 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        //AsPath with AS_SEQ with one AS
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        bgpId = 536936448;
        locRibAsNum = 200;
        isIbgp = true;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(1));
    }

    /**
     * firstPathAttribute has 1 AS count and secondPathAttribute has 2 AS count
     * and firstPathAttribute has shortest Origin value than secondPathAttribute.
     */
    @Test
    public void selectionAlgoTest2() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        int bgpId = 168427777;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x02 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x08, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xea, 0x02, 0x01, (byte) 0xfd, (byte) 0xea };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        bgpId = 536936448;
        locRibAsNum = 200;
        isIbgp = true;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(-1));
    }

    /**
     * firstPathAttribute and secondPathAttribute has same AS value
     * and firstPathAttribute has shortest Origin value than secondPathAttribute.
     */
    @Test
    public void selectionAlgoTest3() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        int bgpId = 168427777;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x02 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        bgpId = 536936448;
        locRibAsNum = 200;
        isIbgp = true;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(1));
    }

    /**
     * firstPathAttribute has lowest med than secondPathAttribute.
     */
    @Test
    public void selectionAlgoTest4() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] med = new byte[] {(byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00,
                0x00 };
        buffer.writeBytes(med);
        pathAttribute1 = Med.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        int bgpId = 168427777;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x02 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        med = new byte[] {(byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x01 };
        buffer.writeBytes(med);
        pathAttribute2 = Med.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        bgpId = 536936448;
        locRibAsNum = 200;
        isIbgp = true;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(1));
    }

    /**
     * secondPathAttribute has higher local preference than firstPathAttribute.
     */
    @Test
    public void selectionAlgoTest5() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] locPref = new byte[] {(byte) 0x00, 0x05, 0x04, 0x00, 0x00,
                0x00, 0x01 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(locPref);
        pathAttribute1 = LocalPref.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        int bgpId = 168427777;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        locPref = new byte[] {(byte) 0x00, 0x05, 0x04, 0x00, 0x00, 0x00, 0x0a };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(locPref);
        pathAttribute2 = LocalPref.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        bgpId = 536936448;
        locRibAsNum = 200;
        isIbgp = true;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(-1));
    }

    /**
     * secondPathAttribute is EBGP than firstPathAttribute is IBGP.
     */
    @Test
    public void selectionAlgoTest6() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        int bgpId = 168427777;
        short locRibAsNum = 100;
        boolean isIbgp = true;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        bgpId = 536936448;
        locRibAsNum = 200;
        isIbgp = false;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, false, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(-1));
    }

    /**
     * firstPathAttribute has lower BGPID than secondPathAttribute.
     */
    @Test
    public void selectionAlgoTest7() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        //A0A0A00
        Integer bgpId = 168430080;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        //B0A0A00
        bgpId = 185207296;
        locRibAsNum = 200;
        isIbgp = false;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(1));
    }

    /**
     * secondPathAttribute has lowest peer address than firstPathAttribute.
     */
    @Test
    public void selectionAlgoTest8() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0b, 0x0b, 0x0b, 0x0b };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        //A0A0A00
        Integer bgpId = 168430080;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        //A0A0A00
        bgpId = 168430080;
        locRibAsNum = 200;
        isIbgp = false;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(-1));
    }

    /**
     * firstPathAttribute and secondPathAttribute are same.
     */
    @Test
    public void selectionAlgoTest9() throws BgpParseException {

        byte[] peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes1 = new LinkedList<>();
        BgpValueType pathAttribute1;
        byte[] origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute1 = Origin.read(buffer);
        pathAttributes1.add(pathAttribute1);
        byte[] asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute1 = AsPath.read(buffer);
        pathAttributes1.add(pathAttribute1);

        IpAddress ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        //A0A0A00
        Integer bgpId = 168430080;
        short locRibAsNum = 100;
        boolean isIbgp = false;
        PathAttrNlriDetails attrList1 = new PathAttrNlriDetails();
        attrList1.setIdentifier(0);
        attrList1.setPathAttribute(pathAttributes1);
        attrList1.setProtocolID(ProtocolType.ISIS_LEVEL_ONE);
        PathAttrNlriDetailsLocalRib list1 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList1);

        peerIp = new byte[] {0x0a, 0x0a, 0x0a, 0x0a };
        LinkedList<BgpValueType> pathAttributes2 = new LinkedList<>();
        BgpValueType pathAttribute2;
        origin = new byte[] {0x40, 0x01, 0x01, 0x00 };
        buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(origin);
        pathAttribute2 = Origin.read(buffer);
        pathAttributes2.add(pathAttribute2);
        asPath = new byte[] {0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd,
                (byte) 0xe9 };
        buffer.writeBytes(asPath);
        pathAttribute2 = AsPath.read(buffer);
        pathAttributes2.add(pathAttribute2);

        ipAddress = IpAddress.valueOf(Version.INET, peerIp);
        //A0A0A00
        bgpId = 168430080;
        locRibAsNum = 200;
        isIbgp = false;
        PathAttrNlriDetails attrList2 = new PathAttrNlriDetails();
        attrList2.setIdentifier(0);
        attrList2.setPathAttribute(pathAttributes2);
        attrList2.setProtocolID(ProtocolType.OSPF_V2);
        PathAttrNlriDetailsLocalRib list2 = new PathAttrNlriDetailsLocalRib(
                ipAddress, bgpId, locRibAsNum, isIbgp, attrList2);
        BgpSelectionAlgo algo = new BgpSelectionAlgo();
        int result = algo.compare(list1, list2);
        assertThat(result, is(0));
    }
}
