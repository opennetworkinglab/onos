package org.onlab.onos.store.common.impl;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.NodeId;

import com.google.common.base.Function;

/**
 * Function to convert ControllerNode to NodeId.
 */
public final class ControllerNodeToNodeId
    implements Function<ControllerNode, NodeId> {

    private static final ControllerNodeToNodeId INSTANCE = new ControllerNodeToNodeId();

    @Override
    public NodeId apply(ControllerNode input) {
        return input.id();
    }

    public static ControllerNodeToNodeId toNodeId() {
        return INSTANCE;
    }
}
