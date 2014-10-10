package org.onlab.onos.store.link.impl;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by GossipLinkStore peer-peer communication.
 */
public final class GossipLinkStoreMessageSubjects {

    private GossipLinkStoreMessageSubjects() {}

    public static final MessageSubject LINK_UPDATE = new MessageSubject("peer-link-update");
    public static final MessageSubject LINK_REMOVED = new MessageSubject("peer-link-removed");
}
