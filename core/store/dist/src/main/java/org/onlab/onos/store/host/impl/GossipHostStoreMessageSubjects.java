package org.onlab.onos.store.host.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

public final class GossipHostStoreMessageSubjects {
    private GossipHostStoreMessageSubjects() {}
    public static final MessageSubject HOST_UPDATED = new MessageSubject("peer-host-updated");
    public static final MessageSubject HOST_REMOVED = new MessageSubject("peer-host-removed");
}
