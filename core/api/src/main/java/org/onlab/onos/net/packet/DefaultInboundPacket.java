package org.onlab.onos.net.packet;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.packet.Ethernet;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of an immutable inbound packet.
 */
public class DefaultInboundPacket implements InboundPacket {

    private final ConnectPoint receivedFrom;
    private final Ethernet parsed;
    private final ByteBuffer unparsed;

    /**
     * Creates an immutable inbound packet.
     *
     * @param receivedFrom connection point where received
     * @param parsed       parsed ethernet frame
     * @param unparsed     unparsed raw bytes
     */
    public  DefaultInboundPacket(ConnectPoint receivedFrom, Ethernet parsed,
                                ByteBuffer unparsed) {
        this.receivedFrom = receivedFrom;
        this.parsed = parsed;
        this.unparsed = unparsed;
    }

    @Override
    public ConnectPoint receivedFrom() {
        return receivedFrom;
    }

    @Override
    public Ethernet parsed() {
        return parsed;
    }

    @Override
    public ByteBuffer unparsed() {
        // FIXME: figure out immutability here
        return unparsed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(receivedFrom, parsed, unparsed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InboundPacket) {
            final DefaultInboundPacket other = (DefaultInboundPacket) obj;
            return Objects.equals(this.receivedFrom, other.receivedFrom) &&
                    Objects.equals(this.parsed, other.parsed) &&
                    Objects.equals(this.unparsed, other.unparsed);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("receivedFrom", receivedFrom)
                .add("parsed", parsed)
                .toString();
    }
}
