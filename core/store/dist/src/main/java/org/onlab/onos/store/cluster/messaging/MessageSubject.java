package org.onlab.onos.store.cluster.messaging;

/**
 * Representation of a message subject.
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
