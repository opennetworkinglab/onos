package org.onlab.onos.store.cluster.messaging.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

public final class ClusterMessageSubjects {
    private ClusterMessageSubjects() {}
    public static final MessageSubject CLUSTER_MEMBERSHIP_EVENT = new MessageSubject("CLUSTER_MEMBERSHIP_EVENT");
}
