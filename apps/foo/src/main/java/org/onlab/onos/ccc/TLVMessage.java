package org.onlab.onos.ccc;

import org.onlab.nio.AbstractMessage;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Base message for cluster-wide communications using TLVs.
 */
public class TLVMessage extends AbstractMessage {

    private final int type;
    private final Object data;

    /**
     * Creates an immutable TLV message.
     *
     * @param type   message type
     * @param length message length
     * @param data   message data
     */
    public TLVMessage(int type, int length, Object data) {
        this.length = length;
        this.type = type;
        this.data = data;
    }

    /**
     * Returns the message type indicator.
     *
     * @return message type
     */
    public int type() {
        return type;
    }

    /**
     * Returns the data object.
     *
     * @return message data
     */
    public Object data() {
        return data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TLVMessage other = (TLVMessage) obj;
        return Objects.equals(this.type, other.type) &&
                Objects.equals(this.data, other.data);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("type", type).add("length", length).toString();
    }

}
