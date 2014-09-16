package org.onlab.onos.net.host;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.packet.IPAddress;
import org.onlab.packet.MACAddress;
import org.onlab.packet.VLANID;

import java.util.Set;

/**
 * Test adapter for host service.
 */
public class HostServiceAdapter implements HostService {
    @Override
    public int getHostCount() {
        return 0;
    }

    @Override
    public Iterable<Host> getHosts() {
        return null;
    }

    @Override
    public Host getHost(HostId hostId) {
        return null;
    }

    @Override
    public Set<Host> getHostsByVlan(VLANID vlanId) {
        return null;
    }

    @Override
    public Set<Host> getHostsByMac(MACAddress mac) {
        return null;
    }

    @Override
    public Set<Host> getHostsByIp(IPAddress ip) {
        return null;
    }

    @Override
    public Set<Host> getConnectedHosts(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Set<Host> getConnectedHosts(DeviceId deviceId) {
        return null;
    }

    @Override
    public void addListener(HostListener listener) {
    }

    @Override
    public void removeListener(HostListener listener) {
    }

}
