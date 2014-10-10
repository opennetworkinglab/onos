package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import static org.onlab.onos.net.HostId.hostId;

/**
 * Test for the host identifier.
 */
public class HostIdTest {

    private static final MacAddress MAC1 = MacAddress.valueOf("00:11:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:22:00:00:00:02");
    private static final VlanId VLAN1 = VlanId.vlanId((short) 11);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 22);

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(hostId(MAC1, VLAN1), hostId(MAC1, VLAN1))
                .addEqualityGroup(hostId(MAC2, VLAN2), hostId(MAC2, VLAN2))
                .testEquals();
    }

}
