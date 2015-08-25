/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.topo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link Highlights}.
 */
public class HighlightsTest {

    private Highlights hl;

    @Test
    public void basic() {
        hl = new Highlights();

        assertEquals("devices", 0, hl.devices().size());
        assertEquals("hosts", 0, hl.hosts().size());
        assertEquals("links", 0, hl.links().size());
        assertEquals("sudue", Highlights.Amount.ZERO, hl.subdueLevel());
    }

    // NOTE: further unit tests involving the Highlights class are done
    //       in TopoJsonTest.

}
