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
package org.onosproject.ospf.protocol.lsa.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfExternalDestination;

import java.net.UnknownHostException;
import java.util.Vector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for ExternalLsa.
 */
public class ExternalLsaTest {

    private static final Ip4Address LOCAL_ADDRESS = Ip4Address.valueOf("127.0.0.1");

    private ExternalLsa externalLsa;
    private Vector<OspfExternalDestination> externalDestinations = new Vector<OspfExternalDestination>();
    private Ip4Address result;
    private OspfExternalDestination ospfExternalDestination;
    private OspfExternalDestination ospfExternalDestination1;
    private LsaHeader lsaHeader;
    private byte[] inputByteArray;
    private ChannelBuffer channelBuffer;
    private byte[] result1;
    private OspfLsaType ospflsaType;
    private int result2;

    @Before
    public void setUp() throws Exception {
        externalLsa = new ExternalLsa(new LsaHeader());
    }

    @After
    public void tearDown() throws Exception {
        externalLsa = null;
        externalDestinations = null;
        result = null;
        ospfExternalDestination = null;
        ospfExternalDestination1 = null;
        lsaHeader = null;
        inputByteArray = null;
        channelBuffer = null;
        result1 = null;
        ospflsaType = null;
    }

    /**
     * Tests networkMask() getter method.
     */
    @Test
    public void testGetNetworkMask() throws Exception {
        externalLsa.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = externalLsa.networkMask();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests networkMask() setter method.
     */
    @Test
    public void testSetNetworkMask() throws Exception {
        externalLsa.setNetworkMask(Ip4Address.valueOf("10.226.165.164"));
        result = externalLsa.networkMask();
        assertThat(result, is(notNullValue()));
        assertThat(result, is(Ip4Address.valueOf("10.226.165.164")));
    }

    /**
     * Tests addExternalDesitnation() method.
     */
    @Test
    public void testAddExternalDesitnation() throws Exception {
        externalLsa.addExternalDestination(createOspfExternalDestination());
        assertThat(externalLsa, is(notNullValue()));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashcode() throws Exception {

        result2 = externalLsa.hashCode();
        assertThat(result2, is(notNullValue()));

    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        ospfExternalDestination = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(2);
        ospfExternalDestination.setMetric(100);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination);
        ospfExternalDestination1 = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(3);
        ospfExternalDestination.setMetric(50);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination1);
        ospfExternalDestination.setForwardingAddress(LOCAL_ADDRESS);
        inputByteArray = createByteForNetworkLsa();
        lsaHeader = createLsaHeader();
        externalLsa = new ExternalLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        externalLsa.readFrom(channelBuffer);
        assertThat(externalLsa, is(notNullValue()));
    }

    /**
     * Tests readFrom() method.
     */
    @Test(expected = Exception.class)
    public void testReadFrom1() throws Exception {
        ospfExternalDestination = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(2);
        ospfExternalDestination.setMetric(100);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination);
        ospfExternalDestination1 = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(3);
        ospfExternalDestination.setMetric(50);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination1);
        ospfExternalDestination.setForwardingAddress(LOCAL_ADDRESS);
        byte[] temp = {0, 0, 0};
        inputByteArray = temp;
        lsaHeader = createLsaHeader();
        externalLsa = new ExternalLsa(lsaHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(inputByteArray);
        externalLsa.readFrom(channelBuffer);
        assertThat(externalLsa, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result1 = externalLsa.asBytes();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test
    public void testGetLsaBodyAsByteArray() throws Exception {
        ospfExternalDestination = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(2);
        ospfExternalDestination.setMetric(100);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination);
        ospfExternalDestination1 = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(3);
        ospfExternalDestination.setMetric(100);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination1);
        result1 = externalLsa.getLsaBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getLsaBodyAsByteArray() method.
     */
    @Test
    public void testGetLsaBodyAsByteArray1() throws Exception {
        externalLsa.setNetworkMask(Ip4Address.valueOf("255.255.255.255"));
        ospfExternalDestination = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(2);
        ospfExternalDestination.setMetric(100);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination);
        ospfExternalDestination1 = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(3);
        ospfExternalDestination.setMetric(100);
        ospfExternalDestination.setType1orType2Metric(true);
        externalLsa.addExternalDestination(ospfExternalDestination1);
        result1 = externalLsa.getLsaBodyAsByteArray();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests getOspfLsaType() getter method.
     */
    @Test
    public void testGetOspfLsaType() throws Exception {
        ospflsaType = externalLsa.getOspfLsaType();
        assertThat(ospflsaType, is(notNullValue()));
        assertThat(ospflsaType, is(OspfLsaType.EXTERNAL_LSA));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(externalLsa.toString(), is(notNullValue()));
    }

    /**
     * Utility method used by junit methods.
     */
    private byte[] createByteForNetworkLsa() {
        byte[] packet = {2, 1, 1, 52, -64, -88, 56, 1, -64, -88, 56, 1, 0, 100, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, -64,
                -88, 56, 1, 0, 10, 1, 1, 0, 0, 0, 40, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56, 1, -64, -88, 56,
                1};
        return packet;
    }

    /**
     * Utility method used by junit methods.
     */
    private LsaHeader createLsaHeader() {
        lsaHeader = new LsaHeader();
        lsaHeader.setLsType(1);
        lsaHeader.setLsPacketLen(48);
        lsaHeader.setLsCheckSum(10);
        lsaHeader.setAge(4);
        lsaHeader.setLinkStateId("10.226.165.164");
        lsaHeader.setLsSequenceNo(250);
        lsaHeader.setAdvertisingRouter(Ip4Address.valueOf("100.226.165.165"));
        lsaHeader.setOptions(2);
        return lsaHeader;
    }

    /**
     * Utility method used by junit methods.
     */
    private OspfExternalDestination createOspfExternalDestination() throws UnknownHostException {
        ospfExternalDestination = new OspfExternalDestination();
        ospfExternalDestination.setExternalRouterTag(1);
        ospfExternalDestination.setMetric(10);
        ospfExternalDestination.setType1orType2Metric(true);
        ospfExternalDestination.setForwardingAddress(LOCAL_ADDRESS);
        return ospfExternalDestination;
    }
}
