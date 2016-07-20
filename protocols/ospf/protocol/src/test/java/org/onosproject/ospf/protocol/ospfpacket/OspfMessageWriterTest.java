/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.ospf.protocol.ospfpacket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for OspfMessageWriter.
 */
public class OspfMessageWriterTest {

    private OspfMessageWriter ospfMessageWriter;
    private HelloPacket helloPacket;
    private DdPacket ddPacket;
    private LsAcknowledge lsAck;
    private LsRequest lsReq;
    private LsUpdate lsUpdate;

    @Before
    public void setUp() throws Exception {
        ospfMessageWriter = new OspfMessageWriter();
    }

    @After
    public void tearDown() throws Exception {
        ospfMessageWriter = null;
        helloPacket = null;
        ddPacket = null;
        lsAck = null;
        lsReq = null;
        lsUpdate = null;
    }

    /**
     * Tests getMessage() method.
     */
    @Test
    public void testGetMessage() throws Exception {
        helloPacket = new HelloPacket();
        helloPacket.setAuthType(1);
        helloPacket.setOspftype(1);
        helloPacket.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        helloPacket.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        helloPacket.setChecksum(201);
        helloPacket.setAuthentication(2);
        helloPacket.setOspfPacLength(48);
        helloPacket.setOspfVer(2);
        helloPacket.setNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        helloPacket.setOptions(2); //not setting now
        helloPacket.setHelloInterval(10);
        helloPacket.setRouterPriority(1);
        helloPacket.setRouterDeadInterval(40);
        helloPacket.setDr(Ip4Address.valueOf("1.1.1.1"));
        helloPacket.setBdr(Ip4Address.valueOf("2.2.2.2"));
        helloPacket.addNeighbor(Ip4Address.valueOf("8.8.8.8"));
        helloPacket.setDestinationIp(Ip4Address.valueOf("5.5.5.5"));
        ospfMessageWriter.getMessage(helloPacket, 7, 1);
        assertThat(ospfMessageWriter, is(notNullValue()));
    }

    @Test(expected = Exception.class)
    public void testGetMessage1() throws Exception {

        ddPacket = new DdPacket();
        ddPacket.setAuthType(1);
        ddPacket.setOspftype(2);
        ddPacket.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        ddPacket.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        ddPacket.setChecksum(201);
        ddPacket.setAuthentication(2);
        ddPacket.setOspfPacLength(48);
        ddPacket.setOspfVer(2);
        ospfMessageWriter.getMessage(ddPacket, 1, 1);
        assertThat(ospfMessageWriter, is(notNullValue()));
    }

    @Test(expected = Exception.class)
    public void testGetMessage2() throws Exception {

        lsAck = new LsAcknowledge();
        lsAck.setAuthType(1);
        lsAck.setOspftype(5);
        lsAck.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsAck.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        lsAck.setChecksum(201);
        lsAck.setAuthentication(2);
        lsAck.setOspfPacLength(48);
        lsAck.setOspfVer(2);
        ospfMessageWriter.getMessage(lsAck, 1, 1);
        assertThat(ospfMessageWriter, is(notNullValue()));
    }

    @Test(expected = Exception.class)
    public void testGetMessage3() throws Exception {
        lsReq = new LsRequest();
        lsReq.setAuthType(1);
        lsReq.setOspftype(3);
        lsReq.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsReq.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        lsReq.setChecksum(201);
        lsReq.setAuthentication(2);
        lsReq.setOspfPacLength(48);
        lsReq.setOspfVer(2);
        ospfMessageWriter.getMessage(lsReq, 1, 1);
        assertThat(ospfMessageWriter, is(notNullValue()));
    }

    /**
     * Tests getMessage() method.
     */
    @Test(expected = Exception.class)
    public void testGetMessage4() throws Exception {
        lsUpdate = new LsUpdate();
        lsUpdate.setAuthType(1);
        lsUpdate.setOspftype(3);
        lsUpdate.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsUpdate.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        lsUpdate.setChecksum(201);
        lsUpdate.setAuthentication(2);
        lsUpdate.setOspfPacLength(48);
        lsUpdate.setOspfVer(2);
        ospfMessageWriter.getMessage(lsUpdate, 1, 1);
        assertThat(ospfMessageWriter, is(notNullValue()));
    }

    /**
     * Tests getMessage() method.
     */
    @Test(expected = Exception.class)
    public void testGetMessage5() throws Exception {
        lsAck = new LsAcknowledge();
        lsAck.setAuthType(1);
        lsAck.setOspftype(5);
        lsAck.setRouterId(Ip4Address.valueOf("10.226.165.164"));
        lsAck.setAreaId(Ip4Address.valueOf("10.226.165.100"));
        lsAck.setChecksum(201);
        lsAck.setAuthentication(2);
        lsAck.setOspfPacLength(48);
        lsAck.setOspfVer(2);
        ospfMessageWriter.getMessage(lsAck, 1, 1);
    }
}