package org.onlab.util;

import org.junit.Test;

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
        Counter tt = new Counter();
        tt.add(123L);
        assertEquals("incorrect number of bytes", 123L, tt.total());
        delay(1000);
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
}
