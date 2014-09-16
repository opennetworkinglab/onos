package org.onlab.onos.net.topology;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;

import java.util.Set;

/**
 * Test adapter for topology service.
 */
public class TopologyServiceAdapter implements TopologyService {
    @Override
    public Topology currentTopology() {
        return null;
    }

    @Override
    public boolean isLatest(Topology topology) {
        return false;
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        return null;
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        return null;
    }

    @Override
    public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
        return null;
    }

    @Override
    public Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster) {
        return null;
    }

    @Override
    public Set<Link> getClusterLinks(Topology topology, TopologyCluster cluster) {
        return null;
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        return null;
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
        return null;
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        return false;
    }

    @Override
    public boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint) {
        return false;
    }

    @Override
    public void addListener(TopologyListener listener) {
    }

    @Override
    public void removeListener(TopologyListener listener) {
    }

}
