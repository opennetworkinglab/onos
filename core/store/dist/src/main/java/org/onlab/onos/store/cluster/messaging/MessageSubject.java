package org.onlab.onos.store.cluster.messaging;

/**
 * Representation of a message subject.
 * Cluster messages have associated subjects that dictate how they get handled
 * on the receiving side.
 */
public class MessageSubject {

    private final String value;

    public MessageSubject(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
