package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.onos.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.Link.Type.INDIRECT;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Test of the default link model entity.
 */
public class DefaultLinkTest {

    private static final ProviderId PID = new ProviderId("foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);

    public static ConnectPoint cp(DeviceId id, PortNumber pn) {
        return new ConnectPoint(id, pn);
    }

    @Test
    public void testEquality() {
        Link l1 = new DefaultLink(PID, cp(DID1, P1), cp(DID2, P2), DIRECT);
        Link l2 = new DefaultLink(PID, cp(DID1, P1), cp(DID2, P2), DIRECT);
        Link l3 = new DefaultLink(PID, cp(DID1, P2), cp(DID2, P2), DIRECT);
        Link l4 = new DefaultLink(PID, cp(DID1, P2), cp(DID2, P2), DIRECT);
        Link l5 = new DefaultLink(PID, cp(DID1, P2), cp(DID2, P2), INDIRECT);

        new EqualsTester().addEqualityGroup(l1, l2)
                .addEqualityGroup(l3, l4)
                .addEqualityGroup(l5)
                .testEquals();
    }

    @Test
    public void basics() {
        Link link = new DefaultLink(PID, cp(DID1, P1), cp(DID2, P2), DIRECT);
        assertEquals("incorrect src", cp(DID1, P1), link.src());
        assertEquals("incorrect dst", cp(DID2, P2), link.dst());
        assertEquals("incorrect type", DIRECT, link.type());
    }

}
