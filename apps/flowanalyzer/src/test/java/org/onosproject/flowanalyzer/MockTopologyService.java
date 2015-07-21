package org.onosproject.flowanalyzer;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyServiceAdapter;


/**
 * Created by nikcheerla on 7/20/15.
 */
public class MockTopologyService extends TopologyServiceAdapter {
    TopologyGraph cur;

    public MockTopologyService(TopologyGraph g) {
        cur = g;
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        return cur;
    }
}
