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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.protocol.lsa.LsaHeader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsaBinImpl.
 */
public class LsaBinImplTest {
    private LsaBinImpl lsaBin;
    private LsaHeader ospflsa1;
    private LsaWrapper lsaWrapper;


    @Before
    public void setUp() throws Exception {
        ospflsa1 = new LsaHeader();
        ospflsa1.setAge(20);
        lsaBin = new LsaBinImpl(1);
    }

    @After
    public void tearDown() throws Exception {
        ospflsa1 = null;
        lsaBin = null;
    }

    /**
     * Tests binNumber() getter method.
     */
    @Test
    public void testGetBinNumber() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        lsaBin.addOspfLsa("lsa1", lsaWrapper);
        assertThat(lsaBin.binNumber(), is(1));
    }

    /**
     * Tests addOspfLsa() method.
     */
    @Test
    public void testAddOspfLsa() throws Exception {
        LsaWrapper lsaWrapper = new LsaWrapperImpl();
        lsaBin.addOspfLsa("lsa1", lsaWrapper);
        assertThat(lsaBin, is(notNullValue()));
    }

    /**
     * Tests ospfLsa() getter method.
     */
    @Test
    public void testGetOspfLsa() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        lsaBin.addOspfLsa("lsa1", lsaWrapper);
        assertThat(lsaBin, is(notNullValue()));
        assertThat(lsaBin.ospfLsa("lsa1"), is(lsaWrapper));
    }

    /**
     * Tests removeOspfLsa()  method.
     */
    @Test
    public void testRemoveOspfLsa() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        lsaBin.addOspfLsa("lsa1", lsaWrapper);
        lsaBin.removeOspfLsa("lsa1", lsaWrapper);
        assertThat(lsaBin, is(notNullValue()));
    }

    /**
     * Tests listOfLsa()  method.
     */
    @Test
    public void testGetListOfLsa() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        lsaBin.addOspfLsa("lsa1", lsaWrapper);
        assertThat(lsaBin.listOfLsa().size(), is(1));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lsaBin.toString(), is(notNullValue()));
    }
}