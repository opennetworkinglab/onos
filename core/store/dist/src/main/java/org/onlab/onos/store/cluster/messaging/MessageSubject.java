package org.onlab.onos.store.cluster.messaging;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Representation of a message subject.
 * Cluster messages have associated subjects that dictate how they get handled
 * on the receiving side.
 */
public class MessageSubject {

    private final String value;

    public MessageSubject(String value) {
        this.value = checkNotNull(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MessageSubject that = (MessageSubject) obj;
        return Objects.equals(this.value, that.value);
    }
}
