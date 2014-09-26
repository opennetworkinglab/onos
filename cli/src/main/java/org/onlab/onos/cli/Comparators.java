package org.onlab.onos.cli;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.topology.TopologyCluster;

import java.util.Comparator;

/**
 * Various comparators.
 */
public final class Comparators {

    // Ban construction
    private Comparators() {
    }

    public static final Comparator<ElementId> ELEMENT_ID_COMPARATOR = new Comparator<ElementId>() {
        @Override
        public int compare(ElementId id1, ElementId id2) {
            return id1.uri().toString().compareTo(id2.uri().toString());
        }
    };

    public static final Comparator<Element> ELEMENT_COMPARATOR = new Comparator<Element>() {
        @Override
        public int compare(Element e1, Element e2) {
            return e1.id().uri().toString().compareTo(e2.id().uri().toString());
        }
    };

    public static final Comparator<FlowRule> FLOW_RULE_COMPARATOR = new Comparator<FlowRule>() {
        @Override
        public int compare(FlowRule f1, FlowRule f2) {
            return Long.valueOf(f1.id().value()).compareTo(f2.id().value());
        }
    };

    public static final Comparator<Port> PORT_COMPARATOR = new Comparator<Port>() {
        @Override
        public int compare(Port p1, Port p2) {
            long delta = p1.number().toLong() - p2.number().toLong();
            return delta == 0 ? 0 : (delta < 0 ? -1 : +1);
        }
    };

    public static final Comparator<TopologyCluster> CLUSTER_COMPARATOR = new Comparator<TopologyCluster>() {
        @Override
        public int compare(TopologyCluster c1, TopologyCluster c2) {
            return c1.id().index() - c2.id().index();
        }
    };

    public static final Comparator<ControllerNode> NODE_COMPARATOR = new Comparator<ControllerNode>() {
        @Override
        public int compare(ControllerNode ci1, ControllerNode ci2) {
            return ci1.id().toString().compareTo(ci2.id().toString());
        }
    };

}
