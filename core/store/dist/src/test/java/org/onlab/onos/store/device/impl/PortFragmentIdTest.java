package org.onlab.onos.store.device.impl;

import static org.onlab.onos.net.DeviceId.deviceId;

import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.testing.EqualsTester;

public class PortFragmentIdTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);

    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);

    @Test
    public final void testEquals() {
        new EqualsTester()
        .addEqualityGroup(new PortFragmentId(DID1, PID, PN1),
                          new PortFragmentId(DID1, PID, PN1))
        .addEqualityGroup(new PortFragmentId(DID2, PID, PN1),
                          new PortFragmentId(DID2, PID, PN1))
        .addEqualityGroup(new PortFragmentId(DID1, PIDA, PN1),
                          new PortFragmentId(DID1, PIDA, PN1))
        .addEqualityGroup(new PortFragmentId(DID2, PIDA, PN1),
                          new PortFragmentId(DID2, PIDA, PN1))

        .addEqualityGroup(new PortFragmentId(DID1, PID, PN2),
                          new PortFragmentId(DID1, PID, PN2))
        .addEqualityGroup(new PortFragmentId(DID2, PID, PN2),
                          new PortFragmentId(DID2, PID, PN2))
        .addEqualityGroup(new PortFragmentId(DID1, PIDA, PN2),
                          new PortFragmentId(DID1, PIDA, PN2))
        .addEqualityGroup(new PortFragmentId(DID2, PIDA, PN2),
                          new PortFragmentId(DID2, PIDA, PN2))
        .testEquals();
    }

}
