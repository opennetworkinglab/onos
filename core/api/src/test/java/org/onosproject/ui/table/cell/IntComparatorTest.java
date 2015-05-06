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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.ui.table.CellComparator;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link IntComparator}.
 */
public class IntComparatorTest {

    private CellComparator cmp = IntComparator.INSTANCE;

    @Test
    public void twoNulls() {
        assertTrue("two nulls", cmp.compare(null, null) == 0);
    }

    @Test
    public void nullVsNegValue() {
        assertTrue("null vs neg value", cmp.compare(null, -5) < 0);
    }

    @Test
    public void nullVsPosValue() {
        assertTrue("null vs pos value", cmp.compare(null, 5) < 0);
    }

    @Test
    public void negValueVsNull() {
        assertTrue("neg value vs null", cmp.compare(-5, null) > 0);
    }

    @Test
    public void posValueVsNull() {
        assertTrue("pos value vs null", cmp.compare(5, null) > 0);
    }


    @Test
    public void smallVsBig() {
        assertTrue("small vs big", cmp.compare(25, 75) < 0);
    }

    @Test
    public void bigVsSmall() {
        assertTrue("big vs small", cmp.compare(75, 25) > 0);
    }

    @Test
    public void sameValue() {
        assertTrue("same value", cmp.compare(50, 50) == 0);
    }
}
