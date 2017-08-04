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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellComparator;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DefaultCellComparator}.
 */
public class DefaultCellComparatorTest {

    private static final String SOME = "SoMeStRiNg";
    private static final String OTHER = "OtherSTRING";

    private CellComparator cmp = DefaultCellComparator.INSTANCE;

    // default comparator should detect Comparable<T> impls and use that

    @Test
    public void sameString() {
        assertTrue("same string", cmp.compare(SOME, SOME) == 0);
    }

    @Test
    public void someVsOther() {
        assertTrue("some vs other", cmp.compare(SOME, OTHER) > 0);
    }

    @Test
    public void otherVsSome() {
        assertTrue("other vs some", cmp.compare(OTHER, SOME) < 0);
    }

    @Test
    public void someVsNull() {
        assertTrue("some vs null", cmp.compare(SOME, null) > 0);
    }

    @Test
    public void nullVsSome() {
        assertTrue("null vs some", cmp.compare(null, SOME) < 0);
    }

    @Test(expected = ClassCastException.class)
    public void mismatch() {
        cmp.compare(42, SOME);
    }


    @Test
    public void strElevenTwo() {
        assertTrue("str 11 vs 2", cmp.compare("11", "2") < 0);
    }

    @Test
    public void intElevenTwo() {
        assertTrue("int 11 vs 2", cmp.compare(11, 2) > 0);
    }


    @Test
    public void intSmallBig() {
        assertTrue("int 2 vs 4", cmp.compare(2, 4) < 0);
    }

    @Test
    public void intBigSmall() {
        assertTrue("int 4 vs 2", cmp.compare(4, 2) > 0);
    }

    @Test
    public void intEqual() {
        assertTrue("int 4 vs 4", cmp.compare(4, 4) == 0);
    }

    @Test
    public void longSmallBig() {
        assertTrue("long 2 vs 4", cmp.compare(2L, 4L) < 0);
    }

    @Test
    public void longBigSmall() {
        assertTrue("long 4 vs 2", cmp.compare(4L, 2L) > 0);
    }

    @Test
    public void longEqual() {
        assertTrue("long 4 vs 4", cmp.compare(4L, 4L) == 0);
    }


    private enum SmallStarWars { C3PO, R2D2, LUKE }

    @Test
    public void swEpisodeI() {
        assertTrue("c3po r2d2",
                   cmp.compare(SmallStarWars.C3PO, SmallStarWars.R2D2) < 0);
    }

    @Test
    public void swEpisodeIi() {
        assertTrue("r2d2 c3po",
                   cmp.compare(SmallStarWars.R2D2, SmallStarWars.C3PO) > 0);
    }

    @Test
    public void swEpisodeIii() {
        assertTrue("luke c3po",
                   cmp.compare(SmallStarWars.LUKE, SmallStarWars.C3PO) > 0);
    }

    @Test
    public void swEpisodeIv() {
        assertTrue("c3po luke",
                   cmp.compare(SmallStarWars.C3PO, SmallStarWars.LUKE) < 0);
    }

    @Test
    public void swEpisodeV() {
        assertTrue("luke r2d2",
                   cmp.compare(SmallStarWars.LUKE, SmallStarWars.R2D2) > 0);
    }

    @Test
    public void swEpisodeVi() {
        assertTrue("r2d2 luke",
                   cmp.compare(SmallStarWars.R2D2, SmallStarWars.LUKE) < 0);
    }
}
