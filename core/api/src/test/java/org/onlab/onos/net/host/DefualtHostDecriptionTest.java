package org.onlab.onos.net.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;
import org.onlab.packet.IPAddress;
import org.onlab.packet.MACAddress;
import org.onlab.packet.VLANID;

import com.google.common.collect.Sets;

/**
 * Test for the default host description.
 */
public class DefualtHostDecriptionTest {

    private static final MACAddress MAC = MACAddress.valueOf("00:00:11:00:00:01");
    private static final VLANID VLAN = VLANID.vlanId((short) 10);
    private static final HostLocation LOC = new HostLocation(
                DeviceId.deviceId("of:foo"),
                PortNumber.portNumber(100),
                123L
            );
    private static final Set<IPAddress> IPS = Sets.newHashSet(
                IPAddress.valueOf("10.0.0.1"),
                IPAddress.valueOf("10.0.0.2")
            );

    @Test
    public void basics() {
        HostDescription host =
                new DefaultHostDescription(MAC, VLAN, LOC, IPS);
        assertEquals("incorrect mac", MAC, host.hwAddress());
        assertEquals("incorrect vlan", VLAN, host.vlan());
        assertEquals("incorrect location", LOC, host.location());
        assertTrue("incorrect ip's", IPS.equals(host.ipAddresses()));
        assertTrue("incorrect toString", host.toString().contains("vlan=10"));
    }

}
