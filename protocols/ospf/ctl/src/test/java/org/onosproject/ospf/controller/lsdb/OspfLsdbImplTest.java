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
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.controller.area.OspfInterfaceImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.types.AsbrSummaryLsa;
import org.onosproject.ospf.protocol.lsa.types.ExternalLsa;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa11;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa9;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.lsa.types.SummaryLsa;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for OspfLsdbImpl.
 */
public class OspfLsdbImplTest {
    private OspfLsdbImpl ospfLsdb;
    private RouterLsa routerLsa;
    private NetworkLsa networkLsa;
    private SummaryLsa summaryLsa;
    private AsbrSummaryLsa asbrSummaryLsa;
    private OpaqueLsa9 opaqueLsa9;
    private OpaqueLsa10 opaqueLsa10;
    private OpaqueLsa11 opaqueLsa11;
    private ExternalLsa externalLsa;
    private OpaqueLsaHeader opaqueLsaHeader;
    private LsaWrapperImpl lsaWrapper;
    private OpaqueLsaHeader opaqueLsaHeader1;
    private String key;

    @Before
    public void setUp() throws Exception {
        OspfAreaImpl ospfArea = new OspfAreaImpl();
        ospfLsdb = new OspfLsdbImpl(ospfArea);
        routerLsa = new RouterLsa();
        networkLsa = new NetworkLsa();
        summaryLsa = new SummaryLsa(new LsaHeader());
        asbrSummaryLsa = new AsbrSummaryLsa(new LsaHeader());
        opaqueLsa9 = new OpaqueLsa9(new OpaqueLsaHeader());
        opaqueLsa10 = new OpaqueLsa10(new OpaqueLsaHeader());
        opaqueLsa11 = new OpaqueLsa11(new OpaqueLsaHeader());
        externalLsa = new ExternalLsa(new LsaHeader());
    }

    @After
    public void tearDown() throws Exception {
        ospfLsdb = null;
        routerLsa = null;
        externalLsa = null;
        summaryLsa = null;
        asbrSummaryLsa = null;
        opaqueLsa10 = null;
        opaqueLsa11 = null;
        opaqueLsa9 = null;
        networkLsa = null;
        lsaWrapper = null;
        opaqueLsaHeader = null;
        opaqueLsaHeader1 = null;
        key = null;
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(ospfLsdb.equals(new OspfLsdbImpl(new OspfAreaImpl())), is(false));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        int hashCode = ospfLsdb.hashCode();
        assertThat(hashCode, is(notNullValue()));
    }

    /**
     * Tests initializeDb() method.
     */
    @Test
    public void testInitializeDb() throws Exception {
        ospfLsdb.initializeDb();
        assertThat(ospfLsdb, is(notNullValue()));
    }

    /**
     * Tests getAllLsaHeaders() method.
     */
    @Test
    public void testGetAllLsaHeaders() throws Exception {
        ospfLsdb.initializeDb();
        routerLsa.setLsType(1);
        assertThat(ospfLsdb.addLsa(routerLsa, false, new OspfInterfaceImpl()), is(true));
        networkLsa.setLsType(2);
        assertThat(ospfLsdb.addLsa(networkLsa, false, new OspfInterfaceImpl()), is(true));
        summaryLsa.setLsType(3);
        assertThat(ospfLsdb.addLsa(summaryLsa, false, new OspfInterfaceImpl()), is(true));
        asbrSummaryLsa.setLsType(4);
        assertThat(ospfLsdb.addLsa(asbrSummaryLsa, false, new OspfInterfaceImpl()), is(true));
        externalLsa.setLsType(5);
        assertThat(ospfLsdb.addLsa(externalLsa, false, new OspfInterfaceImpl()), is(true));
        ospfLsdb.initializeDb();
        assertThat(ospfLsdb.getAllLsaHeaders(true, true).size(), is(5));
    }

    /**
     * Tests getLsaKey() method.
     */
    @Test
    public void testGetLsaKey() throws Exception {
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(1);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(2);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(3);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(4);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(5);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(9);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(10);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(11);
        assertThat(ospfLsdb.getLsaKey(opaqueLsaHeader), is(notNullValue()));
    }

    /**
     * Tests lsaLookup() method.
     */
    @Test
    public void testLsaLookup() throws Exception {
        ospfLsdb.initializeDb();
        opaqueLsaHeader = new OpaqueLsaHeader();
        ospfLsdb.addLsa(opaqueLsaHeader, true, new OspfInterfaceImpl());
        opaqueLsaHeader.setLsType(1);
        String key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        assertThat(ospfLsdb.lsaLookup(opaqueLsaHeader), is(nullValue()));
    }

    /**
     * Tests findLsa() method.
     */
    @Test
    public void testFindlsa() throws Exception {
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(1);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(2);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(3);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(4);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(5);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(9);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(10);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(11);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        ospfLsdb.addLsa(new RouterLsa(), false, new OspfInterfaceImpl());
        lsaWrapper = (LsaWrapperImpl) ospfLsdb.findLsa(opaqueLsaHeader.lsType(), key);
        assertThat(lsaWrapper, is(nullValue()));
    }

    /**
     * Tests addLSA() method.
     */
    @Test
    public void testAddLsa() throws Exception {
        routerLsa.setLsType(1);
        assertThat(ospfLsdb.addLsa(routerLsa, false, new OspfInterfaceImpl()), is(true));
        networkLsa.setLsType(2);
        assertThat(ospfLsdb.addLsa(networkLsa, false, new OspfInterfaceImpl()), is(true));
        summaryLsa.setLsType(3);
        assertThat(ospfLsdb.addLsa(summaryLsa, false, new OspfInterfaceImpl()), is(true));
        asbrSummaryLsa.setLsType(4);
        assertThat(ospfLsdb.addLsa(asbrSummaryLsa, false, new OspfInterfaceImpl()), is(true));
        externalLsa.setLsType(5);
        assertThat(ospfLsdb.addLsa(externalLsa, false, new OspfInterfaceImpl()), is(true));
        opaqueLsa9.setLsType(9);
        assertThat(ospfLsdb.addLsa(opaqueLsa9, false, new OspfInterfaceImpl()), is(true));
        opaqueLsa10.setLsType(10);
        assertThat(ospfLsdb.addLsa(opaqueLsa10, false, new OspfInterfaceImpl()), is(true));
        opaqueLsa11.setLsType(11);
        assertThat(ospfLsdb.addLsa(opaqueLsa11, false, new OspfInterfaceImpl()), is(true));

    }

    /**
     * Tests addLsaToMaxAgeBin() method.
     */
    @Test
    public void testAddLsaToMaxAgeBin() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(1);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader((OpaqueLsaHeader) opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(2);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(3);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(4);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(5);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(9);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(10);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(11);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        assertThat(ospfLsdb, is(notNullValue()));
    }

    /**
     * Tests removeLsaFromBin() method.
     */
    @Test
    public void testRemoveLsaFromBin() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(1);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(2);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(3);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(4);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(5);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(9);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(10);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(11);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        ospfLsdb.removeLsaFromBin(lsaWrapper);
        assertThat(ospfLsdb, is(notNullValue()));
    }

    /**
     * Tests isNewerOrSameLsa() method.
     */
    @Test
    public void testIsNewerorSameLsa() throws Exception {
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(1);
        key = ospfLsdb.getLsaKey(opaqueLsaHeader1);
        lsaWrapper.setLsaHeader(opaqueLsaHeader1);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        lsaWrapper = new LsaWrapperImpl();
        opaqueLsaHeader = new OpaqueLsaHeader();
        opaqueLsaHeader.setLsType(2);
        ospfLsdb.getLsaKey(opaqueLsaHeader);
        lsaWrapper.setLsaHeader(opaqueLsaHeader);
        ospfLsdb.addLsaToMaxAgeBin(key, lsaWrapper);
        assertThat(ospfLsdb.isNewerOrSameLsa(opaqueLsaHeader1, opaqueLsaHeader), is(notNullValue()));
        assertThat(ospfLsdb, is(notNullValue()));
    }

    /**
     * Tests getLsSequenceNumber() method.
     */
    @Test
    public void testGetLsSequenceNumber() throws Exception {
        assertThat(ospfLsdb.getLsSequenceNumber(OspfLsaType.NETWORK), is(notNullValue()));
        assertThat(ospfLsdb.getLsSequenceNumber(OspfLsaType.ROUTER), is(notNullValue()));
    }

    /**
     * Tests deleteLsa() method.
     */
    @Test
    public void testDeleteLsa() throws Exception {
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(1);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(2);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(3);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(4);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(5);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(9);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(10);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        opaqueLsaHeader1 = new OpaqueLsaHeader();
        opaqueLsaHeader1.setLsType(11);
        ospfLsdb.deleteLsa(opaqueLsaHeader1);
        assertThat(ospfLsdb, is(notNullValue()));

    }

    /**
     * Tests getLsSequenceNumber() method.
     */
    @Test
    public void testSetRouterLsaSeqNo() throws Exception {
        ospfLsdb.setRouterLsaSeqNo(-2147483647);
        assertThat(ospfLsdb.getLsSequenceNumber(OspfLsaType.ROUTER), is(-2147483647L));
    }

    /**
     * Tests getLsSequenceNumber() method.
     */
    @Test
    public void testSetNetworkLsaSeqNo() throws Exception {
        ospfLsdb.setNetworkLsaSeqNo(111111);
        assertThat(ospfLsdb.getLsSequenceNumber(OspfLsaType.NETWORK), is(111111L));
    }
}