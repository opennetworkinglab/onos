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

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.junit.ImmutableClassChecker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

public class FrequencyTest {

    private final Frequency frequency1 = Frequency.ofMHz(1000);
    private final Frequency sameFrequency1 = Frequency.ofMHz(1000);
    private final Frequency frequency2 = Frequency.ofGHz(1000);
    private final Frequency sameFrequency2 = Frequency.ofGHz(1000);
    private final Frequency moreSameFrequency2 = Frequency.ofTHz(1);
    private final Frequency frequency3 = Frequency.ofTHz(193.1);
    private final Frequency sameFrequency3 = Frequency.ofGHz(193100);

    /**
     * Tests immutability of Frequency.
     */
    @Test
    public void testImmutability() {
        ImmutableClassChecker.assertThatClassIsImmutable(Frequency.class);
    }

    /**
     * Tests equality of Frequency instances.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(frequency1, sameFrequency1)
                .addEqualityGroup(frequency2, sameFrequency2, moreSameFrequency2)
                .addEqualityGroup(frequency3, sameFrequency3)
                .testEquals();
    }

    /**
     * Tests the first object is less than the second object.
     */
    @Test
    public void testLessThan() {
        assertThat(frequency1, is(lessThan(frequency2)));
        assertThat(frequency1.isLessThan(frequency2), is(true));
    }

    @Test
    public void testGreaterThan() {
        assertThat(frequency2, is(greaterThan(frequency1)));
        assertThat(frequency2.isGreaterThan(frequency1), is(true));
    }

    /**
     * Tests add operation of two Frequencies.
     */
    @Test
    public void testAdd() {
        Frequency low = Frequency.ofMHz(100);
        Frequency high = Frequency.ofGHz(1);
        Frequency expected = Frequency.ofMHz(1100);

        assertThat(low.add(high), is(expected));
    }

    /**
     * Tests subtract operation of two Frequencies.
     */
    @Test
    public void testSubtract() {
        Frequency high = Frequency.ofGHz(1);
        Frequency low = Frequency.ofMHz(100);
        Frequency expected = Frequency.ofMHz(900);

        assertThat(high.subtract(low), is(expected));
    }

    /**
     * Tests multiply operation of Frequency.
     */
    @Test
    public void testMultiply() {
        Frequency frequency = Frequency.ofMHz(1000);
        long factor = 5;
        Frequency expected = Frequency.ofGHz(5);

        assertThat(frequency.multiply(5), is(expected));
    }
    /**
     * Tests floorDivision operation of Frequency.
     */
    @Test
    public void testfloorDivision() {
        Frequency frequency = Frequency.ofGHz(1);
        long factor = 5;
        Frequency expected = Frequency.ofMHz(200);

        assertThat(frequency.floorDivision(factor), is(expected));
    }
    /**
     * Tests asHz operation of Frequency.
     */
    @Test
    public void testasHz() {
        Frequency frequency = Frequency.ofKHz(1);

        assertThat(frequency.asHz(), is(1000L));
    }
    /**
     * Tests asKHz operation of Frequency.
     */
    @Test
    public void testasKHz() {
        Frequency frequency = Frequency.ofMHz(1);

        assertThat(frequency.asKHz(), is(1000.0));
    }
    /**
     * Tests asMHz operation of Frequency.
     */
    @Test
    public void testasMHz() {
        Frequency frequency = Frequency.ofGHz(1);

        assertThat(frequency.asMHz(), is(1000.0));
    }
    /**
     * Tests asGHz operation of Frequency.
     */
    @Test
    public void testasGHz() {
        Frequency frequency = Frequency.ofTHz(1);

        assertThat(frequency.asGHz(), is(1000.0));
    }
    /**
     * Tests asThz operation of Frequency.
     */
    @Test
    public void testasTHz() {
        Frequency frequency = Frequency.ofHz(1_000_000_000_000L);

        assertThat(frequency.asTHz(), is(1.0));
    }

    /**
     * Tests the ofKHz (with a double parameter) operation of Frequency.
     */
    @Test
    public void testofKHz() {
        Frequency frequency = Frequency.ofKHz(1.0);
        assertThat(frequency.asHz(), is(1000L));
    }
    /**
     * Tests the ofMHz (with a double parameter) operation of Frequency.
     */
    @Test
    public void testofMHz() {
        Frequency frequency = Frequency.ofMHz(1.0);
        assertThat(frequency.asKHz(), is(1000.0));
    }
    /**
     * Tests the ofGHz (with a double parameter) operation of Frequency.
     */
    @Test
    public void testofGHz() {
        Frequency frequency = Frequency.ofGHz(1.0);
        assertThat(frequency.asMHz(), is(1000.0));
    }
    /**
     * Tests the ofTHz (with a double parameter) operation of Frequency.
     */
    @Test
    public void testofTHz() {
        Frequency frequency = Frequency.ofTHz(1.0);
        assertThat(frequency.asGHz(), is(1000.0));
    }



}

