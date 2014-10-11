package org.onlab.onos.store.device.impl.peermsg;

import static org.onlab.onos.net.DeviceId.deviceId;

import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.testing.EqualsTester;

public class DeviceFragmentIdTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

    @Test
    public final void testEquals() {

        new EqualsTester()
            .addEqualityGroup(new DeviceFragmentId(DID1, PID),
                              new DeviceFragmentId(DID1, PID))
            .addEqualityGroup(new DeviceFragmentId(DID2, PID),
                              new DeviceFragmentId(DID2, PID))
            .addEqualityGroup(new DeviceFragmentId(DID1, PIDA),
                              new DeviceFragmentId(DID1, PIDA))
            .addEqualityGroup(new DeviceFragmentId(DID2, PIDA),
                              new DeviceFragmentId(DID2, PIDA))
        .testEquals();
    }

}
