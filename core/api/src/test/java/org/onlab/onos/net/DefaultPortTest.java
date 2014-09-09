package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.onos.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.Device.Type.SWITCH;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Test of the default port model entity.
 */
public class DefaultPortTest {

    private static final ProviderId PID = new ProviderId("foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    @Test
    public void testEquality() {
        Device device = new DefaultDevice(PID, DID1, SWITCH, "m", "h", "s", "n");
        Port p1 = new DefaultPort(device, portNumber(1), true);
        Port p2 = new DefaultPort(device, portNumber(1), true);
        Port p3 = new DefaultPort(device, portNumber(2), true);
        Port p4 = new DefaultPort(device, portNumber(2), true);
        Port p5 = new DefaultPort(device, portNumber(1), false);

        new EqualsTester().addEqualityGroup(p1, p2)
                .addEqualityGroup(p3, p4)
                .addEqualityGroup(p5)
                .testEquals();
    }

    @Test
    public void basics() {
        Device device = new DefaultDevice(PID, DID1, SWITCH, "m", "h", "s", "n");
        Port port = new DefaultPort(device, portNumber(1), true);
        assertEquals("incorrect element", device, port.element());
        assertEquals("incorrect number", portNumber(1), port.number());
        assertEquals("incorrect state", true, port.isEnabled());
    }

}
