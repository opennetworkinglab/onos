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
package org.onosproject.ospf.protocol.lsa.subtypes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OspfRouterId.
 */
public class OspfExternalDestinationTest {

    private static final Ip4Address LOCAL_ADDRESS = Ip4Address.valueOf("127.0.0.1");

    private OspfExternalDestination ospfExternalDestination;

    @Before
    public void setUp() throws Exception {
        ospfExternalDestination = new OspfExternalDestination();
    }

    @After
    public void tearDown() throws Exception {
        ospfExternalDestination = null;
    }

    /**
     * Tests isType1orType2Metric() getter method.
     */
    @Test
    public void testIsType1orType2Metric() throws Exception {
        ospfExternalDestination.setType1orType2Metric(true);
        assertThat(ospfExternalDestination.isType1orType2Metric(), is(true));
    }

    /**
     * Tests isType1orType2Metric() setter method.
     */
    @Test
    public void testSetType1orType2Metric() throws Exception {
        ospfExternalDestination.setType1orType2Metric(true);
        assertThat(ospfExternalDestination.isType1orType2Metric(), is(true));
    }

    /**
     * Tests metric() getter method.
     */
    @Test
    public void testGetMetric() throws Exception {
        ospfExternalDestination.setMetric(100);
        assertThat(ospfExternalDestination.metric(), is(100));
    }

    /**
     * Tests metric() setter method.
     */
    @Test
    public void testSetMetric() throws Exception {
        ospfExternalDestination.setMetric(100);
        assertThat(ospfExternalDestination.metric(), is(100));
    }

    /**
     * Tests forwardingAddress() getter method.
     */
    @Test
    public void testGetForwardingAddress() throws Exception {
        ospfExternalDestination.setForwardingAddress(LOCAL_ADDRESS);
        assertThat(ospfExternalDestination.forwardingAddress(), is(LOCAL_ADDRESS));

    }

    /**
     * Tests forwardingAddress() setter method.
     */
    @Test
    public void testSetForwardingAddress() throws Exception {
        ospfExternalDestination.setForwardingAddress(LOCAL_ADDRESS);
        assertThat(ospfExternalDestination.forwardingAddress(), is(LOCAL_ADDRESS));
    }

    /**
     * Tests externalRouterTag() getter method.
     */
    @Test
    public void testGetExternalRouterTag() throws Exception {
        ospfExternalDestination.setExternalRouterTag(100);
        assertThat(ospfExternalDestination.externalRouterTag(), is(100));
    }

    /**
     * Tests externalRouterTag() setter method.
     */
    @Test
    public void testSetExternalRouterTag() throws Exception {
        ospfExternalDestination.setExternalRouterTag(100);
        assertThat(ospfExternalDestination.externalRouterTag(), is(100));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfExternalDestination.toString(), is(notNullValue()));
    }
}
