package org.onlab.onos.store.impl;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.onlab.onos.store.Timestamp;
import org.onlab.util.KryoPool;

import com.google.common.testing.EqualsTester;

/**
 * Tests for {@link WallClockTimestamp}.
 */
public class WallClockTimestampTest {

    @Test
    public final void testBasic() throws InterruptedException {
        WallClockTimestamp ts1 = new WallClockTimestamp();
        Thread.sleep(50);
        WallClockTimestamp ts2 = new WallClockTimestamp();

        assertTrue(ts1.compareTo(ts1) == 0);
        assertTrue(ts2.compareTo(ts1) > 0);
        assertTrue(ts1.compareTo(ts2) < 0);
    }

    @Test
    public final void testKryoSerializable() {
        WallClockTimestamp ts1 = new WallClockTimestamp();
        final ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        final KryoPool kryos = KryoPool.newBuilder()
                .register(WallClockTimestamp.class)
                .build();

        kryos.serialize(ts1, buffer);
        buffer.flip();
        Timestamp copy = kryos.deserialize(buffer);

        new EqualsTester()
            .addEqualityGroup(ts1, copy)
            .testEquals();
    }
}
