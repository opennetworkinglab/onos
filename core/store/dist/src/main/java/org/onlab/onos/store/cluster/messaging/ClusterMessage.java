package org.onlab.onos.store.cluster.messaging;

import org.onlab.nio.AbstractMessage;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Base message for cluster-wide communications.
 */
public abstract class ClusterMessage extends AbstractMessage {

    private final MessageSubject subject;

    /**
     * Creates a cluster message.
     *
     * @param subject message subject
     */
    protected ClusterMessage(MessageSubject subject) {
        this.subject = subject;
    }

    /**
     * Returns the message subject indicator.
     *
     * @return message subject
     */
    public MessageSubject subject() {
        return subject;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("subject", subject).add("length", length).toString();
    }

}
