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
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisLsdbAge;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.io.util.IsisConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * Unit test case for DefaultIsisLsdb.
 */
public class DefaultIsisLsdbTest {
    private final int l1LspSeqNo = IsisConstants.STARTLSSEQUENCENUM;
    private final int l2LspSeqNo = IsisConstants.STARTLSSEQUENCENUM;
    private final String srcId = "1111.1111.1111";

    private DefaultIsisLsdb defaultIsisLsdb;
    private IsisLsdbAge lsdbAge = null;


    private int resultInt;
    private Map<String, LspWrapper> resultMap = new ConcurrentHashMap<>();
    private IsisLsdb resultLsdb;
    private LspWrapper resultLspWrapper;


    @Before
    public void setUp() throws Exception {
        defaultIsisLsdb = new DefaultIsisLsdb();
    }

    @After
    public void tearDown() throws Exception {
        defaultIsisLsdb = null;
    }

    /**
     * Tests initializeDb() method.
     */
    @Test
    public void testInitializeDb() throws Exception {
        defaultIsisLsdb.initializeDb();
        assertThat(lsdbAge, is(nullValue()));
    }

    /**
     * Tests setL1LspSeqNo() method.
     */
    @Test
    public void testSetL1LspSeqNo() throws Exception {
        defaultIsisLsdb.setL1LspSeqNo(l1LspSeqNo);
        assertThat(defaultIsisLsdb, is(notNullValue()));
    }

    /**
     * Tests setL2LspSeqNo() method.
     */
    @Test
    public void testSetL2LspSeqNo() throws Exception {
        defaultIsisLsdb.setL2LspSeqNo(l2LspSeqNo);
        assertThat(defaultIsisLsdb, is(notNullValue()));
    }

    /**
     * Tests setL2LspSeqNo() method.
     */
    @Test
    public void testLspKey() throws Exception {
        defaultIsisLsdb.lspKey(srcId);
        assertThat(defaultIsisLsdb, is(notNullValue()));
    }

    /**
     * Tests setL2LspSeqNo() method.
     */
    @Test
    public void testGetL1Db() throws Exception {
        resultMap = defaultIsisLsdb.getL1Db();
        assertThat(resultMap.isEmpty(), is(true));
    }

    /**
     * Tests setL2LspSeqNo() method.
     */
    @Test
    public void testGetL2Db() throws Exception {
        resultMap = defaultIsisLsdb.getL2Db();
        assertThat(resultMap.isEmpty(), is(true));
    }

    /**
     * Tests setL2LspSeqNo() method.
     */
    @Test
    public void testIsisLsdb() throws Exception {
        resultLsdb = defaultIsisLsdb.isisLsdb();
        assertThat(resultLsdb, is(notNullValue()));
    }

    /**
     * Tests findLsp() method.
     */
    @Test
    public void testFindLsp() throws Exception {
        resultLspWrapper = defaultIsisLsdb.findLsp(IsisPduType.L1HELLOPDU, srcId);
        assertThat(resultLspWrapper, is(nullValue()));
    }
}

