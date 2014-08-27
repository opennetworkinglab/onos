package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

/**
 * Test of the provider identifier.
 */
public class DeviceIdTest extends ElementIdTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(new DeviceId(uri("of:foo")),
                                  new DeviceId(uri("of:foo")))
                .addEqualityGroup(new DeviceId(uri("of:bar")))
                .testEquals();
    }

}
