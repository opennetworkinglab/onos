package org.onlab.onos.net.topology;

import org.onlab.graph.Vertex;
import org.onlab.onos.net.DeviceId;

/**
 * Represents a vertex in the topology graph.
 */
public interface TopoVertex extends Vertex {

    /**
     * Returns the associated infrastructure device identification.
     *
     * @return device identifier
     */
    DeviceId deviceId();

}
