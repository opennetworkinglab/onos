package org.onlab.onos.store.flow.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by DistributedFlowRuleStore peer-peer communication.
 */
public final class FlowStoreMessageSubjects {
    private FlowStoreMessageSubjects() {}
    public static final  MessageSubject STORE_FLOW_RULE = new MessageSubject("peer-forward-store-flow-rule");
    public static final MessageSubject DELETE_FLOW_RULE = new MessageSubject("peer-forward-delete-flow-rule");
    public static final MessageSubject ADD_OR_UPDATE_FLOW_RULE =
        new MessageSubject("peer-forward-add-or-update-flow-rule");
    public static final MessageSubject REMOVE_FLOW_RULE = new MessageSubject("peer-forward-remove-flow-rule");
}
