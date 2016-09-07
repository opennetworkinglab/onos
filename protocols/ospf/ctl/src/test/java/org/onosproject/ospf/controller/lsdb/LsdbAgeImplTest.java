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
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.controller.LsaBin;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.area.OspfAreaImpl;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsdbAgeImpl.
 */
public class LsdbAgeImplTest {
    private LsdbAgeImpl lsdbAge;
    private OspfAreaImpl ospfAreaImpl;
    private LsaBinImpl lsaBin;
    private LsaBin lsaBin1;
    private LsaWrapperImpl lsaWrapper;
    private OspfArea ospfArea;
    private Channel channel;

    @Before
    public void setUp() throws Exception {
        ospfAreaImpl = EasyMock.createMock(OspfAreaImpl.class);
        lsdbAge = new LsdbAgeImpl(ospfAreaImpl);
    }

    @After
    public void tearDown() throws Exception {
        lsdbAge = null;
        lsaBin = null;
        lsaBin1 = null;
        ospfAreaImpl = null;
        lsaWrapper = null;
        channel = null;
        ospfArea = null;
    }

    /**
     * Tests getLsaBin() method.
     */
    @Test
    public void testGetLsaBin() throws Exception {
        lsaBin = new LsaBinImpl(1);
        lsdbAge.addLsaBin(1, lsaBin);
        assertThat(lsdbAge, is(notNullValue()));
        lsaBin1 = lsdbAge.getLsaBin(1);
        assertThat(lsaBin, instanceOf(LsaBin.class));
        assertThat(lsaBin1, instanceOf(LsaBin.class));
    }

    /**
     * Tests addLsaBin() method.
     */
    @Test
    public void testAddLsaBin() throws Exception {
        lsaBin = new LsaBinImpl(1);
        lsdbAge.addLsaBin(1, lsaBin);
        assertThat(lsdbAge, is(notNullValue()));
        assertThat(lsaBin, instanceOf(LsaBin.class));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(lsdbAge.equals(lsdbAge), is(true));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        int hashCode = lsdbAge.hashCode();
        assertThat(hashCode, is(notNullValue()));
    }

    /**
     * Tests addLsaToMaxAgeBin() method.
     */
    @Test
    public void testAddLsaToMaxAgeBin() throws Exception {
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa1", lsaWrapper);
        assertThat(lsdbAge, is(notNullValue()));
    }

    /**
     * Tests removeLsaFromBin() method.
     */
    @Test
    public void testRemoveLsaFromBin() throws Exception {
        lsaBin = EasyMock.createMock(LsaBinImpl.class);
        lsaWrapper = new LsaWrapperImpl();
        lsaWrapper.setBinNumber(-1);
        lsaBin.addOspfLsa("1", lsaWrapper);
        lsdbAge.startDbAging();
        lsdbAge.addLsaToMaxAgeBin("3600", lsaWrapper);
        lsdbAge.addLsaBin(-1, lsaBin);
        lsdbAge.removeLsaFromBin(lsaWrapper);
        assertThat(lsdbAge, is(notNullValue()));
    }

    /**
     * Tests startDbAging() method.
     */
    @Test
    public void testStartDbAging() throws Exception {
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa1", lsaWrapper);
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa2", lsaWrapper);
        lsdbAge.startDbAging();
        assertThat(lsdbAge, is(notNullValue()));
    }

    /**
     * Tests ageLsaAndFlood() method.
     */
    @Test
    public void testAgeLsaAndFlood() throws Exception {
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa1", lsaWrapper);
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa2", lsaWrapper);
        lsdbAge.startDbAging();
        lsdbAge.ageLsaAndFlood();
        Assert.assertNotNull(lsdbAge);
    }

    /**
     * Tests maxAgeLsa() method.
     */
    @Test
    public void testMaxageLsa() throws Exception {
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        ospfArea = new OspfAreaImpl();
        lsdbAge = new LsdbAgeImpl(ospfArea);
        lsaWrapper.setLsdbAge(lsdbAge);
        lsdbAge.addLsaToMaxAgeBin("lsa1", lsaWrapper);
        lsaBin = new LsaBinImpl(1);
        lsaBin.addOspfLsa("1", lsaWrapper);
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa2", lsaWrapper);
        lsaBin.addOspfLsa("2", lsaWrapper);
        lsdbAge.startDbAging();
        lsdbAge = new LsdbAgeImpl(new OspfAreaImpl());
        lsdbAge.ageLsaAndFlood();
        lsdbAge.maxAgeLsa();
        assertThat(lsdbAge, is(notNullValue()));

    }

    /**
     * Tests refreshLsa() method.
     */
    @Test
    public void testRefereshLsa() throws Exception {
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsaWrapper.setBinNumber(0);
        lsaWrapper.setLsaType(OspfLsaType.NETWORK);
        lsdbAge.addLsaToMaxAgeBin("lsa1", lsaWrapper);
        lsdbAge.ageLsaAndFlood();
        lsdbAge.startDbAging();
        lsdbAge.refreshLsa();
        assertThat(lsdbAge, is(notNullValue()));
    }

    /**
     * Tests checkAges() method.
     */
    @Test
    public void testCheckAges() throws Exception {
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa1", lsaWrapper);
        lsaWrapper = EasyMock.createMock(LsaWrapperImpl.class);
        lsdbAge.addLsaToMaxAgeBin("lsa2", lsaWrapper);
        lsdbAge.startDbAging();
        lsdbAge.checkAges();
        assertThat(lsdbAge, is(notNullValue()));

    }

    /**
     * Tests getChannel() getter method.
     */
    @Test
    public void testGetChannel() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        lsdbAge.setChannel(channel);
        assertThat(lsdbAge.getChannel(), is(notNullValue()));
    }

    /**
     * Tests setChannel() setter method.
     */
    @Test
    public void testSetChannel() throws Exception {
        channel = EasyMock.createMock(Channel.class);
        lsdbAge.setChannel(channel);
        assertThat(lsdbAge.getChannel(), is(notNullValue()));
    }

    /**
     * Tests getAgeCounter() method.
     */
    @Test
    public void testGetAgeCounter() throws Exception {
        lsaBin = new LsaBinImpl(1);
        lsdbAge.addLsaBin(1, lsaBin);
        int age = lsdbAge.getAgeCounter();
        assertThat(age, is(notNullValue()));
    }

    /**
     * Tests getAgeCounterRollOver() method.
     */
    @Test
    public void testGetAgeCounterRollOver() throws Exception {
        lsaBin = new LsaBinImpl(1);
        lsdbAge.addLsaBin(1, lsaBin);
        lsdbAge.startDbAging();
        assertThat(lsdbAge.getAgeCounterRollOver(), is(notNullValue()));
    }

    /**
     * Tests getMaxAgeBin() method.
     */
    @Test
    public void testGetMaxAgeBin() throws Exception {
        lsaBin = new LsaBinImpl(1);
        lsdbAge.addLsaBin(1, lsaBin);
        lsdbAge.startDbAging();
        assertThat(lsdbAge.getMaxAgeBin(), is(notNullValue()));
    }

    /**
     * Tests age2Bin() method.
     */
    @Test
    public void testAge2Bin() throws Exception {
        int age = lsdbAge.age2Bin(0);
        assertThat(age, is(notNullValue()));
    }
}