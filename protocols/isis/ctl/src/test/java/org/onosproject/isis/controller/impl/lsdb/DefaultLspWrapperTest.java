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
import org.onosproject.isis.controller.impl.DefaultIsisInterface;

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

    @Before
    public void setUp() throws Exception {
        defaultLspWrapper = new DefaultLspWrapper();
        isisInterface = new DefaultIsisInterface();
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

}