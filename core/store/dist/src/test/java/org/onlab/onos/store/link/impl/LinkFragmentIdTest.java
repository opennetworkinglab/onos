package org.onlab.onos.store.link.impl;

import static org.onlab.onos.net.DeviceId.deviceId;

import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;
import com.google.common.testing.EqualsTester;

public class LinkFragmentIdTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);

    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);

    private static final ConnectPoint CP1 = new ConnectPoint(DID1, P1);
    private static final ConnectPoint CP2 = new ConnectPoint(DID2, P2);

    private static final ConnectPoint CP3 = new ConnectPoint(DID1, P2);
    private static final ConnectPoint CP4 = new ConnectPoint(DID2, P3);

    private static final LinkKey L1 = LinkKey.linkKey(CP1, CP2);
    private static final LinkKey L2 = LinkKey.linkKey(CP3, CP4);

    @Test
    public void testEquals() {
        new EqualsTester()
            .addEqualityGroup(new LinkFragmentId(L1, PID),
                              new LinkFragmentId(L1, PID))
            .addEqualityGroup(new LinkFragmentId(L2, PID),
                              new LinkFragmentId(L2, PID))
            .addEqualityGroup(new LinkFragmentId(L1, PIDA),
                              new LinkFragmentId(L1, PIDA))
            .addEqualityGroup(new LinkFragmentId(L2, PIDA),
                              new LinkFragmentId(L2, PIDA))
            .testEquals();
    }

}
