package org.onlab.onos.net;

import com.google.common.testing.EqualsTester;

import org.junit.Test;
import org.onlab.packet.MACAddress;
import org.onlab.packet.VLANID;

import static org.onlab.onos.net.HostId.hostId;

/**
 * Test for the host identifier.
 */
public class HostIdTest extends ElementIdTest {

    private static final MACAddress MAC1 = MACAddress.valueOf("00:11:00:00:00:01");
    private static final MACAddress MAC2 = MACAddress.valueOf("00:22:00:00:00:02");
    private static final VLANID VLAN1 = VLANID.vlanId((short) 11);
    private static final VLANID VLAN2 = VLANID.vlanId((short) 22);

    @Override
    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(hostId("nic:00:11:00:00:00:01/11"),
                                  hostId(MAC1, VLAN1))
                .addEqualityGroup(hostId(MAC2, VLAN2))
                .testEquals();
    }

}
