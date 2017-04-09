/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.ui.AbstractUiTest;

import static org.junit.Assert.*;
import static org.onosproject.ui.topo.LayoutLocation.Type;
import static org.onosproject.ui.topo.LayoutLocation.layoutLocation;

/**
 * Unit tests for {@link LayoutLocation}.
 */
public class LayoutLocationTest extends AbstractUiTest {

    private static final String SOME_ID = "foo";
    private static final double SQRT2 = 1.414;
    private static final double PI = 3.142;
    private static final double ZERO = 0.0;

    private LayoutLocation ll;

    @Test
    public void basic() {
        ll = layoutLocation(SOME_ID, Type.GRID, SQRT2, PI);
        print(ll);
        assertEquals("bad id", SOME_ID, ll.id());
        assertEquals("bad type", Type.GRID, ll.locType());
        assertEquals("bad Y", SQRT2, ll.latOrY(), TOLERANCE);
        assertEquals("bad X", PI, ll.longOrX(), TOLERANCE);
        assertFalse("bad origin check", ll.isOrigin());
    }

    @Test
    public void createGeoLocFromStringType() {
        ll = layoutLocation(SOME_ID, "geo", SQRT2, PI);
        assertEquals("bad type - not geo", Type.GEO, ll.locType());
    }

    @Test
    public void createGridLocFromStringType() {
        ll = layoutLocation(SOME_ID, "grid", SQRT2, PI);
        assertEquals("bad type - not grid", Type.GRID, ll.locType());
    }

    @Test
    public void zeroLatitude() {
        ll = layoutLocation(SOME_ID, Type.GEO, ZERO, PI);
        assertFalse("shouldn't be origin for zero latitude", ll.isOrigin());
    }

    @Test
    public void zeroLongitude() {
        ll = layoutLocation(SOME_ID, Type.GEO, PI, ZERO);
        assertFalse("shouldn't be origin for zero longitude", ll.isOrigin());
    }

    @Test
    public void origin() {
        ll = layoutLocation(SOME_ID, Type.GRID, ZERO, ZERO);
        assertTrue("should be origin", ll.isOrigin());
    }

    @Test(expected = IllegalArgumentException.class)
    public void badType() {
        layoutLocation(SOME_ID, "foo", ZERO, PI);
    }

    @Test(expected = NullPointerException.class)
    public void nullId() {
        layoutLocation(null, Type.GRID, PI, PI);
    }
}
