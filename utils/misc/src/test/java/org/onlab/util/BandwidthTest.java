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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for Bandwidth.
 */
public class BandwidthTest {

    private final Bandwidth small = Bandwidth.kbps(100.0);
    private final Bandwidth large = Bandwidth.mbps(1.0);

    /**
     * Tests equality of Bandwidth instances.
     */
    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(Bandwidth.kbps(1000.0), Bandwidth.kbps(1000.0), Bandwidth.mbps(1.0))
                .addEqualityGroup(Bandwidth.gbps(1.0))
                .testEquals();
    }

    /**
     * Tests add operation of two Bandwidths.
     */
    @Test
    public void testAdd() {
        Bandwidth expected = Bandwidth.kbps(1100.0);

        assertThat(small.add(large), is(expected));
    }

    /**
     * Tests subtract operation of two Bandwidths.
     */
    @Test
    public void testSubtract() {
        Bandwidth expected = Bandwidth.kbps(900.0);

        assertThat(large.subtract(small), is(expected));
    }

    /**
     * Tests if the first object is less than the second object.
     */
    @Test
    public void testLessThan() {
        assertThat(small, is(lessThan(large)));
        assertThat(small.isLessThan(large), is(true));
    }

    /**
     * Tests if the first object is greater than the second object.
     */
    @Test
    public void testGreaterThan() {
        assertThat(large, is(greaterThan(small)));
        assertThat(large.isGreaterThan(small), is(true));
    }
}
