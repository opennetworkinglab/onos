package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.onlab.onos.net.HostId.hostId;

/**
 * Test of the host identifier.
 */
public class HostIdTest extends ElementIdTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(hostId("nic:foo"),
                                  hostId("nic:foo"))
                .addEqualityGroup(hostId("nic:bar"))
                .testEquals();
    }

}
