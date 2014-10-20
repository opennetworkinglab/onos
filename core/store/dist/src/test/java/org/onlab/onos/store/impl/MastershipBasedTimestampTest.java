package org.onlab.onos.store.impl;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.serializers.MastershipBasedTimestampSerializer;
import org.onlab.util.KryoNamespace;

import com.google.common.testing.EqualsTester;

/**
 * Test of {@link MastershipBasedTimestamp}.
 */
public class MastershipBasedTimestampTest {

    private static final Timestamp TS_1_1 = new MastershipBasedTimestamp(1, 1);
    private static final Timestamp TS_1_2 = new MastershipBasedTimestamp(1, 2);
    private static final Timestamp TS_2_1 = new MastershipBasedTimestamp(2, 1);
    private static final Timestamp TS_2_2 = new MastershipBasedTimestamp(2, 2);

    @Test
    public final void testBasic() {
        final int termNumber = 5;
        final int sequenceNumber = 6;
        MastershipBasedTimestamp ts = new MastershipBasedTimestamp(termNumber,
                                                sequenceNumber);

        assertEquals(termNumber, ts.termNumber());
        assertEquals(sequenceNumber, ts.sequenceNumber());
    }

    @Test
    public final void testCompareTo() {
        assertTrue(TS_1_1.compareTo(TS_1_1) == 0);
        assertTrue(TS_1_1.compareTo(new MastershipBasedTimestamp(1, 1)) == 0);

        assertTrue(TS_1_1.compareTo(TS_1_2) < 0);
        assertTrue(TS_1_2.compareTo(TS_1_1) > 0);

        assertTrue(TS_1_2.compareTo(TS_2_1) < 0);
        assertTrue(TS_1_2.compareTo(TS_2_2) < 0);
        assertTrue(TS_2_1.compareTo(TS_1_1) > 0);
        assertTrue(TS_2_2.compareTo(TS_1_1) > 0);
    }

    @Test
    public final void testEqualsObject() {
        new EqualsTester()
        .addEqualityGroup(new MastershipBasedTimestamp(1, 1),
                          new MastershipBasedTimestamp(1, 1), TS_1_1)
        .addEqualityGroup(new MastershipBasedTimestamp(1, 2),
                          new MastershipBasedTimestamp(1, 2), TS_1_2)
        .addEqualityGroup(new MastershipBasedTimestamp(2, 1),
                          new MastershipBasedTimestamp(2, 1), TS_2_1)
        .addEqualityGroup(new MastershipBasedTimestamp(2, 2),
                          new MastershipBasedTimestamp(2, 2), TS_2_2)
        .testEquals();
    }

    @Test
    public final void testKryoSerializable() {
        final ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        final KryoNamespace kryos = KryoNamespace.newBuilder()
                .register(MastershipBasedTimestamp.class)
                .build();

        kryos.serialize(TS_2_1, buffer);
        buffer.flip();
        Timestamp copy = kryos.deserialize(buffer);

        new EqualsTester()
            .addEqualityGroup(TS_2_1, copy)
            .testEquals();
    }

    @Test
    public final void testKryoSerializableWithHandcraftedSerializer() {
        final ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        final KryoNamespace kryos = KryoNamespace.newBuilder()
                .register(MastershipBasedTimestamp.class, new MastershipBasedTimestampSerializer())
                .build();

        kryos.serialize(TS_1_2, buffer);
        buffer.flip();
        Timestamp copy = kryos.deserialize(buffer);

        new EqualsTester()
            .addEqualityGroup(TS_1_2, copy)
            .testEquals();
    }

}
