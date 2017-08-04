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

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;

/**
 * Unit tests for LongBandwidth.
 */
public class LongBandwidthTest {
    private final long billion = 1000000000;
    private final long one = 1;

    //These are LongBandwidth objects because if a Bandwidth object is passed a
    //long parameter it becomes a LongBandwidth object
    Bandwidth notLongSmall = Bandwidth.kbps(1.0);
    private final Bandwidth notLongBig = Bandwidth.kbps(1000.0);
    private final Bandwidth big = Bandwidth.kbps(billion);
    private final Bandwidth small = Bandwidth.kbps(one);

    /**
     * Tests the getter method of LongBandwidths.
     */
    @Test
    public void testBps() {
        Bandwidth expected = Bandwidth.bps(one);

        assertEquals(one, expected.bps(), 0.0);
    }

    /**
     * Tests add operation of two LongBandwidths and two Bandwidths.
     */
    @Test
    public void testAdd() {
        final long add = billion + one;
        Bandwidth expected = Bandwidth.kbps(add);

        assertThat(big.add(small), is(expected));

        final double notLongAdd = 1001.0;
        Bandwidth notLongExpected = Bandwidth.kbps(notLongAdd);

        assertThat(notLongSmall.add(notLongBig), is(notLongExpected));
    }

    /**
     * Tests subtract operation of two LongBandwidths and two Bandwidths.
     */
    @Test
    public void testSubtract() {
        final long sub = billion - one;
        Bandwidth expected = Bandwidth.kbps(sub);

        assertThat(big.subtract(small), is(expected));

        final double notLongSubtract = 999.0;
        Bandwidth notLongExpected = Bandwidth.kbps(notLongSubtract);

        assertThat(notLongBig.subtract(notLongSmall), is(notLongExpected));
    }


    /**
     * Tests if the first object is less than the second object, and then it
     * tests if the compareTo function works correctly if comparing a smaller
     * object to a larger one, and vice versa.
     * Also tests the same thing but with Bandwidth objects.
     */
    @Test
    public void testLessThan() {

        assertThat(small, is(lessThan(big)));
        assertThat(small.compareTo(big), is(-1));

        assertThat(big, is(greaterThan(small)));
        assertThat(big.compareTo(small), is(1));

        assertThat(notLongSmall, is(lessThan(notLongBig)));
        assertThat(notLongSmall.compareTo(notLongBig), is(-1));

        assertThat(notLongBig, is(greaterThan(notLongSmall)));
        assertThat(notLongBig.compareTo(notLongSmall), is(1));
    }

    /**
     * Tests the equals function between itself, another equivalent Bandwidth
     * object, a nonequivalent Bandwidth object, and an int.
     */
    @Test
    public void testEquals() {
        Bandwidth expected = Bandwidth.kbps(one);
        assertFalse(small.equals(big));
        assertTrue(small.equals(expected));
        assertTrue(small.equals(small));
        assertFalse(small.equals(1000));
    }

    /**
     * Tests the hashcode function of a LongBandwidth.
     */
    @Test
    public void testHashCode() {
        Long expected = (one * 1000);
        assertEquals(small.hashCode(), expected.hashCode());
    }

    /**
     * Tests the toString function of a LongBandwidth.
     */
    @Test
    public void testToString() {
        String expected = "1000";
        assertEquals(small.toString(), expected);
    }
}
