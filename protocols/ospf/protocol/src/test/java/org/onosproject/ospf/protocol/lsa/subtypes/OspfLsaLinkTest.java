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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for OspfLsaLink.
 */
public class OspfLsaLinkTest {

    private OspfLsaLink ospfLsaLink;

    @Before
    public void setUp() throws Exception {
        ospfLsaLink = new OspfLsaLink();
    }

    @After
    public void tearDown() throws Exception {
        ospfLsaLink = null;
    }

    /**
     * Tests linkId() getter method.
     */
    @Test
    public void testGetLinkID() throws Exception {
        ospfLsaLink.setLinkId("1.1.1.1");
        assertThat(ospfLsaLink.linkId(), is("1.1.1.1"));
    }

    /**
     * Tests linkId() setter method.
     */
    @Test
    public void testSetLinkID() throws Exception {
        ospfLsaLink.setLinkId("1.1.1.1");
        assertThat(ospfLsaLink.linkId(), is("1.1.1.1"));
    }

    /**
     * Tests linkData() getter method.
     */
    @Test
    public void testGetLinkData() throws Exception {
        ospfLsaLink.setLinkData("1.1.1.1");
        assertThat(ospfLsaLink.linkData(), is("1.1.1.1"));
    }

    /**
     * Tests linkData() setter method.
     */
    @Test
    public void testSetLinkData() throws Exception {
        ospfLsaLink.setLinkData("1.1.1.1");
        assertThat(ospfLsaLink.linkData(), is("1.1.1.1"));
    }

    /**
     * Tests linkType() getter method.
     */
    @Test
    public void testGetLinkType() throws Exception {
        ospfLsaLink.setLinkType(1);
        assertThat(ospfLsaLink.linkType(), is(1));
    }

    /**
     * Tests linkType() setter method.
     */
    @Test
    public void testSetLinkType() throws Exception {
        ospfLsaLink.setLinkType(1);
        assertThat(ospfLsaLink.linkType(), is(1));
    }

    /**
     * Tests metric() getter method.
     */
    @Test
    public void testGetMetric() throws Exception {
        ospfLsaLink.setMetric(100);
        assertThat(ospfLsaLink.metric(), is(100));
    }

    /**
     * Tests metric() setter method.
     */
    @Test
    public void testSetMetric() throws Exception {
        ospfLsaLink.setMetric(100);
        assertThat(ospfLsaLink.metric(), is(100));
    }

    /**
     * Tests tos() getter method.
     */
    @Test
    public void testGetTos() throws Exception {
        ospfLsaLink.setTos(100);
        assertThat(ospfLsaLink.tos(), is(100));
    }

    /**
     * Tests tos() setter method.
     */
    @Test
    public void testSetTos() throws Exception {
        ospfLsaLink.setTos(100);
        assertThat(ospfLsaLink.tos(), is(100));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(ospfLsaLink.toString(), is(notNullValue()));
    }
}