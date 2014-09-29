package org.onlab.onos.store.cluster.impl;

import org.onlab.nio.AbstractMessage;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Base message for cluster-wide communications using TLVs.
 */
public class TLVMessage extends AbstractMessage {

    private final int type;
    private final byte[] data;

    /**
     * Creates an immutable TLV message.
     *
     * @param type   message type
     * @param data   message data bytes
     */
    public TLVMessage(int type, byte[] data) {
        this.length = data.length + TLVMessageStream.METADATA_LENGTH;
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
     * Returns the data bytes.
     *
     * @return message data
     */
    public byte[] data() {
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
