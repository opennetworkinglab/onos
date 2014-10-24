package org.onlab.onos.store.flow.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by DistributedFlowRuleStore peer-peer communication.
 */
public final class FlowStoreMessageSubjects {
    private FlowStoreMessageSubjects() {}

    public static final  MessageSubject APPLY_BATCH_FLOWS
        = new MessageSubject("peer-forward-apply-batch");

    public static final MessageSubject GET_FLOW_ENTRY
        = new MessageSubject("peer-forward-get-flow-entry");

    public static final MessageSubject GET_DEVICE_FLOW_ENTRIES
        = new MessageSubject("peer-forward-get-device-flow-entries");
}
