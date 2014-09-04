package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Test of the device identifier.
 */
public class DeviceIdTest extends ElementIdTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(deviceId("of:foo"),
                                  deviceId("of:foo"))
                .addEqualityGroup(deviceId("of:bar"))
                .testEquals();
    }

}
