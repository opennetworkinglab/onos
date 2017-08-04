/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.ui.topo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link BiLinkMap}.
 */
public class BiLinkMapTest extends BiLinkTestBase {


    private ConcreteLink clink;
    private ConcreteLinkMap linkMap;

    @Before
    public void setUp() {
        linkMap = new ConcreteLinkMap();
    }

    @Test
    public void basic() {
        assertEquals("wrong map size", 0, linkMap.size());
        assertTrue("unexpected links", linkMap.biLinks().isEmpty());
    }

    @Test
    public void addSameLinkTwice() {
        linkMap.add(LINK_AB);
        assertEquals("wrong map size", 1, linkMap.size());
        clink = linkMap.biLinks().iterator().next();
        assertEquals("wrong link one", LINK_AB, clink.one());
        assertNull("unexpected link two", clink.two());

        linkMap.add(LINK_AB);
        assertEquals("wrong map size", 1, linkMap.size());
        clink = linkMap.biLinks().iterator().next();
        assertEquals("wrong link one", LINK_AB, clink.one());
        assertNull("unexpected link two", clink.two());
    }

    @Test
    public void addPairOfLinks() {
        linkMap.add(LINK_AB);
        assertEquals("wrong map size", 1, linkMap.size());
        clink = linkMap.biLinks().iterator().next();
        assertEquals("wrong link one", LINK_AB, clink.one());
        assertNull("unexpected link two", clink.two());

        linkMap.add(LINK_BA);
        assertEquals("wrong map size", 1, linkMap.size());
        clink = linkMap.biLinks().iterator().next();
        assertEquals("wrong link one", LINK_AB, clink.one());
        assertEquals("wrong link two", LINK_BA, clink.two());
    }
}
