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
package org.onosproject.isis.controller.impl;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisProcess;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for DefaultIsisProcess.
 */
public class DefaultIsisProcessTest {

    private final String processId = "1";
    private IsisProcess isisProcess;
    private String result;
    private IsisProcess defaultIsisProcess;
    private IsisInterface isisInterface;
    private List<IsisInterface> isisInterfaceList;
    private List<IsisInterface> result1;

    @Before
    public void setUp() throws Exception {
        isisProcess = new DefaultIsisProcess();
        isisInterface = EasyMock.createNiceMock(DefaultIsisInterface.class);
        defaultIsisProcess = new DefaultIsisProcess();
    }

    @After
    public void tearDown() throws Exception {
        isisProcess = null;
    }

    /**
     * Tests processId() setter method.
     */
    @Test
    public void testProcessId() throws Exception {
        isisProcess.setProcessId(processId);
        result = isisProcess.processId();
        assertThat(result, is(processId));
    }

    /**
     * Tests processId() getter method.
     */
    @Test
    public void testSetProcessId() throws Exception {
        isisProcess.setProcessId(processId);
        result = isisProcess.processId();
        assertThat(result, is(processId));
    }

    /**
     * Tests isisInterfaceList() setter method.
     */
    @Test
    public void testIsisInterfaceList() throws Exception {
        isisProcess.setIsisInterfaceList(isisInterfaceList);
        result1 = isisProcess.isisInterfaceList();
        assertThat(result1, is(nullValue()));
    }

    /**
     * Tests isisInterfaceList() getter method.
     */
    @Test
    public void testSetIsisInterfaceList() throws Exception {
        isisProcess.setIsisInterfaceList(isisInterfaceList);
        result1 = isisProcess.isisInterfaceList();
        assertThat(result1, is(nullValue()));
    }
}