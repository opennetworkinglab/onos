package org.onlab.onos.cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

public class MastershipTermTest {

    private static final NodeId N1 = new NodeId("foo");
    private static final NodeId N2 = new NodeId("bar");

    private static final MastershipTerm TERM1 = MastershipTerm.of(N1, 0);
    private static final MastershipTerm TERM2 = MastershipTerm.of(N2, 1);
    private static final MastershipTerm TERM3 = MastershipTerm.of(N2, 1);
    private static final MastershipTerm TERM4 = MastershipTerm.of(N1, 1);

    @Test
    public void basics() {
        assertEquals("incorrect term number", 0, TERM1.termNumber());
        assertEquals("incorrect master", new NodeId("foo"), TERM1.master());
    }

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(MastershipTerm.of(N1, 0), TERM1)
        .addEqualityGroup(TERM2, TERM3)
        .addEqualityGroup(TERM4);
    }

}
