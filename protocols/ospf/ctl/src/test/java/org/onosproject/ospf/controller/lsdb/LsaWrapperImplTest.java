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
package org.onosproject.ospf.controller.lsdb;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsaWrapperImpl.
 */
public class LsaWrapperImplTest {

    private LsaWrapperImpl lsaWrapper;
    private LsdbAgeImpl lsdbAge;
    private LsaHeader header;
    private OspfInterfaceImpl ospfInterfaceImpl;

    @Before
    public void setUp() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
    }

    @After
    public void tearDown() throws Exception {
        lsaWrapper = null;
        header = null;
        lsdbAge = null;
        ospfInterfaceImpl = null;
    }

    /**
     * Tests lsaType() getter method.
     */
    @Test
    public void testGetLsaType() throws Exception {
        lsaWrapper.setLsaType(OspfLsaType.ROUTER);
        assertThat(lsaWrapper.lsaType(), is(OspfLsaType.ROUTER));
    }

    /**
     * Tests lsaType() setter method.
     */
    @Test
    public void testSetLsaType() throws Exception {
        lsaWrapper.setLsaType(OspfLsaType.ROUTER);
        assertThat(lsaWrapper.lsaType(), is(OspfLsaType.ROUTER));
    }

    /**
     * Tests isSelfOriginated() getter method.
     */
    @Test
    public void testIsSelfOriginated() throws Exception {
        lsaWrapper.setIsSelfOriginated(true);
        assertThat(lsaWrapper.isSelfOriginated(), is(true));
    }

    /**
     * Tests isSelfOriginated() setter method.
     */
    @Test
    public void testSetIsSelfOriginated() throws Exception {
        lsaWrapper.setIsSelfOriginated(true);
        assertThat(lsaWrapper.isSelfOriginated(), is(true));
    }

    /**
     * Tests addLsa() method.
     */
    @Test
    public void testAddLsa() throws Exception {
        lsaWrapper.addLsa(OspfLsaType.ROUTER, new RouterLsa());
        assertThat(lsaWrapper, is(notNullValue()));
    }

    /**
     * Tests lsaAgeReceived() getter method.
     */
    @Test
    public void testGetLsaAgeReceived() throws Exception {
        lsaWrapper.setLsaAgeReceived(10);
        assertThat(lsaWrapper.lsaAgeReceived(), is(10));
    }

    /**
     * Tests lsaAgeReceived() setter method.
     */
    @Test
    public void testSetLsaAgeReceived() throws Exception {
        lsaWrapper.setLsaAgeReceived(10);
        assertThat(lsaWrapper.lsaAgeReceived(), is(10));
    }

    /**
     * Tests lsaHeader() getter method.
     */
    @Test
    public void testGetLsaHeader() throws Exception {
        lsdbAge = new LsdbAgeImpl(new OspfAreaImpl());
        lsdbAge.ageLsaAndFlood();
        lsaWrapper.setLsdbAge(lsdbAge);
        header = new LsaHeader();
        lsaWrapper.setLsaHeader(header);
        assertThat(lsaWrapper.lsaHeader(), instanceOf(LsaHeader.class));
    }

    /**
     * Tests lsaHeader() setter method.
     */
    @Test
    public void testSetLsaHeader() throws Exception {
        lsdbAge = new LsdbAgeImpl(new OspfAreaImpl());
        lsdbAge.ageLsaAndFlood();
        lsaWrapper.setLsdbAge(lsdbAge);
        header = new LsaHeader();
        lsaWrapper.setLsaHeader(header);
        assertThat(lsaWrapper.lsaHeader(), instanceOf(LsaHeader.class));
    }

    /**
     * Tests setOspfLsa() setter method.
     */
    @Test
    public void testSetOspfLsa() throws Exception {
        lsaWrapper.setOspfLsa(new RouterLsa());
        assertThat(lsaWrapper, is(notNullValue()));
    }

    /**
     * Tests noReTransmissionLists() getter method.
     */
    @Test
    public void testGetNoReTransmissionLists() throws Exception {
        lsaWrapper.setNoReTransmissionLists(10);
        assertThat(lsaWrapper.noReTransmissionLists(), is(10));
    }

    /**
     * Tests noReTransmissionLists() setter method.
     */
    @Test
    public void testSetNoReTransmissionLists() throws Exception {
        lsaWrapper.setNoReTransmissionLists(10);
        assertThat(lsaWrapper.noReTransmissionLists(), is(10));
    }

    /**
     * Tests isInAnAgeBin() getter method.
     */
    @Test
    public void testIsInAnAgeBin() throws Exception {
        lsaWrapper.setInAnAgeBin(true);
        assertThat(lsaWrapper.isInAnAgeBin(), is(true));
    }

    /**
     * Tests isInAnAgeBin() setter method.
     */
    @Test
    public void testSetInAnAgeBin() throws Exception {
        lsaWrapper.setInAnAgeBin(true);
        assertThat(lsaWrapper.isInAnAgeBin(), is(true));
    }

    /**
     * Tests isChangedSinceLastFlood() getter method.
     */
    @Test
    public void testIsChangedSinceLastFlood() throws Exception {
        lsaWrapper.setChangedSinceLastFlood(true);
        assertThat(lsaWrapper.isChangedSinceLastFlood(), is(true));
    }

    /**
     * Tests isChangedSinceLastFlood() setter method.
     */
    @Test
    public void testSetChangedSinceLastFlood() throws Exception {
        lsaWrapper.setChangedSinceLastFlood(true);
        assertThat(lsaWrapper.isChangedSinceLastFlood(), is(true));
    }

    /**
     * Tests isSequenceRollOver() method.
     */
    @Test
    public void testIsSequenceRollOver() throws Exception {
        lsaWrapper.setIsSequenceRollOver(true);
        assertThat(lsaWrapper.isSequenceRollOver(), is(true));
    }

    /**
     * Tests isSentReplyForOlderLsa() method.
     */
    @Test
    public void testIsSentReplyForOlderLsa() throws Exception {
        lsaWrapper.setSentReplyForOlderLsa(true);
        assertThat(lsaWrapper.isSentReplyForOlderLsa(), is(true));
    }

    /**
     * Tests isCheckAge() getter method.
     */
    @Test
    public void testIsCheckAge() throws Exception {
        lsaWrapper.setCheckAge(true);
        assertThat(lsaWrapper.isCheckAge(), is(true));
    }

    /**
     * Tests isSentReplyForOlderLsa() getter method.
     */
    @Test
    public void testSetSentReplyForOlderLsa() throws Exception {
        lsaWrapper.setSentReplyForOlderLsa(true);
        assertThat(lsaWrapper.isSentReplyForOlderLsa(), is(true));
    }

    /**
     * Tests isSentReplyForOlderLsa() setter method.
     */
    @Test
    public void testSetCheckAge() throws Exception {
        lsaWrapper.setCheckAge(true);
        assertThat(lsaWrapper.isCheckAge(), is(true));
    }

    /**
     * Tests isAging() getter method.
     */
    @Test
    public void testIsAging() throws Exception {
        lsaWrapper.setIsAging(true);
        assertThat(lsaWrapper.isAging(), is(true));
    }

    /**
     * Tests isAging() setter method.
     */
    @Test
    public void testSetIsAging() throws Exception {
        lsaWrapper.setIsAging(true);
        assertThat(lsaWrapper.isAging(), is(true));
    }

    /**
     * Tests currentAge() method.
     */
    @Test
    public void testGetCurrentAge() throws Exception {
        lsdbAge = new LsdbAgeImpl(new OspfAreaImpl());
        lsdbAge.ageLsaAndFlood();
        lsaWrapper.setLsdbAge(lsdbAge);
        assertThat(lsaWrapper.currentAge(), is(notNullValue()));
    }

    /**
     * Tests ageCounterWhenReceived() getter method.
     */
    @Test
    public void testGetAgeCounterWhenReceived() throws Exception {
        lsaWrapper.setAgeCounterWhenReceived(10);
        assertThat(lsaWrapper.ageCounterWhenReceived(), is(10));
    }

    /**
     * Tests ageCounterWhenReceived() setter method.
     */
    @Test
    public void testSetAgeCounterWhenReceived() throws Exception {
        lsaWrapper.setAgeCounterWhenReceived(10);
        assertThat(lsaWrapper.ageCounterWhenReceived(), is(10));
    }

    /**
     * Tests lsaProcessing() getter method.
     */
    @Test
    public void testGetLsaProcessing() throws Exception {
        lsaWrapper.setLsaProcessing("router");
        assertThat(lsaWrapper.lsaProcessing(), is("router"));
    }

    /**
     * Tests lsaProcessing() setter method.
     */
    @Test
    public void testSetLsaProcessing() throws Exception {
        lsaWrapper.setLsaProcessing("router");
        assertThat(lsaWrapper.lsaProcessing(), is("router"));
    }

    /**
     * Tests binNumber() getter method.
     */
    @Test
    public void testGetBinNumber() throws Exception {
        lsaWrapper.setBinNumber(10);
        assertThat(lsaWrapper.binNumber(), is(10));
    }

    /**
     * Tests binNumber() setter method.
     */
    @Test
    public void testSetBinNumber() throws Exception {
        lsaWrapper.setBinNumber(10);
        assertThat(lsaWrapper.binNumber(), is(10));
    }

    /**
     * Tests ospfInterface() getter method.
     */
    @Test
    public void testGetOspfInterface() throws Exception {
        ospfInterfaceImpl = EasyMock.createMock(OspfInterfaceImpl.class);
        lsaWrapper.setOspfInterface(ospfInterfaceImpl);
        assertThat(lsaWrapper.ospfInterface(), is(notNullValue()));
    }

    /**
     * Tests ospfInterface() setter method.
     */
    @Test
    public void testSetOspfInterface() throws Exception {
        ospfInterfaceImpl = EasyMock.createMock(OspfInterfaceImpl.class);
        lsaWrapper.setOspfInterface(ospfInterfaceImpl);
        assertThat(lsaWrapper.ospfInterface(), is(notNullValue()));
    }

    /**
     * Tests getLsdbAge() method.
     */
    @Test
    public void testGetLsdbAge() throws Exception {
        assertThat(lsaWrapper.getLsdbAge(), is(nullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lsaWrapper.toString(), is(notNullValue()));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(lsaWrapper.equals(new LsaWrapperImpl()), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        int hashCode = lsaWrapper.hashCode();
        assertThat(hashCode, is(notNullValue()));
    }
}