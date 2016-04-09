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
import org.onosproject.ospf.controller.OspfProcess;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for Configuration.
 */
public class ConfigurationTest {
    private Configuration configuration;
    private List<OspfProcess> ospfProcesses;
    private List result;
    private OspfProcessImpl ospfProcessImpl;

    @Before
    public void setUp() throws Exception {
        configuration = new Configuration();
    }

    @After
    public void tearDown() throws Exception {
        configuration = null;
        ospfProcessImpl = new OspfProcessImpl();
        result = null;
        ospfProcesses = null;
    }

    /**
     * Tests getProcesses() getter method.
     */
    @Test
    public void testGetOspfProcess() throws Exception {
        ospfProcesses = new ArrayList();
        ospfProcesses.add(ospfProcessImpl);
        ospfProcesses.add(ospfProcessImpl);
        configuration.setProcesses(ospfProcesses);
        result = configuration.getProcesses();
        assertThat(result.size(), is(2));
    }

    /**
     * Tests setProcesses() setter method.
     */
    @Test
    public void testSetOspfProcess() throws Exception {
        ospfProcesses = new ArrayList();
        ospfProcesses.add(ospfProcessImpl);
        ospfProcesses.add(ospfProcessImpl);
        configuration.setProcesses(ospfProcesses);
        result = configuration.getProcesses();
        assertThat(result.size(), is(2));
    }

    /**
     * Tests getMethod() getter method.
     */
    @Test
    public void testGetMethod() throws Exception {
        configuration.setMethod("method");
        assertThat(configuration.getMethod(), is("method"));
    }

    /**
     * Tests setMethod() setter method.
     */
    @Test
    public void testSetMethod() throws Exception {
        configuration.setMethod("method");
        assertThat(configuration.getMethod(), is("method"));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(configuration.toString(), is(notNullValue()));
    }
}