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
package org.onlab.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the sliding window counter.
 */
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
    public void testSlidingWindowStats() {
        SlidingWindowCounter counter = new SlidingWindowCounter(3);

        // 1
        counter.incrementCount(1);
        assertEquals(1, counter.getWindowCount());
        assertEquals(1, counter.getWindowCount(1));

        // 0, 1
        counter.advanceHead();
        assertEquals(1, counter.getWindowCount());
        assertEquals(0, counter.getWindowCount(1));
        assertEquals(1, counter.getWindowCount(2));

        // 2, 1
        counter.incrementCount(2);
        assertEquals(3, counter.getWindowCount());
        assertEquals(2, counter.getWindowCount(1));
        assertEquals(3, counter.getWindowCount(2));

        // 0, 2, 1
        counter.advanceHead();
        assertEquals(3, counter.getWindowCount());
        assertEquals(0, counter.getWindowCount(1));
        assertEquals(2, counter.getWindowCount(2));
        assertEquals(3, counter.getWindowCount(3));

        // 3, 2, 1
        counter.incrementCount(3);
        assertEquals(6, counter.getWindowCount());
        assertEquals(3, counter.getWindowCount(1));
        assertEquals(5, counter.getWindowCount(2));
        assertEquals(6, counter.getWindowCount(3));

        // 0, 3, 2
        counter.advanceHead();
        assertEquals(5, counter.getWindowCount());
        assertEquals(0, counter.getWindowCount(1));
        assertEquals(3, counter.getWindowCount(2));
        assertEquals(5, counter.getWindowCount(3));

        // 4, 3, 2
        counter.incrementCount(4);
        assertEquals(9, counter.getWindowCount());
        assertEquals(4, counter.getWindowCount(1));
        assertEquals(7, counter.getWindowCount(2));
        assertEquals(9, counter.getWindowCount(3));

        // 0, 4, 3
        counter.advanceHead();
        assertEquals(7, counter.getWindowCount());
        assertEquals(0, counter.getWindowCount(1));
        assertEquals(4, counter.getWindowCount(2));
        assertEquals(7, counter.getWindowCount(3));

        // 5, 4, 3
        counter.incrementCount(5);
        assertEquals(12, counter.getWindowCount());
        assertEquals(5, counter.getWindowCount(1));
        assertEquals(9, counter.getWindowCount(2));
        assertEquals(12, counter.getWindowCount(3));

        // 0, 5, 4
        counter.advanceHead();
        assertEquals(9, counter.getWindowCount());
        assertEquals(0, counter.getWindowCount(1));
        assertEquals(5, counter.getWindowCount(2));
        assertEquals(9, counter.getWindowCount(3));

        counter.destroy();
    }

    @Test
    public void testRates() {
        assertEquals(0, counter.getWindowRate(), 0.01);
        assertEquals(0, counter.getOverallRate(), 0.01);
        assertEquals(0, counter.getOverallCount());
        counter.incrementCount();
        assertEquals(1, counter.getWindowRate(), 0.01);
        assertEquals(1, counter.getOverallRate(), 0.01);
        assertEquals(1, counter.getOverallCount());
        counter.advanceHead();
        counter.incrementCount();
        counter.incrementCount();
        assertEquals(1.5, counter.getWindowRate(), 0.01);
        assertEquals(2, counter.getWindowRate(1), 0.01);
        assertEquals(1.5, counter.getOverallRate(), 0.01);
        assertEquals(3, counter.getOverallCount());
        counter.advanceHead();
        counter.incrementCount();
        counter.incrementCount();
        counter.incrementCount();
        assertEquals(2.5, counter.getWindowRate(), 0.01);
        assertEquals(2, counter.getOverallRate(), 0.01);
        assertEquals(6, counter.getOverallCount());
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
