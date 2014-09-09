package org.onlab.onos.net.link;

import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.DefaultLinkTest.cp;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.Link.Type.DIRECT;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Test of the default link description.
 */
public class DefaultLinkDescriptionTest {

    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);

    @Test
    public void basics() {
        LinkDescription desc = new DefaultLinkDescription(cp(DID1, P1), cp(DID2, P1), DIRECT);
        assertEquals("incorrect src", cp(DID1, P1), desc.src());
        assertEquals("incorrect dst", cp(DID2, P1), desc.dst());
        assertEquals("incorrect type", DIRECT, desc.type());
    }

}
