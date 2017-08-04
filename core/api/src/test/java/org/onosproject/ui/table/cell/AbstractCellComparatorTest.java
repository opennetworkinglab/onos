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
 * Unit tests for {@link AbstractCellComparator}.
 */
public class AbstractCellComparatorTest {

    private static class Concrete extends AbstractCellComparator {
        @Override
        protected int nonNullCompare(Object o1, Object o2) {
            return 42;
        }
    }

    private CellComparator cmp = new Concrete();

    @Test
    public void twoNullArgs() {
        assertTrue("two nulls", cmp.compare(null, null) == 0);
    }

    @Test
    public void nullArgOne() {
        assertTrue("null one", cmp.compare(null, 1) < 0);
    }

    @Test
    public void nullArgTwo() {
        assertTrue("null two", cmp.compare(1, null) > 0);
    }

    // mock output, but check that our method was invoked...
    @Test
    public void noNulls() {
        assertTrue("no Nulls", cmp.compare(1, 2) == 42);
    }
}
