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
package org.onlab.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the sliding window counter.
 */

@Ignore("Disable these for now because of intermittent load related failures on Jenkins runs.")
public class SlidingWindowCounterTest {

    private SlidingWindowCounter counter;

    @Before
    public void setUp() {
        counter = new SlidingWindowCounter(2);
    }

    @After
    public void tearDown() {
        counter.destroy();
    }

    @Test
    public void testIncrementCount() {
        assertEquals(0, counter.get(1));
        assertEquals(0, counter.get(2));
        counter.incrementCount();
        assertEquals(1, counter.get(1));
        assertEquals(1, counter.get(2));
        counter.incrementCount(2);
        assertEquals(3, counter.get(2));
    }

    @Test
    public void testSlide() {
        counter.incrementCount();
        counter.advanceHead();
        assertEquals(0, counter.get(1));
        assertEquals(1, counter.get(2));
        counter.incrementCount(2);
        assertEquals(2, counter.get(1));
        assertEquals(3, counter.get(2));
    }

    @Test
    public void testWrap() {
        counter.incrementCount();
        counter.advanceHead();
        counter.incrementCount(2);
        counter.advanceHead();
        assertEquals(0, counter.get(1));
        assertEquals(2, counter.get(2));
        counter.advanceHead();
        assertEquals(0, counter.get(1));
        assertEquals(0, counter.get(2));

    }

    @Test
    public void testCornerCases() {
        try {
            counter.get(3);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new SlidingWindowCounter(0);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            new SlidingWindowCounter(-1);
            fail("Exception should have been thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }
}
