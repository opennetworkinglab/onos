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
package org.onosproject.ospf.controller.area;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.controller.OspfArea;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OspfProcessImpl.
 */
public class OspfProcessImplTest {

    private OspfProcessImpl ospfProcess;
    private List<OspfArea> list;
    private List result;

    @Before
    public void setUp() throws Exception {
        ospfProcess = new OspfProcessImpl();
    }

    @After
    public void tearDown() throws Exception {
        ospfProcess = null;
        list = null;
    }

    /**
     * Tests areas() getter method.
     */
    @Test
    public void testGetAreas() throws Exception {
        list = new ArrayList();
        list.add(new OspfAreaImpl());
        list.add(new OspfAreaImpl());
        ospfProcess.setAreas(list);
        result = ospfProcess.areas();
        assertThat(result.size(), is(2));
    }

    /**
     * Tests areas() setter method.
     */
    @Test
    public void testSetAreas() throws Exception {
        list = new ArrayList();
        list.add(new OspfAreaImpl());
        list.add(new OspfAreaImpl());
        ospfProcess.setAreas(list);
        result = ospfProcess.areas();
        assertThat(result.size(), is(2));
    }

    /**
     * Tests processId() getter method.
     */
    @Test
    public void testGetProcessId() throws Exception {
        ospfProcess.setProcessId("1.1.1.1");
        assertThat(ospfProcess.processId(), is("1.1.1.1"));
    }

    /**
     * Tests processId() setter method.
     */
    @Test
    public void testSetProcessId() throws Exception {
        ospfProcess.setProcessId("1.1.1.1");
        assertThat(ospfProcess.processId(), is("1.1.1.1"));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfProcess.toString(), is(notNullValue()));
    }
}