/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.ui.topo.Highlights.Amount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Highlights}.
 */
public class HighlightsTest {

    private static final String DEV_1 = "dev-1";
    private static final String DEV_2 = "dev-2";
    private static final String HOST_A = "Host...A";

    private Highlights highlights;
    private DeviceHighlight dh1;
    private DeviceHighlight dh2;
    private HostHighlight hha;

    @Before
    public void setUp() {
        highlights = new Highlights();
    }

    @Test
    public void basic() {
        assertEquals("devices", 0, highlights.devices().size());
        assertEquals("hosts", 0, highlights.hosts().size());
        assertEquals("links", 0, highlights.links().size());
        assertEquals("sudue", Amount.ZERO, highlights.subdueLevel());
    }

    @Test
    public void coupleOfDevices() {
        dh1 = new DeviceHighlight(DEV_1);
        dh2 = new DeviceHighlight(DEV_2);

        highlights.add(dh1);
        highlights.add(dh2);
        assertTrue("missing dh1", highlights.devices().contains(dh1));
        assertTrue("missing dh2", highlights.devices().contains(dh2));
    }

    @Test
    public void alternateSubdue() {
        highlights.subdueAllElse(Amount.MINIMALLY);
        assertEquals("wrong level", Amount.MINIMALLY, highlights.subdueLevel());
    }

    @Test
    public void highlightRetrieval() {
        dh1 = new DeviceHighlight(DEV_1);
        hha = new HostHighlight(HOST_A);
        highlights.add(dh1)
                .add(hha);

        assertNull("dev as host", highlights.getHost(DEV_1));
        assertNull("host as dev", highlights.getDevice(HOST_A));

        assertEquals("missed dev as dev", dh1, highlights.getDevice(DEV_1));
        assertEquals("missed dev as node", dh1, highlights.getNode(DEV_1));

        assertEquals("missed host as host", hha, highlights.getHost(HOST_A));
        assertEquals("missed host as node", hha, highlights.getNode(HOST_A));
    }

    // NOTE: further unit tests involving the Highlights class are done
    //       in TopoJsonTest.
}
