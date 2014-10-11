package org.onlab.onos.cluster;

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

    /**
     * Returns a Function to convert ControllerNode to NodeId.
     *
     * @return ControllerNodeToNodeId instance.
     */
    public static ControllerNodeToNodeId toNodeId() {
        return INSTANCE;
    }
}
