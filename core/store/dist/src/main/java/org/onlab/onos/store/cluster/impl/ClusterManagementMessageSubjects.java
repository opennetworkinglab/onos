package org.onlab.onos.store.cluster.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

//Not used right now
public final class ClusterManagementMessageSubjects {
    // avoid instantiation
    private ClusterManagementMessageSubjects() {}

    public static final MessageSubject CLUSTER_MEMBERSHIP_EVENT = new MessageSubject("CLUSTER_MEMBERSHIP_EVENT");
}
