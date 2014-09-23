package org.onlab.onos.net.host.impl;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

public class DefaultPortAddresses implements PortAddresses {

    private final ConnectPoint connectPoint;
    private final IpAddress ipAddress;
    private final MacAddress macAddress;

    public DefaultPortAddresses(ConnectPoint connectPoint,
            IpAddress ip, MacAddress mac) {
        this.connectPoint = connectPoint;
        this.ipAddress = ip;
        this.macAddress = mac;
    }

    @Override
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    @Override
    public IpAddress ip() {
        return ipAddress;
    }

    @Override
    public MacAddress mac() {
        return macAddress;
    }

}
