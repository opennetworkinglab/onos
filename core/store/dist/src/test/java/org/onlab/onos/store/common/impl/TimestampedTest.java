package org.onlab.onos.store.common.impl;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.impl.OnosTimestamp;
import org.onlab.util.KryoPool;

import com.google.common.testing.EqualsTester;

public class TimestampedTest {

    private static final Timestamp TS_1_1 = new OnosTimestamp(1, 1);
    private static final Timestamp TS_1_2 = new OnosTimestamp(1, 2);
    private static final Timestamp TS_2_1 = new OnosTimestamp(2, 1);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testHashCode() {
        Timestamped<String> a = new Timestamped<>("a", TS_1_1);
        Timestamped<String> b = new Timestamped<>("b", TS_1_1);
        assertTrue("value does not impact hashCode",
                a.hashCode() == b.hashCode());
    }

    @Test
    public final void testEquals() {
        Timestamped<String> a = new Timestamped<>("a", TS_1_1);
        Timestamped<String> b = new Timestamped<>("b", TS_1_1);
        assertTrue("value does not impact equality",
                a.equals(b));

        new EqualsTester()
        .addEqualityGroup(new Timestamped<>("a", TS_1_1),
                          new Timestamped<>("b", TS_1_1),
                          new Timestamped<>("c", TS_1_1))
        .addEqualityGroup(new Timestamped<>("a", TS_1_2),
                          new Timestamped<>("b", TS_1_2),
                          new Timestamped<>("c", TS_1_2))
        .addEqualityGroup(new Timestamped<>("a", TS_2_1),
                          new Timestamped<>("b", TS_2_1),
                          new Timestamped<>("c", TS_2_1))
        .testEquals();

    }

    @Test
    public final void testValue() {
       final Integer n = Integer.valueOf(42);
       Timestamped<Integer> tsv = new Timestamped<>(n, TS_1_1);
       assertSame(n, tsv.value());

    }

    @Test(expected = NullPointerException.class)
    public final void testValueNonNull() {
        new Timestamped<>(null, TS_1_1);
    }

    @Test(expected = NullPointerException.class)
    public final void testTimestampNonNull() {
        new Timestamped<>("Foo", null);
    }

    @Test
    public final void testIsNewer() {
        Timestamped<String> a = new Timestamped<>("a", TS_1_2);
        Timestamped<String> b = new Timestamped<>("b", TS_1_1);
        assertTrue(a.isNewer(b));
        assertFalse(b.isNewer(a));
    }

    @Test
    public final void testKryoSerializable() {
        final ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        final KryoPool kryos = KryoPool.newBuilder()
                .register(Timestamped.class,
                        OnosTimestamp.class)
                .build();

        Timestamped<String> original = new Timestamped<>("foobar", TS_1_1);
        kryos.serialize(original, buffer);
        buffer.flip();
        Timestamped<String> copy = kryos.deserialize(buffer);

        new EqualsTester()
            .addEqualityGroup(original, copy)
            .testEquals();
    }
}
