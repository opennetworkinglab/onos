package org.onlab.onos.store.cluster.messaging;

import static org.onlab.onos.store.cluster.messaging.MessageSubject.AE_ADVERTISEMENT;
import java.util.Map;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.Timestamp;

import com.google.common.collect.ImmutableMap;

/**
 * Anti-Entropy advertisement message.
 * <p>
 * Message to advertise the information this node holds.
 *
 * @param <ID> ID type
 */
public class AntiEntropyAdvertisement<ID> extends ClusterMessage {

    private final NodeId sender;
    private final ImmutableMap<ID, Timestamp> advertisement;

    /**
     * Creates anti-entropy advertisement message.
     *
     * @param sender sender of this message
     * @param advertisement timestamp information of the data sender holds
     */
    public AntiEntropyAdvertisement(NodeId sender, Map<ID, Timestamp> advertisement) {
        super(AE_ADVERTISEMENT);
        this.sender = sender;
        this.advertisement = ImmutableMap.copyOf(advertisement);
    }

    public NodeId sender() {
        return sender;
    }

    public ImmutableMap<ID, Timestamp> advertisement() {
        return advertisement;
    }

    // Default constructor for serializer
    protected AntiEntropyAdvertisement() {
        super(AE_ADVERTISEMENT);
        this.sender = null;
        this.advertisement = null;
    }
}
