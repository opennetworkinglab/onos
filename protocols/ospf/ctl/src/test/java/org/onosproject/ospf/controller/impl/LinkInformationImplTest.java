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
package org.onosproject.ospf.controller.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for LinkInformationImpl.
 */
public class LinkInformationImplTest {

    private LinkInformationImpl linkInformation;

    @Before
    public void setUp() throws Exception {
        linkInformation = new LinkInformationImpl();
    }

    @After
    public void tearDown() throws Exception {
        linkInformation = null;
    }

    /**
     * Tests linkId() getter method.
     */
    @Test
    public void testLinkId() throws Exception {
        linkInformation.setLinkId("1.1.1.1");
        assertThat(linkInformation.linkId(), is("1.1.1.1"));
    }

    /**
     * Tests linkId() setter method.
     */
    @Test
    public void testSetLinkId() throws Exception {
        linkInformation.setLinkId("1.1.1.1");
        assertThat(linkInformation.linkId(), is("1.1.1.1"));
    }

    /**
     * Tests isAlreadyCreated() getter method.
     */
    @Test
    public void testIsAlreadyCreated() throws Exception {
        linkInformation.setAlreadyCreated(true);
        assertThat(linkInformation.isAlreadyCreated(), is(true));
    }

    /**
     * Tests isAlreadyCreated() setter method.
     */
    @Test
    public void testSetAlreadyCreated() throws Exception {
        linkInformation.setAlreadyCreated(true);
        assertThat(linkInformation.isAlreadyCreated(), is(true));
    }

    /**
     * Tests isLinkSrcIdNotRouterId() getter method.
     */
    @Test
    public void testIsLinkSrcIdNotRouterId() throws Exception {
        linkInformation.setLinkSrcIdNotRouterId(true);
        assertThat(linkInformation.isLinkSrcIdNotRouterId(), is(true));
    }

    /**
     * Tests isLinkSrcIdNotRouterId() setter method.
     */
    @Test
    public void testSetLinkSrcIdNotRouterId() throws Exception {
        linkInformation.setLinkSrcIdNotRouterId(true);
        assertThat(linkInformation.isLinkSrcIdNotRouterId(), is(true));
    }

    /**
     * Tests linkDestinationId() getter method.
     */
    @Test
    public void testLinkDestinationId() throws Exception {
        linkInformation.setLinkDestinationId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkDestinationId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkDestinationId() setter method.
     */
    @Test
    public void testSetLinkDestinationId() throws Exception {
        linkInformation.setLinkDestinationId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkDestinationId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkSourceId() getter method.
     */
    @Test
    public void testLinkSourceId() throws Exception {
        linkInformation.setLinkSourceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkSourceId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkSourceId() setter method.
     */
    @Test
    public void testSetLinkSourceId() throws Exception {
        linkInformation.setLinkSourceId(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkSourceId(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests interfaceIp() getter method.
     */
    @Test
    public void testInterfaceIp() throws Exception {
        linkInformation.setInterfaceIp(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.interfaceIp(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests interfaceIp() setter method.
     */
    @Test
    public void testSetInterfaceIp() throws Exception {
        linkInformation.setInterfaceIp(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.interfaceIp(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkSourceIpAddress() getter method.
     */
    @Test
    public void testLinkSourceIpAddress() throws Exception {
        linkInformation.setLinkSourceIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkSourceIpAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkSourceIpAddress() setter method.
     */
    @Test
    public void testSetLinkSourceIpAddress() throws Exception {
        linkInformation.setLinkSourceIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkSourceIpAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkDestinationId() getter method.
     */
    @Test
    public void testLinkDestinationIpAddress() throws Exception {
        linkInformation.setLinkDestinationIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkDestinationIpAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }

    /**
     * Tests linkDestinationId() setter method.
     */
    @Test
    public void testSetLinkDestinationIpAddress() throws Exception {
        linkInformation.setLinkDestinationIpAddress(Ip4Address.valueOf("1.1.1.1"));
        assertThat(linkInformation.linkDestinationIpAddress(), is(Ip4Address.valueOf("1.1.1.1")));
    }
}