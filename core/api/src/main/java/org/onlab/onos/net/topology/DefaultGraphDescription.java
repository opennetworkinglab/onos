package org.onlab.onos.net.topology;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;

import java.util.Map;

/**
 * Default implementation of an immutable topology graph data carrier.
 */
public class DefaultGraphDescription implements GraphDescription {

    private final long nanos;
    private final ImmutableSet<TopologyVertex> vertexes;
    private final ImmutableSet<TopologyEdge> edges;

    private final Map<DeviceId, TopologyVertex> vertexesById = Maps.newHashMap();


    /**
     * Creates a minimal topology graph description to allow core to construct
     * and process the topology graph.
     *
     * @param nanos   time in nanos of when the topology description was created
     * @param devices collection of infrastructure devices
     * @param links   collection of infrastructure links
     */
    public DefaultGraphDescription(long nanos, Iterable<Device> devices, Iterable<Link> links) {
        this.nanos = nanos;
        this.vertexes = buildVertexes(devices);
        this.edges = buildEdges(links);
        vertexesById.clear();
    }

    @Override
    public long timestamp() {
        return nanos;
    }

    @Override
    public ImmutableSet<TopologyVertex> vertexes() {
        return vertexes;
    }

    @Override
    public ImmutableSet<TopologyEdge> edges() {
        return edges;
    }

    // Builds a set of topology vertexes from the specified list of devices
    private ImmutableSet<TopologyVertex> buildVertexes(Iterable<Device> devices) {
        ImmutableSet.Builder<TopologyVertex> vertexes = ImmutableSet.builder();
        for (Device device : devices) {
            TopologyVertex vertex = new DefaultTopologyVertex(device.id());
            vertexes.add(vertex);
            vertexesById.put(vertex.deviceId(), vertex);
        }
        return vertexes.build();
    }

    // Builds a set of topology vertexes from the specified list of links
    private ImmutableSet<TopologyEdge> buildEdges(Iterable<Link> links) {
        ImmutableSet.Builder<TopologyEdge> edges = ImmutableSet.builder();
        for (Link link : links) {
            edges.add(new DefaultTopologyEdge(vertexOf(link.src()),
                                              vertexOf(link.dst()), link));
        }
        return edges.build();
    }

    // Fetches a vertex corresponding to the given connection point device.
    private TopologyVertex vertexOf(ConnectPoint connectPoint) {
        DeviceId id = connectPoint.deviceId();
        TopologyVertex vertex = vertexesById.get(id);
        if (vertex == null) {
            // If vertex does not exist, create one and register it.
            vertex = new DefaultTopologyVertex(id);
            vertexesById.put(id, vertex);
        }
        return vertex;
    }

}
