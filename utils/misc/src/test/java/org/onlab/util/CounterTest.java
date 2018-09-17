/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.testing.EqualsTester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.TestTools.delay;

/**
 * Tests of the Counter utility.
 */
public class CounterTest {

    @Test
    public void basics() {
        Counter tt = new Counter();
        assertEquals("incorrect number of bytes", 0L, tt.total());
        assertEquals("incorrect throughput", 0.0, tt.throughput(), 0.0001);
        tt.add(1234567890L);
        assertEquals("incorrect number of bytes", 1234567890L, tt.total());
        assertTrue("incorrect throughput", 1234567890.0 < tt.throughput());
        delay(1500);
        tt.add(1L);
        assertEquals("incorrect number of bytes", 1234567891L, tt.total());
        assertTrue("incorrect throughput", 1234567891.0 > tt.throughput());
        tt.reset();
        assertEquals("incorrect number of bytes", 0L, tt.total());
        assertEquals("incorrect throughput", 0.0, tt.throughput(), 0.0001);
    }

    @Test
    public void freeze() {
        long now = System.currentTimeMillis();
        Counter tt = new Counter(now, 123L, now + 1000);
        tt.freeze();
        tt.add(123L);
        assertEquals("incorrect number of bytes", 123L, tt.total());

        double d = tt.duration();
        double t = tt.throughput();
        assertEquals("incorrect duration", d, tt.duration(), 0.0001);
        assertEquals("incorrect throughput", t, tt.throughput(), 0.0001);
        assertEquals("incorrect number of bytes", 123L, tt.total());
    }

    @Test
    public void reset() {
        Counter tt = new Counter();
        tt.add(123L);
        assertEquals("incorrect number of bytes", 123L, tt.total());

        double d = tt.duration();
        double t = tt.throughput();
        assertEquals("incorrect duration", d, tt.duration(), 0.0001);
        assertEquals("incorrect throughput", t, tt.throughput(), 0.0001);
        assertEquals("incorrect number of bytes", 123L, tt.total());

        tt.reset();
        assertEquals("incorrect throughput", 0.0, tt.throughput(), 0.0001);
        assertEquals("incorrect number of bytes", 0, tt.total());
    }

    @Test
    public void syntheticTracker() {
        Counter tt = new Counter(5000, 1000, 6000);
        assertEquals("incorrect duration", 1, tt.duration(), 0.1);
        assertEquals("incorrect throughput", 1000, tt.throughput(), 1.0);
    }

    @Test
    public void equals() {
        long start = 100L;
        long total = 300L;
        long end = 200L;
        Counter tt = new Counter(start, total, end);
        Counter same = new Counter(start, total, end);
        Counter diff = new Counter(300L, 700L, 400L);
        new EqualsTester()
                .addEqualityGroup(tt, same)
                .addEqualityGroup(400)
                .addEqualityGroup("")
                .addEqualityGroup(diff)
                .testEquals();

    }

    @Test
    public void toStringTest() {
        Counter tt = new Counter(100L, 300L, 200L);
        assertEquals("Counter{total=300, start=100, end=200}", tt.toString());
        Counter another = new Counter(200L, 500L, 300L);
        assertEquals("Counter{total=500, start=200, end=300}", another.toString());
    }
}
