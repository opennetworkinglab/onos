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

package org.onosproject.ui.table;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link DefaultCellComparator}.
 */
public class DefaultCellComparatorTest {

    private static class TestClass {
        @Override
        public String toString() {
            return SOME;
        }
    }

    private static final String SOME = "SoMeStRiNg";
    private static final String OTHER = "OtherSTRING";
    private static final int NUMBER = 42;
    private static final TestClass OBJECT = new TestClass();

    private CellComparator cmp = new DefaultCellComparator();

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
    public void someVsObject() {
        assertTrue("some vs object", cmp.compare(SOME, OBJECT) == 0);
    }

    @Test
    public void otherVsObject() {
        assertTrue("other vs object", cmp.compare(OTHER, OBJECT) < 0);
    }

    @Test
    public void otherVsNumber() {
        assertTrue("other vs 42", cmp.compare(OTHER, NUMBER) > 0);
    }

    @Test
    public void someVsNull() {
        assertTrue("some vs null", cmp.compare(SOME, null) > 0);
    }

    @Test
    public void nullVsSome() {
        assertTrue("null vs some", cmp.compare(null, SOME) < 0);
    }

}
