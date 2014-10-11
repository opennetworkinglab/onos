package org.onlab.onos.net.host;

import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for the default host description.
 */
public class DefualtHostDecriptionTest {

    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final VlanId VLAN = VlanId.vlanId((short) 10);
    private static final IpPrefix IP = IpPrefix.valueOf("10.0.0.1");

    private static final HostLocation LOC = new HostLocation(
            DeviceId.deviceId("of:foo"),
            PortNumber.portNumber(100),
            123L
    );

    @Test
    public void basics() {
        HostDescription host =
                new DefaultHostDescription(MAC, VLAN, LOC, IP);
        assertEquals("incorrect mac", MAC, host.hwAddress());
        assertEquals("incorrect vlan", VLAN, host.vlan());
        assertEquals("incorrect location", LOC, host.location());
        assertEquals("incorrect ip's", IP, host.ipAddress());
        assertTrue("incorrect toString", host.toString().contains("vlan=10"));
    }

}
