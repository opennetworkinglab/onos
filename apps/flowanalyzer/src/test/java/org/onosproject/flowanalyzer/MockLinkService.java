package org.onosproject.flowanalyzer;

import org.onosproject.net.PortNumber;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Annotations;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyVertex;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import static org.onosproject.net.Link.State.ACTIVE;


/**
 * Created by nikcheerla on 7/21/15.
 */
public class MockLinkService extends LinkServiceAdapter {
    DefaultMutableTopologyGraph createdGraph = new DefaultMutableTopologyGraph(new HashSet<>(), new HashSet<>());
    List<Link> links = new ArrayList<>();

    @Override
    public int getLinkCount() {
        return links.size();
    }

    @Override
    public Iterable<Link> getLinks() {
        return links;
    }

    @Override
    public Set<Link> getDeviceLinks(DeviceId deviceId) {
        Set<Link> egress = getDeviceEgressLinks(deviceId);
        egress.addAll(getDeviceIngressLinks(deviceId));
        return egress;
    }

    @Override
    public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
        Set<Link> setL = new HashSet<>();
        for (Link l: links) {
            if (l.src().elementId() instanceof DeviceId && l.src().deviceId().equals(deviceId)) {
                setL.add(l);
            }
        }
        return setL;
    }

    @Override
    public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
        Set<Link> setL = new HashSet<>();
        for (Link l: links) {
            if (l.dst().elementId() instanceof DeviceId && l.dst().deviceId().equals(deviceId)) {
                setL.add(l);
            }
        }
        return setL;
    }


    @Override
    public Set<Link> getEgressLinks(ConnectPoint pt) {
        Set<Link> setL = new HashSet<>();
        for (Link l: links) {
            if (l.src().equals(pt)) {
                setL.add(l);
            }
        }
        return setL;
    }

    @Override
    public Set<Link> getIngressLinks(ConnectPoint pt) {
        Set<Link> setL = new HashSet<>();
        for (Link l: links) {
            if (l.dst().equals(pt)) {
                setL.add(l);
            }
        }
        return setL;
    }

    @Override
    public Set<Link> getLinks(ConnectPoint pt) {
        Set<Link> setL = new HashSet<>();
        for (Link l: links) {
            if (l.src().equals(pt) || l.dst().equals(pt)) {
                setL.add(l);
            }
        }
        return setL;
    }

    public void addLink(String device, long port, String device2, long port2) {
        ElementId d1;
        if (device.charAt(0) == 'H') {
            device = device.substring(1, device.length());
            d1 = HostId.hostId(device);
        } else {
            d1 = DeviceId.deviceId(device);
        }

        ElementId d2;
        if (device2.charAt(0) == 'H') {
            d2 = HostId.hostId(device2.substring(1, device2.length()));
        } else {
            d2 = DeviceId.deviceId(device2);
        }

        ConnectPoint src = new ConnectPoint(d1, PortNumber.portNumber(port));
        ConnectPoint dst = new ConnectPoint(d2, PortNumber.portNumber(port2));
        Link curLink;
        curLink = new Link() {
            @Override
            public ConnectPoint src() {
                return src;
            }

            @Override
            public ConnectPoint dst() {
                return dst;
            }

            @Override
            public boolean isDurable() {
                return true;
            }

            @Override
            public Annotations annotations() {
                return null;
            }

            @Override
            public Type type() {
                return null;
            }

            @Override
            public ProviderId providerId() {
                return null;
            }

            @Override
            public State state() {
                return ACTIVE;
            }
        };
        links.add(curLink);
        if (d1 instanceof DeviceId && d2 instanceof DeviceId) {
            TopologyVertex v1 = () -> (DeviceId) d1, v2 = () -> (DeviceId) d2;
            createdGraph.addVertex(v1);
            createdGraph.addVertex(v2);
            createdGraph.addEdge(new TopologyEdge() {
                @Override
                public Link link() {
                    return curLink;
                }

                @Override
                public TopologyVertex src() {
                    return v1;
                }

                @Override
                public TopologyVertex dst() {
                    return v2;
                }
            });
        }
    }


}
