package org.onlab.onos.net.link;

import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;

/**
 * Test adapter for link service.
 */
public class LinkServiceAdapter implements LinkService {
    @Override
    public int getLinkCount() {
        return 0;
    }

    @Override
    public Iterable<Link> getLinks() {
        return null;
    }

    @Override
    public Set<Link> getDeviceLinks(DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<Link> getLinks(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Link getLink(ConnectPoint src, ConnectPoint dst) {
        return null;
    }

    @Override
    public void addListener(LinkListener listener) {
    }

    @Override
    public void removeListener(LinkListener listener) {
    }


}
