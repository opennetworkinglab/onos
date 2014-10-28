package org.onlab.onos.net.host;

import java.util.Set;

import org.junit.Test;
import org.onlab.onos.event.AbstractEventTest;
import org.onlab.onos.net.DefaultHost;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.Sets;

public class HostEventTest extends AbstractEventTest {

    private Host createHost() {
        MacAddress mac = MacAddress.valueOf("00:00:11:00:00:01");
        VlanId vlan = VlanId.vlanId((short) 10);
        HostLocation loc = new HostLocation(
                    DeviceId.deviceId("of:foo"),
                    PortNumber.portNumber(100),
                    123L
                );
        Set<IpAddress> ipset = Sets.newHashSet(
                    IpAddress.valueOf("10.0.0.1"),
                    IpAddress.valueOf("10.0.0.2")
                );
        HostId hid = HostId.hostId(mac, vlan);

        return new DefaultHost(
                new ProviderId("of", "foo"), hid, mac, vlan, loc, ipset);
    }

    @Override
    @Test
    public void withTime() {
        Host host = createHost();
        HostEvent event = new HostEvent(HostEvent.Type.HOST_ADDED, host, 123L);
        validateEvent(event, HostEvent.Type.HOST_ADDED, host, 123L);
    }

    @Override
    @Test
    public void withoutTime() {
        Host host = createHost();
        long before = System.currentTimeMillis();
        HostEvent event = new HostEvent(HostEvent.Type.HOST_ADDED, host, before);
        long after = System.currentTimeMillis();
        validateEvent(event, HostEvent.Type.HOST_ADDED, host, before, after);
    }
}
