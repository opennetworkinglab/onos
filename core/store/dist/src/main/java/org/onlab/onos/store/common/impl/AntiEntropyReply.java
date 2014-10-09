package org.onlab.onos.store.common.impl;

import java.util.Map;
import java.util.Set;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.device.VersionedValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Anti-Entropy reply message.
 * <p>
 * Message to send in reply to advertisement or another reply.
 * Suggest to the sender about the more up-to-date data this node has,
 * and request for more recent data that the receiver has.
 */
public class AntiEntropyReply<ID, V extends VersionedValue<?>> {

    private final NodeId sender;
    private final ImmutableMap<ID, V> suggestion;
    private final ImmutableSet<ID> request;

    /**
     * Creates a reply to anti-entropy message.
     *
     * @param sender sender of this message
     * @param suggestion collection of more recent values, sender had
     * @param request Collection of identifiers
     */
    public AntiEntropyReply(NodeId sender,
                            Map<ID, V> suggestion,
                            Set<ID> request) {
        this.sender = sender;
        this.suggestion = ImmutableMap.copyOf(suggestion);
        this.request = ImmutableSet.copyOf(request);
    }

    public NodeId sender() {
        return sender;
    }

    /**
     * Returns collection of values, which the recipient of this reply is likely
     * to be missing or has outdated version.
     *
     * @return
     */
    public ImmutableMap<ID, V> suggestion() {
        return suggestion;
    }

    /**
     * Returns collection of identifier to request.
     *
     * @return collection of identifier to request
     */
    public ImmutableSet<ID> request() {
        return request;
    }

    /**
     * Checks if reply contains any suggestion or request.
     *
     * @return true if nothing is suggested and requested
     */
    public boolean isEmpty() {
        return suggestion.isEmpty() && request.isEmpty();
    }

    // Default constructor for serializer
    protected AntiEntropyReply() {
        this.sender = null;
        this.suggestion = null;
        this.request = null;
    }
}
