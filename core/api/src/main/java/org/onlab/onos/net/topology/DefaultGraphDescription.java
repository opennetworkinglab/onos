/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.onos.net.topology;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onlab.onos.net.AbstractDescription;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.SparseAnnotations;

import java.util.Map;

/**
 * Default implementation of an immutable topology graph data carrier.
 */
public class DefaultGraphDescription extends AbstractDescription
        implements GraphDescription {

    private final long nanos;
    private final ImmutableSet<TopologyVertex> vertexes;
    private final ImmutableSet<TopologyEdge> edges;

    private final Map<DeviceId, TopologyVertex> vertexesById = Maps.newHashMap();

    /**
     * Creates a minimal topology graph description to allow core to construct
     * and process the topology graph.
     *
     * @param nanos       time in nanos of when the topology description was created
     * @param devices     collection of infrastructure devices
     * @param links       collection of infrastructure links
     * @param annotations optional key/value annotations map
     */
    public DefaultGraphDescription(long nanos, Iterable<Device> devices,
                                   Iterable<Link> links,
                                   SparseAnnotations... annotations) {
        super(annotations);
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
            throw new IllegalArgumentException("Vertex missing for " + id);
        }
        return vertex;
    }

}
