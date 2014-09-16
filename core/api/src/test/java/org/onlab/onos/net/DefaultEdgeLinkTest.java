package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.onos.net.provider.ProviderId;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.DefaultLinkTest.cp;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.HostId.hostId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Test of the default edge link model entity.
 */
public class DefaultEdgeLinkTest {

    private static final ProviderId PID = new ProviderId("foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final HostId HID1 = hostId("nic:foobar");
    private static final HostId HID2 = hostId("nic:barfoo");
    private static final PortNumber P0 = portNumber(0);
    private static final PortNumber P1 = portNumber(1);

    @Test
    public void testEquality() {
        EdgeLink l1 = new DefaultEdgeLink(PID, cp(HID1, P0),
                                          new HostLocation(DID1, P1, 123L), true);
        EdgeLink l2 = new DefaultEdgeLink(PID, cp(HID1, P0),
                                          new HostLocation(DID1, P1, 123L), true);

        EdgeLink l3 = new DefaultEdgeLink(PID, cp(HID2, P0),
                                          new HostLocation(DID1, P1, 123L), false);
        EdgeLink l4 = new DefaultEdgeLink(PID, cp(HID2, P0),
                                          new HostLocation(DID1, P1, 123L), false);

        EdgeLink l5 = new DefaultEdgeLink(PID, cp(HID1, P0),
                                          new HostLocation(DID1, P1, 123L), false);

        new EqualsTester().addEqualityGroup(l1, l2)
                .addEqualityGroup(l3, l4)
                .addEqualityGroup(l5)
                .testEquals();
    }

    @Test
    public void basics() {
        HostLocation hostLocation = new HostLocation(DID1, P1, 123L);
        EdgeLink link = new DefaultEdgeLink(PID, cp(HID1, P0), hostLocation, false);
        assertEquals("incorrect src", cp(HID1, P0), link.dst());
        assertEquals("incorrect dst", hostLocation, link.src());
        assertEquals("incorrect type", Link.Type.EDGE, link.type());
        assertEquals("incorrect hostId", HID1, link.hostId());
        assertEquals("incorrect connect point", hostLocation, link.hostLocation());
        assertEquals("incorrect time", 123L, link.hostLocation().time());
    }

}
