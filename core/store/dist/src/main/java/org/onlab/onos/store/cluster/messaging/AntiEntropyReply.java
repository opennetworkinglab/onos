package org.onlab.onos.store.cluster.messaging;

import static org.onlab.onos.store.cluster.messaging.MessageSubject.AE_REPLY;

import java.util.Map;
import java.util.Set;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.device.impl.VersionedValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class AntiEntropyReply<ID, VALUE> extends ClusterMessage {

    private final NodeId sender;
    private final ImmutableMap<ID, VersionedValue<VALUE>> suggestion;
    private final ImmutableSet<ID> request;

    /**
     * Creates a reply to anti-entropy message.
     *
     * @param sender sender of this message
     * @param suggestion collection of more recent values, sender had
     * @param request Collection of identifiers
     */
    public AntiEntropyReply(NodeId sender,
                            Map<ID, VersionedValue<VALUE>> suggestion,
                            Set<ID> request) {
        super(AE_REPLY);
        this.sender = sender;
        this.suggestion = ImmutableMap.copyOf(suggestion);
        this.request = ImmutableSet.copyOf(request);
    }

    public NodeId sender() {
        return sender;
    }

    public ImmutableMap<ID, VersionedValue<VALUE>> suggestion() {
        return suggestion;
    }

    public ImmutableSet<ID> request() {
        return request;
    }

    // Default constructor for serializer
    protected AntiEntropyReply() {
        super(AE_REPLY);
        this.sender = null;
        this.suggestion = null;
        this.request = null;
    }
}
