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
import org.onosproject.isis.controller.LspWrapper;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultIsisLspBin.
 */
public class DefaultIsisLspBinTest {

    private DefaultIsisLspBin defaultIsisLspBin;
    private int result;
    private String key = "1";
    private LspWrapper lspWrapper;
    private LspWrapper result1;
    private Map<String, LspWrapper> listOfLsp;

    @Before
    public void setUp() throws Exception {
        defaultIsisLspBin = new DefaultIsisLspBin(1);
        lspWrapper = new DefaultLspWrapper();
    }

    @After
    public void tearDown() throws Exception {
        defaultIsisLspBin = null;
    }

    /**
     * Tests addIsisLsp() method.
     */
    @Test
    public void testAddIsisLsp() throws Exception {
        defaultIsisLspBin.addIsisLsp(key, lspWrapper);
        assertThat(defaultIsisLspBin, is(notNullValue()));
    }

    /**
     * Tests isisLsp() method.
     */
    @Test
    public void testIsisLsp() throws Exception {
        defaultIsisLspBin.addIsisLsp(key, lspWrapper);
        result1 = defaultIsisLspBin.isisLsp(key);
        assertThat(result1, is(notNullValue()));
    }

    /**
     * Tests removeIsisLsp() method.
     */
    @Test
    public void testRemoveIsisLsp() throws Exception {
        defaultIsisLspBin.addIsisLsp(key, lspWrapper);
        defaultIsisLspBin.removeIsisLsp(key, lspWrapper);
        assertThat(defaultIsisLspBin, is(notNullValue()));
    }

    /**
     * Tests listOfLsp() method.
     */
    @Test
    public void testListOfLsp() throws Exception {
        defaultIsisLspBin.addIsisLsp(key, lspWrapper);
        listOfLsp = defaultIsisLspBin.listOfLsp();
        assertThat(listOfLsp.size(), is(1));
    }

    /**
     * Tests binNumber() method.
     */
    @Test
    public void testBinNumber() throws Exception {
        result = defaultIsisLspBin.binNumber();
        assertThat(result, is(1));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(defaultIsisLspBin.toString(), is(notNullValue()));
    }
}