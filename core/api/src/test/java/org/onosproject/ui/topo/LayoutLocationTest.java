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

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.*;
import static org.onosproject.ui.topo.LayoutLocation.*;

/**
 * Unit tests for {@link LayoutLocation}.
 */
public class LayoutLocationTest extends AbstractUiTest {

    private static final String SOME_ID = "foo";
    private static final String OTHER_ID = "bar";
    private static final double SQRT2 = 1.414;
    private static final double PI = 3.142;
    private static final double ZERO = 0.0;

    private static final String COMPACT_LL_1 = "foo,geo,3.142,1.414";
    private static final String COMPACT_LL_2 = "bar,grid,1.414,3.142";

    private static final String COMPACT_LIST = COMPACT_LL_1 + "~" + COMPACT_LL_2;

    private LayoutLocation ll;
    private LayoutLocation ll2;

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

    @Test
    public void compactString() {
        ll = layoutLocation(SOME_ID, Type.GEO, PI, SQRT2);
        String s = ll.toCompactListString();
        assertEquals("wrong compactness", COMPACT_LL_1, s);
    }

    @Test
    public void fromCompactStringTest() {
        ll = fromCompactString(COMPACT_LL_1);
        verifyLL1(ll);
    }

    private void verifyLL1(LayoutLocation ll) {
        assertEquals("LL1 bad id", SOME_ID, ll.id());
        assertEquals("LL1 bad type", Type.GEO, ll.locType());
        assertEquals("LL1 bad Y", PI, ll.latOrY(), TOLERANCE);
        assertEquals("LL1 bad X", SQRT2, ll.longOrX(), TOLERANCE);
    }

    private void verifyLL2(LayoutLocation ll) {
        assertEquals("LL1 bad id", OTHER_ID, ll.id());
        assertEquals("LL1 bad type", Type.GRID, ll.locType());
        assertEquals("LL1 bad Y", SQRT2, ll.latOrY(), TOLERANCE);
        assertEquals("LL1 bad X", PI, ll.longOrX(), TOLERANCE);
    }


    @Test(expected = IllegalArgumentException.class)
    public void badCompactTooShort() {
        fromCompactString("one,two,three");
    }

    @Test(expected = IllegalArgumentException.class)
    public void badCompactTooLong() {
        fromCompactString("one,two,three,4,5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void badCompactNoId() {
        fromCompactString(",GEO,1,2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void badCompactUnparsableY() {
        fromCompactString("foo,GEO,yyy,2.3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void badCompactUnparsableX() {
        fromCompactString("foo,GEO,0.2,xxx");
    }

    @Test
    public void toCompactList() {
        ll = layoutLocation(SOME_ID, Type.GEO, PI, SQRT2);
        ll2 = layoutLocation(OTHER_ID, Type.GRID, SQRT2, PI);
        String compact = toCompactListString(ll, ll2);
        print(compact);
        assertEquals("wrong list encoding", COMPACT_LIST, compact);
    }

    @Test
    public void toCompactList2() {
        ll = layoutLocation(SOME_ID, Type.GEO, PI, SQRT2);
        ll2 = layoutLocation(OTHER_ID, Type.GRID, SQRT2, PI);
        List<LayoutLocation> locs = of(ll, ll2);
        String compact = toCompactListString(locs);
        print(compact);
        assertEquals("wrong list encoding", COMPACT_LIST, compact);
    }

    @Test
    public void fromCompactList() {
        List<LayoutLocation> locs = fromCompactListString(COMPACT_LIST);
        ll = locs.get(0);
        ll2 = locs.get(1);
        verifyLL1(ll);
        verifyLL2(ll2);
    }

    @Test
    public void fromCompactListNull() {
        List<LayoutLocation> locs = fromCompactListString(null);
        assertEquals("non-empty list", 0, locs.size());
    }

    @Test
    public void fromCompactListEmpty() {
        List<LayoutLocation> locs = fromCompactListString("");
        assertEquals("non-empty list", 0, locs.size());
    }

    @Test
    public void toCompactListStringNullList() {
        String s = toCompactListString((List<LayoutLocation>) null);
        assertEquals("not empty string", "", s);
    }

    @Test
    public void toCompactListStringNullArray() {
        String s = toCompactListString((LayoutLocation[]) null);
        assertEquals("not empty string", "", s);
    }

    @Test
    public void toCompactListStringEmptyArray() {
        String s = toCompactListString();
        assertEquals("not empty string", "", s);
    }

    @Test
    public void toCompactListStringEmptyList() {
        String s = toCompactListString(new ArrayList<>());
        assertEquals("not empty string", "", s);
    }
}
