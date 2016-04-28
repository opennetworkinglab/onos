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
package org.onosproject.isis.io.util;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.io.isispacket.IsisHeader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for LspGenerator.
 */
public class LspGeneratorTest {
    private LspGenerator generator;
    private IsisHeader isisHeader;
    private IsisInterface isisInterface;
    private IsisLsdb isisLsdb;

    @Before
    public void setUp() throws Exception {
        generator = new LspGenerator();
        isisLsdb = EasyMock.createMock(IsisLsdb.class);
        isisInterface = EasyMock.createMock(IsisInterface.class);
    }

    @After
    public void tearDown() throws Exception {
        generator = null;
    }

    /**
     * Tests getHeader() method.
     */
    @Test
    public void testGetHeader() throws Exception {
        isisHeader = generator.getHeader(IsisPduType.L1CSNP);
        assertThat(isisHeader, is(instanceOf(IsisHeader.class)));
    }
}