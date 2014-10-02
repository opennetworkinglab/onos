package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Test of the connetion point entity.
 */
public class ConnectPointTest {

    private static final DeviceId DID1 = deviceId("1");
    private static final DeviceId DID2 = deviceId("2");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    @Test
    public void basics() {
        ConnectPoint p = new ConnectPoint(DID1, P2);
        assertEquals("incorrect element id", DID1, p.deviceId());
        assertEquals("incorrect element id", P2, p.port());
    }


    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(new ConnectPoint(DID1, P1), new ConnectPoint(DID1, P1))
                .addEqualityGroup(new ConnectPoint(DID1, P2), new ConnectPoint(DID1, P2))
                .addEqualityGroup(new ConnectPoint(DID2, P1), new ConnectPoint(DID2, P1))
                .testEquals();
    }
}