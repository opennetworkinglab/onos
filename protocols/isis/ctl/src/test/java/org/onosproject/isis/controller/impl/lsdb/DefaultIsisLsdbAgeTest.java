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
import org.onosproject.isis.controller.IsisLspBin;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultIsisLsdbAge.
 */
public class DefaultIsisLsdbAgeTest {
    private DefaultIsisLsdbAge defaultIsisLsdbAge;
    private IsisLspBin isisLspBin;
    private int resultInt;
    private IsisLspBin resultLspBin;
    private DefaultLspWrapper lspWrapper;
    private LsPdu lsPdu;
    private IsisHeader isisHeader;
    private String lspId = "1234.1234.1234";

    @Before
    public void setUp() throws Exception {
        defaultIsisLsdbAge = new DefaultIsisLsdbAge();
        isisLspBin = new DefaultIsisLspBin(1);
        lspWrapper = new DefaultLspWrapper();
        lspWrapper.setBinNumber(1);
        isisHeader = new IsisHeader();
        lsPdu = new LsPdu(isisHeader);
        lsPdu.setLspId(lspId);
        lspWrapper.setLsPdu(lsPdu);
    }

    @After
    public void tearDown() throws Exception {
        defaultIsisLsdbAge = null;
        isisLspBin = null;
    }

    /**
     * Tests ageCounter() method.
     */
    @Test
    public void testAgeCounter() throws Exception {
        resultInt = defaultIsisLsdbAge.ageCounter();
        assertThat(resultInt, is(0));
    }

    /**
     * Tests ageCounterRollOver() method.
     */
    @Test
    public void testAgeCounterRollOver() throws Exception {
        resultInt = defaultIsisLsdbAge.ageCounterRollOver();
        assertThat(resultInt, is(0));
    }

    /**
     * Tests addLspBin() method.
     */
    @Test
    public void testAddLspBin() throws Exception {
        defaultIsisLsdbAge.addLspBin(1400, isisLspBin);
        resultLspBin = defaultIsisLsdbAge.getLspBin(1);
        assertThat(resultLspBin, is(notNullValue()));
    }

    /**
     * Tests getLspBin() method.
     */
    @Test
    public void testGetLspBin() throws Exception {
        defaultIsisLsdbAge.addLspBin(1, isisLspBin);
        resultLspBin = defaultIsisLsdbAge.getLspBin(1);
        assertThat(resultLspBin, is(notNullValue()));
    }

    /**
     * Tests removeLspFromBin() method.
     */
    @Test
    public void testRemoveLspFromBin() throws Exception {
        defaultIsisLsdbAge.addLspBin(1400, isisLspBin);
        defaultIsisLsdbAge.removeLspFromBin(lspWrapper);
        assertThat(resultLspBin, is(nullValue()));
    }

    /**
     * Tests age2Bin() method.
     */
    @Test
    public void testAge2Bin() throws Exception {
        defaultIsisLsdbAge.age2Bin(1);
        assertThat(defaultIsisLsdbAge, is(notNullValue()));

        defaultIsisLsdbAge.age2Bin(-1);
        assertThat(defaultIsisLsdbAge, is(notNullValue()));
    }

    /**
     * Tests startDbAging() method.
     */
    @Test
    public void testStartDbAging() throws Exception {
        defaultIsisLsdbAge.startDbAging();
        assertThat(defaultIsisLsdbAge, is(notNullValue()));
    }

    /**
     * Tests ageLsp() method.
     */
    @Test
    public void testAgeLsp() throws Exception {
        defaultIsisLsdbAge.age2Bin(1);
        defaultIsisLsdbAge.startDbAging();
        defaultIsisLsdbAge.ageLsp();
        assertThat(defaultIsisLsdbAge, is(notNullValue()));
    }

    /**
     * Tests maxAgeLsa() method.
     */
    @Test
    public void testMaxAgeLsa() throws Exception {
        defaultIsisLsdbAge.age2Bin(1);
        defaultIsisLsdbAge.startDbAging();
        defaultIsisLsdbAge.maxAgeLsa();
        assertThat(defaultIsisLsdbAge, is(notNullValue()));

        defaultIsisLsdbAge.age2Bin(1400);
        defaultIsisLsdbAge.startDbAging();
        defaultIsisLsdbAge.maxAgeLsa();
        assertThat(defaultIsisLsdbAge, is(notNullValue()));
    }

    /**
     * Tests refreshLsa() method.
     */
    @Test
    public void testRefreshLsa() throws Exception {
        defaultIsisLsdbAge.age2Bin(1);
        defaultIsisLsdbAge.startDbAging();
        defaultIsisLsdbAge.refreshLsa();
        assertThat(defaultIsisLsdbAge, is(notNullValue()));

        defaultIsisLsdbAge.age2Bin(1400);
        defaultIsisLsdbAge.startDbAging();
        defaultIsisLsdbAge.refreshLsa();
        assertThat(defaultIsisLsdbAge, is(notNullValue()));
    }
}

