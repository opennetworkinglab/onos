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
package org.onosproject.isis.controller.impl.lsdb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisLsdbAge;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.impl.DefaultIsisInterface;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultLspWrapper.
 */
public class DefaultLspWrapperTest {

    private DefaultLspWrapper defaultLspWrapper;
    private String processing = "processing";
    private String result;
    private int result1;
    private IsisInterface isisInterface;
    private IsisInterface result2;
    private IsisPduType isisPduType;
    private boolean result3;
    private LsPdu lsPdu;
    private LsPdu pdu;
    private DefaultIsisLsdbAge defaultIsisLsdbAge;
    private IsisLsdbAge lsdbAge;

    @Before
    public void setUp() throws Exception {
        defaultLspWrapper = new DefaultLspWrapper();
        isisInterface = new DefaultIsisInterface();
        pdu = new LsPdu(new IsisHeader());
        defaultIsisLsdbAge = new DefaultIsisLsdbAge();
        defaultIsisLsdbAge.startDbAging();
    }

    @After
    public void tearDown() throws Exception {
        defaultLspWrapper = null;
    }

    /**
     * Tests lspProcessing() getter method.
     */
    @Test
    public void testLspProcessing() throws Exception {
        defaultLspWrapper.setLspProcessing(processing);
        result = defaultLspWrapper.lspProcessing();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests lspProcessing() setter method.
     */
    @Test
    public void testSetLspProcessing() throws Exception {
        defaultLspWrapper.setLspProcessing(processing);
        result = defaultLspWrapper.lspProcessing();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests lspAgeReceived() getter method.
     */
    @Test
    public void testLspAgeReceived() throws Exception {
        defaultLspWrapper.setLspAgeReceived(1);
        result1 = defaultLspWrapper.lspAgeReceived();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests lspAgeReceived() setter method.
     */
    @Test
    public void testSetLspAgeReceived() throws Exception {
        defaultLspWrapper.setLspAgeReceived(1);
        result1 = defaultLspWrapper.lspAgeReceived();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests lspAgeReceived() getter method.
     */
    @Test
    public void testIsisInterface() throws Exception {
        defaultLspWrapper.setIsisInterface(isisInterface);
        result2 = defaultLspWrapper.isisInterface();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests lspAgeReceived() getter method.
     */
    @Test
    public void testSetIsisInterface() throws Exception {
        defaultLspWrapper.setIsisInterface(isisInterface);
        result2 = defaultLspWrapper.isisInterface();
        assertThat(result2, is(notNullValue()));
    }

    /**
     * Tests ageCounterWhenReceived() getter method.
     */
    @Test
    public void testAgeCounterWhenReceived() throws Exception {
        defaultLspWrapper.setAgeCounterWhenReceived(1);
        result1 = defaultLspWrapper.ageCounterWhenReceived();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests ageCounterWhenReceived() setter method.
     */
    @Test
    public void testSetAgeCounterWhenReceived() throws Exception {
        defaultLspWrapper.setAgeCounterWhenReceived(1);
        result1 = defaultLspWrapper.ageCounterWhenReceived();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests ageCounterRollOverWhenAdded() getter method.
     */
    @Test
    public void testAgeCounterRollOverWhenAdded() throws Exception {
        defaultLspWrapper.setAgeCounterRollOverWhenAdded(1);
        result1 = defaultLspWrapper.ageCounterRollOverWhenAdded();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests ageCounterRollOverWhenAdded() setter method.
     */
    @Test
    public void testSetAgeCounterRollOverWhenAdded() throws Exception {
        defaultLspWrapper.setAgeCounterRollOverWhenAdded(1);
        result1 = defaultLspWrapper.ageCounterRollOverWhenAdded();
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests lspType() getter method.
     */
    @Test
    public void testLspType() throws Exception {
        defaultLspWrapper.setLspType(IsisPduType.L1LSPDU);
        isisPduType = defaultLspWrapper.lspType();
        assertThat(isisPduType, is(IsisPduType.L1LSPDU));
    }

    /**
     * Tests lspType() setter method.
     */
    @Test
    public void testSetLspType() throws Exception {
        defaultLspWrapper.setLspType(IsisPduType.L1LSPDU);
        isisPduType = defaultLspWrapper.lspType();
        assertThat(isisPduType, is(IsisPduType.L1LSPDU));
    }

    /**
     * Tests isSelfOriginated() getter method.
     */
    @Test
    public void testIsSelfOriginated() throws Exception {
        defaultLspWrapper.setSelfOriginated(true);
        result3 = defaultLspWrapper.isSelfOriginated();
        assertThat(result3, is(true));
    }

    /**
     * Tests isSelfOriginated() setter method.
     */
    @Test
    public void testSetSelfOriginated() throws Exception {
        defaultLspWrapper.setSelfOriginated(true);
        result3 = defaultLspWrapper.isSelfOriginated();
        assertThat(result3, is(true));
    }

    /**
     * Tests binNumber() getter method.
     */
    @Test
    public void testBinNumber() throws Exception {
        defaultLspWrapper.setBinNumber(1);
        result1 = defaultLspWrapper.binNumber();
        assertThat(result1, is(1));
    }

    /**
     * Tests binNumber() setter method.
     */
    @Test
    public void testSetBinNumber() throws Exception {
        defaultLspWrapper.setBinNumber(1);
        result1 = defaultLspWrapper.binNumber();
        assertThat(result1, is(1));
    }

    /**
     * Tests lsPdu() getter method.
     */
    @Test
    public void testLsPdu() throws Exception {
        defaultLspWrapper.setLsPdu(pdu);
        lsPdu = defaultLspWrapper.lsPdu();
        assertThat(lsPdu, is(pdu));
    }

    /**
     * Tests lsPdu() setter method.
     */
    @Test
    public void testSetLsPdu() throws Exception {
        defaultLspWrapper.setLsPdu(pdu);
        lsPdu = defaultLspWrapper.lsPdu();
        assertThat(lsPdu, is(pdu));
    }

    /**
     * Tests lsdbAge() getter method.
     */
    @Test
    public void testlsdbAge() throws Exception {
        defaultLspWrapper.setLsdbAge(defaultIsisLsdbAge);
        lsdbAge = defaultLspWrapper.lsdbAge();
        assertThat(lsdbAge, is(defaultIsisLsdbAge));
    }

    /**
     * Tests lsdbAge() setter method.
     */
    @Test
    public void testSetLsdbAge() throws Exception {
        defaultLspWrapper.setLsdbAge(defaultIsisLsdbAge);
        lsdbAge = defaultLspWrapper.lsdbAge();
        assertThat(lsdbAge, is(defaultIsisLsdbAge));
    }

    /**
     * Tests remainingLifetime() getter method.
     */
    @Test
    public void testRemainingLifetime() throws Exception {
        defaultLspWrapper.setLsdbAge(defaultIsisLsdbAge);
        defaultLspWrapper.setAgeCounterWhenReceived(1);
        defaultLspWrapper.currentAge();
        defaultLspWrapper.setRemainingLifetime(1);
        result1 = defaultLspWrapper.remainingLifetime();
        assertThat(result1, is(1));
    }

    /**
     * Tests remainingLifetime() setter method.
     */
    @Test
    public void testSetRemainingLifetime() throws Exception {
        defaultLspWrapper.setLsdbAge(defaultIsisLsdbAge);
        defaultLspWrapper.setAgeCounterWhenReceived(1);
        defaultLspWrapper.currentAge();
        defaultLspWrapper.setRemainingLifetime(1);
        result1 = defaultLspWrapper.remainingLifetime();
        assertThat(result1, is(1));
    }
}