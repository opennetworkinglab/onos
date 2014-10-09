package org.onlab.onos.cluster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.onlab.onos.net.device.DeviceMastershipTerm;

import com.google.common.testing.EqualsTester;

public class MastershipTermTest {

    private static final NodeId N1 = new NodeId("foo");
    private static final NodeId N2 = new NodeId("bar");

    private static final DeviceMastershipTerm TERM1 = DeviceMastershipTerm.of(N1, 0);
    private static final DeviceMastershipTerm TERM2 = DeviceMastershipTerm.of(N2, 1);
    private static final DeviceMastershipTerm TERM3 = DeviceMastershipTerm.of(N2, 1);
    private static final DeviceMastershipTerm TERM4 = DeviceMastershipTerm.of(N1, 1);

    @Test
    public void basics() {
        assertEquals("incorrect term number", 0, TERM1.termNumber());
        assertEquals("incorrect master", new NodeId("foo"), TERM1.master());
    }

    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(DeviceMastershipTerm.of(N1, 0), TERM1)
        .addEqualityGroup(TERM2, TERM3)
        .addEqualityGroup(TERM4);
    }

}
